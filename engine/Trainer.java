package pokefuego.engine;
import java.util.*;

public class Trainer {
  public String name, cls;
  public List<TMon> team = new ArrayList<TMon>();
  public int reward; public String badge = "";
  public boolean boss, rival, done;
  public String intro = "";
  public String key = "";
  public Trainer(String cls, String name) { this.cls = cls; this.name = name; }
  public String full() { return cls.isEmpty() ? name : cls + " " + name; }
  public int maxLevel() { int m = 0; for (TMon t : team) m = Math.max(m, t.level); return m; }
}
