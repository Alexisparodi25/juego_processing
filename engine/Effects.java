package pokefuego.engine;
import java.util.*;

public class Effects {
  public static final Set<String> SPECIAL = new HashSet<String>(Arrays.asList(
    "protect", "detect", "kingsshield", "spikyshield", "banefulbunker", "maxguard", "endure", "rest", "haze", "bellydrum",
    "healbell", "aromatherapy", "painsplit", "sleeptalk", "metronome", "splash", "celebrate", "holdhands", "teleport", "growth",
    "defog", "batonpass", "partingshot", "synthesis", "moonlight", "morningsun", "shoreup", "taunt", "trickroom"));
  public static final Set<String> SIDE = new HashSet<String>(Arrays.asList(
    "stealthrock", "spikes", "toxicspikes", "reflect", "lightscreen", "auroraveil", "tailwind", "stickyweb", "safeguard"));

  /** ¿El efecto de este movimiento de estado está implementado en el motor? */
  public static boolean statusSupported(Move m) {
    if (SPECIAL.contains(m.id)) return true;
    if (!m.status.isEmpty() && !m.target.equals("self")) return true;
    if (!m.weather.isEmpty() || !m.terrain.isEmpty()) return true;
    if (!m.sideCond.isEmpty() && SIDE.contains(m.sideCond.toLowerCase())) return true;
    if (m.heal != null && m.target.equals("self")) return true;
    if (!m.selfBoost.isEmpty() || !m.tgtBoost.isEmpty()) return true;
    if (m.vol.equals("confusion") || m.vol.equals("leechseed") || m.vol.equals("focusenergy")) return true;
    if (m.forceSwitch) return true;
    return false;
  }
}
