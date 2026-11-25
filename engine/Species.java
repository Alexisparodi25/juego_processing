package pokefuego.engine;
import java.util.*;

public class Species {
  public String id, name, t1, t2 = "";
  public int num, gen;
  public int[] base = new int[6];                 // hp atk def spa spd spe
  public String[] abilities = new String[3];      // slot0, slot1, oculta
  public String baseSpecies, forme = "";
  public boolean mega, primal, gmax, cosmetic, battleOnly, noDmax, past;
  public String[] reqItems = new String[0];
  public String reqMove = "";
  public String battleOnlyFrom = "";
  public List<Evo> evos = new ArrayList<Evo>();
  public String prevo = "";
  public double weight;
  public String tags = "";
  public int[] learnLvl = new int[0];
  public String[] learnMove = new String[0];
  public String[] otherMoves = new String[0];

  public int bst() { int s = 0; for (int b : base) s += b; return s; }
  public boolean legendary() { return tags.contains("Legendary") || tags.contains("Mythical"); }
  public boolean restricted() { return tags.contains("RestrictedLegendary") || tags.contains("Mythical"); }
  public boolean isBaseForm() { return !mega && !primal && !gmax && !battleOnly && !cosmetic; }
  public String toString() { return name; }
}
