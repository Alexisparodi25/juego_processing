// Segunda prueba de interfaz: monta una partida avanzada y captura menús y combates con Mega y Dinamax.
import pokefuego.engine.*;
import processing.core.*;
import processing.event.KeyEvent;
import java.io.File;
import java.util.*;

public class UiTest2 {
  static Sketch s; static String outDir;
  static void press(char c, int code) throws Exception { s.postEvent(new KeyEvent(null, System.currentTimeMillis(), KeyEvent.PRESS, 0, c, code)); Thread.sleep(110); }
  static void ok() throws Exception { press('z', 90); }
  static void down() throws Exception { press((char) PConstants.CODED, PConstants.DOWN); }
  static void right() throws Exception { press((char) PConstants.CODED, PConstants.RIGHT); }
  static void ch(char c) throws Exception { press(c, 0); }
  static void shot(String n) throws Exception { Thread.sleep(300); s.noLoop(); Thread.sleep(250); s.save(outDir + "/" + n + ".png"); s.loop(); System.out.println("shot " + n); }
  static Mon mk(Game g, String sp, int lv, String item) {
    Mon m = Mon.create(Dex.sp(sp), lv, g.rng); m.setMoves(MoveGen.smartSet(m.normalSp, lv, g.rng)); m.item = item; m.checkItemForme(); return m;
  }
  public static void main(String[] a) throws Exception {
    outDir = a[0]; new File(outDir).mkdirs();
    s = new Sketch();
    PApplet.runSketch(new String[]{"--sketch-path=" + a[1], "Sketch"}, s);
    Thread.sleep(3500);
    Rules r = new Rules();
    Game g = new Game(r, 42);
    g.chooseStarter("charmander");
    g.party.clear();
    g.party.add(mk(g, "charizard", 36, "charizarditey"));
    g.party.add(mk(g, "gengar", 34, "gengarite"));
    g.party.add(mk(g, "lapras", 33, "leftovers"));
    g.party.add(mk(g, "garchomp", 35, "lifeorb"));
    g.party.add(mk(g, "pikachu", 30, "lightball"));
    g.party.get(4).hp = 10; g.party.get(4).status = "par";
    g.box.add(mk(g, "mewtwo", 40, "")); g.box.add(mk(g, "eevee", 12, ""));
    g.badges = 3; g.money = 18500; g.placeIdx = 11;
    for (String id : new String[]{"potion", "superpotion", "revive", "pokeball", "greatball", "ultraball", "firestone", "rarecandy", "xattack", "leftovers", "choicescarf", "venusaurite", "blastoisinite", "rayquazamega".substring(0,0) + "salamencite", "redorb", "sitrusberry"}) g.bag.add(id, 2);
    s.game = g; s.stack.clear();
    s.pushScene(s.new MapScene()); shot("20_map");
    s.pushScene(s.new PartyScene(0, null, null)); shot("21_party");
    s.pushScene(s.new SummaryScene(g.party.get(0))); shot("22_summary"); right(); shot("23_summary_moves");
    s.stack.clear(); s.pushScene(s.new MapScene());
    s.pushScene(s.new BagScene(false, null)); shot("24_bag"); right(); right(); right(); shot("25_bag_stones"); right(); shot("25b_bag_mega");
    s.stack.clear(); s.pushScene(s.new MapScene());
    s.pushScene(s.new ShopScene(2)); shot("26_shop_all");
    s.stack.clear(); s.pushScene(s.new MapScene()); s.pushScene(s.new BoxScene()); shot("27_box");
    s.stack.clear(); s.pushScene(s.new MapScene()); s.pushScene(s.new DexScene()); for (int i = 0; i < 30; i++) down(); shot("28_dex");
    // combate contra un jefe con Mega
    s.stack.clear(); s.pushScene(s.new MapScene());
    Trainer boss = g.nextBoss();
    s.pushScene(s.new BattleScene(g.trainerBattle(boss)));
    for (int i = 0; i < 12; i++) { ok(); Thread.sleep(60); }
    shot("30_battle_menu");
    ch('m'); shot("31_battle_mega_armed"); ok(); shot("32_battle_moves");
    ok(); Thread.sleep(600);
    for (int i = 0; i < 6; i++) { ok(); Thread.sleep(150); } shot("33_battle_mega_active");
    for (int i = 0; i < 25; i++) { ok(); Thread.sleep(120); } shot("34_battle_after_turn");
    // segundo combate: Dinamax / Gigamax
    s.stack.clear(); s.pushScene(s.new MapScene());
    for (Mon m : g.party) m.fullHeal();
    Collections.swap(g.party, 0, 2); g.placeIdx = 6; g.badges = 1;
    s.pushScene(s.new BattleScene(g.trainerBattle(g.nextBoss())));
    for (int i = 0; i < 12; i++) { ok(); Thread.sleep(60); }
    ch('g'); Thread.sleep(200); ok(); Thread.sleep(200); shot("35_dmax_armed_moves"); ok();
    for (int i = 0; i < 8; i++) { ok(); Thread.sleep(150); } shot("36_dmax_active");
    for (int i = 0; i < 30; i++) { ok(); Thread.sleep(100); } shot("37_dmax_after");
    System.out.println("done"); System.exit(0);
  }
}
