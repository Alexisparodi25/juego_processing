// Prueba de interfaz: arranca el sketch real (bajo Xvfb), pulsa teclas y guarda capturas.
import pokefuego.engine.*;
import processing.core.*;
import processing.event.KeyEvent;
import java.io.File;

public class UiTest {
  static Sketch s;
  static String outDir;

  static void press(char c, int code) throws Exception {
    s.postEvent(new KeyEvent(null, System.currentTimeMillis(), KeyEvent.PRESS, 0, c, code));
    Thread.sleep(120);
  }
  static void ok() throws Exception { press('z', 90); }
  static void back() throws Exception { press('x', 88); }
  static void down() throws Exception { press((char) PConstants.CODED, PConstants.DOWN); }
  static void up() throws Exception { press((char) PConstants.CODED, PConstants.UP); }
  static void right() throws Exception { press((char) PConstants.CODED, PConstants.RIGHT); }
  static void left() throws Exception { press((char) PConstants.CODED, PConstants.LEFT); }
  static void ch(char c) throws Exception { press(c, 0); }
  static void shot(String n) throws Exception { Thread.sleep(250); s.save(outDir + "/" + n + ".png"); System.out.println("shot " + n); }
  static void okN(int n) throws Exception { for (int i = 0; i < n; i++) { ok(); Thread.sleep(60); } }

  public static void main(String[] a) throws Exception {
    outDir = a[0];
    new File(outDir).mkdirs();
    s = new Sketch();
    PApplet.runSketch(new String[]{"--sketch-path=" + a[1], "Sketch"}, s);
    Thread.sleep(3500);
    shot("01_title");
    ok(); shot("02_newgame");
    right(); down(); right(); shot("02b_newgame_modes"); right(); right();
    down(); down(); ok(); Thread.sleep(300); shot("03_starter");
    ok(); Thread.sleep(200); ok(); Thread.sleep(300);
    okN(4); shot("04_map");
    // combate salvaje
    ok(); Thread.sleep(500);
    okN(3); shot("05_battle_intro");
    for (int i = 0; i < 14; i++) { ok(); Thread.sleep(80); }
    shot("06_battle_menu");
    ok(); shot("07_battle_moves");
    ok(); Thread.sleep(300);
    for (int i = 0; i < 30; i++) { ok(); Thread.sleep(70); }
    shot("08_battle_after");
    for (int i = 0; i < 40; i++) { ok(); Thread.sleep(70); }
    shot("09_battle_later");
    System.out.println("done");
    System.exit(0);
  }
}
