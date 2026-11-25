// =====================================================
//  Datos: especies, movimientos, habilidades, objetos
//  (cargados desde data/*.tsv, generados con tools/export_data.js)
// =====================================================
import java.util.*;

interface DataLoader { String[] lines(String file); }

class Evo {
  String to; int level; String type = "", item = "", cond = "", move = "";
}

class Species {
  String id, name, t1, t2 = "";
  int num, gen;
  int[] base = new int[6];                 // hp atk def spa spd spe
  String[] abilities = new String[3];      // slot0, slot1, oculta
  String baseSpecies, forme = "";
  boolean mega, primal, gmax, cosmetic, battleOnly, noDmax, past;
  String[] reqItems = new String[0];
  String reqMove = "";
  String battleOnlyFrom = "";
  List<Evo> evos = new ArrayList<Evo>();
  String prevo = "";
  double weight;
  String tags = "";
  int[] learnLvl = new int[0];
  String[] learnMove = new String[0];
  String[] otherMoves = new String[0];

  int bst() { int s = 0; for (int b : base) s += b; return s; }
  boolean legendary() { return tags.contains("Legendary") || tags.contains("Mythical"); }
  boolean restricted() { return tags.contains("RestrictedLegendary") || tags.contains("Mythical"); }
  boolean isBaseForm() { return !mega && !primal && !gmax && !battleOnly && !cosmetic; }
  public String toString() { return name; }
}

class Move {
  String id, name, type, target = "normal", flags = "";
  char cat = 'S';           // P, S, N(status)
  int bp, acc, pp, prio, crit = 1;
  String status = "", vol = "";
  String selfBoost = "", tgtBoost = "";
  int secChance; String secStatus = "", secVol = "", secBoost = "";
  String selfAfter = ""; int selfAfterChance = 100;
  int[] drain, recoil, heal;
  int multiMin, multiMax;
  boolean selfdestruct, ohko, forceSwitch, selfSwitch, breaksProtect, ignoreDef, willCrit;
  String weather = "", terrain = "", sideCond = "", damage = "", pseudo = "", slotCond = "";
  String overrideOff = "", overrideDefPoke = "";
  int maxBP; String isMax = "";
  String desc = "";
  boolean supported = true;
  boolean has(String f) { return flags.indexOf(f) >= 0; }
  boolean contact() { return has("contact"); }
  boolean status() { return cat == 'N'; }
  Move copy() {
    Move m = new Move();
    m.id = id; m.name = name; m.type = type; m.target = target; m.flags = flags; m.cat = cat; m.bp = bp; m.acc = acc;
    m.pp = pp; m.prio = prio; m.crit = crit; m.status = status; m.vol = vol; m.selfBoost = selfBoost; m.tgtBoost = tgtBoost;
    m.secChance = secChance; m.secStatus = secStatus; m.secVol = secVol; m.secBoost = secBoost; m.selfAfter = selfAfter;
    m.selfAfterChance = selfAfterChance; m.drain = drain; m.recoil = recoil; m.heal = heal; m.multiMin = multiMin;
    m.multiMax = multiMax; m.selfdestruct = selfdestruct; m.ohko = ohko; m.forceSwitch = forceSwitch;
    m.selfSwitch = selfSwitch; m.breaksProtect = breaksProtect; m.ignoreDef = ignoreDef; m.willCrit = willCrit;
    m.weather = weather; m.terrain = terrain; m.sideCond = sideCond; m.damage = damage; m.pseudo = pseudo;
    m.slotCond = slotCond; m.overrideOff = overrideOff; m.overrideDefPoke = overrideDefPoke; m.maxBP = maxBP;
    m.isMax = isMax; m.desc = desc; m.supported = supported;
    return m;
  }
  public String toString() { return name; }
}

class AbilityDef { String id, name, desc; int gen; double rating; }

class ItemDef {
  String id, name, desc, megaStone = "", megaEvolves = "", plateType = "", memoryType = "", driveType = "", ngType = "";
  String[] users = new String[0];
  int gen, fling, ngBP;
  boolean berry, ball, choice;
  public String toString() { return name; }
}

class Dex {
  static final String[] TYPES = {"Normal","Fire","Water","Electric","Grass","Ice","Fighting","Poison","Ground",
    "Flying","Psychic","Bug","Rock","Ghost","Dragon","Dark","Steel","Fairy"};
  static final String[] STATS = {"HP","Ataque","Defensa","At. Esp.","Def. Esp.","Velocidad"};
  static Map<String, Species> species = new LinkedHashMap<String, Species>();
  static Map<String, Move> moves = new LinkedHashMap<String, Move>();
  static Map<String, AbilityDef> abilities = new LinkedHashMap<String, AbilityDef>();
  static Map<String, ItemDef> items = new LinkedHashMap<String, ItemDef>();
  static Map<String, Species> byName = new HashMap<String, Species>();
  static Map<String, List<Species>> megasOf = new HashMap<String, List<Species>>();
  static Map<String, Species> gmaxOf = new HashMap<String, Species>();
  static Map<String, Move> gmaxMoveOf = new HashMap<String, Move>();   // especie base -> movimiento G-Max
  static Map<String, Species> itemForme = new HashMap<String, Species>(); // "baseSpeciesId|itemId" -> forma
  static Map<String, Integer> typeIndex = new HashMap<String, Integer>();
  static double[][] chart = new double[18][18];
  static List<Species> basePool = new ArrayList<Species>();     // especies "normales" (para aleatorio/salvajes)
  static boolean loaded = false;

  static String toId(String s) { return s == null ? "" : s.toLowerCase().replaceAll("[^a-z0-9]", ""); }

  static String[] cols(String line) {
    String[] p = line.split("\t", -1);
    return p;
  }
  static String col(String[] p, int i) { return i < p.length ? p[i] : ""; }
  static int num(String s) { try { return s.isEmpty() ? 0 : (int) Double.parseDouble(s); } catch (Exception e) { return 0; } }
  static int[] frac(String s) {
    if (s.isEmpty()) return null;
    String[] f = s.split("/");
    if (f.length < 2) return null;
    return new int[]{num(f[0]), num(f[1])};
  }

  static void load(DataLoader L) {
    if (loaded) return;
    for (String ln : L.lines("types.tsv")) {
      String[] p = cols(ln);
      if (p[0].startsWith("atk")) { for (int i = 1; i < p.length; i++) typeIndex.put(p[i], i - 1); continue; }
      Integer a = typeIndex.get(p[0]);
      if (a == null) continue;
      for (int i = 1; i < p.length && i <= 18; i++) chart[a][i - 1] = Double.parseDouble(p[i]);
    }
    for (String ln : L.lines("species.tsv")) {
      if (ln.trim().isEmpty()) continue;
      String[] p = cols(ln);
      Species s = new Species();
      s.id = p[0]; s.name = p[1]; s.num = num(p[2]); s.gen = num(p[3]); s.t1 = p[4]; s.t2 = p[5];
      for (int i = 0; i < 6; i++) s.base[i] = num(p[6 + i]);
      s.abilities[0] = p[12]; s.abilities[1] = p[13]; s.abilities[2] = p[14];
      s.baseSpecies = p[15]; s.forme = p[16];
      String fl = p[17];
      s.mega = fl.contains("mega"); s.primal = fl.contains("primal"); s.gmax = fl.contains("gmax");
      s.cosmetic = fl.contains("cosmetic"); s.battleOnly = fl.contains("battleonly"); s.noDmax = fl.contains("nodmax");
      s.past = fl.contains("past");
      s.reqItems = p[18].isEmpty() ? new String[0] : p[18].split("\\|");
      s.reqMove = p[19]; s.battleOnlyFrom = p[20];
      String[] ev = p[21].isEmpty() ? new String[0] : p[21].split("\\|");
      String[] ei = p[22].isEmpty() ? new String[0] : p[22].split("\\|");
      for (int i = 0; i < ev.length; i++) {
        Evo e = new Evo(); e.to = ev[i];
        if (i < ei.length) {
          String[] q = ei[i].split(":", -1);
          e.level = num(col(q, 0)); e.type = col(q, 1); e.item = col(q, 2); e.cond = col(q, 3); e.move = col(q, 4);
        }
        s.evos.add(e);
      }
      s.prevo = p[23]; s.weight = p[24].isEmpty() ? 50 : Double.parseDouble(p[24]); s.tags = col(p, 25);
      species.put(s.id, s); byName.put(toId(s.name), s);
    }
    for (String ln : L.lines("learnsets.tsv")) {
      if (ln.trim().isEmpty()) continue;
      String[] p = cols(ln);
      Species s = species.get(p[0]);
      if (s == null) continue;
      List<String> lm = new ArrayList<String>(); List<Integer> ll = new ArrayList<Integer>();
      if (p.length > 1 && !p[1].isEmpty()) for (String t : p[1].split(",")) {
        int c = t.indexOf(':');
        ll.add(num(t.substring(0, c))); lm.add(t.substring(c + 1));
      }
      s.learnLvl = new int[ll.size()]; s.learnMove = new String[ll.size()];
      for (int i = 0; i < ll.size(); i++) { s.learnLvl[i] = ll.get(i); s.learnMove[i] = lm.get(i); }
      s.otherMoves = (p.length > 2 && !p[2].isEmpty()) ? p[2].split(",") : new String[0];
    }
    for (String ln : L.lines("moves.tsv")) {
      if (ln.trim().isEmpty()) continue;
      String[] p = cols(ln);
      Move m = new Move();
      m.id = p[0]; m.name = p[1]; m.type = p[2]; m.cat = p[3].charAt(0) == 'S' ? 'S' : p[3].charAt(0) == 'P' ? 'P' : 'N';
      m.bp = num(p[4]); m.acc = num(p[5]); m.pp = num(p[6]); m.prio = num(p[7]); m.crit = Math.max(1, num(p[8]));
      m.target = p[9]; m.flags = p[10]; m.status = p[11]; m.vol = p[12]; m.selfBoost = p[13]; m.tgtBoost = p[14];
      m.secChance = num(p[15]); m.secStatus = p[16]; m.secVol = p[17]; m.secBoost = p[18];
      m.selfAfter = p[19]; m.selfAfterChance = num(p[20]); if (m.selfAfterChance == 0) m.selfAfterChance = 100;
      m.drain = frac(p[21]); m.recoil = frac(p[22]); m.heal = frac(p[23]);
      String mh = p[24];
      if (!mh.isEmpty()) { String[] q = mh.split("-"); m.multiMin = num(q[0]); m.multiMax = num(q.length > 1 ? q[1] : q[0]); }
      m.selfdestruct = num(p[25]) == 1; m.ohko = num(p[26]) == 1; m.weather = p[27]; m.terrain = p[28]; m.sideCond = p[29];
      m.forceSwitch = num(p[30]) == 1; m.selfSwitch = num(p[31]) == 1; m.damage = p[32];
      m.maxBP = num(p[33]); m.isMax = col(p, 34); m.pseudo = col(p, 35); m.slotCond = col(p, 36);
      m.breaksProtect = num(col(p, 37)) == 1; m.overrideOff = col(p, 38); m.overrideDefPoke = col(p, 39);
      m.ignoreDef = num(col(p, 40)) == 1; m.willCrit = num(col(p, 41)) == 1; m.desc = col(p, 42);
      if (m.cat == 'N' && m.bp == 0) m.supported = Effects.statusSupported(m);
      moves.put(m.id, m);
    }
    for (String ln : L.lines("abilities.tsv")) {
      if (ln.trim().isEmpty()) continue;
      String[] p = cols(ln);
      AbilityDef a = new AbilityDef(); a.id = p[0]; a.name = p[1]; a.gen = num(p[2]);
      try { a.rating = Double.parseDouble(p[3]); } catch (Exception e) { }
      a.desc = col(p, 4);
      abilities.put(toId(a.name), a);
    }
    for (String ln : L.lines("items.tsv")) {
      if (ln.trim().isEmpty()) continue;
      String[] p = cols(ln);
      ItemDef it = new ItemDef(); it.id = p[0]; it.name = p[1]; it.gen = num(p[2]); it.megaStone = p[3];
      it.users = p[4].isEmpty() ? new String[0] : p[4].split("\\|");
      it.berry = p[5].equals("1"); it.ball = p[6].equals("1"); it.plateType = p[7]; it.memoryType = p[8]; it.driveType = p[9];
      it.fling = num(p[10]); it.ngType = p[11]; it.ngBP = num(p[12]); it.choice = p[13].equals("1"); it.desc = col(p, 14);
      items.put(it.id, it);
    }
    // ---- índices derivados ----
    for (Species s : species.values()) {
      Species base = byName.get(toId(s.baseSpecies));
      if (s.mega || s.primal) {
        String key = toId(s.battleOnlyFrom.isEmpty() ? s.baseSpecies : s.battleOnlyFrom);
        Species b = byName.get(key); if (b == null) b = base;
        if (b != null) { List<Species> l = megasOf.get(b.id); if (l == null) { l = new ArrayList<Species>(); megasOf.put(b.id, l); } l.add(s); }
      } else if (s.gmax) {
        Species gb = byName.get(toId(s.name.replace("-Gmax", "")));
        if (gb == null) gb = base;
        if (gb != null) gmaxOf.put(gb.id, s);
      } else if (!s.battleOnly && !s.cosmetic && s.reqItems.length > 0 && base != null && !s.reqItems[0].isEmpty()) {
        for (String ri : s.reqItems) itemForme.put(base.id + "|" + toId(ri), s);
      }
      if (wildEligible(s)) basePool.add(s);
    }
    for (Move m : moves.values()) {
      if (!m.isMax.isEmpty() && !m.isMax.equals("Max")) gmaxMoveOf.put(toId(m.isMax), m);
    }
    loaded = true;
  }

  static Species sp(String id) { Species s = species.get(toId(id)); if (s == null) s = byName.get(toId(id)); return s; }
  static Move mv(String id) { return moves.get(toId(id)); }
  static ItemDef item(String id) { return id == null || id.isEmpty() ? null : items.get(toId(id)); }
  static AbilityDef ab(String name) { return abilities.get(toId(name)); }
  static int ti(String type) { Integer i = typeIndex.get(type); return i == null ? 0 : i; }

  static double eff(String atkType, String d1, String d2) {
    double e = chart[ti(atkType)][ti(d1)];
    if (d2 != null && !d2.isEmpty()) e *= chart[ti(atkType)][ti(d2)];
    return e;
  }

  /** Los Pokémon base que pueden aparecer como salvajes / en modo aleatorio. */
  static boolean wildEligible(Species s) {
    return s.isBaseForm() && s.num > 0 && s.reqItems.length == 0 && s.learnMove.length > 0;
  }

  // ---- movimientos aprendibles ----
  static List<String> learnableAt(Species s, int level) {
    List<String> out = new ArrayList<String>();
    for (int i = 0; i < s.learnMove.length; i++) if (s.learnLvl[i] <= Math.max(1, level) && !out.contains(s.learnMove[i])) out.add(s.learnMove[i]);
    return out;
  }
  static List<String> allLearnable(Species s) {
    List<String> out = new ArrayList<String>();
    for (String m : s.learnMove) if (!out.contains(m)) out.add(m);
    for (String m : s.otherMoves) if (!out.contains(m)) out.add(m);
    return out;
  }
}
