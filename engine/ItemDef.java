package pokefuego.engine;
import java.util.*;

public class ItemDef {
  public String id, name, desc, megaStone = "", megaEvolves = "", plateType = "", memoryType = "", driveType = "", ngType = "";
  public String[] users = new String[0];
  public int gen, fling, ngBP;
  public boolean berry, ball, choice;
  public String es = "", descEs = "";
  public String dn() { return es.isEmpty() ? name : es; }
  public String dd() { return descEs.isEmpty() ? desc : descEs; }
  public String toString() { return name; }
}
