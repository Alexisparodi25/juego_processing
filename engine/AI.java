package pokefuego.engine;
import java.util.*;

public class AI {
  /** Mejor % de daño que puede hacer a sobre d con cualquiera de sus movimientos. */
  public static double bestPct(Battle b, int sa, Mon a, int sd, Mon d) {
    double best = 0;
    for (int i = 0; i < a.moves.length; i++) {
      if (a.pp[i] <= 0) continue;
      Move m = Dex.mv(a.moves[i]);
      if (m == null) continue;
      best = Math.max(best, Calc.expectedPercent(b, b.sides[sa], a, b.sides[sd], d, m));
    }
    return best;
  }

  /** Valor del enfrentamiento a vs d (positivo = favorable a a). */
  public static double matchup(Battle b, int sa, Mon a, int sd, Mon d) {
    double mine = Math.min(1.5, bestPct(b, sa, a, sd, d));
    double theirs = Math.min(1.5, bestPct(b, sd, d, sa, a));
    double spd = Calc.speed(b, b.sides[sa], a) - Calc.speed(b, b.sides[sd], d);
    double myTurns = mine <= 0 ? 99 : Math.ceil(d.hpFrac() / mine);
    double theirTurns = theirs <= 0 ? 99 : Math.ceil(a.hpFrac() / theirs);
    double v = theirTurns - myTurns;
    if (spd > 0) v += 0.5; else if (spd < 0) v -= 0.5;
    v += (mine - theirs) * 0.5;
    return v;
  }

  public static int bestReplacement(Battle b, int si) {
    Side s = b.sides[si];
    Mon foe = b.sides[1 - si].active;
    int best = -1; double bv = -1e9;
    for (int i = 0; i < s.party.size(); i++) {
      Mon m = s.party.get(i);
      if (!m.alive() || m == s.active) continue;
      double v = foe == null || !foe.alive() ? m.hpFrac() : matchup(b, si, m, 1 - si, foe) + m.hpFrac();
      if (v > bv) { bv = v; best = i; }
    }
    return best;
  }

  public static Action choose(Battle b, int si, boolean hard) {
    Side me = b.sides[si];
    Mon u = me.active, t = b.sides[1 - si].active;
    Random r = b.rng;
    if (!u.chargingMove.isEmpty() || u.recharge > 0 || (u.lockTurns > 0 && !u.lockedMove.isEmpty() && !u.dynamaxed)) return Action.move(0, false, false);
    if (b.mustStruggle(u)) return Action.move(0, false, false);
    List<Integer> usable = b.usableMoves(u);
    if (usable.isEmpty()) return Action.move(0, false, false);

    // ¿transformarse?
    boolean mega = false, dyna = false;
    if (!me.transformed && me.trainer && me.canTransform) {
      boolean isAce = me.aiAce >= 0 && me.party.indexOf(u) == me.aiAce;
      if (b.canMega(me)) mega = true;
      else if ((isAce || me.alive() == 1) && b.canDynamax(me) && (hard || r.nextInt(2) == 0)) dyna = true;
    }
    if (!hard) {                       // IA sencilla: aleatorio ponderado por daño
      int pick = usable.get(r.nextInt(usable.size()));
      double bestv = -1;
      for (int i : usable) {
        Move m = Dex.mv(u.moves[i]); if (m == null) continue;
        double v = Calc.expectedPercent(b, me, u, b.sides[1 - si], t, m) + r.nextDouble() * 0.3;
        if (v > bestv) { bestv = v; pick = i; }
      }
      return Action.move(pick, mega, dyna);
    }
    // ------ DIFÍCIL ------
    boolean willDyna = dyna;
    int bestIdx = usable.get(0); double bestScore = -1e9;
    double myHp = u.hpFrac();
    double foeBest = bestPct(b, 1 - si, t, si, u);
    double mySpd = Calc.speed(b, me, u), foeSpd = Calc.speed(b, b.sides[1 - si], t);
    boolean faster = mySpd > foeSpd;
    for (int i : usable) {
      Move base = Dex.mv(u.moves[i]);
      if (base == null) continue;
      Move m = u.dynamaxed || willDyna ? (willDyna ? b.maxVersion(u, base) : b.effectiveMove(u, i)) : base;
      double score;
      if (m.cat != 'N' || m.bp > 0 || !m.damage.isEmpty()) {
        double pct = Calc.expectedPercent(b, me, u, b.sides[1 - si], t, m);
        score = pct * 100;
        if (pct >= t.hpFrac()) { score += 120; if (b.prioOf(u, m) > 0 || faster) score += 30; }
        else if (b.prioOf(u, m) > 0 && t.hpFrac() <= 0.3) score += 20;
        if (m.recoil != null && myHp < 0.35) score -= 25;
        if (m.has("recharge")) score -= 15;
        if (m.selfdestruct) score -= (myHp > 0.3 ? 80 : -10);
        if (m.selfSwitch && me.hasBackup()) score += (pct < 0.15 ? 25 : 5);
        if (m.has("charge") && !u.dynamaxed) score -= 10;
        if (m.bp == 0 && m.damage.isEmpty() && !m.ohko && pct == 0) score = -50;
        score += r.nextDouble() * 6;
      } else {
        score = statusScore(b, si, u, t, m, myHp, foeBest, faster);
      }
      if (score > bestScore) { bestScore = score; bestIdx = i; }
    }
    // ¿cambiar?
    if (me.hasBackup() && b.canSwitch(me) && !(u.dynamaxed)) {
      boolean bad = bestScore < 18;
      double mu = matchup(b, si, u, 1 - si, t);
      boolean threatened = foeBest >= u.hpFrac() && !faster && bestScore < 120;
      if ((bad || threatened || mu < -1.5) && r.nextInt(100) < (bad ? 70 : 40)) {
        int rep = bestReplacement(b, si);
        if (rep >= 0) {
          Mon cand = me.party.get(rep);
          if (matchup(b, si, cand, 1 - si, t) > mu + 0.5) return Action.sw(rep);
        }
      }
    }
    return Action.move(bestIdx, mega, dyna);
  }

  public static double statusScore(Battle b, int si, Mon u, Mon t, Move m, double myHp, double foeBest, boolean faster) {
    String id = m.id;
    Side me = b.sides[si], foe = b.sides[1 - si];
    double s = 10;
    if (!m.supported) return -100;
    if (m.heal != null && m.target.equals("self")) return myHp < 0.5 ? (myHp < 0.3 ? 140 : 90) : -80;
    if (id.equals("rest")) return myHp < 0.4 && u.status.isEmpty() ? 95 : (myHp < 0.4 ? 110 : -80);
    if (PROT.contains(id)) return (u.protectCount == 0 && (t.status.equals("tox") || t.vol.contains("leechseed") || b.turn == 1 || foeBest > 0.4)) ? 40 : -30;
    if (!m.status.isEmpty()) {
      if (!t.status.isEmpty()) return -90;
      if (m.status.equals("slp")) return t.isType("Grass") && m.has("powder") ? -90 : 80;
      if (m.status.equals("par")) return t.stat[5] > u.stat[5] ? 70 : 35;
      if (m.status.equals("brn")) return t.stat[1] >= t.stat[3] ? 70 : 30;
      return 60;
    }
    if (!m.sideCond.isEmpty()) {
      String sc = m.sideCond.toLowerCase();
      if (sc.equals("stealthrock")) return foe.stealthRock == 0 && foe.hasBackup() ? 75 : -90;
      if (sc.equals("spikes")) return foe.spikes < 3 && foe.hasBackup() ? 60 : -90;
      if (sc.equals("toxicspikes")) return foe.toxicSpikes < 2 && foe.hasBackup() ? 50 : -90;
      if (sc.equals("reflect")) return me.reflect == 0 && b.bestIsPhys(t) ? 55 : -90;
      if (sc.equals("lightscreen")) return me.lightScreen == 0 && !b.bestIsPhys(t) ? 55 : -90;
      if (sc.equals("tailwind")) return me.tailwind == 0 ? 45 : -90;
      if (sc.equals("auroraveil")) return me.auraVeil == 0 && b.weather.equals("hail") ? 60 : -90;
    }
    if (!m.weather.isEmpty()) { return 40; }
    if (!m.selfBoost.isEmpty()) {
      // sólo potenciarse si es seguro
      boolean safe = foeBest < 0.35 || (faster && foeBest < 0.5);
      int cur = 0; for (int i = 1; i < 6; i++) cur += u.stage[i];
      if (!safe || cur >= 4 || myHp < 0.5) return -20;
      return 75 - cur * 10;
    }
    if (!m.tgtBoost.isEmpty()) return 12;
    if (m.vol.equals("leechseed")) return t.vol.contains("leechseed") || t.isType("Grass") ? -90 : 55;
    if (m.vol.equals("confusion")) return t.vol.contains("confusion") ? -90 : 40;
    if (m.forceSwitch) return t.stage[1] + t.stage[3] >= 2 ? 90 : 5;
    return s;
  }

  public static final Set<String> PROT = new HashSet<String>(Arrays.asList("protect", "detect", "kingsshield", "spikyshield", "banefulbunker"));
}
