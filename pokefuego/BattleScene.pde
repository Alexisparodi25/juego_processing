// =====================================================================
//  Escena de combate
//  Reproduce los eventos del motor (Battle.java) con animaciones,
//  y gestiona los menús: Luchar / Mochila / Pokémon / Huir.
//  M = activar Megaevolución este turno · G = activar Dinamax/Gigamax
// =====================================================================
class BattleScene extends Scene {
  Battle b;
  ArrayDeque<BEvent> q = new ArrayDeque<BEvent>();
  BEvent cur = null;
  BEvent last = null;
  int chars = 0, hold = 0;
  float[] dispHp = {1, 1};
  int phase = 0;                 // 0 reproduciendo, 1 menú, 2 movimientos, 3 terminado
  int menuSel = 0, moveSel = 0;
  boolean wantMega = false, wantDyna = false;
  int[] shake = new int[2], faintAnim = new int[2], sendAnim = new int[2];
  int flash = 0;
  boolean ended = false;
  String lastSpId0 = "", lastSpId1 = "";

  BattleScene(Battle battle) { b = battle; }

  void enter() {
    b.start();
    enqueue();
  }

  void enqueue() { for (BEvent e : b.takeEvents()) q.add(e); phase = 0; }

  Species spOf(int side) {
    if (cur != null && cur.spId[side] != null) { Species s = Dex.species.get(cur.spId[side]); if (s != null) return s; }
    if (last != null && last.spId[side] != null) { Species s = Dex.species.get(last.spId[side]); if (s != null) return s; }
    Mon m = b.sides[side].active; return m != null ? m.sp : Dex.sp("rattata");
  }

  BEvent view() { return cur != null ? cur : last; }

  void nextEvent() {
    BEvent e = q.poll();
    if (e == null) { cur = null; return; }
    cur = e; last = e; chars = 0; hold = 0;
    if (e.hitSide >= 0) shake[e.hitSide] = 16;
    if (e.anim.startsWith("faint")) faintAnim[e.anim.charAt(5) - '0'] = 50;
    if (e.anim.startsWith("send")) sendAnim[e.anim.charAt(4) - '0'] = 30;
    if (e.anim.startsWith("mega") || e.anim.startsWith("dmax")) flash = 26;
    for (int i = 0; i < 2; i++) {
      if (e.spId[i] != null) {
        float f = e.maxHp[i] > 0 ? e.hp[i] / (float) e.maxHp[i] : 0;
        String id = e.spId[i];
        if (i == 0 && !id.equals(lastSpId0)) { dispHp[0] = f; lastSpId0 = id; }
        if (i == 1 && !id.equals(lastSpId1)) { dispHp[1] = f; lastSpId1 = id; }
      }
    }
  }

  // ------------------------------------------------------------------
  void draw() {
    // reproducción de eventos
    if (phase == 0) {
      if (cur == null) { nextEvent(); if (cur == null) afterPlay(); }
      else {
        int len = cur.msg.length();
        if (len == 0) { hold++; if (hold > 22) cur = null; }
        else {
          if (chars < len) chars = min(len, chars + 2); else { hold++; if (hold > 55) cur = null; }
        }
      }
    }
    BEvent v = view();
    for (int i = 0; i < 2; i++) {
      if (v != null && v.maxHp[i] > 0) {
        float target = v.hp[i] / (float) v.maxHp[i];
        if (dispHp[i] > target) dispHp[i] = max(target, dispHp[i] - 0.012f);
        else if (dispHp[i] < target) dispHp[i] = min(target, dispHp[i] + 0.012f);
      }
      if (shake[i] > 0) shake[i]--;
      if (faintAnim[i] > 0) faintAnim[i]--;
      if (sendAnim[i] > 0) sendAnim[i]--;
    }
    if (flash > 0) flash--;
    drawArena(v);
    drawHud(v);
    drawBottom(v);
    if (flash > 0) { fill(255, 255, 255, flash * 8); noStroke(); rect(0, 0, width, height); }
  }

  void drawArena(BEvent v) {
    // cielo y campo
    noStroke();
    for (int y = 0; y < 260; y += 4) { fill(lerpColor(color(110, 180, 240), color(200, 235, 255), y / 260.0f)); rect(0, y, width, 4); }
    fill(96, 168, 88); rect(0, 250, width, height - 250);
    fill(84, 150, 76); ellipse(710, 270, 380, 90); ellipse(250, 430, 440, 110);
    String w = v == null ? "" : v.weather;
    if (w != null && !w.isEmpty()) {
      if (w.equals("rain")) { stroke(80, 120, 220, 160); strokeWeight(2); for (int i = 0; i < 70; i++) { float x = (i * 53 + tick * 9) % width, y = (i * 91 + tick * 14) % 400; line(x, y, x - 6, y + 16); } noStroke(); }
      else if (w.equals("sun")) { fill(255, 240, 120, 50); rect(0, 0, width, height); fill(255, 230, 80, 180); ellipse(900, 50, 70, 70); }
      else if (w.equals("sand")) { fill(214, 180, 110, 70); rect(0, 0, width, height); }
      else if (w.equals("hail")) { fill(255, 255, 255, 220); for (int i = 0; i < 50; i++) ellipse((i * 67 + tick * 3) % width, (i * 37 + tick * 5) % 400, 5, 5); }
    }
    // enemigo
    if (v != null && v.spId[1] != null) {
      Species s = spOf(1);
      float ox = shake[1] > 0 ? sin(shake[1] * 1.7f) * 8 : 0;
      float oy = faintAnim[1] > 0 ? (50 - faintAnim[1]) * 3.0f : 0;
      float slide = sendAnim[1] > 0 ? sendAnim[1] * 8.0f : 0;
      float al = faintAnim[1] > 0 ? faintAnim[1] * 5.0f : (v.hp[1] <= 0 && cur == null ? 0.0f : 255.0f);
      if (v.hp[1] <= 0 && faintAnim[1] == 0) al = 0;
      if (al > 0) drawCreature(s, 700 + ox + slide, 190 + oy + sin(tick * 0.05f) * 3, 120, false, v.mega[1], v.dyna[1], al);
    }
    // jugador
    if (v != null && v.spId[0] != null) {
      Species s = spOf(0);
      float ox = shake[0] > 0 ? sin(shake[0] * 1.7f) * 8 : 0;
      float oy = faintAnim[0] > 0 ? (50 - faintAnim[0]) * 3.0f : 0;
      float slide = sendAnim[0] > 0 ? -sendAnim[0] * 8.0f : 0;
      float al = faintAnim[0] > 0 ? faintAnim[0] * 5.0f : 255.0f;
      if (v.hp[0] <= 0 && faintAnim[0] == 0) al = 0;
      if (al > 0) drawCreature(s, 240 + ox + slide, 360 + oy + sin(tick * 0.05f + 1) * 3, 170, true, v.mega[0], v.dyna[0], al);
    }
  }

  void drawHud(BEvent v) {
    if (v == null) return;
    // enemigo
    panel(30, 24, 360, 98);
    String n1 = v.name[1] == null ? "" : v.name[1];
    txt(n1, 46, 30, fNormal, color(255));
    txtr("Nv" + v.level[1], 376, 32, fNormal, color(255));
    if (v.status[1] != null && !v.status[1].isEmpty()) { fill(statusColor(v.status[1])); rect(46, 62, 44, 18, 6); txtc(statusEs(v.status[1]), 68, 63, fSmall, color(255)); }
    float tagx = 100;
    if (v.mega[1]) { txt("MEGA", tagx, 61, fSmall, color(255, 220, 90)); tagx += 52; }
    if (v.dyna[1]) { txt(v.spId[1] != null && v.spId[1].endsWith("gmax") ? "GIGAMAX" : "DINAMAX", tagx, 61, fSmall, color(255, 110, 110)); }
    hpBar(46, 88, 330, dispHp[1]);
    if (!b.wild) {
      for (int i = 0; i < 6; i++) {
        if (i < b.sides[1].party.size()) { Mon m = b.sides[1].party.get(i); fill(m.alive() ? color(240, 90, 80) : color(80)); } else fill(40);
        ellipse(52 + i * 16, 110, 10, 10);
      }
      txtr(b.sides[1].name, 376, 100, fSmall, color(210));
    } else txtr("Pokémon salvaje", 376, 100, fSmall, color(210));
    // jugador
    float py = height - 150 - 118;
    panel(560, py, 370, 108);
    txt(v.name[0] == null ? "" : v.name[0], 576, py + 6, fNormal, color(255));
    txtr("Nv" + v.level[0], 916, py + 8, fNormal, color(255));
    if (v.status[0] != null && !v.status[0].isEmpty()) { fill(statusColor(v.status[0])); rect(576, py + 38, 44, 18, 6); txtc(statusEs(v.status[0]), 598, py + 39, fSmall, color(255)); }
    float tx = 630;
    if (v.mega[0]) { txt("MEGA", tx, py + 37, fSmall, color(255, 220, 90)); tx += 52; }
    if (v.dyna[0]) { txt(v.spId[0] != null && v.spId[0].endsWith("gmax") ? "GIGAMAX" : "DINAMAX", tx, py + 37, fSmall, color(255, 110, 110)); }
    hpBar(576, py + 66, 340, dispHp[0]);
    txtr(max(0, round(dispHp[0] * v.maxHp[0])) + " / " + v.maxHp[0], 916, py + 80, fSmall, color(240));
    // clima y terreno
    String lab = "";
    if (v.weather != null && !v.weather.isEmpty()) lab += v.weather.equals("rain") ? "☂ Lluvia  " : v.weather.equals("sun") ? "☀ Sol intenso  " : v.weather.equals("sand") ? "Tormenta de arena  " : "❄ Granizo  ";
    if (v.terrain != null && !v.terrain.isEmpty()) lab += "Campo " + (v.terrain.equals("electric") ? "eléctrico" : v.terrain.equals("grassy") ? "de hierba" : v.terrain.equals("misty") ? "de niebla" : "psíquico");
    if (!lab.isEmpty()) { txtr(lab, 930, 130, fSmall, color(20)); }
    // trampas / pantallas
    String hz = "";
    Side s0 = b.sides[0], s1 = b.sides[1];
    if (s0.stealthRock > 0) hz += "Rocas "; if (s0.spikes > 0) hz += "Púas×" + s0.spikes + " "; if (s0.toxicSpikes > 0) hz += "Tóx "; if (s0.reflect > 0) hz += "Reflejo "; if (s0.lightScreen > 0) hz += "P.Luz "; if (s0.auraVeil > 0) hz += "V.Aurora "; if (s0.tailwind > 0) hz += "Viento ";
    if (!hz.isEmpty()) txt("Tu campo: " + hz, 30, 290, fSmall, color(20));
    String hz1 = "";
    if (s1.stealthRock > 0) hz1 += "Rocas "; if (s1.spikes > 0) hz1 += "Púas×" + s1.spikes + " "; if (s1.toxicSpikes > 0) hz1 += "Tóx "; if (s1.reflect > 0) hz1 += "Reflejo "; if (s1.lightScreen > 0) hz1 += "P.Luz "; if (s1.auraVeil > 0) hz1 += "V.Aurora "; if (s1.tailwind > 0) hz1 += "Viento ";
    if (!hz1.isEmpty()) txt("Campo rival: " + hz1, 30, 310, fSmall, color(20));
  }

  void drawBottom(BEvent v) {
    panel(30, height - 150, width - 60, 130);
    if (phase == 0) {
      String s = cur != null ? cur.msg : (last != null ? last.msg : "");
      txt(cur != null ? s.substring(0, min(chars, s.length())) : s, 56, height - 128, fNormal, color(245));
      if (cur != null && chars >= cur.msg.length() && cur.msg.length() > 0 && (tick / 20) % 2 == 0) { fill(255, 220, 120); triangle(width - 70, height - 52, width - 50, height - 52, width - 60, height - 40); }
    } else if (phase == 1) {
      Mon u = b.sides[0].active;
      txt("¿Qué debe hacer " + u.name() + "?", 56, height - 128, fNormal, color(245));
      String[] opts = {"LUCHAR", "MOCHILA", "POKÉMON", b.wild ? "HUIR" : "(sin huida)"};
      panel(width - 400, height - 150, 370, 130);
      for (int i = 0; i < 4; i++) {
        float x = width - 380 + (i % 2) * 175, y = height - 134 + (i / 2) * 52;
        panelSel(x, y, 165, 44, i == menuSel);
        txt(opts[i], x + 14, y + 10, fNormal, i == menuSel ? color(255, 230, 140) : color(235));
      }
      String hint = "";
      if (b.canMega(b.sides[0])) hint += "[M] Mega disponible   ";
      if (b.canDynamax(b.sides[0])) hint += "[G] Dinamax disponible";
      if (b.sides[0].transformed) hint = "Ya usaste tu transformación";
      txt(hint, 56, height - 60, fSmall, color(255, 220, 120));
    } else if (phase == 2) {
      Mon u = b.sides[0].active;
      panel(30, height - 150, 640, 130);
      for (int i = 0; i < 4; i++) {
        float x = 44 + (i % 2) * 316, y = height - 140 + (i / 2) * 58;
        panelSel(x, y, 304, 52, i == moveSel);
        if (i < u.moves.length) {
          Move m = b.effectiveMove(u, i);
          boolean useMax = u.dynamaxed || wantDyna;
          if (wantDyna && !u.dynamaxed) m = b.maxVersion(u, Dex.mv(u.moves[i]));
          fill(typeColor(m.type)); rect(x + 8, y + 8, 6, 36, 3);
          txt(m.dn(), x + 22, y + 4, fSmall, u.pp[i] > 0 ? color(255) : color(140));
          txt("PP " + u.pp[i] + "/" + u.maxPp[i], x + 22, y + 26, fSmall, u.pp[i] == 0 ? color(255, 90, 90) : color(200));
          if (m.bp > 0) txtr("" + m.bp, x + 296, y + 4, fSmall, color(255, 230, 140));
        }
      }
      // panel de info del movimiento
      panel(width - 280, height - 150, 250, 130);
      if (moveSel < u.moves.length) {
        Move m = b.effectiveMove(u, moveSel);
        if (wantDyna && !u.dynamaxed) m = b.maxVersion(u, Dex.mv(u.moves[moveSel]));
        typeChip(m.type, width - 268, height - 140);
        txt((m.cat == 'P' ? "Físico" : m.cat == 'S' ? "Especial" : "Estado") + (m.acc > 0 ? "  Prec " + m.acc : ""), width - 184, height - 140, fSmall, color(220));
        textFont(fSmall); fill(210); textAlign(LEFT, TOP); text(m.dd(), width - 268, height - 112, 228, 70);
      }
      String t = "";
      if (b.canMega(b.sides[0])) t += (wantMega ? "[M] MEGA ✔  " : "[M] Mega  ");
      if (b.canDynamax(b.sides[0])) t += (wantDyna ? "[G] DINAMAX ✔" : "[G] Dinamax");
      txt(t, 44, height - 170, fSmall, color(255, 230, 120));
    }
  }

  // ------------------------------------------------------------------
  void afterPlay() {
    if (ended) return;
    if (b.over) { finish(); return; }
    // el rival debe elegir reemplazo
    if (b.needSwitch[1]) {
      int r = AI.bestReplacement(b, 1);
      if (r < 0) { b.needSwitch[1] = false; b.checkOver(); if (b.over) { finish(); return; } }
      else { b.replace(1, r); enqueue(); return; }
    }
    if (b.needSwitch[0]) {
      phase = 3;
      PartyScene ps = new PartyScene(1, b, new IntCb() { public void run(int i) { if (i >= 0) { b.replace(0, i); enqueue(); } } });
      ps.forced = true;
      pushScene(ps);
      return;
    }
    // acciones forzadas (carga / recarga / furia)
    Mon u = b.sides[0].active;
    if (!u.chargingMove.isEmpty() || u.recharge > 0 || (u.lockTurns > 0 && !u.lockedMove.isEmpty() && !u.dynamaxed) || b.mustStruggle(u)) {
      submit(Action.move(0, false, false));
      return;
    }
    phase = 1; wantMega = false; wantDyna = false;
  }

  void submit(Action a) {
    Action e = AI.choose(b, 1, !b.wild);
    b.turn(a, e);
    enqueue();
  }

  void finish() {
    ended = true; phase = 3;
    final Game.Result res = game.finish(b, null);
    final ArrayList<String> ms = new ArrayList<String>(res.msgs);
    if (res.fled) ms.add(0, "Escapaste del combate.");
    else if (res.won && !res.captured) ms.add(0, b.wild ? "¡Has ganado el combate!" : "¡Has derrotado a " + b.sides[1].name + "!");
    final ArrayList<Cb> steps = new ArrayList<Cb>();
    for (final Object[] l : res.learn) {
      steps.add(new Cb() { public void run() { pushScene(new LearnMoveScene((Mon) l[0], (String) l[1], new Cb() { public void run() { runSteps(steps); } })); } });
    }
    for (final Object[] ev : res.evolve) {
      steps.add(new Cb() { public void run() { pushScene(new EvolutionScene((Mon) ev[0], (Species) ev[1], new Cb() { public void run() { runSteps(steps); } })); } });
    }
    if (game.rules.nuzlocke && res.won && !res.gameOver) {
      boolean any = false; for (Mon m : b.sides[0].party) if (m.dead) any = true;
      if (any) ms.add("En Nuzlocke, los Pokémon debilitados han muerto para siempre...");
    }
    final boolean gameOver = res.gameOver;
    game.checkChampion();
    final boolean champ = game.champion;
    msgs(ms, new Cb() { public void run() {
      stepsFinal = new Cb() { public void run() {
        popTo(BattleScene.this); popScene();
        if (gameOver) { stack.clear(); pushScene(new TitleScene()); pushScene(new GameOverScene()); }
        else if (champ) { pushScene(new VictoryScene()); }
      } };
      runSteps(steps);
    } });
  }

  Cb stepsFinal;
  void runSteps(ArrayList<Cb> steps) {
    if (steps.isEmpty()) { if (stepsFinal != null) stepsFinal.run(); return; }
    Cb c = steps.remove(0);
    c.run();
  }

  // ------------------------------------------------------------------
  void key(int k, char c) {
    if (phase == 0) {
      if (k == K_OK || k == K_BACK) {
        if (cur != null) {
          if (chars < cur.msg.length()) chars = cur.msg.length();
          else cur = null;
        }
      }
      return;
    }
    if (phase == 1) {
      if (k == K_LEFT || k == K_RIGHT) menuSel = menuSel ^ 1;
      else if (k == K_UP || k == K_DOWN) menuSel = menuSel ^ 2;
      else if (k == K_CHAR && (c == 'm' || c == 'M') && b.canMega(b.sides[0])) { wantMega = !wantMega; wantDyna = false; say(wantMega ? "Megaevolución activada: elige un movimiento" : "Megaevolución cancelada"); }
      else if (k == K_CHAR && (c == 'g' || c == 'G') && b.canDynamax(b.sides[0])) { wantDyna = !wantDyna; wantMega = false; say(wantDyna ? "Dinamax activado: elige un movimiento" : "Dinamax cancelado"); }
      else if (k == K_OK) {
        if (menuSel == 0) { phase = 2; moveSel = 0; }
        else if (menuSel == 1) {
          BagScene bs = new BagScene(true, b);
          bs.useCb = new StrCb() { public void run(String s) {
            if (s.isEmpty()) return;
            if (s.startsWith("ball:")) { submit(Action.ball(s.substring(5))); }
            else if (s.startsWith("item:")) { String[] p = s.split(":"); submit(Action.item(p[1], Integer.parseInt(p[2]))); }
          } };
          pushScene(bs);
        } else if (menuSel == 2) {
          pushScene(new PartyScene(1, b, new IntCb() { public void run(int i) {
            if (i < 0) return;
            if (!b.canSwitch(b.sides[0])) { say("¡No puedes cambiar: estás atrapado!"); return; }
            submit(Action.sw(i));
          } }));
        } else {
          if (!b.wild) { say("¡No puedes huir de un combate contra un entrenador!"); return; }
          submit(Action.run());
        }
      }
      return;
    }
    if (phase == 2) {
      Mon u = b.sides[0].active;
      if (k == K_LEFT || k == K_RIGHT) moveSel = moveSel ^ 1;
      else if (k == K_UP || k == K_DOWN) moveSel = moveSel ^ 2;
      else if (k == K_BACK) phase = 1;
      else if (k == K_CHAR && (c == 'm' || c == 'M') && b.canMega(b.sides[0])) { wantMega = !wantMega; wantDyna = false; }
      else if (k == K_CHAR && (c == 'g' || c == 'G') && b.canDynamax(b.sides[0])) { wantDyna = !wantDyna; wantMega = false; }
      else if (k == K_OK) {
        if (moveSel >= u.moves.length) return;
        if (u.pp[moveSel] <= 0) { say("¡No quedan PP para ese movimiento!"); return; }
        if (!b.usableMoves(u).contains(moveSel)) { say("¡No puedes usar ese movimiento ahora!"); return; }
        submit(Action.move(moveSel, wantMega, wantDyna));
      }
    }
  }
}
