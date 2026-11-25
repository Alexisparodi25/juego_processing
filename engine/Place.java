package pokefuego.engine;
import java.util.*;

public class Place {
  public String name, desc;
  public int minLv, maxLv;
  public String[] theme;           // tipos que aparecen en la hierba
  public boolean center, shop, bigShop, megaShop;
  public List<Trainer> trainers = new ArrayList<Trainer>();
  public Trainer boss;             // líder / rival / alto mando (hay que vencerlo para avanzar)
  public List<Trainer> bossChain = new ArrayList<Trainer>();   // Alto Mando: se enfrentan en orden
  public int bossStep;
  public boolean encounterUsed;    // Nuzlocke: primera captura por zona
  public boolean encounterFailed;
  public int mapX, mapY;
  public Place(String n, String d, int lo, int hi, String... theme) { name = n; desc = d; minLv = lo; maxLv = hi; this.theme = theme; }
}
