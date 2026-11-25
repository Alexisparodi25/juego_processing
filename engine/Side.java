package pokefuego.engine;
import java.util.*;

public class Side {
  public String name = "";
  public boolean human, trainer;
  public List<Mon> party = new ArrayList<Mon>();
  public Mon active;
  public boolean transformed;               // ya usó Mega o Dinamax en este combate
  public boolean canTransform = true;       // (IA) permitido usar Mega/Dinamax
  public int stealthRock, spikes, toxicSpikes, safeguard;
  public boolean stickyWeb;
  public int reflect, lightScreen, auraVeil, tailwind, dot;
  public String dotName = "";
  public int aiAce = -1;                    // índice del "as" que se transforma (IA)

  public int alive() { int n = 0; for (Mon m : party) if (m.alive()) n++; return n; }
  public boolean hasBackup() { for (Mon m : party) if (m.alive() && m != active) return true; return false; }
  public int indexOf(Mon m) { return party.indexOf(m); }
}
