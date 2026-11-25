package pokefuego.engine;
import java.util.*;

public class BEvent {
  public String msg = "";
  public String[] name = new String[2], status = new String[2], spId = new String[2];
  public int[] hp = new int[2], maxHp = new int[2], level = new int[2];
  public boolean[] dyna = new boolean[2], mega = new boolean[2];
  public int hitSide = -1;       // lado que recibió un golpe (animación)
  public String anim = "";       // "faint0", "faint1", "send0"...
  public String weather = "", terrain = "";
}
