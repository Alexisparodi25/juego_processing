package pokefuego.engine;
import java.util.*;

public class MoveGen {
  /** Los últimos 4 movimientos por nivel (comportamiento clásico). */
  public static List<String> levelUpSet(Species s, int level) {
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

  public static double score(Species s, Move m, Set<String> have) {
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
  public static List<String> smartSet(Species s, int level, Random r) {
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
