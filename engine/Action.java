package pokefuego.engine;
import java.util.*;

public class Action {
  public static final int MOVE = 0, SWITCH = 1, ITEM = 2, RUN = 3, BALL = 4, NONE = 5;
  public int type = NONE, idx;
  public boolean mega, dyna;
  public String item = "";
  public Action(int t, int i) { type = t; idx = i; }
  public static Action move(int i, boolean mega, boolean dyna) { Action a = new Action(MOVE, i); a.mega = mega; a.dyna = dyna; return a; }
  public static Action sw(int i) { return new Action(SWITCH, i); }
  public static Action item(String it, int target) { Action a = new Action(ITEM, target); a.item = it; return a; }
  public static Action ball(String it) { Action a = new Action(BALL, 0); a.item = it; return a; }
  public static Action run() { return new Action(RUN, 0); }
}
