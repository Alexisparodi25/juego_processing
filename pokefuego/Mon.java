// =====================================================
//  Mon: un Pokémon concreto (con nivel, naturaleza, movimientos...)
// =====================================================
import java.util.*;

class Mon {
  static final String[] NATURES = {"Fuerte","Huraña","Audaz","Firme","Pícara","Osada","Dócil","Plácida","Agitada","Floja",
    "Miedosa","Activa","Seria","Alegre","Ingenua","Modesta","Afable","Mansa","Tímida","Alocada","Serena","Amable","Grosera","Cauta","Rara"};
  static final int[] NAT_STAT = {1, 2, 5, 3, 4};

  // ---- identidad ----
  Species sp;          // forma actual (cambia con mega / forma por objeto)
  Species normalSp;    // forma fuera de combate
  String nick = "";
  int level = 5, exp, nature;
  int[] iv = new int[6], ev = new int[6];
  String ability = "";
  String item = "";
  String ball = "pokeball";
  String origin = "";   // lugar donde se obtuvo
  boolean shiny;
  int friendship = 70;

  // ---- estado ----
  int hp, maxHp;
  int[] stat = new int[6];
  String status = "";         // brn par psn tox slp frz
  int sleepTurns, toxicTurns;
  String[] moves = new String[0];
  int[] pp = new int[0];
  int[] maxPp = new int[0];
  boolean dead;               // muerto para siempre (Nuzlocke)
  boolean participated;

  // ---- estado de combate (se reinicia al salir / al terminar) ----
  int[] stage = new int[8];   // 1-5 stats, 6 precisión, 7 evasión
  Set<String> vol = new HashSet<String>();
  String type1, type2;
  boolean megaed, dynamaxed, usedItem, itemConsumed;
  int dynaHpBonus;
  String lockedMove = "";     // movimiento bloqueado (Choice / Outrage)
  int tauntTurns, lockTurns, confusionTurns, protectCount, turnsOut, recharge, toxicCounter;
  String chargingMove = "";   // movimiento a dos turnos
  String lastMove = "";
  String abilityOverride = null, origAbility = null;
  boolean disguiseBroken, flashFireOn, truantSkip;
  int lastDamageTaken; String lastDamageCat = ""; boolean tookDamageThisTurn;
  Move dynaMoves[] = null;

  Mon(Species s, int level) {
    sp = s; normalSp = s; this.level = level;
    type1 = s.t1; type2 = s.t2;
  }

  // ---------- creación ----------
  static Mon create(Species s, int level, Random r) {
    Mon m = new Mon(s, level);
    m.nature = r.nextInt(25);
    for (int i = 0; i < 6; i++) m.iv[i] = 10 + r.nextInt(22);
    m.pickAbility(r, false);
    m.exp = expFor(level);
    m.shiny = r.nextInt(512) == 0;
    m.recalc(true);
    m.learnDefaultMoves(r);
    m.checkItemForme();
    return m;
  }

  void pickAbility(Random r, boolean allowHidden) {
    String[] a = normalSp.abilities;
    List<String> pool = new ArrayList<String>();
    if (!a[0].isEmpty()) pool.add(a[0]);
    if (!a[1].isEmpty()) pool.add(a[1]);
    String pick = pool.isEmpty() ? (a[2].isEmpty() ? "" : a[2]) : pool.get(r.nextInt(pool.size()));
    if (allowHidden && !a[2].isEmpty() && r.nextInt(4) == 0) pick = a[2];
    ability = pick;
  }

  void learnDefaultMoves(Random r) {
    List<String> mv = MoveGen.levelUpSet(normalSp, level);
    setMoves(mv);
  }

  void setMoves(List<String> mv) {
    int n = Math.min(4, mv.size());
    moves = new String[n]; pp = new int[n]; maxPp = new int[n];
    for (int i = 0; i < n; i++) {
      moves[i] = mv.get(i);
      Move d = Dex.mv(moves[i]);
      maxPp[i] = pp[i] = d == null ? 10 : d.pp;
    }
  }

  static int expFor(int level) { return level * level * level; }

  // ---------- estadísticas ----------
  void recalc(boolean fullHeal) {
    int oldMax = maxHp;
    double ratio = oldMax > 0 ? hp / (double) oldMax : 1;
    int[] b = sp.base;
    if (sp.base[0] == 1) maxHp = 1;
    else maxHp = (2 * b[0] + iv[0] + ev[0] / 4) * level / 100 + level + 10;
    if (dynamaxed) maxHp = (int) (maxHp * 1.5);
    for (int i = 1; i < 6; i++) {
      int v = (2 * b[i] + iv[i] + ev[i] / 4) * level / 100 + 5;
      stat[i] = (int) (v * natureMult(i));
    }
    if (fullHeal) hp = maxHp;
    else if (oldMax > 0) hp = Math.max(hp > 0 ? 1 : 0, (int) Math.round(ratio * maxHp));
  }

  double natureMult(int stat) {
    int plus = NAT_STAT[nature / 5], minus = NAT_STAT[nature % 5];
    if (plus == minus) return 1;
    if (stat == plus) return 1.1;
    if (stat == minus) return 0.9;
    return 1;
  }

  String natureName() { return NATURES[nature]; }

  // ---------- consultas ----------
  String name() { return nick.isEmpty() ? normalSp.name : nick; }
  boolean fainted() { return hp <= 0; }
  boolean alive() { return hp > 0 && !dead; }
  String abilityId() { return Dex.toId(abilityOverride != null ? abilityOverride : ability); }
  boolean hasAbility(String id) { return abilityId().equals(id); }
  String itemId() { return itemConsumed ? "" : Dex.toId(item); }
  boolean hasItem(String id) { return itemId().equals(id); }
  boolean isType(String t) { return t.equals(type1) || t.equals(type2); }
  boolean grounded() {
    if (isType("Flying") || hasAbility("levitate") || hasItem("airballoon")) return false;
    return true;
  }
  int moveIndex(String id) { for (int i = 0; i < moves.length; i++) if (moves[i].equals(id)) return i; return -1; }
  boolean hasUsableMove() { for (int p : pp) if (p > 0) return true; return false; }
  double hpFrac() { return maxHp == 0 ? 0 : hp / (double) maxHp; }

  // ---------- cambios de forma ----------
  void setSpecies(Species s) {
    sp = s; type1 = s.t1; type2 = s.t2;
    recalc(false);
  }

  void checkItemForme() {
    if (megaed || dynamaxed) return;
    Species f = Dex.itemForme.get(normalSp.id + "|" + Dex.toId(item));
    if (f != null) { normalSp = f; setSpecies(f); }
    else if (normalSp.reqItems.length > 0) {   // ya es una forma por objeto pero ya no lo lleva
      boolean has = false;
      for (String ri : normalSp.reqItems) if (Dex.toId(ri).equals(Dex.toId(item))) has = true;
      if (!has) {
        Species base = Dex.byName.get(Dex.toId(normalSp.baseSpecies));
        if (base != null && !base.id.equals(normalSp.id)) { normalSp = base; setSpecies(base); }
      }
    }
  }

  /** Megaevolución disponible con el objeto equipado. */
  Species megaForm() {
    if (megaed || dynamaxed || item.isEmpty()) return null;
    List<Species> l = Dex.megasOf.get(normalSp.id);
    if (l == null) return null;
    String it = Dex.toId(item);
    for (Species s : l) {
      for (String ri : s.reqItems) if (Dex.toId(ri).equals(it)) return s;
      if (!s.reqMove.isEmpty() && moveIndex(Dex.toId(s.reqMove)) >= 0 && s.reqItems.length == 0) return s;
    }
    return null;
  }

  boolean canDynamax() {
    return !dynamaxed && !megaed && !normalSp.noDmax && !sp.noDmax;
  }

  Species gmaxForm() { return Dex.gmaxOf.get(normalSp.id); }

  // ---------- reinicio ----------
  void resetVolatile() {
    Arrays.fill(stage, 0);
    vol.clear();
    type1 = sp.t1; type2 = sp.t2;
    tauntTurns = 0; lockedMove = ""; lockTurns = 0; confusionTurns = 0; protectCount = 0; turnsOut = 0; recharge = 0;
    chargingMove = ""; abilityOverride = null; toxicCounter = 0; flashFireOn = false; truantSkip = false;
    if (status.equals("tox")) toxicTurns = 0;
  }

  /** Al terminar el combate: vuelve a su forma normal (megas, dynamax, etc.). */
  void endBattle() {
    boolean wasDyna = dynamaxed;
    double ratio = maxHp > 0 ? hp / (double) maxHp : 0;
    dynamaxed = false; megaed = false;
    sp = normalSp;
    resetVolatile();
    if (origAbility != null) { ability = origAbility; origAbility = null; }
    if (itemConsumed) { if (Items.isConsumable(item)) item = ""; itemConsumed = false; }
    disguiseBroken = false;
    recalc(false);
    if (wasDyna && hp > 0) hp = Math.max(1, Math.min(maxHp, (int) Math.round(ratio * maxHp)));
    participated = false;
  }

  void fullHeal() {
    if (dead) return;
    hp = maxHp; status = ""; sleepTurns = 0;
    for (int i = 0; i < pp.length; i++) pp[i] = maxPp[i];
  }

  // ---------- experiencia ----------
  /** Devuelve los niveles subidos. */
  int gainExp(int amount, int levelCap) {
    int gained = 0;
    exp += amount;
    while (level < 100 && level < levelCap && exp >= expFor(level + 1)) {
      level++; gained++;
    }
    if (level >= levelCap && exp > expFor(level)) exp = Math.min(exp, expFor(Math.min(100, level + 1)) - 1);
    if (gained > 0) { int oldMax = maxHp; recalc(false); hp = Math.min(maxHp, hp + (maxHp - oldMax)); }
    return gained;
  }

  Evo checkEvolution(boolean inBattleEnd) {
    for (Evo e : normalSp.evos) {
      Species to = Dex.sp(e.to);
      if (to == null || e.type.equals("useItem")) continue;
      int need = evoLevel(e);
      if (need > 0 && level >= need) return e;
    }
    return null;
  }

  static int evoLevel(Evo e) {
    if (e.type.equals("useItem")) return 0;
    if (e.level > 0) return e.level;
    if (e.type.equals("trade")) return 36;
    if (e.type.equals("levelFriendship")) return 26;
    return 32;
  }

  Evo itemEvolution(String itemId) {
    for (Evo e : normalSp.evos)
      if (e.type.equals("useItem") && Dex.toId(e.item).equals(Dex.toId(itemId))) return e;
    return null;
  }

  void evolveTo(Species to) {
    boolean keepName = !nick.isEmpty() && !nick.equals(normalSp.name);
    normalSp = to; sp = to; type1 = to.t1; type2 = to.t2;
    if (!keepName) nick = "";
    String[] a = to.abilities;
    boolean slot0 = Dex.toId(ability).equals(Dex.toId(a[0]));
    boolean slot1 = Dex.toId(ability).equals(Dex.toId(a[1]));
    if (!slot0 && !slot1 && !Dex.toId(ability).equals(Dex.toId(a[2]))) ability = a[0].isEmpty() ? ability : a[0];
    recalc(false);
    checkItemForme();
  }

  /** Movimientos nuevos que aprende al subir de nivel (exactamente en ese nivel). */
  List<String> newMovesAt(int lv) {
    List<String> out = new ArrayList<String>();
    for (int i = 0; i < normalSp.learnMove.length; i++)
      if (normalSp.learnLvl[i] == lv && moveIndex(normalSp.learnMove[i]) < 0 && !out.contains(normalSp.learnMove[i])) {
        Move m = Dex.mv(normalSp.learnMove[i]);
        if (m != null) out.add(normalSp.learnMove[i]);
      }
    return out;
  }

  void learnMove(String id, int replaceSlot) {
    Move d = Dex.mv(id);
    if (d == null) return;
    if (moves.length < 4) {
      moves = Arrays.copyOf(moves, moves.length + 1); pp = Arrays.copyOf(pp, pp.length + 1); maxPp = Arrays.copyOf(maxPp, maxPp.length + 1);
      replaceSlot = moves.length - 1;
    }
    moves[replaceSlot] = id; maxPp[replaceSlot] = pp[replaceSlot] = d.pp;
  }
}

class MoveGen {
  /** Los últimos 4 movimientos por nivel (comportamiento clásico). */
  static List<String> levelUpSet(Species s, int level) {
    List<String> out = new ArrayList<String>();
    for (int i = 0; i < s.learnMove.length; i++) {
      if (s.learnLvl[i] > level) continue;
      Move m = Dex.mv(s.learnMove[i]);
      if (m == null || !m.supported) continue;
      out.remove(s.learnMove[i]);
      out.add(s.learnMove[i]);
    }
    while (out.size() > 4) out.remove(0);
    if (out.isEmpty()) out.add("tackle");
    return out;
  }

  static double score(Species s, Move m, Set<String> have) {
    if (!m.supported) return -1;
    double sc;
    boolean stab = m.type.equals(s.t1) || m.type.equals(s.t2);
    if (m.cat != 'N') {
      int atkStat = m.cat == 'P' ? s.base[1] : s.base[3];
      int otherStat = m.cat == 'P' ? s.base[3] : s.base[1];
      double bp = Math.max(m.bp, m.damage.isEmpty() ? 0 : 40);
      if (m.multiMax > 1) bp *= (m.multiMin + m.multiMax) / 2.0 * 0.9;
      sc = bp * (stab ? 1.5 : 1.0) * (m.acc == 0 ? 1.0 : m.acc / 100.0);
      if (atkStat < otherStat * 0.8) sc *= 0.35;
      if (m.has("charge") || m.has("recharge")) sc *= 0.55;
      if (m.selfdestruct) sc *= 0.35;
      if (m.drain != null) sc *= 1.15;
      if (m.secChance > 0) sc *= 1.08;
      if (m.name.equals("Focus Punch") || m.name.equals("Last Resort") || m.name.equals("Dream Eater") || m.name.equals("Sucker Punch") || m.id.equals("fakeout") || m.id.equals("belch") || m.id.equals("steelroller")) sc *= 0.1;
    } else {
      sc = 0;
      if (m.heal != null && m.id.matches("recover|roost|softboiled|slackoff|moonlight|morningsun|synthesis|milkdrink|shoreup|healorder")) sc = 90;
      else if (m.selfBoost.contains("atk:2") || m.selfBoost.contains("spa:2") || m.selfBoost.contains("spe:2") || m.id.equals("bulkup") || m.id.equals("calmmind") || m.id.equals("nastyplot") || m.id.equals("dragondance") || m.id.equals("quiverdance")) sc = 70;
      else if (m.id.equals("toxic") || m.id.equals("willowisp") || m.id.equals("thunderwave") || m.id.equals("spore") || m.id.equals("sleeppowder") || m.id.equals("hypnosis") || m.id.equals("stunspore")) sc = 60;
      else if (m.id.equals("stealthrock") || m.id.equals("spikes") || m.id.equals("toxicspikes")) sc = 55;
      else if (m.id.equals("reflect") || m.id.equals("lightscreen") || m.id.equals("protect") || m.id.equals("substitute")) sc = 35;
      else if (!m.selfBoost.isEmpty() || !m.tgtBoost.isEmpty()) sc = 25;
      else sc = 5;
    }
    if (have.contains(m.type) && m.cat != 'N') sc *= 0.75;       // variedad de tipos
    return sc;
  }

  /** Un set "competitivo": mejores ataques STAB + cobertura + utilidad. Para entrenadores difíciles y modo aleatorio. */
  static List<String> smartSet(Species s, int level, Random r) {
    List<String> pool = Dex.allLearnable(s);
    // sólo lo que puede conocer a su nivel: nivel-up <= level, y movimientos de máquina a partir de nivel 20
    List<String> okMoves = new ArrayList<String>();
    Set<String> lv = new HashSet<String>(Dex.learnableAt(s, level));
    for (String id : pool) {
      if (lv.contains(id)) okMoves.add(id);
      else if (level >= 22 && !s.otherMoves.equals(null)) {
        for (String o : s.otherMoves) if (o.equals(id)) { okMoves.add(id); break; }
      }
    }
    if (okMoves.isEmpty()) return levelUpSet(s, level);
    List<String> out = new ArrayList<String>();
    Set<String> types = new HashSet<String>();
    for (int slot = 0; slot < 4 && !okMoves.isEmpty(); slot++) {
      String bestId = null; double best = -1;
      for (String id : okMoves) {
        Move m = Dex.mv(id);
        if (m == null) continue;
        double sc = score(s, m, types) * (0.9 + r.nextDouble() * 0.2);
        if (slot >= 2 && m.cat == 'N' && sc >= 50) sc *= 1.8;
        if (sc > best) { best = sc; bestId = id; }
      }
      if (bestId == null) break;
      out.add(bestId); okMoves.remove(bestId);
      Move bm = Dex.mv(bestId);
      if (bm.cat != 'N') types.add(bm.type);
    }
    if (out.isEmpty()) return levelUpSet(s, level);
    return out;
  }
}
