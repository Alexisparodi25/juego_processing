// Bot que juega la partida completa (todos los modos) para validar la capa de juego.
import pokefuego.engine.*;
import java.nio.file.*;
import java.util.*;

public class PlaySim {
  static Random rr = new Random(99);

  static void runBattle(Game g, Battle b, boolean tryCatch) {
    b.start();
    int guard = 0;
    int throws_ = 0;
    while (!b.over && guard++ < 400) {
      Action a;
      Mon u = b.sides[0].active;
      if (tryCatch && b.wild && throws_ < 12 && g.bag.count("pokeball") > 0 && !b.nuzlockeNoCatch && g.party.size() < 6 && (g.rules.nuzlocke || g.party.size() < 6)) { a = Action.ball("pokeball"); throws_++; }
      else a = AI.choose(b, 0, true);
      b.turn(a, AI.choose(b, 1, true));
      for (int k = 0; k < 4 && !b.over && (b.needSwitch[0] || b.needSwitch[1]); k++) {
        for (int i = 0; i < 2; i++) if (b.needSwitch[i]) {
          int r = AI.bestReplacement(b, i);
          if (r < 0) { b.needSwitch[i] = false; b.checkOver(); } else b.replace(i, r);
        }
      }
      b.takeEvents();
    }
  }

  static void equip(Game g) {
    for (String id : g.bag.ids()) {
      ItemDef d = Dex.items.get(id);
      if (d == null || d.megaStone.isEmpty()) continue;
      for (Mon m : g.party) if (m.item.isEmpty() && Dex.megasOf.containsKey(m.normalSp.id)) {
        List<Species> ml = Dex.megasOf.get(m.normalSp.id);
        for (Species s : ml) for (String ri : s.reqItems) if (Dex.toId(ri).equals(id) && g.bag.remove(id, 1)) { m.item = id; break; }
      }
    }
    for (Mon m : g.party) if (m.item.isEmpty() && rr.nextInt(2) == 0) { m.item = "leftovers"; }
  }

  static void applyResult(Game g, Game.Result r) {
    for (Object[] ev : r.evolve) { Mon m = (Mon) ev[0]; m.evolveTo((Species) ev[1]); }
    for (Object[] l : r.learn) { Mon m = (Mon) l[0]; m.learnMove((String) l[1], m.moves.length < 4 ? m.moves.length : rr.nextInt(4)); }
  }

  public static void main(String[] args) throws Exception {
    final String dir = "pokefuego/data";
    Dex.load(new DataLoader() { public String[] lines(String f) { try { return Files.readAllLines(Paths.get(dir, f)).toArray(new String[0]); } catch (Exception e) { throw new RuntimeException(e); } } });
    int runs = args.length > 0 ? Integer.parseInt(args[0]) : 6;
    int fails = 0;
    for (int run = 0; run < runs; run++) {
      Rules r = new Rules(); r.random = (run & 1) == 1; r.nuzlocke = (run & 2) == 2;
      Game g = new Game(r, 1000 + run);
      List<String> ch = g.starterChoices();
      g.chooseStarter(ch.get(run % 3));
      int battles = 0, caught = 0, whiteouts = 0;
      try {
        outer:
        while (!g.gameOver && !g.champion) {
          if (whiteouts > 12) { System.out.println("  (el bot se rindió en " + g.place().name + ")"); break; }
          Place p = g.place();
          g.healAll(); g.bag.add("pokeball", 6);
          // capturar primer encuentro y entrenar
          int wilds = 0;
          while (wilds < 14) {
            double avg = 0; int n = 0; for (Mon m : g.party) { avg += m.level; n++; }
            avg /= Math.max(1, n);
            if (avg >= g.levelCap() - 1 && wilds >= 1) break;
            if (g.firstAlive() == null) break;
            Battle b = g.wildBattle();
            runBattle(g, b, true);
            Game.Result res = g.finish(b, null);
            applyResult(g, res); equip(g); battles++; wilds++;
            if (res.captured) caught++;
            if (res.lost) { whiteouts++; if (g.gameOver) break outer; continue outer; }
            g.healAll();
          }
          for (Trainer t : p.trainers) {
            if (t.done) continue; g.healAll();
            Battle b = g.trainerBattle(t);
            runBattle(g, b, false);
            Game.Result res = g.finish(b, null); applyResult(g, res); equip(g); battles++;
            if (res.lost) { whiteouts++; if (g.gameOver) break outer; continue outer; }
          }
          Trainer boss;
          while ((boss = g.nextBoss()) != null) {
            g.healAll();
            Battle b = g.trainerBattle(boss);
            runBattle(g, b, false);
            Game.Result res = g.finish(b, null); applyResult(g, res); equip(g); battles++;
            if (res.lost) { whiteouts++; if (g.gameOver) break outer; continue outer; }
          }
          g.checkChampion();
          if (g.champion) break;
          if (g.canAdvance()) g.advance(); else { System.out.println("  atascado en " + p.name); fails++; break; }
        }
      } catch (Throwable e) { e.printStackTrace(); fails++; }
      // guardar / cargar
      List<String> sv = g.serialize();
      Game g2 = Game.deserialize(sv);
      boolean okSave = g2 != null && g2.party.size() == g.party.size() && g2.placeIdx == g.placeIdx && g2.money == g.money;
      StringBuilder sb = new StringBuilder();
      for (Mon m : g.party) sb.append(m.name()).append(" L").append(m.level).append(m.dead ? "†" : "").append(", ");
      System.out.println("run" + run + " random=" + r.random + " nuz=" + r.nuzlocke + " lugar=" + g.place().name + " medallas=" + g.badges + " campeón=" + g.champion
        + " gameover=" + g.gameOver + " combates=" + battles + " capturas=" + caught + " muertos=" + g.graveyard.size() + " guardado=" + okSave + "\n   " + sb);
      if (!okSave) fails++;
    }
    System.exit(fails > 0 ? 1 : 0);
  }
}
