package pokefuego.engine;
import java.util.*;

public class Mon {
  public static final String[] NATURES = {"Fuerte","Huraña","Audaz","Firme","Pícara","Osada","Dócil","Plácida","Agitada","Floja",
    "Miedosa","Activa","Seria","Alegre","Ingenua","Modesta","Afable","Mansa","Tímida","Alocada","Serena","Amable","Grosera","Cauta","Rara"};
  public static final int[] NAT_STAT = {1, 2, 5, 3, 4};

  // ---- identidad ----
  public Species sp;          // forma actual (cambia con mega / forma por objeto)
  public Species normalSp;    // forma fuera de combate
  public String nick = "";
  public int level = 5, exp, nature;
  public int[] iv = new int[6], ev = new int[6];
  public String ability = "";
  public String item = "";
  public String ball = "pokeball";
  public String origin = "";   // lugar donde se obtuvo
  public boolean shiny;
  public int friendship = 70;

  // ---- estado ----
  public int hp, maxHp;
  public int[] stat = new int[6];
  public String status = "";         // brn par psn tox slp frz
  public int sleepTurns, toxicTurns;
  public String[] moves = new String[0];
  public int[] pp = new int[0];
  public int[] maxPp = new int[0];
  public boolean dead;               // muerto para siempre (Nuzlocke)
  public boolean participated;

  // ---- estado de combate (se reinicia al salir / al terminar) ----
  public int[] stage = new int[8];   // 1-5 stats, 6 precisión, 7 evasión
  public Set<String> vol = new HashSet<String>();
  public String type1, type2;
  public boolean megaed, dynamaxed, usedItem, itemConsumed;
  public int dynaHpBonus;
  public String lockedMove = "";     // movimiento bloqueado (Choice / Outrage)
  public int tauntTurns, lockTurns, confusionTurns, protectCount, turnsOut, recharge, toxicCounter;
  public String chargingMove = "";   // movimiento a dos turnos
  public String lastMove = "";
  public String abilityOverride = null, origAbility = null;
  public boolean disguiseBroken, flashFireOn, truantSkip;
  public int lastDamageTaken; public String lastDamageCat = ""; public boolean tookDamageThisTurn;
  Move dynaMoves[] = null;

  public Mon(Species s, int level) {
    sp = s; normalSp = s; this.level = level;
    type1 = s.t1; type2 = s.t2;
  }

  // ---------- creación ----------
  public static Mon create(Species s, int level, Random r) {
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

  public void pickAbility(Random r, boolean allowHidden) {
    String[] a = normalSp.abilities;
    List<String> pool = new ArrayList<String>();
    if (!a[0].isEmpty()) pool.add(a[0]);
    if (!a[1].isEmpty()) pool.add(a[1]);
    String pick = pool.isEmpty() ? (a[2].isEmpty() ? "" : a[2]) : pool.get(r.nextInt(pool.size()));
    if (allowHidden && !a[2].isEmpty() && r.nextInt(4) == 0) pick = a[2];
    ability = pick;
  }

  public void learnDefaultMoves(Random r) {
    List<String> mv = MoveGen.levelUpSet(normalSp, level);
    setMoves(mv);
  }

  public void setMoves(List<String> mv) {
    int n = Math.min(4, mv.size());
    moves = new String[n]; pp = new int[n]; maxPp = new int[n];
    for (int i = 0; i < n; i++) {
      moves[i] = mv.get(i);
      Move d = Dex.mv(moves[i]);
      maxPp[i] = pp[i] = d == null ? 10 : d.pp;
    }
  }

  public static int expFor(int level) { return level * level * level; }

  // ---------- estadísticas ----------
  public void recalc(boolean fullHeal) {
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

  public double natureMult(int stat) {
    int plus = NAT_STAT[nature / 5], minus = NAT_STAT[nature % 5];
    if (plus == minus) return 1;
    if (stat == plus) return 1.1;
    if (stat == minus) return 0.9;
    return 1;
  }

  public String natureName() { return NATURES[nature]; }

  // ---------- consultas ----------
  public String name() { return nick.isEmpty() ? normalSp.name : nick; }
  public boolean fainted() { return hp <= 0; }
  public boolean alive() { return hp > 0 && !dead; }
  public String abilityId() { return Dex.toId(abilityOverride != null ? abilityOverride : ability); }
  public boolean hasAbility(String id) { return abilityId().equals(id); }
  public String itemId() { return itemConsumed ? "" : Dex.toId(item); }
  public boolean hasItem(String id) { return itemId().equals(id); }
  public boolean isType(String t) { return t.equals(type1) || t.equals(type2); }
  public boolean grounded() {
    if (isType("Flying") || hasAbility("levitate") || hasItem("airballoon")) return false;
    return true;
  }
  public int moveIndex(String id) { for (int i = 0; i < moves.length; i++) if (moves[i].equals(id)) return i; return -1; }
  public boolean hasUsableMove() { for (int p : pp) if (p > 0) return true; return false; }
  public double hpFrac() { return maxHp == 0 ? 0 : hp / (double) maxHp; }

  // ---------- cambios de forma ----------
  public void setSpecies(Species s) {
    sp = s; type1 = s.t1; type2 = s.t2;
    recalc(false);
  }

  public void checkItemForme() {
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
  public Species megaForm() {
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

  public boolean canDynamax() {
    return !dynamaxed && !megaed && !normalSp.noDmax && !sp.noDmax;
  }

  public Species gmaxForm() { return Dex.gmaxOf.get(normalSp.id); }

  // ---------- reinicio ----------
  public void resetVolatile() {
    Arrays.fill(stage, 0);
    vol.clear();
    type1 = sp.t1; type2 = sp.t2;
    tauntTurns = 0; lockedMove = ""; lockTurns = 0; confusionTurns = 0; protectCount = 0; turnsOut = 0; recharge = 0;
    chargingMove = ""; abilityOverride = null; toxicCounter = 0; flashFireOn = false; truantSkip = false;
    if (status.equals("tox")) toxicTurns = 0;
  }

  /** Al terminar el combate: vuelve a su forma normal (megas, dynamax, etc.). */
  public void endBattle() {
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

  public void fullHeal() {
    if (dead) return;
    hp = maxHp; status = ""; sleepTurns = 0;
    for (int i = 0; i < pp.length; i++) pp[i] = maxPp[i];
  }

  // ---------- experiencia ----------
  /** Devuelve los niveles subidos. */
  public int gainExp(int amount, int levelCap) {
    int gained = 0;
    exp += amount;
    while (level < 100 && level < levelCap && exp >= expFor(level + 1)) {
      level++; gained++;
    }
    if (level >= levelCap && exp > expFor(level)) exp = Math.min(exp, expFor(Math.min(100, level + 1)) - 1);
    if (gained > 0) { int oldMax = maxHp; recalc(false); hp = Math.min(maxHp, hp + (maxHp - oldMax)); }
    return gained;
  }

  public Evo checkEvolution(boolean inBattleEnd) {
    for (Evo e : normalSp.evos) {
      Species to = Dex.sp(e.to);
      if (to == null || e.type.equals("useItem")) continue;
      int need = evoLevel(e);
      if (need > 0 && level >= need) return e;
    }
    return null;
  }

  public static int evoLevel(Evo e) {
    if (e.type.equals("useItem")) return 0;
    if (e.level > 0) return e.level;
    if (e.type.equals("trade")) return 36;
    if (e.type.equals("levelFriendship")) return 26;
    return 32;
  }

  public Evo itemEvolution(String itemId) {
    for (Evo e : normalSp.evos)
      if (e.type.equals("useItem") && Dex.toId(e.item).equals(Dex.toId(itemId))) return e;
    return null;
  }

  public void evolveTo(Species to) {
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
  public List<String> newMovesAt(int lv) {
    List<String> out = new ArrayList<String>();
    for (int i = 0; i < normalSp.learnMove.length; i++)
      if (normalSp.learnLvl[i] == lv && moveIndex(normalSp.learnMove[i]) < 0 && !out.contains(normalSp.learnMove[i])) {
        Move m = Dex.mv(normalSp.learnMove[i]);
        if (m != null) out.add(normalSp.learnMove[i]);
      }
    return out;
  }

  public void learnMove(String id, int replaceSlot) {
    Move d = Dex.mv(id);
    if (d == null) return;
    if (moves.length < 4) {
      moves = Arrays.copyOf(moves, moves.length + 1); pp = Arrays.copyOf(pp, pp.length + 1); maxPp = Arrays.copyOf(maxPp, maxPp.length + 1);
      replaceSlot = moves.length - 1;
    }
    moves[replaceSlot] = id; maxPp[replaceSlot] = pp[replaceSlot] = d.pp;
  }
}
