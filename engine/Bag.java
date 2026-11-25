package pokefuego.engine;
import java.util.*;

public class Bag {
  public Map<String, Integer> counts = new LinkedHashMap<String, Integer>();
  public void add(String id, int n) { id = Dex.toId(id); counts.put(id, count(id) + n); }
  public int count(String id) { Integer c = counts.get(Dex.toId(id)); return c == null ? 0 : c; }
  public boolean remove(String id, int n) {
    id = Dex.toId(id);
    int c = count(id);
    if (c < n) return false;
    if (c == n) counts.remove(id); else counts.put(id, c - n);
    return true;
  }
  public List<String> ids() { return new ArrayList<String>(counts.keySet()); }
}
