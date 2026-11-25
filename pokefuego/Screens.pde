// =====================================================================
//  Pantallas: título, nueva partida, mapa de Kanto, equipo, mochila,
//  tienda, cajas, Pokédex, resumen, aprender movimiento, evolución
// =====================================================================

interface IntCb { void run(int i); }
interface StrCb { void run(String s); }

String abEs(String n) { AbilityDef a = Dex.ab(n); return a == null ? n : a.dn(); }

boolean hasSave() { return new File(sketchPath("save.txt")).exists(); }

void saveGame() {
  try {
    ArrayList<String> l = new ArrayList<String>(game.serialize());
    saveStrings(sketchPath("save.txt"), l.toArray(new String[0]));
    say("Partida guardada.");
  } catch (Throwable t) { say("Error al guardar: " + t.getMessage()); }
}

boolean loadGame() {
  try {
    String[] l = loadStrings(sketchPath("save.txt"));
    if (l == null) return false;
    ArrayList<String> a = new ArrayList<String>();
    for (String s : l) a.add(s);
    Game g = Game.deserialize(a);
    if (g == null) return false;
    game = g;
    return true;
  } catch (Throwable t) { t.printStackTrace(); return false; }
}

// =====================================================================
//  TÍTULO
// =====================================================================
class TitleScene extends Scene {
  ListMenu m = new ListMenu();
  TitleScene() { refresh(); }
  void refresh() {
    ArrayList<String> l = new ArrayList<String>();
    l.add("Nueva partida");
    if (hasSave()) l.add("Continuar");
    l.add("Salir");
    m.set(l); m.visible = l.size();
  }
  void draw() {
    noStroke();
    for (int i = 0; i < height; i += 4) { fill(lerpColor(color(60, 8, 8), color(250, 120, 20), i / (float) height)); rect(0, i, width, 4); }
    for (int i = 0; i < 40; i++) {
      float x = (i * 97 + tick * (0.6f + (i % 5) * 0.3f)) % (width + 60) - 30;
      float y = height - ((tick * (0.4f + (i % 4) * 0.25f) + i * 53) % (height + 40));
      fill(255, 200 - (i % 5) * 30, 40, 120); ellipse(x, y, 6 + i % 7, 6 + i % 7);
    }
    txtc("POKÉMON", width / 2, 70, fTitle, color(255, 240, 160));
    txtc("ROJO FUEGO", width / 2, 135, fTitle, color(255, 255, 255));
    txtc("EDICIÓN TOTAL", width / 2, 215, fBig, color(255, 220, 120));
    txtc("Todas las especies · Todas las megas · Dinamax · Aleatorio · Nuzlocke · Difícil", width / 2, 262, fSmall, color(255, 240, 220));
    panel(width / 2 - 170, 340, 340, 40 + m.items.size() * 40);
    m.draw(width / 2 - 160, 352, 320, 40);
    txtc(Dex.species.size() + " especies/formas · " + Dex.moves.size() + " movimientos · " + Dex.abilities.size() + " habilidades · " + Dex.items.size() + " objetos", width / 2, height - 60, fSmall, color(255, 230, 200));
  }
  void key(int k, char c) {
    if (m.move(k)) return;
    if (k == K_OK) {
      String s = m.items.get(m.sel);
      if (s.equals("Nueva partida")) pushScene(new NewGameScene());
      else if (s.equals("Continuar")) {
        if (loadGame()) { stack.clear(); pushScene(new MapScene()); say("Partida cargada."); } else say("No se pudo cargar la partida.");
      } else exit();
    }
  }
}

// =====================================================================
//  NUEVA PARTIDA
// =====================================================================
class NewGameScene extends Scene {
  boolean random = false, nuz = false;
  int sel = 0;
  void draw() {
    background(16, 20, 32);
    txtc("NUEVA PARTIDA", width / 2, 40, fBig, color(255, 220, 120));
    String[] names = {"Modo Aleatorio", "Modo Nuzlocke", "Dificultad", "¡Comenzar!"};
    boolean[] on = {random, nuz, true, false};
    for (int i = 0; i < 4; i++) {
      float y = 110 + i * 62;
      panelSel(150, y, 660, 54, sel == i);
      txt(names[i], 175, y + 12, fNormal, sel == i ? color(255, 230, 140) : color(235));
      if (i < 2) txtr(on[i] ? "[X] SÍ" : "[  ] NO", 790, y + 12, fNormal, on[i] ? color(120, 230, 140) : color(180));
      if (i == 2) txtr("DIFÍCIL (fija)", 790, y + 12, fNormal, color(255, 120, 100));
    }
    panel(150, 370, 660, 220);
    String[] d = {
      "ALEATORIO: especies salvajes, equipos de entrenadores y los 3 iniciales son aleatorios (todas las generaciones).",
      "NUZLOCKE: sólo capturas el primer Pokémon de cada zona; si se debilita, muere para siempre. Si todo tu equipo cae, fin.",
      "DIFÍCIL: IA que calcula daño real y cambia a counters, jefes con Mega/Dinamax, EVs en entrenadores, tope de nivel por jefe.",
      "Mecánicas: SÓLO Megaevolución y Dinamax/Gigamax (duran todo el combate; una por combate y bando)."};
    textFont(fSmall); textAlign(LEFT, TOP);
    fill(235); text(d[sel], 175, 390, 610, 80);
    if (sel < 3) { fill(255, 220, 140); text(d[3], 175, 475, 610, 60); }
    txt("Z/Enter: cambiar · X: volver", 175, 560, fSmall, color(170));
  }
  void key(int k, char c) {
    if (k == K_UP) sel = (sel + 3) % 4;
    else if (k == K_DOWN) sel = (sel + 1) % 4;
    else if (k == K_BACK) popScene();
    else if (k == K_OK || k == K_LEFT || k == K_RIGHT) {
      if (sel == 0) random = !random;
      else if (sel == 1) nuz = !nuz;
      else if (sel == 3 && k == K_OK) {
        Rules r = new Rules(); r.random = random; r.nuzlocke = nuz;
        game = new Game(r, System.nanoTime());
        popScene(); pushScene(new StarterScene());
      }
    }
  }
}

// =====================================================================
//  ELEGIR INICIAL
// =====================================================================
class StarterScene extends Scene {
  ArrayList<String> ch; int sel = 0;
  StarterScene() { ch = new ArrayList<String>(game.starterChoices()); }
  void draw() {
    txtc("¡Elige a tu primer Pokémon!", width / 2, 40, fBig, color(255, 220, 120));
    if (game.rules.random) txtc("(modo aleatorio: tres iniciales al azar)", width / 2, 85, fSmall, color(200));
    for (int i = 0; i < 3; i++) {
      Species s = Dex.sp(ch.get(i));
      float x = 170 + i * 310;
      panel(x - 130, 130, 260, 360);
      if (sel == i) { noFill(); stroke(255, 205, 70); strokeWeight(4); rect(x - 134, 126, 268, 368, 12); noStroke(); }
      drawCreature(s, x, 260, 140, false, false, false, 255.0f);
      txtc(s.name, x, 340, fBig, color(255));
      typeChip(s.t1, x - (s.t2.isEmpty() ? 39 : 82), 385);
      if (!s.t2.isEmpty()) typeChip(s.t2, x + 4, 385);
      txtc("Hab: " + abEs(s.abilities[0]), x, 420, fSmall, color(220));
      txtc("Total base: " + s.bst(), x, 445, fSmall, color(200));
    }
    txtc("←/→ elegir · Z aceptar", width / 2, 560, fSmall, color(180));
  }
  void key(int k, char c) {
    if (k == K_LEFT) sel = (sel + 2) % 3;
    else if (k == K_RIGHT) sel = (sel + 1) % 3;
    else if (k == K_BACK) popScene();
    else if (k == K_OK) {
      final String id = ch.get(sel);
      pushScene(new ConfirmScene("¿Quieres a " + Dex.sp(id).name + " como tu inicial?", new Cb() { public void run() {
        game.chooseStarter(id);
        stack.clear();
        pushScene(new MapScene());
        ArrayList<String> l = new ArrayList<String>();
        l.add("¡" + Dex.sp(id).name + " es ahora tu compañero!");
        l.add("Tu rival Azul te desafía. ¡Ten cuidado: este viaje es DIFÍCIL!");
        l.add("Recuerda: sólo existen la Megaevolución (con piedra) y el Dinamax. Duran todo el combate.");
        msgs(l, null);
      } }, null));
    }
  }
}

// =====================================================================
//  MAPA DE KANTO (menú principal)
// =====================================================================
class MapScene extends Scene {
  ArrayList<String> labels = new ArrayList<String>(), ids = new ArrayList<String>();
  ListMenu m = new ListMenu();
  void enter() { m.visible = 9; }
  void refresh() {
    labels.clear(); ids.clear();
    Place p = game.place();
    labels.add("Hierba alta (combate salvaje)"); ids.add("wild");
    int pend = 0; for (Trainer t : p.trainers) if (!t.done) pend++;
    if (!p.trainers.isEmpty()) { labels.add("Entrenadores (" + pend + " pendientes)"); ids.add("trainers"); }
    Trainer b = game.nextBoss();
    if (b != null) { labels.add("★ Desafiar a " + b.full()); ids.add("boss"); }
    if (p.center) { labels.add("Centro Pokémon (curar)"); ids.add("heal"); }
    if (p.shop) { labels.add("Tienda"); ids.add("shop"); }
    labels.add("Equipo"); ids.add("party");
    labels.add("Mochila"); ids.add("bag");
    labels.add("Cajas" + (game.rules.nuzlocke ? " y Cementerio" : "")); ids.add("box");
    labels.add("Pokédex"); ids.add("dex");
    labels.add("Guardar partida"); ids.add("save");
    if (game.placeIdx < game.places.size() - 1) { labels.add(game.canAdvance() ? "Avanzar → " + game.places.get(game.placeIdx + 1).name : "(Derrota al jefe para avanzar)"); ids.add("next"); }
    if (game.placeIdx > 0) { labels.add("← Volver a " + game.places.get(game.placeIdx - 1).name); ids.add("prev"); }
    labels.add("Menú principal"); ids.add("menu");
    m.set(labels);
  }
  void draw() {
    if (game.gameOver) { stack.clear(); pushScene(new TitleScene()); pushScene(new GameOverScene()); return; }
    refresh();
    background(18, 34, 60);
    // océano / mapa
    noStroke(); fill(30, 70, 110); rect(0, 0, 590, height);
    fill(60, 120, 70); rect(14, 56, 560, 514, 20);
    Place cur = game.place();
    // caminos
    stroke(200, 170, 110); strokeWeight(6);
    for (int i = 0; i + 1 < game.places.size(); i++) {
      Place a = game.places.get(i), b = game.places.get(i + 1);
      line(mx(a), my(a), mx(b), my(b));
    }
    noStroke();
    for (int i = 0; i < game.places.size(); i++) {
      Place p = game.places.get(i);
      boolean done = p.boss != null && game.allBossDone(p);
      boolean gym = p.boss != null;
      fill(i == game.placeIdx ? color(255, 220, 60) : done ? color(120, 220, 140) : i < game.placeIdx ? color(190, 200, 190) : color(120, 130, 120));
      stroke(20); strokeWeight(2);
      if (gym) rect(mx(p) - 11, my(p) - 11, 22, 22, 4); else ellipse(mx(p), my(p), 18, 18);
      noStroke();
      if (i == game.placeIdx) {
        fill(255, 80, 60); float bob = sin(tick * 0.12f) * 4;
        triangle(mx(p) - 8, my(p) - 34 + bob, mx(p) + 8, my(p) - 34 + bob, mx(p), my(p) - 20 + bob);
      }
    }
    txt("Región de Kanto", 24, 70, fSmall, color(255));
    // barra superior
    fill(0, 120); rect(0, 0, width, 40);
    txt("$" + game.money, 16, 8, fNormal, color(255, 230, 120));
    txt("Medallas: " + game.badges + "/8", 140, 8, fNormal, color(255));
    txt("Tope de nivel: " + game.levelCap(), 290, 8, fNormal, color(255, 170, 120));
    String mode = (game.rules.random ? "ALEATORIO " : "") + (game.rules.nuzlocke ? "NUZLOCKE " : "") + "DIFÍCIL";
    txtr(mode, width - 16, 8, fNormal, color(255, 120, 100));
    // panel derecho
    panel(600, 52, 350, 576);
    txt(cur.name, 616, 62, fBig, color(255, 230, 140));
    textFont(fSmall); fill(220); textAlign(LEFT, TOP); text(cur.desc, 616, 100, 318, 44);
    float yy = 150;
    txt("Nivel salvaje: " + cur.minLv + "–" + cur.maxLv, 616, yy, fSmall, color(200));
    if (!game.rules.random && cur.theme != null && cur.theme.length > 0) {
      String th = ""; for (int i = 0; i < cur.theme.length; i++) th += (i > 0 ? ", " : "") + typeEs(cur.theme[i]);
      txt("Tipos comunes: " + th, 616, yy + 20, fSmall, color(200));
    } else if (game.rules.random) txt("Aparece cualquier Pokémon", 616, yy + 20, fSmall, color(200));
    if (game.rules.nuzlocke) txt(cur.encounterUsed ? "Nuzlocke: captura de la zona USADA" : "Nuzlocke: ¡primer encuentro disponible!", 616, yy + 40, fSmall, cur.encounterUsed ? color(255, 120, 100) : color(120, 230, 140));
    m.draw(612, 215, 326, 37);
    // equipo en miniatura
    for (int i = 0; i < 6; i++) {
      float x = 24 + i * 92, y = height - 62;
      if (i < game.party.size()) {
        Mon mo = game.party.get(i);
        fill(0, 140); rect(x, y, 86, 54, 8);
        drawCreature(mo.normalSp, x + 22, y + 26, 34, false, false, false, mo.alive() ? 255.0f : 90.0f);
        txt("Nv" + mo.level, x + 42, y + 4, fSmall, color(255));
        hpBar(x + 42, y + 32, 38, mo.hpFrac());
        if (mo.dead) txt("†", x + 66, y + 16, fSmall, color(255, 80, 80));
      } else { fill(0, 70); rect(x, y, 86, 54, 8); }
    }
  }
  float mx(Place p) { return p.mapX + 30; }
  float my(Place p) { return p.mapY; }

  void key(int k, char c) {
    if (m.move(k)) return;
    if (k != K_OK) return;
    if (m.sel < 0 || m.sel >= ids.size()) return;
    String id = ids.get(m.sel);
    final Place p = game.place();
    if (game.firstAlive() == null && (id.equals("wild") || id.equals("trainers") || id.equals("boss"))) { say("Ningún Pokémon puede luchar. Cura a tu equipo."); return; }
    if (id.equals("wild")) {
      pushScene(new BattleScene(game.wildBattle()));
    } else if (id.equals("trainers")) {
      pushScene(new TrainerListScene());
    } else if (id.equals("boss")) {
      Trainer t = game.nextBoss();
      pushScene(new BattleScene(game.trainerBattle(t)));
    } else if (id.equals("heal")) {
      game.healAll();
      msg("¡Tu equipo está como nuevo!" + (game.rules.nuzlocke ? " (los Pokémon caídos no vuelven)" : ""), null);
    } else if (id.equals("shop")) {
      pushScene(new ShopMenuScene());
    } else if (id.equals("party")) pushScene(new PartyScene(0, null, null));
    else if (id.equals("bag")) pushScene(new BagScene(false, null));
    else if (id.equals("box")) pushScene(new BoxScene());
    else if (id.equals("dex")) pushScene(new DexScene());
    else if (id.equals("save")) saveGame();
    else if (id.equals("next")) {
      if (game.canAdvance()) { game.advance(); msg("Viajas a " + game.place().name + ".", null); }
      else say("Primero debes derrotar al jefe de esta zona.");
    } else if (id.equals("prev")) { game.placeIdx--; msg("Vuelves a " + game.place().name + ".", null); }
    else if (id.equals("menu")) { stack.clear(); pushScene(new TitleScene()); }
  }
}

class TrainerListScene extends Scene {
  ListMenu m = new ListMenu();
  ArrayList<Trainer> ts = new ArrayList<Trainer>();
  void enter() {
    Place p = game.place();
    ArrayList<String> l = new ArrayList<String>();
    for (Trainer t : p.trainers) { ts.add(t); l.add(t.full() + (t.done ? "  ✔ derrotado" : "")); }
    m.set(l);
  }
  void draw() {
    background(16, 20, 32);
    txt("Entrenadores de " + game.place().name, 40, 30, fBig, color(255, 220, 120));
    panel(40, 90, 520, 400);
    m.draw(50, 100, 500, 40);
    txt("Z: desafiar · X: volver", 40, 520, fSmall, color(180));
    if (!ts.isEmpty()) {
      Trainer t = ts.get(m.sel);
      if (!t.done) {
        game.prepare(t, game.place());
        panel(590, 90, 340, 400);
        txt("Equipo rival (" + t.team.size() + ")", 606, 100, fNormal, color(255, 230, 140));
        for (int i = 0; i < t.team.size(); i++) {
          TMon tm = t.team.get(i); Species s = Dex.sp(tm.species);
          drawCreature(s, 636, 175 + i * 52, 36, false, false, false, 255.0f);
          txt(s.name + "  Nv" + tm.level, 670, 160 + i * 52, fSmall, color(240));
        }
        txt("Premio: $" + t.reward, 606, 450, fSmall, color(255, 230, 120));
      }
    }
  }
  void key(int k, char c) {
    if (m.move(k)) return;
    if (k == K_BACK) popScene();
    else if (k == K_OK && !ts.isEmpty()) {
      Trainer t = ts.get(m.sel);
      if (t.done) { say("Ya derrotaste a este entrenador."); return; }
      if (game.firstAlive() == null) { say("Ningún Pokémon puede luchar."); return; }
      pushScene(new BattleScene(game.trainerBattle(t)));
    }
  }
}

// =====================================================================
//  EQUIPO
// =====================================================================
class PartyScene extends Scene {
  // modo 0: ver/gestionar · 1: elegir para combate (cambio) · 2: elegir objetivo para objeto
  int mode, sel = 0;
  Battle battle; IntCb pick; boolean forced = false;
  PartyScene(int mode, Battle b, IntCb cb) { this.mode = mode; this.battle = b; this.pick = cb; }
  List<Mon> list() { return mode == 1 && battle != null ? battle.sides[0].party : game.party; }
  void draw() {
    background(16, 20, 32);
    List<Mon> l = list();
    txt(mode == 1 ? "¿A qué Pokémon enviar?" : mode == 2 ? "¿A qué Pokémon aplicarlo?" : "Tu equipo", 40, 20, fBig, color(255, 220, 120));
    for (int i = 0; i < 6; i++) {
      float x = 30 + (i % 2) * 460, y = 80 + (i / 2) * 150;
      panelSel(x, y, 440, 138, i == sel);
      if (i >= l.size()) continue;
      Mon m = l.get(i);
      drawCreature(m.sp, x + 60, y + 70, 70, false, m.megaed, m.dynamaxed, m.alive() ? 255.0f : 100.0f);
      txt(m.name() + (m.dead ? "  †" : ""), x + 120, y + 8, fNormal, color(255));
      txt("Nv " + m.level, x + 120, y + 36, fSmall, color(230));
      if (!m.status.isEmpty()) { fill(statusColor(m.status)); rect(x + 190, y + 38, 40, 18, 6); txtc(statusEs(m.status), x + 210, y + 39, fSmall, color(255)); }
      typeChip(m.sp.t1, x + 120, y + 62); if (!m.sp.t2.isEmpty()) typeChip(m.sp.t2, x + 204, y + 62);
      hpBar(x + 120, y + 92, 200, m.hpFrac());
      txt(m.hp + "/" + m.maxHp, x + 330, y + 86, fSmall, color(240));
      String it = m.item.isEmpty() ? "sin objeto" : Items.name(m.item);
      txt("Obj: " + it, x + 120, y + 108, fSmall, color(200, 220, 255));
      if (m.megaForm() != null) txt("★ MEGA", x + 360, y + 8, fSmall, color(255, 220, 90));
      else if (m.canDynamax()) txt("◆ DMAX", x + 360, y + 8, fSmall, color(255, 120, 120));
    }
    txt("Z: elegir · X: volver" + (mode == 1 ? " · (debe estar vivo)" : ""), 30, height - 30, fSmall, color(180));
  }
  void key(int k, char c) {
    List<Mon> l = list();
    if (k == K_UP) sel = (sel + 4) % 6; else if (k == K_DOWN) sel = (sel + 2) % 6;
    else if (k == K_LEFT || k == K_RIGHT) sel = sel ^ 1;
    else if (k == K_BACK) { if (forced) return; popScene(); if (mode == 1 && pick != null) pick.run(-1); }
    else if (k == K_OK) {
      if (sel >= l.size()) return;
      Mon m = l.get(sel);
      if (mode == 1) {
        if (!m.alive()) { say(m.name() + " no puede luchar."); return; }
        if (battle != null && m == battle.sides[0].active) { say(m.name() + " ya está en combate."); return; }
        popScene(); if (pick != null) pick.run(sel);
      } else if (mode == 2) {
        popScene(); if (pick != null) pick.run(sel);
      } else {
        final Mon fm = m; final int fi = sel;
        pushScene(new ChoiceScene(m.name(), new String[]{"Resumen", "Subir en la lista", "Dar / quitar objeto", "Mover a la caja", "Cancelar"}, new Cb[]{
          new Cb() { public void run() { pushScene(new SummaryScene(fm)); } },
          new Cb() { public void run() { if (fi > 0) { game.party.remove(fi); game.party.add(fi - 1, fm); sel = fi - 1; } } },
          new Cb() { public void run() { pushScene(new GiveItemScene(fm)); } },
          new Cb() { public void run() {
            if (game.party.size() <= 1) { say("No puedes dejar el equipo vacío."); return; }
            game.party.remove(fm); game.box.add(fm); say(fm.name() + " fue enviado a la caja."); } },
          new Cb() { public void run() { } } }));
      }
    }
  }
}

// =====================================================================
//  RESUMEN
// =====================================================================
class SummaryScene extends Scene {
  Mon m; int page = 0;
  SummaryScene(Mon m) { this.m = m; }
  void draw() {
    background(16, 20, 32);
    panel(20, 20, 920, 600);
    drawCreature(m.sp, 170, 190, 150, false, m.megaed, m.dynamaxed, 255.0f);
    txt(m.name() + "   Nv " + m.level, 330, 30, fBig, color(255, 230, 140));
    txt("#" + m.sp.num + " " + m.sp.name + (m.sp.forme.isEmpty() ? "" : "  (" + m.sp.forme + ")"), 330, 72, fSmall, color(200));
    typeChip(m.sp.t1, 330, 100); if (!m.sp.t2.isEmpty()) typeChip(m.sp.t2, 414, 100);
    txt("Naturaleza: " + m.natureName(), 40, 300, fNormal, color(255));
    AbilityDef ab = Dex.ab(m.ability);
    txt("Habilidad: " + (ab != null ? ab.dn() : m.ability), 40, 330, fNormal, color(255, 220, 140));
    if (ab != null) { textFont(fSmall); fill(215); textAlign(LEFT, TOP); text(ab.dd(), 40, 360, 420, 70); }
    ItemDef it = Dex.item(m.item);
    txt("Objeto: " + (m.item.isEmpty() ? "ninguno" : Items.name(m.item)), 40, 440, fNormal, color(180, 220, 255));
    if (it != null) { textFont(fSmall); fill(215); textAlign(LEFT, TOP); text(it.dd(), 40, 470, 420, 70); }
    txt("PS " + m.hp + "/" + m.maxHp, 40, 548, fNormal, color(255));
    hpBar(150, 556, 280, m.hpFrac());
    if (page == 0) {
      String[] nm = {"PS", "Ataque", "Defensa", "At. Esp.", "Def. Esp.", "Velocidad"};
      for (int i = 0; i < 6; i++) {
        float y = 150 + i * 34;
        txt(nm[i], 520, y, fNormal, color(230));
        txt(i == 0 ? "" + m.maxHp : "" + m.stat[i], 650, y, fNormal, color(255));
        fill(60); rect(710, y + 6, 200, 12, 6);
        fill(typeColor(i % 2 == 0 ? m.sp.t1 : (m.sp.t2.isEmpty() ? m.sp.t1 : m.sp.t2)));
        rect(710, y + 6, constrain(m.sp.base[i] / 255.0f, 0, 1) * 200, 12, 6);
        txt("" + m.sp.base[i], 920 - 40, y, fSmall, color(180));
      }
      int ev = 0; for (int e : m.ev) ev += e;
      txt("Total base " + m.sp.bst() + " · EVs " + ev + "/510", 520, 370, fSmall, color(200));
      txt("EXP " + m.exp + " · próximo nv: " + max(0, Mon.expFor(min(100, m.level + 1)) - m.exp), 520, 395, fSmall, color(200));
      txt("Origen: " + (m.origin.isEmpty() ? "Pueblo Paleta" : m.origin), 520, 420, fSmall, color(200));
      if (m.megaForm() != null) txt("★ Puede Megaevolucionar a " + m.megaForm().name, 520, 460, fSmall, color(255, 220, 90));
      if (m.canDynamax()) txt(m.gmaxForm() != null ? "◆ Puede Gigamaxizarse (G-Max)" : "◆ Puede Dinamaxizarse", 520, 485, fSmall, color(255, 130, 130));
      for (Evo e : m.normalSp.evos) txt("Evoluciona a " + e.to + (Mon.evoLevel(e) > 0 ? " (Nv " + Mon.evoLevel(e) + ")" : "") + (e.type.equals("useItem") ? " con " + Items.name(e.item) : ""), 520, 520 + 0, fSmall, color(180, 230, 180));
    } else {
      for (int i = 0; i < 4; i++) {
        float y = 100 + i * 118;
        panelSel(500, y, 420, 108, false);
        if (i >= m.moves.length) continue;
        Move mv = Dex.mv(m.moves[i]);
        if (mv == null) continue;
        txt(mv.dn(), 514, y + 6, fNormal, color(255));
        typeChip(mv.type, 514, y + 36);
        txt((mv.cat == 'P' ? "Físico" : mv.cat == 'S' ? "Especial" : "Estado") + (mv.bp > 0 ? " · Pod " + mv.bp : "") + (mv.acc > 0 ? " · Prec " + mv.acc : ""), 600, y + 36, fSmall, color(220));
        txtr("PP " + m.pp[i] + "/" + m.maxPp[i], 905, y + 8, fSmall, color(255, 230, 140));
        textFont(fSmall); fill(200); textAlign(LEFT, TOP); text(mv.dd(), 514, y + 62, 396, 40);
      }
    }
    txt("←/→: página · X: volver", 330, 556, fSmall, color(170));
  }
  void key(int k, char c) {
    if (k == K_LEFT || k == K_RIGHT) page = 1 - page;
    else if (k == K_BACK || k == K_OK) popScene();
  }
}

// =====================================================================
//  DAR / QUITAR OBJETO
// =====================================================================
class GiveItemScene extends Scene {
  Mon mon; ListMenu m = new ListMenu(); ArrayList<String> ids = new ArrayList<String>();
  GiveItemScene(Mon mo) { mon = mo; }
  void enter() { build(); m.visible = 11; }
  void build() {
    ids.clear(); ArrayList<String> l = new ArrayList<String>();
    if (!mon.item.isEmpty()) { ids.add("#take"); l.add("« Quitar " + Items.name(mon.item) + " »"); }
    for (String id : game.bag.ids()) {
      ItemDef d = Dex.items.get(id);
      if (d == null || d.ball) continue;
      ids.add(id); l.add(d.name + " ×" + game.bag.count(id));
    }
    if (l.isEmpty()) l.add("(no tienes objetos equipables)");
    m.set(l);
  }
  void draw() {
    background(16, 20, 32);
    txt("Objeto de " + mon.name(), 40, 20, fBig, color(255, 220, 120));
    panel(40, 80, 520, 480);
    m.draw(50, 90, 500, 40);
    if (!ids.isEmpty() && m.sel < ids.size()) {
      String id = ids.get(m.sel);
      panel(580, 80, 350, 300);
      if (!id.equals("#take")) { txt(Items.name(id), 596, 92, fNormal, color(255, 230, 140)); textFont(fSmall); fill(220); textAlign(LEFT, TOP); text(Items.desc(id), 596, 130, 320, 220); }
    }
  }
  void key(int k, char c) {
    if (m.move(k)) return;
    if (k == K_BACK) popScene();
    else if (k == K_OK && !ids.isEmpty()) {
      String id = ids.get(m.sel);
      if (id.equals("#take")) { game.bag.add(mon.item, 1); say("Recuperaste " + Items.name(mon.item)); mon.item = ""; mon.checkItemForme(); }
      else {
        if (!mon.item.isEmpty()) game.bag.add(mon.item, 1);
        game.bag.remove(id, 1); mon.item = id; mon.checkItemForme();
        say(mon.name() + " lleva " + Items.name(id));
      }
      build();
    }
  }
}

// =====================================================================
//  MOCHILA
// =====================================================================
class BagScene extends Scene {
  boolean inBattle; Battle battle; StrCb useCb; // en combate: devuelve "item:id" o "ball:id"
  int tab = 0; ListMenu m = new ListMenu(); ArrayList<String> ids = new ArrayList<String>();
  String[] tabs = {"Médico", "Poké Balls", "Equipables", "Mejoras / Piedras", "Mega"};
  BagScene(boolean inBattle, Battle b) { this.inBattle = inBattle; this.battle = b; }
  void enter() { build(); m.visible = 10; }
  boolean inTab(String id) {
    Consumable c = Items.CONS.get(id);
    ItemDef d = Dex.items.get(id);
    if (tab == 0) return c != null && !c.kind.equals("candy") && !c.kind.equals("ev");
    if (tab == 1) return d != null && d.ball;
    if (tab == 3) return (c != null && (c.kind.equals("candy") || c.kind.equals("ev"))) || (d != null && d.id.endsWith("stone") && !d.id.equals("everstone") && !d.id.equals("floatstone") && d.megaStone.isEmpty());
    if (tab == 4) return d != null && (!d.megaStone.isEmpty() || d.id.equals("redorb") || d.id.equals("blueorb"));
    return c == null && d != null && !d.ball && d.megaStone.isEmpty() && !(d.id.endsWith("stone") && !d.id.equals("everstone") && !d.id.equals("floatstone")) && !d.id.equals("redorb") && !d.id.equals("blueorb");
  }
  void build() {
    ids.clear(); ArrayList<String> l = new ArrayList<String>();
    ArrayList<String> all = new ArrayList<String>(game.bag.ids());
    java.util.Collections.sort(all);
    for (String id : all) if (inTab(id)) { ids.add(id); l.add(Items.name(id) + "  ×" + game.bag.count(id)); }
    if (l.isEmpty()) l.add("(vacío)");
    m.sel = 0; m.top = 0;
    m.set(l);
  }
  void draw() {
    background(16, 20, 32);
    txt("Mochila", 40, 14, fBig, color(255, 220, 120));
    for (int i = 0; i < tabs.length; i++) {
      float x = 40 + i * 180;
      panelSel(x, 56, 172, 34, i == tab);
      txtc(tabs[i], x + 86, 62, fSmall, i == tab ? color(255, 230, 140) : color(210));
    }
    panel(40, 100, 500, 470);
    m.draw(50, 110, 480, 42);
    panel(560, 100, 370, 470);
    if (!ids.isEmpty() && m.sel < ids.size()) {
      String id = ids.get(m.sel);
      txt(Items.name(id), 576, 112, fNormal, color(255, 230, 140));
      textFont(fSmall); fill(220); textAlign(LEFT, TOP); text(Items.desc(id), 576, 150, 340, 300);
      int p = Items.price(id); if (p > 0) txt("Valor: $" + p, 576, 520, fSmall, color(180));
    }
    txt("←/→: bolsillo · Z: usar · X: volver  · Dinero $" + game.money, 40, height - 36, fSmall, color(170));
  }
  void key(int k, char c) {
    if (m.move(k)) return;
    if (k == K_LEFT) { tab = (tab + tabs.length - 1) % tabs.length; build(); }
    else if (k == K_RIGHT) { tab = (tab + 1) % tabs.length; build(); }
    else if (k == K_BACK) { popScene(); if (inBattle && useCb != null) useCb.run(""); }
    else if (k == K_OK && !ids.isEmpty() && m.sel < ids.size()) {
      final String id = ids.get(m.sel);
      Consumable c0 = Items.CONS.get(id);
      ItemDef d = Dex.items.get(id);
      if (inBattle) {
        if (d != null && d.ball) { popScene(); useCb.run("ball:" + id); }
        else if (c0 != null && Items.usableInBattle(id)) {
          pushScene(new PartyScene(2, battle, new IntCb() { public void run(int i) { popScene(); useCb.run("item:" + id + ":" + i); } }));
        } else say("No se puede usar en combate.");
        return;
      }
      if (c0 != null) {
        pushScene(new PartyScene(2, null, new IntCb() { public void run(int i) { useOutside(id, i); } }));
      } else if (d != null && d.ball) say("Se usan en combate.");
      else if (d != null && d.id.endsWith("stone") && d.megaStone.isEmpty() && !d.id.equals("everstone") && !d.id.equals("floatstone")) {
        pushScene(new PartyScene(2, null, new IntCb() { public void run(int i) { useStone(id, i); } }));
      } else {
        pushScene(new PartyScene(2, null, new IntCb() { public void run(int i) {
          Mon mo = game.party.get(i);
          if (!mo.item.isEmpty()) game.bag.add(mo.item, 1);
          game.bag.remove(id, 1); mo.item = id; mo.checkItemForme();
          say(mo.name() + " lleva ahora " + Items.name(id)); build();
        } }));
      }
    }
  }
  void useStone(String id, int i) {
    Mon mo = game.party.get(i);
    Evo e = mo.itemEvolution(id);
    if (e == null) { say("No tendrá ningún efecto."); return; }
    Species to = Dex.sp(e.to);
    if (to == null) return;
    game.bag.remove(id, 1); build();
    pushScene(new EvolutionScene(mo, to, null));
  }
  void useOutside(String id, int i) {
    Mon mo = game.party.get(i);
    if (id.equals("rarecandy")) {
      if (mo.level >= game.levelCap() || mo.level >= 100) { say("Tope de nivel alcanzado."); return; }
      game.bag.remove(id, 1);
      mo.level++; mo.exp = Math.max(mo.exp, Mon.expFor(mo.level)); mo.recalc(false);
      say(mo.name() + " subió al nivel " + mo.level);
      for (String mv : mo.newMovesAt(mo.level)) pushScene(new LearnMoveScene(mo, mv, null));
      Evo e = mo.checkEvolution(false);
      if (e != null) { Species to = Dex.sp(e.to); if (to != null) pushScene(new EvolutionScene(mo, to, null)); }
      build(); return;
    }
    String r = Items.useOnMon(null, mo, id, false);
    if (!r.startsWith("No tendrá")) { game.bag.remove(id, 1); build(); }
    say(r);
  }
}

// =====================================================================
//  TIENDA
// =====================================================================
class ShopMenuScene extends Scene {
  ListMenu m = new ListMenu(); ArrayList<Integer> kinds = new ArrayList<Integer>();
  void enter() {
    ArrayList<String> l = new ArrayList<String>();
    l.add("Médico y Poké Balls"); kinds.add(0);
    l.add("Piedras evolutivas y mejoras"); kinds.add(1);
    if (game.place().bigShop) { l.add("Gran Almacén: TODOS los objetos"); kinds.add(2); }
    if (game.place().megaShop) { l.add("Piedras Mega y Orbes"); kinds.add(3); }
    l.add("Salir"); kinds.add(-1);
    m.set(l); m.visible = l.size();
  }
  boolean opaque() { return false; }
  void draw() {
    panel(280, 120, 400, 60 + m.items.size() * 40);
    txt("Tienda  ($" + game.money + ")", 296, 128, fNormal, color(255, 230, 140));
    m.draw(290, 164, 380, 40);
  }
  void key(int k, char c) {
    if (m.move(k)) return;
    if (k == K_BACK) popScene();
    else if (k == K_OK) { int kind = kinds.get(m.sel); if (kind < 0) popScene(); else pushScene(new ShopScene(kind)); }
  }
}

class ShopScene extends Scene {
  int kind, qty = 1; ListMenu m = new ListMenu(); ArrayList<String> ids;
  ShopScene(int kind) { this.kind = kind; }
  void enter() {
    ids = new ArrayList<String>(game.shopStock(kind));
    ArrayList<String> l = new ArrayList<String>();
    for (String id : ids) l.add(Items.name(id) + "   $" + game.priceOf(id));
    m.set(l); m.visible = 11;
  }
  void draw() {
    background(16, 20, 32);
    txt("Tienda", 40, 14, fBig, color(255, 220, 120));
    txtr("$" + game.money, 920, 20, fBig, color(255, 230, 120));
    panel(40, 70, 520, 500);
    m.draw(50, 80, 500, 42);
    panel(580, 70, 350, 500);
    if (!ids.isEmpty()) {
      String id = ids.get(m.sel);
      txt(Items.name(id), 596, 82, fNormal, color(255, 230, 140));
      textFont(fSmall); fill(220); textAlign(LEFT, TOP); text(Items.desc(id), 596, 120, 320, 260);
      txt("Tienes: " + game.bag.count(id), 596, 400, fSmall, color(200));
      txt("Cantidad: ◄ " + qty + " ►   Total $" + game.priceOf(id) * qty, 596, 430, fNormal, color(255));
    }
    txt("Z: comprar · ←/→: cantidad · X: volver", 40, height - 34, fSmall, color(170));
  }
  void key(int k, char c) {
    if (m.move(k)) { qty = 1; return; }
    if (k == K_LEFT) qty = max(1, qty - 1);
    else if (k == K_RIGHT) qty = min(99, qty + 1);
    else if (k == K_BACK) popScene();
    else if (k == K_OK && !ids.isEmpty()) {
      String id = ids.get(m.sel);
      if (game.buy(id, qty)) say("Compraste " + qty + " × " + Items.name(id)); else say("No tienes suficiente dinero.");
    }
  }
}

// =====================================================================
//  CAJAS Y CEMENTERIO
// =====================================================================
class BoxScene extends Scene {
  int view = 0; // 0 caja, 1 cementerio
  ListMenu m = new ListMenu();
  void enter() { m.visible = 11; build(); }
  List<Mon> src() { return view == 0 ? game.box : game.graveyard; }
  void build() {
    ArrayList<String> l = new ArrayList<String>();
    for (Mon mo : src()) l.add(mo.name() + "  Nv" + mo.level + (view == 1 ? "  †" : (mo.alive() ? "" : "  (caído)")));
    if (l.isEmpty()) l.add(view == 0 ? "(la caja está vacía)" : "(ninguno ha muerto... todavía)");
    m.set(l);
  }
  void draw() {
    background(16, 20, 32);
    txt(view == 0 ? "Caja" : "Cementerio", 40, 14, fBig, color(255, 220, 120));
    txtr(game.rules.nuzlocke ? "←/→: caja / cementerio" : "", 920, 20, fSmall, color(180));
    panel(40, 70, 480, 500);
    m.draw(50, 80, 460, 40);
    panel(540, 70, 390, 500);
    List<Mon> s = src();
    if (!s.isEmpty() && m.sel < s.size()) {
      Mon mo = s.get(m.sel);
      drawCreature(mo.normalSp, 735, 190, 130, false, false, false, view == 1 ? 150.0f : 255.0f);
      txt(mo.name() + "  Nv " + mo.level, 560, 290, fNormal, color(255));
      typeChip(mo.normalSp.t1, 560, 325); if (!mo.normalSp.t2.isEmpty()) typeChip(mo.normalSp.t2, 644, 325);
      txt("Hab: " + abEs(mo.ability), 560, 360, fSmall, color(220));
      txt("Obj: " + (mo.item.isEmpty() ? "-" : Items.name(mo.item)), 560, 385, fSmall, color(220));
      if (view == 0) txt("Z: mover al equipo / intercambiar", 560, 520, fSmall, color(180));
      else txt("Origen: " + mo.origin, 560, 410, fSmall, color(200));
    }
    txt("Equipo: " + game.party.size() + "/6 · X: volver", 40, height - 34, fSmall, color(170));
  }
  void key(int k, char c) {
    if (m.move(k)) return;
    if ((k == K_LEFT || k == K_RIGHT) && game.rules.nuzlocke) { view = 1 - view; m.sel = 0; m.top = 0; build(); }
    else if (k == K_BACK) popScene();
    else if (k == K_OK && view == 0 && !game.box.isEmpty() && m.sel < game.box.size()) {
      final Mon mo = game.box.get(m.sel);
      if (!mo.alive() && game.rules.nuzlocke) { say("Ese Pokémon está caído."); return; }
      if (game.party.size() < 6) { game.box.remove(mo); game.party.add(mo); say(mo.name() + " se unió al equipo."); build(); }
      else pushScene(new PartyScene(2, null, new IntCb() { public void run(int i) {
        Mon out = game.party.get(i); game.party.set(i, mo); game.box.remove(mo); game.box.add(out); say("Intercambiaste a " + out.name() + " por " + mo.name()); build();
      } }));
    }
  }
}

// =====================================================================
//  POKÉDEX
// =====================================================================
class DexScene extends Scene {
  ArrayList<Species> list = new ArrayList<Species>(); ListMenu m = new ListMenu(); int seen = 0, total = 0;
  void enter() {
    for (Species s : Dex.species.values()) {
      if (s.cosmetic) continue;
      list.add(s); total++;
      if (game.dex.contains(s.id)) seen++;
    }
    java.util.Collections.sort(list, new java.util.Comparator<Species>() { public int compare(Species a, Species b) { return a.num != b.num ? a.num - b.num : a.name.compareTo(b.name); } });
    ArrayList<String> l = new ArrayList<String>();
    for (Species s : list) l.add((game.dex.contains(s.id) ? "● " : "○ ") + nf(s.num, 4) + "  " + (game.dex.contains(s.id) ? s.name : "???"));
    m.set(l); m.visible = 13;
  }
  void draw() {
    background(16, 20, 32);
    txt("Pokédex  —  vistos " + seen + " / " + total + " (formas incluidas)", 40, 14, fBig, color(255, 220, 120));
    panel(40, 66, 460, 540);
    m.draw(50, 76, 440, 40);
    Species s = list.get(m.sel);
    boolean known = game.dex.contains(s.id);
    panel(520, 66, 410, 540);
    drawCreature(s, 725, 190, known ? 140.0f : 90.0f, false, false, false, known ? 255.0f : 60.0f);
    txt(known ? s.name : "???", 540, 300, fBig, color(255));
    if (known) {
      typeChip(s.t1, 540, 345); if (!s.t2.isEmpty()) typeChip(s.t2, 624, 345);
      String[] nm = {"PS", "Ata", "Def", "AtE", "DeE", "Vel"};
      for (int i = 0; i < 6; i++) txt(nm[i] + " " + s.base[i], 540 + (i % 3) * 120, 385 + (i / 3) * 26, fSmall, color(230));
      txt("Habilidades: " + abEs(s.abilities[0]) + (s.abilities[1].isEmpty() ? "" : " / " + abEs(s.abilities[1])) + (s.abilities[2].isEmpty() ? "" : " / (" + abEs(s.abilities[2]) + ")"), 540, 450, fSmall, color(210));
      String fl = (s.mega ? "Megaevolución " : "") + (s.primal ? "Regresión primigenia " : "") + (s.gmax ? "Gigamax " : "") + (s.noDmax ? "(sin Dinamax) " : "");
      txt(fl, 540, 480, fSmall, color(255, 220, 120));
    }
    txt("X: volver", 40, height - 28, fSmall, color(170));
  }
  void key(int k, char c) {
    if (k == K_LEFT) { for (int i = 0; i < 10; i++) m.move(K_UP); return; }
    if (k == K_RIGHT) { for (int i = 0; i < 10; i++) m.move(K_DOWN); return; }
    if (m.move(k)) return;
    if (k == K_BACK) popScene();
  }
}

// =====================================================================
//  APRENDER MOVIMIENTO
// =====================================================================
class LearnMoveScene extends Scene {
  Mon mon; String mv; Cb done; int sel = 0;
  LearnMoveScene(Mon m, String mv, Cb done) { this.mon = m; this.mv = mv; this.done = done; }
  void enter() {
    if (mon.moves.length < 4) { mon.learnMove(mv, mon.moves.length); say(mon.name() + " aprendió " + Dex.mv(mv).dn() + "."); popScene(); if (done != null) done.run(); }
  }
  boolean opaque() { return false; }
  void draw() {
    panel(120, 100, 720, 400);
    Move nm = Dex.mv(mv);
    txt(mon.name() + " quiere aprender " + nm.dn() + ".", 140, 112, fNormal, color(255, 230, 140));
    txt("¿Qué movimiento debe olvidar?", 140, 144, fSmall, color(220));
    for (int i = 0; i < 5; i++) {
      float y = 180 + i * 56;
      panelSel(140, y, 680, 50, sel == i);
      Move m = i < 4 ? Dex.mv(mon.moves[i]) : nm;
      txt(m.dn() + (i == 4 ? "  (nuevo)" : ""), 156, y + 12, fNormal, color(255));
      typeChip(m.type, 420, y + 14);
      txt((m.cat == 'P' ? "Fís" : m.cat == 'S' ? "Esp" : "Est") + (m.bp > 0 ? "  Pod " + m.bp : ""), 520, y + 14, fSmall, color(220));
    }
    txt("Z: olvidar el seleccionado · selecciona el nuevo para no aprenderlo", 140, 470, fSmall, color(180));
  }
  void key(int k, char c) {
    if (k == K_UP) sel = (sel + 4) % 5; else if (k == K_DOWN) sel = (sel + 1) % 5;
    else if (k == K_OK) {
      if (sel < 4) { String old = mon.moves[sel]; mon.learnMove(mv, sel); say("Olvidó " + Dex.mv(old).dn() + " y aprendió " + Dex.mv(mv).dn() + "."); }
      else say(mon.name() + " no aprendió " + Dex.mv(mv).dn() + ".");
      popScene(); if (done != null) done.run();
    }
  }
}

// =====================================================================
//  EVOLUCIÓN
// =====================================================================
class EvolutionScene extends Scene {
  Mon mon; Species to; Cb done; int t = 0; boolean cancelled = false, finished = false;
  EvolutionScene(Mon m, Species to, Cb done) { this.mon = m; this.to = to; this.done = done; }
  void draw() {
    background(10, 12, 24);
    float k = t < 150 ? (t % 24 < 12 ? 0 : 1) : 1;
    float a = t < 150 ? 255.0f : 255.0f;
    Species show = (t < 150 && (t % 24 < 12)) ? mon.normalSp : (t < 150 ? to : to);
    if (t >= 150) show = to;
    float glow = t < 150 ? 40 + 40 * sin(t * 0.3f) : 0;
    noStroke(); fill(255, 255, 255, glow + 20); ellipse(width / 2, 260, 300 + glow * 2, 300 + glow * 2);
    drawCreature(show, width / 2, 260, 190, false, false, false, a);
    panel(30, height - 150, width - 60, 130);
    if (t < 150 && !cancelled) { txt("¡¿" + mon.name() + " está evolucionando?! (X para cancelar)", 56, height - 128); t++; if (t == 150) { mon.evolveTo(to); game.own(mon); } }
    else if (cancelled) txt("¡" + mon.name() + " dejó de evolucionar!", 56, height - 128);
    else { txt("¡Felicidades! ¡" + (mon.nick.isEmpty() ? mon.normalSp.name : mon.nick) + " evolucionó en " + to.name + "!", 56, height - 128); finished = true; }
  }
  void key(int k, char c) {
    if (t < 150 && k == K_BACK) { cancelled = true; }
    else if (cancelled && k != 0) { popScene(); if (done != null) done.run(); }
    else if (finished && k == K_OK) {
      popScene();
      for (String mv : mon.newMovesAt(mon.level)) pushScene(new LearnMoveScene(mon, mv, null));
      if (done != null) done.run();
    }
  }
}

// =====================================================================
//  FIN DE PARTIDA
// =====================================================================
class GameOverScene extends Scene {
  void draw() {
    background(10, 0, 0);
    txtc("FIN DE LA PARTIDA", width / 2, 120, fTitle, color(220, 40, 40));
    txtc(game.rules.nuzlocke ? "Tu Nuzlocke ha terminado." : "Has sido derrotado.", width / 2, 240, fBig, color(255));
    txtc("Combates ganados: " + game.battlesWon + " · Medallas: " + game.badges + " · Caídos: " + (game.graveyard.size()), width / 2, 300, fNormal, color(230));
    if (!game.graveyard.isEmpty()) {
      txtc("En memoria de:", width / 2, 360, fNormal, color(200));
      for (int i = 0; i < min(8, game.graveyard.size()); i++) txtc("† " + game.graveyard.get(i).name() + " Nv" + game.graveyard.get(i).level, width / 2, 395 + i * 24, fSmall, color(180));
    }
    txtc("Pulsa Z para volver al título", width / 2, height - 50, fSmall, color(170));
  }
  void key(int k, char c) { if (k == K_OK) { stack.clear(); pushScene(new TitleScene()); } }
}

class VictoryScene extends Scene {
  void draw() {
    background(20, 10, 40);
    for (int i = 0; i < 60; i++) { fill(255, 200 + (i * 7) % 55, 80, 160); float x = (i * 131 + tick * 2) % width, y = (i * 71 + tick * (1 + i % 3)) % height; ellipse(x, y, 5, 5); }
    txtc("¡CAMPEÓN DE LA LIGA POKÉMON!", width / 2, 60, fBig, color(255, 230, 120));
    txtc(game.rules.nuzlocke ? "¡Nuzlocke completado!" : "¡Has completado el juego en modo DIFÍCIL!", width / 2, 110, fNormal, color(255));
    for (int i = 0; i < game.party.size(); i++) {
      Mon m = game.party.get(i);
      float x = 100 + (i % 3) * 270, y = 220 + (i / 3) * 190;
      drawCreature(m.sp, x + 40, y + 30, 90, false, false, false, 255.0f);
      txt(m.name() + " Nv" + m.level, x + 100, y + 6, fNormal, color(255));
      txt(m.item.isEmpty() ? "" : Items.name(m.item), x + 100, y + 34, fSmall, color(200));
    }
    txtc("Combates: " + game.battlesWon + " · Bajas: " + game.graveyard.size() + " · Pulsa Z", width / 2, height - 50, fSmall, color(200));
  }
  void key(int k, char c) { if (k == K_OK) { stack.clear(); pushScene(new TitleScene()); } }
}
