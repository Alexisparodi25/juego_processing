// Prueba determinista: espera al menú de combate, activa Mega / Dinamax con M / G y verifica el estado del motor.
import pokefuego.engine.*;
import processing.core.*;
import processing.event.KeyEvent;
import java.io.File;
import java.util.*;

public class UiTest3 {
  static Sketch s; static String outDir;
  static void press(char c, int code) throws Exception { s.postEvent(new KeyEvent(null, System.currentTimeMillis(), KeyEvent.PRESS, 0, c, code)); Thread.sleep(100); }
  static void ok() throws Exception { press('z', 90); }
  static void ch(char c) throws Exception { press(c, 0); }
  static void shot(String n) throws Exception { Thread.sleep(300); s.noLoop(); Thread.sleep(250); s.save(outDir + "/" + n + ".png"); s.loop(); System.out.println("shot " + n); }
  static Sketch.BattleScene bs() { return (Sketch.BattleScene) s.stack.get(s.stack.size() - 1); }
  static void waitPhase(int ph) throws Exception {
    for (int i = 0; i < 600; i++) {
      Scene top = null;
      if (s.stack.get(s.stack.size() - 1) instanceof Sketch.BattleScene) { Sketch.BattleScene b = bs(); if (b.phase == ph) return; if (b.phase == 0) ok(); }
      Thread.sleep(50);
    }
    throw new RuntimeException("timeout esperando fase " + ph);
  }
  interface Scene {}
  static Mon mk(Game g, String sp, int lv, String item) { Mon m = Mon.create(Dex.sp(sp), lv, g.rng); m.setMoves(MoveGen.smartSet(m.normalSp, lv, g.rng)); m.item = item; m.checkItemForme(); return m; }

  static void battleWith(Game g, Mon lead, boolean mega, String shotName) throws Exception {
    g.party.clear(); g.party.add(lead); g.party.add(mk(g, "pikachu", 20, ""));
    for (Mon m : g.party) m.fullHeal();
    g.placeIdx = 4; g.badges = 0;
    s.stack.clear(); s.pushScene(s.new MapScene());
    s.pushScene(s.new BattleScene(g.trainerBattle(g.nextBoss())));
    waitPhase(1);
    Sketch.BattleScene b = bs();
    System.out.println((mega ? "MEGA" : "DMAX") + " disponible: mega=" + b.b.canMega(b.b.sides[0]) + " dyna=" + b.b.canDynamax(b.b.sides[0]));
    ch(mega ? 'm' : 'g'); Thread.sleep(150);
    System.out.println("armado: wantMega=" + b.wantMega + " wantDyna=" + b.wantDyna);
    ok(); Thread.sleep(150); shot(shotName + "_moves");     // lista de movimientos (muestra los Max si Dinamax)
    ok();                                                   // lanzar el primer movimiento
    Thread.sleep(2500);
    for (int i = 0; i < 10; i++) { ok(); Thread.sleep(120); }
    Mon u = b.b.sides[0].active;
    System.out.println("resultado: megaed=" + u.megaed + " dynamaxed=" + u.dynamaxed + " forma=" + u.sp.name + " hp=" + u.hp + "/" + u.maxHp);
    shot(shotName + "_active");
  }

  public static void main(String[] a) throws Exception {
    outDir = a[0]; new File(outDir).mkdirs();
    s = new Sketch();
    PApplet.runSketch(new String[]{"--sketch-path=" + a[1], "Sketch"}, s);
    Thread.sleep(3500);
    Game g = new Game(new Rules(), 77); g.chooseStarter("charmander");
    battleWith(g, mk(g, "charizard", 50, "charizarditey"), true, "40_mega");
    battleWith(g, mk(g, "lapras", 40, "leftovers"), false, "41_dmax");
    System.out.println("done"); System.exit(0);
  }
}
