// Simulador headless: IA vs IA con equipos aleatorios (incluye Mega y Dinamax).
// Uso: tools/check.sh   (compila el motor y ejecuta esto)
import pokefuego.engine.*;
import java.io.*;
import java.nio.file.*;
import java.util.*;

public class SimTest {
  public static void main(String[] args) throws Exception {
    final String dir = args.length > 0 ? args[0] : "pokefuego/data";
    int n = args.length > 1 ? Integer.parseInt(args[1]) : 2000;
    long seed = args.length > 2 ? Long.parseLong(args[2]) : 1;
    Dex.load(new DataLoader() {
      public String[] lines(String f) {
        try { return Files.readAllLines(Paths.get(dir, f)).toArray(new String[0]); } catch (IOException e) { throw new RuntimeException(e); }
      }
    });
    System.out.println("especies=" + Dex.species.size() + " movs=" + Dex.moves.size() + " habs=" + Dex.abilities.size() + " items=" + Dex.items.size()
      + " megas=" + Dex.megasOf.size() + " gmax=" + Dex.gmaxOf.size() + " pool=" + Dex.basePool.size());
    Random rng = new Random(seed);
    int errors = 0, turns = 0, megas = 0, dmax = 0, wins0 = 0, timeouts = 0;
    Map<String, Integer> errKinds = new TreeMap<String, Integer>();
    List<Species> pool = Dex.basePool;
    List<String> mega = new ArrayList<String>(Dex.megasOf.keySet());
    for (int g = 0; g < n; g++) {
      Battle b = null;
      try {
        Side[] s = new Side[2];
        for (int i = 0; i < 2; i++) {
          s[i] = new Side(); s[i].name = "T" + i; s[i].trainer = true;
          int cnt = 1 + rng.nextInt(6);
          for (int k = 0; k < cnt; k++) {
            Species sp;
            if (k == 0 && rng.nextInt(2) == 0) { sp = Dex.species.get(mega.get(rng.nextInt(mega.size()))); }
            else sp = pool.get(rng.nextInt(pool.size()));
            int lv = 20 + rng.nextInt(70);
            Mon m = Mon.create(sp, lv, rng);
            m.setMoves(MoveGen.smartSet(sp, lv, rng));
            if (k == 0) {
              List<Species> ml = Dex.megasOf.get(sp.id);
              if (ml != null && ml.get(0).reqItems.length > 0) { m.item = ml.get(0).reqItems[0]; }
            }
            if (m.item.isEmpty()) {
              String[] its = {"leftovers", "lifeorb", "choiceband", "choicespecs", "choicescarf", "focussash", "sitrusberry", "lumberry", "assaultvest", "rockyhelmet", "weaknesspolicy", "airballoon", "expertbelt", "charcoal", "mysticwater", "salacberry", "figyberry", "eviolite", "kingsrock", "scopelens", "shellbell", "blacksludge", "flameorb", "toxicorb", "whiteherb"};
              m.item = its[rng.nextInt(its.length)];
            }
            m.checkItemForme();
            s[i].party.add(m);
          }
          s[i].aiAce = 0;
        }
        b = new Battle(s[0], s[1], false, rng);
        b.start();
        int t = 0;
        while (!b.over && t < 300) {
          t++; turns++;
          Action a0 = AI.choose(b, 0, true), a1 = AI.choose(b, 1, true);
          b.turn(a0, a1);
          for (int guard = 0; guard < 4 && !b.over && (b.needSwitch[0] || b.needSwitch[1]); guard++) {
            for (int i = 0; i < 2; i++) if (b.needSwitch[i]) {
              int r = AI.bestReplacement(b, i);
              if (r < 0) { r = -1; for (int k = 0; k < b.sides[i].party.size(); k++) if (b.sides[i].party.get(k).alive() && b.sides[i].party.get(k) != b.sides[i].active) { r = k; break; } }
              if (r < 0) { b.needSwitch[i] = false; b.checkOver(); } else b.replace(i, r);
            }
          }
          b.takeEvents();
        }
        if (!b.over) timeouts++;
        if (b.winner == 0) wins0++;
        for (Side sd : b.sides) for (Mon m : sd.party) { if (m.megaed) megas++; if (m.dynamaxed) dmax++; m.endBattle(); }
      } catch (Throwable e) {
        errors++;
        StackTraceElement[] st = e.getStackTrace();
        String k = e.getClass().getSimpleName() + " @ " + (st.length > 0 ? st[0].getFileName() + ":" + st[0].getLineNumber() : "?") + " " + (e.getMessage() == null ? "" : e.getMessage());
        Integer c = errKinds.get(k); errKinds.put(k, c == null ? 1 : c + 1);
        if (errKinds.size() <= 3 && c == null) e.printStackTrace();
      }
    }
    System.out.println("combates=" + n + " turnos=" + turns + " megas=" + megas + " dynamax=" + dmax + " victoriasJ0=" + wins0 + " timeouts=" + timeouts + " errores=" + errors);
    for (Map.Entry<String, Integer> e : errKinds.entrySet()) System.out.println("  " + e.getValue() + "x " + e.getKey());
    System.exit(errors > 0 ? 1 : 0);
  }
}
