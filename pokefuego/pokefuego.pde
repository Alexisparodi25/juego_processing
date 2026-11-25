// =====================================================================
//  POKÉMON ROJO FUEGO — EDICIÓN TOTAL
//  Juego hecho en Processing (modo Java). Ver README.md
//
//  - Recorrido por Kanto: 8 gimnasios, Alto Mando y Campeón
//  - TODAS las especies/formas, megaevoluciones, movimientos,
//    habilidades y objetos (datos en data/*.tsv)
//  - Únicas mecánicas especiales: MEGAEVOLUCIÓN y DINAMAX/GIGAMAX
//    (duran TODO el combate, como la mega)
//  - Modos: Normal, Aleatorio, Nuzlocke (y combinados). Siempre DIFÍCIL.
//
//  Controles:  Flechas / WASD mover   ·   Z / Enter / Espacio aceptar
//              X / Retroceso / Esc volver   ·   en combate: M = Mega, G = Dinamax/Gigamax
// =====================================================================

import pokefuego.engine.*;

// ---- constantes de teclas lógicas ----
final int K_UP = 1, K_DOWN = 2, K_LEFT = 3, K_RIGHT = 4, K_OK = 5, K_BACK = 6, K_CHAR = 7;

// ---- estado global ----
Game game;
ArrayList<Scene> stack = new ArrayList<Scene>();
PFont fNormal, fSmall, fBig, fTitle;
HashMap<String, PImage> spriteCache = new HashMap<String, PImage>();
HashSet<String> spriteMissing = new HashSet<String>();
int tick = 0;
String toast = "";
int toastTime = 0;
boolean dataOk = false;
String loadError = "";

interface Cb { void run(); }

abstract class Scene {
  void enter() {}
  void draw() {}
  void key(int k, char c) {}
  boolean opaque() { return true; }
}

void settings() {
  size(960, 640);
}

void setup() {
  frameRate(60);
  fNormal = createFont("SansSerif", 20, true);
  fSmall = createFont("SansSerif", 15, true);
  fBig = createFont("SansSerif Bold", 30, true);
  fTitle = createFont("SansSerif Bold", 64, true);
  try {
    Dex.load(new DataLoader() {
      public String[] lines(String f) {
        String[] l = loadStrings("data/" + f);
        if (l == null) throw new RuntimeException("Falta el archivo data/" + f + " (ejecuta tools/export_data.js)");
        return l;
      }
    });
    dataOk = true;
  } catch (Throwable t) {
    loadError = String.valueOf(t.getMessage());
    t.printStackTrace();
  }
  if (dataOk) pushScene(new TitleScene());
}

void draw() {
  tick++;
  background(16, 20, 32);
  if (!dataOk) {
    fill(255, 120, 120); textFont(fNormal); textAlign(LEFT, TOP);
    text("No se pudieron cargar los datos:\n" + loadError, 40, 40);
    return;
  }
  if (stack.isEmpty()) return;
  // dibujar desde la última escena opaca hacia arriba
  int base = stack.size() - 1;
  while (base > 0 && !stack.get(base).opaque()) base--;
  for (int i = base; i < stack.size(); i++) stack.get(i).draw();
  if (toastTime > 0) {
    toastTime--;
    textFont(fNormal); textAlign(CENTER, CENTER);
    fill(0, 200); noStroke(); rect(width / 2 - 260, 8, 520, 34, 8);
    fill(255, 230, 120); text(toast, width / 2, 25);
  }
}

void say(String s) { toast = s; toastTime = 150; }

void pushScene(Scene s) { stack.add(s); s.enter(); }
void popScene() { if (!stack.isEmpty()) stack.remove(stack.size() - 1); }
void popTo(Scene s) { while (!stack.isEmpty() && stack.get(stack.size() - 1) != s) stack.remove(stack.size() - 1); }

void keyPressed() {
  int k = 0; char c = key;
  if (key == CODED) {
    if (keyCode == UP) k = K_UP; else if (keyCode == DOWN) k = K_DOWN; else if (keyCode == LEFT) k = K_LEFT; else if (keyCode == RIGHT) k = K_RIGHT;
  } else {
    if (key == ESC) { key = 0; k = K_BACK; }
    else if (key == ENTER || key == RETURN || key == ' ' || key == 'z' || key == 'Z') k = K_OK;
    else if (key == BACKSPACE || key == 'x' || key == 'X') k = K_BACK;
    else if (key == 'w' || key == 'W') k = K_UP;
    else if (key == 's' || key == 'S') k = K_DOWN;
    else if (key == 'a' || key == 'A') k = K_LEFT;
    else if (key == 'd' || key == 'D') k = K_RIGHT;
    else k = K_CHAR;
  }
  if (!stack.isEmpty()) {
    stack.get(stack.size() - 1).key(k, c);
  }
}

// =====================================================================
//  utilidades de dibujo
// =====================================================================
int typeColor(String t) {
  if (t == null) return color(120);
  if (t.equals("Normal")) return color(168, 168, 120);
  if (t.equals("Fire")) return color(240, 128, 48);
  if (t.equals("Water")) return color(104, 144, 240);
  if (t.equals("Electric")) return color(248, 208, 48);
  if (t.equals("Grass")) return color(120, 200, 80);
  if (t.equals("Ice")) return color(152, 216, 216);
  if (t.equals("Fighting")) return color(192, 48, 40);
  if (t.equals("Poison")) return color(160, 64, 160);
  if (t.equals("Ground")) return color(224, 192, 104);
  if (t.equals("Flying")) return color(168, 144, 240);
  if (t.equals("Psychic")) return color(248, 88, 136);
  if (t.equals("Bug")) return color(168, 184, 32);
  if (t.equals("Rock")) return color(184, 160, 56);
  if (t.equals("Ghost")) return color(112, 88, 152);
  if (t.equals("Dragon")) return color(112, 56, 248);
  if (t.equals("Dark")) return color(112, 88, 72);
  if (t.equals("Steel")) return color(184, 184, 208);
  if (t.equals("Fairy")) return color(238, 153, 172);
  return color(120);
}

String typeEs(String t) {
  if (t == null || t.isEmpty()) return "";
  String[][] m = {{"Normal","Normal"},{"Fire","Fuego"},{"Water","Agua"},{"Electric","Eléctrico"},{"Grass","Planta"},{"Ice","Hielo"},{"Fighting","Lucha"},
    {"Poison","Veneno"},{"Ground","Tierra"},{"Flying","Volador"},{"Psychic","Psíquico"},{"Bug","Bicho"},{"Rock","Roca"},{"Ghost","Fantasma"},
    {"Dragon","Dragón"},{"Dark","Siniestro"},{"Steel","Acero"},{"Fairy","Hada"}};
  for (int i = 0; i < m.length; i++) if (m[i][0].equals(t)) return m[i][1];
  return t;
}

void panel(float x, float y, float w, float h) {
  stroke(235, 235, 245); strokeWeight(3); fill(24, 30, 52, 245);
  rect(x, y, w, h, 10);
  noStroke();
}

void panelSel(float x, float y, float w, float h, boolean sel) {
  noStroke();
  if (sel) { fill(255, 205, 70, 60); stroke(255, 205, 70); strokeWeight(2); } else { fill(255, 255, 255, 10); noStroke(); }
  rect(x, y, w, h, 6);
  noStroke();
}

void txt(String s, float x, float y, PFont f, int c) {
  textFont(f); fill(c); textAlign(LEFT, TOP); text(s, x, y);
}
void txt(String s, float x, float y) { txt(s, x, y, fNormal, color(240)); }
void txtc(String s, float x, float y, PFont f, int c) { textFont(f); fill(c); textAlign(CENTER, TOP); text(s, x, y); }
void txtr(String s, float x, float y, PFont f, int c) { textFont(f); fill(c); textAlign(RIGHT, TOP); text(s, x, y); }

void typeChip(String t, float x, float y) {
  if (t == null || t.isEmpty()) return;
  noStroke(); fill(typeColor(t));
  rect(x, y, 78, 20, 10);
  textFont(fSmall); fill(255); textAlign(CENTER, CENTER);
  text(typeEs(t), x + 39, y + 9);
}

void hpBar(float x, float y, float w, double frac) { hpBar(x, y, w, (float) frac); }
void hpBar(float x, float y, float w, float frac) {
  frac = constrain(frac, 0.0f, 1.0f);
  noStroke(); fill(40); rect(x, y, w, 10, 5);
  if (frac > 0.5f) fill(80, 210, 90); else if (frac > 0.2f) fill(240, 200, 50); else fill(230, 70, 60);
  if (frac > 0) rect(x, y, max(6.0f, w * frac), 10, 5);
}

String statusEs(String s) {
  if (s == null) return "";
  if (s.equals("brn")) return "QUE"; if (s.equals("par")) return "PAR"; if (s.equals("psn")) return "ENV";
  if (s.equals("tox")) return "TOX"; if (s.equals("slp")) return "DOR"; if (s.equals("frz")) return "CON";
  return "";
}
int statusColor(String s) {
  if (s == null) return color(120);
  if (s.equals("brn")) return color(240, 110, 40); if (s.equals("par")) return color(235, 200, 40); if (s.equals("psn") || s.equals("tox")) return color(170, 70, 190);
  if (s.equals("slp")) return color(130, 130, 150); if (s.equals("frz")) return color(120, 200, 230);
  return color(120);
}

// ---- sprites ----
PImage spriteFor(Species s, boolean back) {
  String key = (back ? "b" : "f") + s.num;
  if (spriteCache.containsKey(key)) return spriteCache.get(key);
  if (spriteMissing.contains(key)) return null;
  PImage im = null;
  try {
    File f = new File(sketchPath("data/sprites/" + (back ? "back/" : "") + s.num + ".png"));
    if (f.exists()) im = loadImage(f.getAbsolutePath());
  } catch (Exception e) { }
  if (im == null) { spriteMissing.add(key); return null; }
  spriteCache.put(key, im);
  return im;
}

/** Dibuja un Pokémon: sprite PNG si existe (data/sprites), si no una criatura procedural por tipo. */
void drawCreature(Species s, float cx, float cy, float size, boolean back, boolean mega, boolean dyna, float alpha) {
  PImage im = spriteFor(s, back);
  float sc = dyna ? 1.35f : 1.0f;
  pushMatrix();
  translate(cx, cy);
  if (dyna) {
    noStroke();
    for (int i = 0; i < 4; i++) { fill(255, 40 + i * 10, 80, 30); ellipse(0, 0, size * (1.9f - i * 0.18f), size * (1.9f - i * 0.18f)); }
  }
  if (mega) {
    noStroke();
    float pulse = 0.5f + 0.5f * sin(tick * 0.1f);
    fill(255, 220, 90, 50 + 40 * pulse); ellipse(0, 0, size * 1.8f, size * 1.8f);
    stroke(255, 240, 150, 160); strokeWeight(2); noFill();
    for (int i = 0; i < 6; i++) { float a = i * TWO_PI / 6 + tick * 0.02f; line(cos(a) * size * 0.55f, sin(a) * size * 0.55f, cos(a) * size * 0.85f, sin(a) * size * 0.85f); }
    noStroke();
  }
  scale(sc);
  if (im != null) {
    tint(255, alpha);
    float k = size / max(im.width, im.height) * 1.9f;
    imageMode(CENTER); image(im, 0, 0, im.width * k, im.height * k); imageMode(CORNER);
    noTint();
  } else {
    int c1 = typeColor(s.t1), c2 = s.t2.isEmpty() ? lerpColor(c1, color(255), 0.35f) : typeColor(s.t2);
    int h = abs(s.num * 31 + s.name.length() * 7);
    float w = size * (0.75f + (h % 5) * 0.06f), hh = size * (0.7f + ((h / 5) % 5) * 0.07f);
    noStroke();
    fill(red(c1), green(c1), blue(c1), alpha);
    ellipse(0, 0, w, hh);
    fill(red(c2), green(c2), blue(c2), alpha);
    // orejas / apéndices según hash
    int shape = h % 4;
    if (shape == 0) { triangle(-w * 0.35f, -hh * 0.3f, -w * 0.15f, -hh * 0.75f, 0, -hh * 0.42f); triangle(w * 0.35f, -hh * 0.3f, w * 0.15f, -hh * 0.75f, 0, -hh * 0.42f); }
    else if (shape == 1) { ellipse(-w * 0.38f, -hh * 0.38f, w * 0.28f, w * 0.28f); ellipse(w * 0.38f, -hh * 0.38f, w * 0.28f, w * 0.28f); }
    else if (shape == 2) { rect(-w * 0.08f, -hh * 0.75f, w * 0.16f, hh * 0.4f, 6); }
    else { ellipse(w * 0.45f, hh * 0.1f, w * 0.4f, hh * 0.18f); }
    ellipse(0, hh * 0.18f, w * 0.62f, hh * 0.4f);
    fill(255, alpha); ellipse(-w * 0.17f, -hh * 0.08f, w * 0.2f, w * 0.2f); ellipse(w * 0.17f, -hh * 0.08f, w * 0.2f, w * 0.2f);
    fill(20, alpha); ellipse(-w * 0.15f + (back ? 0 : 0), -hh * 0.08f, w * 0.09f, w * 0.09f); ellipse(w * 0.19f, -hh * 0.08f, w * 0.09f, w * 0.09f);
    if (back) { fill(red(c1) * 0.7f, green(c1) * 0.7f, blue(c1) * 0.7f, alpha); ellipse(0, -hh * 0.05f, w * 0.7f, hh * 0.65f); }
    fill(255, 255, 255, 170 * alpha / 255.0f); textFont(fSmall); textAlign(CENTER, CENTER);
    text("#" + s.num, 0, hh * 0.42f);
  }
  popMatrix();
}

void drawCreatureSmall(Species s, float cx, float cy, float size) {
  drawCreature(s, cx, cy, size, false, false, false, 255.0f);
}

// ---- menús genéricos ----
class ListMenu {
  ArrayList<String> items = new ArrayList<String>();
  int sel = 0, top = 0, visible = 8;
  void set(ArrayList<String> l) { items = l; sel = constrain(sel, 0, max(0, items.size() - 1)); }
  boolean move(int k) {
    if (items.isEmpty()) return false;
    if (k == K_UP) sel = (sel + items.size() - 1) % items.size();
    else if (k == K_DOWN) sel = (sel + 1) % items.size();
    else return false;
    if (sel < top) top = sel;
    if (sel >= top + visible) top = sel - visible + 1;
    if (sel == 0) top = 0;
    return true;
  }
  void draw(float x, float y, float w, float lineH) {
    for (int i = 0; i < visible && top + i < items.size(); i++) {
      int idx = top + i;
      panelSel(x, y + i * lineH, w, lineH - 2, idx == sel);
      txt(items.get(idx), x + 12, y + i * lineH + 4, fNormal, idx == sel ? color(255, 230, 140) : color(235));
    }
    if (top > 0) { fill(255, 220, 120); triangle(x + w - 20, y - 4, x + w - 10, y - 4, x + w - 15, y - 12); }
    if (top + visible < items.size()) { fill(255, 220, 120); triangle(x + w - 20, y + visible * lineH + 2, x + w - 10, y + visible * lineH + 2, x + w - 15, y + visible * lineH + 10); }
  }
}

// =====================================================================
//  Mensajes
// =====================================================================
class MsgScene extends Scene {
  ArrayList<String> lines = new ArrayList<String>();
  int idx = 0, chars = 0;
  Cb done;
  boolean transparent = true;
  MsgScene(String[] ls, Cb done) { for (String s : ls) lines.add(s); this.done = done; }
  MsgScene(ArrayList<String> ls, Cb done) { lines.addAll(ls); this.done = done; }
  boolean opaque() { return false; }
  void draw() {
    panel(30, height - 150, width - 60, 130);
    String s = lines.isEmpty() ? "" : lines.get(idx);
    chars = min(s.length(), chars + 2);
    txt(s.substring(0, chars), 56, height - 128, fNormal, color(245));
    if (chars >= s.length() && (tick / 20) % 2 == 0) { fill(255, 220, 120); triangle(width - 70, height - 52, width - 50, height - 52, width - 60, height - 40); }
  }
  void key(int k, char c) {
    if (k == K_OK || k == K_BACK) {
      String s = lines.isEmpty() ? "" : lines.get(idx);
      if (chars < s.length()) { chars = s.length(); return; }
      idx++; chars = 0;
      if (idx >= lines.size()) { popScene(); if (done != null) done.run(); }
    }
  }
}

void msg(String s, Cb cb) { pushScene(new MsgScene(new String[]{s}, cb)); }
void msgs(ArrayList<String> l, Cb cb) { if (l.isEmpty()) { if (cb != null) cb.run(); return; } pushScene(new MsgScene(l, cb)); }

class ConfirmScene extends Scene {
  String q; Cb yes, no; int sel = 0;
  ConfirmScene(String q, Cb yes, Cb no) { this.q = q; this.yes = yes; this.no = no; }
  boolean opaque() { return false; }
  void draw() {
    panel(30, height - 150, width - 60, 130);
    txt(q, 56, height - 130, fNormal, color(245));
    panel(width - 220, height - 280, 190, 110);
    panelSel(width - 210, height - 270, 170, 40, sel == 0); txt("Sí", width - 190, height - 264);
    panelSel(width - 210, height - 226, 170, 40, sel == 1); txt("No", width - 190, height - 220);
  }
  void key(int k, char c) {
    if (k == K_UP || k == K_DOWN) sel = 1 - sel;
    else if (k == K_OK) { popScene(); if (sel == 0) { if (yes != null) yes.run(); } else if (no != null) no.run(); }
    else if (k == K_BACK) { popScene(); if (no != null) no.run(); }
  }
}

class ChoiceScene extends Scene {
  String title; ArrayList<String> opts = new ArrayList<String>(); Cb[] cbs; ListMenu m = new ListMenu();
  ChoiceScene(String title, String[] o, Cb[] c) { this.title = title; for (String s : o) opts.add(s); cbs = c; m.set(opts); m.visible = min(8, opts.size()); }
  boolean opaque() { return false; }
  void draw() {
    float h = 70 + m.visible * 36;
    float x = width - 330, y = height - 160 - h;
    panel(x, y, 300, h);
    txt(title, x + 16, y + 10, fSmall, color(255, 220, 120));
    m.draw(x + 10, y + 40, 280, 36);
  }
  void key(int k, char c) {
    if (m.move(k)) return;
    if (k == K_OK) { popScene(); cbs[m.sel].run(); }
    else if (k == K_BACK) popScene();
  }
}
