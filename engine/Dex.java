package pokefuego.engine;
import java.util.*;

public class Dex {
  public static final String[] TYPES = {"Normal","Fire","Water","Electric","Grass","Ice","Fighting","Poison","Ground",
    "Flying","Psychic","Bug","Rock","Ghost","Dragon","Dark","Steel","Fairy"};
  public static final String[] STATS = {"HP","Ataque","Defensa","At. Esp.","Def. Esp.","Velocidad"};
  public static Map<String, Species> species = new LinkedHashMap<String, Species>();
  public static Map<String, Move> moves = new LinkedHashMap<String, Move>();
  public static Map<String, AbilityDef> abilities = new LinkedHashMap<String, AbilityDef>();
  public static Map<String, ItemDef> items = new LinkedHashMap<String, ItemDef>();
  public static Map<String, Species> byName = new HashMap<String, Species>();
  public static Map<String, List<Species>> megasOf = new HashMap<String, List<Species>>();
  public static Map<String, Species> gmaxOf = new HashMap<String, Species>();
  public static Map<String, Move> gmaxMoveOf = new HashMap<String, Move>();   // especie base -> movimiento G-Max
  public static Map<String, Species> itemForme = new HashMap<String, Species>(); // "baseSpeciesId|itemId" -> forma
  public static Map<String, Integer> typeIndex = new HashMap<String, Integer>();
  public static double[][] chart = new double[18][18];
  public static List<Species> basePool = new ArrayList<Species>();     // especies "normales" (para aleatorio/salvajes)
  public static boolean loaded = false;

  public static String toId(String s) { return s == null ? "" : s.toLowerCase().replaceAll("[^a-z0-9]", ""); }

  public static String[] cols(String line) {
    String[] p = line.split("\t", -1);
    return p;
  }
  public static String col(String[] p, int i) { return i < p.length ? p[i] : ""; }
  public static int num(String s) { try { return s.isEmpty() ? 0 : (int) Double.parseDouble(s); } catch (Exception e) { return 0; } }
  public static int[] frac(String s) {
    if (s.isEmpty()) return null;
    String[] f = s.split("/");
    if (f.length < 2) return null;
    return new int[]{num(f[0]), num(f[1])};
  }

  public static void load(DataLoader L) {
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
    loadSpanish(L);
    loaded = true;
  }

  /** Nombres y descripciones en español (WikiDex). Opcional: si falta el archivo se usa el inglés. */
  public static void loadSpanish(DataLoader L) {
    try {
      for (String ln : L.lines("es_moves.tsv")) { String[] p = cols(ln); Move m = moves.get(p[0]); if (m != null && p.length > 2) { m.es = p[1]; m.descEs = p[2]; } }
      for (String ln : L.lines("es_abilities.tsv")) { String[] p = cols(ln); AbilityDef a = abilities.get(p[0]); if (a != null && p.length > 2) { a.es = p[1]; a.descEs = p[2]; } }
      for (String ln : L.lines("es_items.tsv")) { String[] p = cols(ln); ItemDef it = items.get(p[0]); if (it != null && p.length > 2) { it.es = p[1]; it.descEs = p[2]; } }
    } catch (RuntimeException e) { /* sin traducción */ }
  }

  public static Species sp(String id) { Species s = species.get(toId(id)); if (s == null) s = byName.get(toId(id)); return s; }
  public static Move mv(String id) { return moves.get(toId(id)); }
  public static ItemDef item(String id) { return id == null || id.isEmpty() ? null : items.get(toId(id)); }
  public static AbilityDef ab(String name) { return abilities.get(toId(name)); }
  public static int ti(String type) { Integer i = typeIndex.get(type); return i == null ? 0 : i; }

  public static double eff(String atkType, String d1, String d2) {
    double e = chart[ti(atkType)][ti(d1)];
    if (d2 != null && !d2.isEmpty()) e *= chart[ti(atkType)][ti(d2)];
    return e;
  }

  /** Los Pokémon base que pueden aparecer como salvajes / en modo aleatorio. */
  public static boolean wildEligible(Species s) {
    return s.isBaseForm() && s.num > 0 && s.reqItems.length == 0 && s.learnMove.length > 0;
  }

  // ---- movimientos aprendibles ----
  public static List<String> learnableAt(Species s, int level) {
    List<String> out = new ArrayList<String>();
    for (int i = 0; i < s.learnMove.length; i++) if (s.learnLvl[i] <= Math.max(1, level) && !out.contains(s.learnMove[i])) out.add(s.learnMove[i]);
    return out;
  }
  public static List<String> allLearnable(Species s) {
    List<String> out = new ArrayList<String>();
    for (String m : s.learnMove) if (!out.contains(m)) out.add(m);
    for (String m : s.otherMoves) if (!out.contains(m)) out.add(m);
    return out;
  }
}
