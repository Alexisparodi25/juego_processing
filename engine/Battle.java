package pokefuego.engine;
import java.util.*;

public class Battle {
  public static final Move STRUGGLE = new Move();
  static {
    STRUGGLE.id = "struggle"; STRUGGLE.name = "Forcejeo"; STRUGGLE.type = "Typeless"; STRUGGLE.cat = 'P'; STRUGGLE.bp = 50; STRUGGLE.acc = 0;
    STRUGGLE.pp = 1; STRUGGLE.flags = "contact|protect"; STRUGGLE.recoil = new int[]{1, 4}; STRUGGLE.target = "normal";
  }
  public static final String[][] MAXMOVES = {
    {"Normal","Max Strike"},{"Fire","Max Flare"},{"Water","Max Geyser"},{"Electric","Max Lightning"},{"Grass","Max Overgrowth"},
    {"Ice","Max Hailstorm"},{"Fighting","Max Knuckle"},{"Poison","Max Ooze"},{"Ground","Max Quake"},{"Flying","Max Airstream"},
    {"Psychic","Max Mindstorm"},{"Bug","Max Flutterby"},{"Rock","Max Rockfall"},{"Ghost","Max Phantasm"},{"Dragon","Max Wyrmwind"},
    {"Dark","Max Darkness"},{"Steel","Max Steelspike"},{"Fairy","Max Starfall"}};

  public Side[] sides = new Side[2];
  public String weather = "", terrain = "";
  public int weatherTurns, terrainTurns, trickRoom;
  public Random rng;
  public List<BEvent> events = new ArrayList<BEvent>();
  public boolean wild, over, caught, escaped, hardMode = true;
  public int winner = -1, turn = 0, runAttempts = 0;
  public boolean[] needSwitch = new boolean[2];
  public Mon caughtMon;
  public Map<Mon, Set<Mon>> participants = new HashMap<Mon, Set<Mon>>();
  public Bag bag;                        // mochila del jugador
  public int catchRateOverride = -1;
  public String evoLock = "";
  public Mon[] pivotPending = new Mon[2];
  public boolean nuzlockeNoCatch, catchEnc;
  public Trainer currentTrainer;
  public Set<String> owned = new HashSet<String>();   // especies ya capturadas (Bola Acopio)

  public boolean bestIsPhys(Mon m) {
    int p = 0, q = 0;
    for (String id : m.moves) { Move mv = Dex.mv(id); if (mv == null) continue; if (mv.cat == 'P') p++; else if (mv.cat == 'S') q++; }
    return p >= q;
  }

  public Battle(Side player, Side enemy, boolean wild, Random rng) {
    sides[0] = player; sides[1] = enemy; this.wild = wild; this.rng = rng;
  }

  // ------------------------------------------------------------------
  //  utilidades
  // ------------------------------------------------------------------
  public Side sideOf(Mon m) { return sides[0].party.contains(m) ? sides[0] : sides[1]; }
  public int idxOf(Mon m) { return sides[0].party.contains(m) ? 0 : 1; }
  public Mon foe(Mon m) { return idxOf(m) == 0 ? sides[1].active : sides[0].active; }
  public Side foeSide(Mon m) { return idxOf(m) == 0 ? sides[1] : sides[0]; }

  public String weatherNow() {
    for (Side s : sides) if (s.active != null && s.active.alive() && (s.active.hasAbility("cloudnine") || s.active.hasAbility("airlock"))) return "";
    return weather;
  }
  public boolean dynamaxTarget(Mon m) { return m.dynamaxed; }

  public void log(String msg) { log(msg, -1, ""); }
  public void log(String msg, int hitSide, String anim) {
    BEvent e = new BEvent();
    e.msg = msg; e.hitSide = hitSide; e.anim = anim;
    for (int i = 0; i < 2; i++) {
      Mon m = sides[i].active;
      if (m == null) continue;
      e.name[i] = m.name(); e.hp[i] = Math.max(0, m.hp); e.maxHp[i] = m.maxHp; e.level[i] = m.level;
      e.status[i] = m.status; e.spId[i] = m.sp.id; e.dyna[i] = m.dynamaxed; e.mega[i] = m.megaed;
    }
    e.weather = weather; e.terrain = terrain;
    events.add(e);
  }
  public List<BEvent> takeEvents() { List<BEvent> l = events; events = new ArrayList<BEvent>(); return l; }

  public static String cap(String s) { return s.isEmpty() ? s : Character.toUpperCase(s.charAt(0)) + s.substring(1); }

  // ------------------------------------------------------------------
  //  inicio
  // ------------------------------------------------------------------
  public void start() {
    for (int i = 0; i < 2; i++) {
      Side s = sides[i];
      Mon first = null;
      for (Mon m : s.party) if (m.alive()) { first = m; break; }
      s.active = first;
      first.resetVolatile();
    }
    if (wild) log("¡Un " + sides[1].active.name() + " salvaje apareció!");
    else log(sides[1].name + " quiere luchar!");
    log("¡Adelante, " + sides[0].active.name() + "!", -1, "send0");
    trackParticipants();
    List<Mon> order = new ArrayList<Mon>(Arrays.asList(sides[0].active, sides[1].active));
    Collections.sort(order, new Comparator<Mon>() {
      public int compare(Mon a, Mon b) { return Double.compare(Calc.speed(Battle.this, sideOf(b), b), Calc.speed(Battle.this, sideOf(a), a)); }
    });
    for (Mon m : order) onEnter(m);
  }

  public void trackParticipants() {
    Mon p = sides[0].active, e = sides[1].active;
    if (p == null || e == null) return;
    Set<Mon> set = participants.get(e);
    if (set == null) { set = new HashSet<Mon>(); participants.put(e, set); }
    set.add(p);
    p.participated = true;
  }

  // ------------------------------------------------------------------
  //  entrada al campo
  // ------------------------------------------------------------------
  public void onEnter(Mon m) {
    Side s = sideOf(m);
    Mon f = foe(m);
    m.turnsOut = 0;
    // trampas
    if (!m.hasItem("heavydutyboots")) {
      if (s.stealthRock > 0 && !m.hasAbility("magicguard")) {
        int d = (int) Math.max(1, m.maxHp * Dex.eff("Rock", m.type1, m.type2) / 8);
        log("¡Unas piedras puntiagudas se clavan en " + m.name() + "!");
        hurt(m, d, null);
      }
      if (s.spikes > 0 && m.grounded() && !m.hasAbility("magicguard")) {
        int d = Math.max(1, m.maxHp / (s.spikes == 1 ? 8 : s.spikes == 2 ? 6 : 4));
        log("¡" + m.name() + " fue herido por las púas!");
        hurt(m, d, null);
      }
      if (s.stickyWeb && m.grounded()) { log("¡" + m.name() + " quedó atrapado en la red viscosa!"); boost(m, 5, -1, null, false); }
      if (s.toxicSpikes > 0 && m.grounded()) {
        if (m.isType("Poison")) { s.toxicSpikes = 0; log("¡" + m.name() + " absorbió las púas tóxicas!"); }
        else setStatus(m, null, s.toxicSpikes >= 2 ? "tox" : "psn", true);
      }
    }
    if (m.fainted()) { faintCheck(m); return; }
    String ab = m.abilityId();
    switch (ab) {
      case "intimidate": case "intrepid": break;
    }
    if (ab.equals("intimidate") && f != null && f.alive()) {
      log("¡La Intimidación de " + m.name() + " baja el Ataque de " + f.name() + "!");
      if (f.hasAbility("innerfocus") || f.hasAbility("oblivious") || f.hasAbility("owntempo") || f.hasAbility("scrappy")) log("¡" + f.name() + " no se inmuta!");
      else if (f.hasAbility("guarddog")) boost(f, 1, 1, m, false);
      else boost(f, 1, -1, m, false);
    }
    if (ab.equals("drizzle")) setWeather("rain", m, 5);
    else if (ab.equals("drought") || ab.equals("orichalcumpulse")) setWeather("sun", m, 5);
    else if (ab.equals("sandstream")) setWeather("sand", m, 5);
    else if (ab.equals("snowwarning")) setWeather("hail", m, 5);
    else if (ab.equals("electricsurge") || ab.equals("hadronengine")) setTerrain("electric", m);
    else if (ab.equals("grassysurge") || ab.equals("seedsower")) setTerrain("grassy", m);
    else if (ab.equals("mistysurge")) setTerrain("misty", m);
    else if (ab.equals("psychicsurge")) setTerrain("psychic", m);
    else if (ab.equals("download") && f != null) {
      boolean spa = f.stat[2] < f.stat[4];
      log("¡" + m.name() + " analizó a su rival!"); boost(m, spa ? 3 : 1, 1, m, true);
    }
    else if (ab.equals("intrepidsword")) boost(m, 1, 1, m, true);
    else if (ab.equals("dauntlessshield")) boost(m, 2, 1, m, true);
    else if (ab.equals("screencleaner")) { for (Side x : sides) { x.reflect = x.lightScreen = x.auraVeil = 0; } log("¡Se disiparon las pantallas!"); }
    else if (ab.equals("pressure")) log("¡" + m.name() + " ejerce su Presión!");
    else if (ab.equals("moldbreaker")) log("¡" + m.name() + " rompe el molde!");
    else if (ab.equals("turboblaze")) log("¡" + m.name() + " irradia un aura ardiente!");
    else if (ab.equals("teravolt")) log("¡" + m.name() + " irradia un aura reventona!");
    else if (ab.equals("trace") && f != null && f.alive()) { m.abilityOverride = f.ability; log("¡" + m.name() + " copió " + Dex.ab(f.ability).dn() + "!"); onEnter2(m); }
    else if (ab.equals("supersweetsyrup") && f != null) boost(f, 7, -1, m, false);
    else if (ab.equals("anticipation") || ab.equals("forewarn") || ab.equals("unnerve")) { }
    // Dinamax: no se puede ser expulsado, etc. (se maneja en los movimientos)
    // formas
    if (ab.equals("forecast") && m.normalSp.name.equals("Castform")) castform(m);
    // objetos de entrada
    if (m.hasItem("airballoon")) log("¡" + m.name() + " flota gracias al Globo Helio!");
    if (m.hasItem("whiteherb")) whiteHerb(m);
  }

  public void onEnter2(Mon m) { // tras Trace
    String ab = m.abilityId();
    if (ab.equals("intimidate")) { Mon f = foe(m); if (f != null && f.alive()) boost(f, 1, -1, m, false); }
  }

  public void castform(Mon m) {
    String w = weatherNow(); String to = "Castform";
    if (w.equals("sun")) to = "Castform-Sunny"; else if (w.equals("rain")) to = "Castform-Rainy"; else if (w.equals("hail")) to = "Castform-Snowy";
    Species s = Dex.sp(to);
    if (s != null && s != m.sp) { m.setSpecies(s); log("¡" + m.name() + " cambió de forma!"); }
  }

  public void whiteHerb(Mon m) {
    boolean any = false;
    for (int i = 1; i < 8; i++) if (m.stage[i] < 0) { m.stage[i] = 0; any = true; }
    if (any) { log("¡" + m.name() + " restauró sus estadísticas con la Hierba Blanca!"); m.itemConsumed = true; }
  }

  public void setWeather(String w, Mon src, int turns) {
    if (weather.equals(w)) return;
    weather = w;
    String it = src == null ? "" : src.itemId();
    if (src != null) {
      if (w.equals("sun") && it.equals("heatrock") || w.equals("rain") && it.equals("dampuock") || w.equals("rain") && it.equals("damprock")
          || w.equals("sand") && it.equals("smoothrock") || w.equals("hail") && it.equals("icyrock")) turns = 8;
    }
    weatherTurns = turns;
    if (w.equals("rain")) log("¡Empezó a llover!"); else if (w.equals("sun")) log("¡El sol brilla intensamente!");
    else if (w.equals("sand")) log("¡Se desató una tormenta de arena!"); else if (w.equals("hail")) log("¡Empezó a granizar!");
    for (Side s : sides) if (s.active != null && s.active.hasAbility("forecast") && s.active.normalSp.name.equals("Castform")) castform(s.active);
  }

  public void setTerrain(String t, Mon src) {
    if (terrain.equals(t)) return;
    terrain = t; terrainTurns = (src != null && src.hasItem("terrainextender")) ? 8 : 5;
    if (t.equals("electric")) log("¡Una corriente eléctrica recorre el campo!");
    else if (t.equals("grassy")) log("¡El campo se cubrió de hierba!");
    else if (t.equals("misty")) log("¡Una niebla cubre el campo!");
    else if (t.equals("psychic")) log("¡El campo se volvió extraño!");
  }

  // ------------------------------------------------------------------
  //  daño directo y curación
  // ------------------------------------------------------------------
  /** Daño indirecto. */
  public int hurt(Mon m, int dmg, Mon src) {
    if (m.fainted() || dmg <= 0) return 0;
    dmg = Math.min(dmg, m.hp);
    m.hp -= dmg;
    log(m.name() + " perdió PS.", idxOf(m), "");
    if (m.fainted()) faintCheck(m);
    else checkBerries(m);
    return dmg;
  }

  public int heal(Mon m, int amount, String why) {
    if (m.fainted() || amount <= 0 || m.hp >= m.maxHp) return 0;
    int a = Math.min(amount, m.maxHp - m.hp);
    m.hp += a;
    log(why == null ? "¡" + m.name() + " recuperó PS!" : why);
    return a;
  }

  public void faintCheck(Mon m) {
    if (!m.fainted() || m.vol.contains("fainted")) return;
    m.vol.add("fainted");
    int s = idxOf(m);
    log("¡" + m.name() + (s == 1 && wild ? " salvaje" : "") + " se debilitó!", s, "faint" + s);
    m.hp = 0; m.status = "";
    if (s == 1) { /* experiencia al final */ }
    needSwitch[s] = true;
    Side side = sides[s];
    if (side.alive() == 0) { needSwitch[s] = false; }
  }

  // ------------------------------------------------------------------
  //  baya y objetos reactivos
  // ------------------------------------------------------------------
  public void checkBerries(Mon m) {
    if (m.fainted() || m.itemConsumed) return;
    String it = m.itemId();
    if (it.isEmpty()) return;
    boolean ripen = m.hasAbility("gluttony");
    double half = 0.5, quarter = ripen ? 0.5 : 0.25;
    if (it.equals("sitrusberry") && m.hpFrac() <= half) { consume(m, "Baya Zidra"); heal(m, m.maxHp / 4, "¡" + m.name() + " recuperó PS con la Baya Zidra!"); }
    else if (it.equals("oranberry") && m.hpFrac() <= half) { consume(m, "Baya Aranja"); heal(m, 10, "¡" + m.name() + " recuperó PS con la Baya Aranja!"); }
    else if (it.equals("berryjuice") && m.hpFrac() <= half) { consume(m, "Zumo de Baya"); heal(m, 20, "¡" + m.name() + " recuperó PS con el Zumo de Baya!"); }
    else if ((it.equals("figyberry") || it.equals("wikiberry") || it.equals("magoberry") || it.equals("aguavberry") || it.equals("iapapaberry")) && m.hpFrac() <= quarter) {
      consume(m, Dex.item(it).dn()); heal(m, m.maxHp / 3, "¡" + m.name() + " recuperó PS con su baya!");
    }
    else if (it.equals("liechiberry") && m.hpFrac() <= quarter) { consume(m, "Baya Lichi"); boost(m, 1, 1, m, true); }
    else if (it.equals("ganlonberry") && m.hpFrac() <= quarter) { consume(m, "Baya Gonlan"); boost(m, 2, 1, m, true); }
    else if (it.equals("salacberry") && m.hpFrac() <= quarter) { consume(m, "Baya Aricoc"); boost(m, 5, 1, m, true); }
    else if (it.equals("petayaberry") && m.hpFrac() <= quarter) { consume(m, "Baya Yapati"); boost(m, 3, 1, m, true); }
    else if (it.equals("apicotberry") && m.hpFrac() <= quarter) { consume(m, "Baya Aricoc"); boost(m, 4, 1, m, true); }
    else if (it.equals("starfberry") && m.hpFrac() <= quarter) { consume(m, "Baya Langsat"); boost(m, 1 + rng.nextInt(5), 2, m, true); }
    else if (it.equals("lumberry") && (!m.status.isEmpty() || m.vol.contains("confusion"))) { consume(m, "Baya Zanama"); cureStatus(m); m.vol.remove("confusion"); m.confusionTurns = 0; }
    else if (it.equals("cheriberry") && m.status.equals("par")) { consume(m, "Baya Zreza"); cureStatus(m); }
    else if (it.equals("chestoberry") && m.status.equals("slp")) { consume(m, "Baya Atania"); cureStatus(m); }
    else if (it.equals("pechaberry") && (m.status.equals("psn") || m.status.equals("tox"))) { consume(m, "Baya Meloc"); cureStatus(m); }
    else if (it.equals("rawstberry") && m.status.equals("brn")) { consume(m, "Baya Safre"); cureStatus(m); }
    else if (it.equals("aspearberry") && m.status.equals("frz")) { consume(m, "Baya Perasi"); cureStatus(m); }
    else if (it.equals("persimberry") && m.vol.contains("confusion")) { consume(m, "Baya Caquic"); m.vol.remove("confusion"); m.confusionTurns = 0; }
    else if (it.equals("leppaberry")) {
      for (int i = 0; i < m.pp.length; i++) if (m.pp[i] == 0) { consume(m, "Baya Zidra"); m.pp[i] = Math.min(m.maxPp[i], 10); log("¡" + m.name() + " restauró PP!"); break; }
    }
    else if (it.equals("whiteherb")) whiteHerb(m);
  }

  public void consume(Mon m, String nm) { m.itemConsumed = true; }

  public void cureStatus(Mon m) {
    if (m.status.isEmpty()) return;
    m.status = ""; m.sleepTurns = 0; m.toxicTurns = 0;
    log("¡" + m.name() + " se curó!");
  }

  // ------------------------------------------------------------------
  //  estados
  // ------------------------------------------------------------------
  public boolean setStatus(Mon t, Mon src, String st, boolean quiet) {
    if (t.fainted() || !t.status.isEmpty()) {
      if (!quiet && src != null) log("¡Pero no tuvo efecto!");
      return false;
    }
    boolean ignore = src != null && src != t && Calc.ignoresAbility(src, Dex.mv("tackle"));
    String ab = ignore ? "" : t.abilityId();
    Side ts = sideOf(t);
    boolean ok = true;
    if (ts.safeguard > 0 && src != t && src != null) { if (!quiet) log("¡Velo Sagrado protege a " + t.name() + "!"); return false; }
    if (st.equals("brn") && (t.isType("Fire") || ab.equals("waterveil") || ab.equals("waterbubble") || ab.equals("thermalexchange") || ab.equals("comatose") && false)) ok = false;
    if ((st.equals("psn") || st.equals("tox")) && ((t.isType("Poison") || t.isType("Steel")) && !(src != null && src.hasAbility("corrosion")) || ab.equals("immunity") || ab.equals("pastelveil"))) ok = false;
    if (st.equals("par") && (t.isType("Electric") || ab.equals("limber"))) ok = false;
    if (st.equals("frz") && (t.isType("Ice") || ab.equals("magmaarmor") || weatherNow().equals("sun"))) ok = false;
    if (st.equals("slp") && (ab.equals("insomnia") || ab.equals("vitalspirit") || ab.equals("sweetveil") || (t.grounded() && terrain.equals("electric")))) ok = false;
    if (ab.equals("purifyingsalt") || ab.equals("comatose")) ok = false;
    if (ab.equals("leafguard") && weatherNow().equals("sun")) ok = false;
    if (t.grounded() && terrain.equals("misty")) ok = false;
    if (!ok) { if (!quiet) log("¡Pero no tuvo efecto en " + t.name() + "!"); return false; }
    t.status = st;
    if (st.equals("slp")) t.sleepTurns = 1 + rng.nextInt(3);
    if (st.equals("tox")) t.toxicTurns = 0;
    String[] msgs = {"brn","¡%s sufrió una quemadura!","par","¡%s está paralizado! ¡Quizá no pueda moverse!","psn","¡%s fue envenenado!","tox","¡%s fue gravemente envenenado!","slp","¡%s se durmió!","frz","¡%s fue congelado!"};
    for (int i = 0; i < msgs.length; i += 2) if (msgs[i].equals(st)) log(String.format(msgs[i + 1], t.name()), -1, "");
    // reacciones
    if (src != null && src != t) {
      if (t.hasAbility("synchronize") && (st.equals("brn") || st.equals("par") || st.equals("psn") || st.equals("tox")) && src.alive() && src.status.isEmpty()) {
        log("¡" + t.name() + " sincronizó su estado!");
        setStatus(src, null, st, true);
      }
    }
    if (t.hasAbility("guts") || t.hasAbility("marvelscale") || t.hasAbility("quickfeet")) { }
    if (t.hasAbility("shedskin") || t.hasAbility("naturalcure")) { }
    checkBerries(t);
    return true;
  }

  public boolean setVolatile(Mon t, Mon src, String v) {
    if (t.fainted()) return false;
    if (v.equals("confusion")) {
      if (t.vol.contains("confusion")) { log("¡" + t.name() + " ya está confuso!"); return false; }
      if (t.hasAbility("owntempo") || (t.grounded() && terrain.equals("misty"))) { log("¡Pero no tuvo efecto en " + t.name() + "!"); return false; }
      t.vol.add("confusion"); t.confusionTurns = 2 + rng.nextInt(4);
      log("¡" + t.name() + " está confuso!");
      checkBerries(t);
      return true;
    }
    if (v.equals("flinch")) {
      if (t.hasAbility("innerfocus") || t.dynamaxed) return false;
      t.vol.add("flinch"); return true;
    }
    if (v.equals("leechseed")) {
      if (t.isType("Grass") || t.vol.contains("leechseed")) { log("¡Pero no tuvo efecto!"); return false; }
      t.vol.add("leechseed"); log("¡" + t.name() + " fue infectado!"); return true;
    }
    if (v.equals("focusenergy")) { t.vol.add("focusenergy"); log("¡" + t.name() + " se está concentrando!"); return true; }
    if (v.equals("taunt")) { return false; }
    return false;
  }

  // ------------------------------------------------------------------
  //  cambios de estadística
  // ------------------------------------------------------------------
  public static final String[] STAT_NAMES = {"", "el Ataque", "la Defensa", "el At. Esp.", "la Def. Esp.", "la Velocidad", "la Precisión", "la Evasión"};

  public void boost(Mon t, int idx, int delta, Mon src, boolean self) {
    if (t.fainted() || delta == 0) return;
    if (t.hasAbility("contrary")) delta = -delta;
    else if (t.hasAbility("simple")) delta *= 2;
    boolean fromFoe = src != null && src != t;
    if (delta < 0 && fromFoe) {
      boolean ignore = Calc.ignoresAbility(src, Dex.mv("tackle"));
      String ab = ignore ? "" : t.abilityId();
      if (ab.equals("clearbody") || ab.equals("whitesmoke") || ab.equals("fullmetalbody") || ab.equals("clearamulet") || t.hasItem("clearamulet")) { log("¡Las estadísticas de " + t.name() + " no bajan!"); return; }
      if (ab.equals("hypercutter") && idx == 1) { log("¡El Ataque de " + t.name() + " no puede bajar!"); return; }
      if (ab.equals("keeneye") && idx == 6) return;
      if (ab.equals("bigpecks") && idx == 2) return;
      if (ab.equals("mirrorarmor")) { boost(src, idx, delta, null, false); return; }
    }
    int old = t.stage[idx];
    int nw = Math.max(-6, Math.min(6, old + delta));
    if (nw == old) { log("¡" + cap(STAT_NAMES[idx]) + " de " + t.name() + " no puede " + (delta > 0 ? "subir" : "bajar") + " más!"); return; }
    t.stage[idx] = nw;
    int d = Math.abs(nw - old);
    String how = d >= 3 ? "aumentó muchísimo" : d == 2 ? (delta > 0 ? "aumentó mucho" : "bajó mucho") : (delta > 0 ? "aumentó" : "bajó");
    if (d >= 3 && delta < 0) how = "bajó muchísimo";
    log("¡" + cap(STAT_NAMES[idx]) + " de " + t.name() + " " + how + "!");
    if (delta < 0 && fromFoe) {
      if (t.hasAbility("defiant")) boost(t, 1, 2, t, true);
      else if (t.hasAbility("competitive")) boost(t, 3, 2, t, true);
    }
    if (delta < 0 && t.hasItem("whiteherb")) whiteHerb(t);
  }

  public void applyBoostString(Mon t, String spec, Mon src, boolean self) {
    if (spec == null || spec.isEmpty()) return;
    for (String p : spec.split("/")) {
      String[] kv = p.split(":");
      int idx = statIdx(kv[0]);
      if (idx > 0) boost(t, idx, Integer.parseInt(kv[1]), src, self);
    }
  }

  public static int statIdx(String k) {
    switch (k) { case "atk": return 1; case "def": return 2; case "spa": return 3; case "spd": return 4; case "spe": return 5; case "accuracy": return 6; case "evasion": return 7; }
    return -1;
  }

  // ------------------------------------------------------------------
  //  inmunidades por habilidad (absorbe el golpe)
  // ------------------------------------------------------------------
  public static boolean absorbs(Mon d, String type, Move m) {
    String ab = d.abilityId();
    if (m.cat == 'N' && m.damage.isEmpty() && !(m.status.length() > 0 || !m.tgtBoost.isEmpty())) return false;
    switch (ab) {
      case "voltabsorb": case "lightningrod": case "motordrive": return type.equals("Electric");
      case "waterabsorb": case "stormdrain": case "dryskin": return type.equals("Water");
      case "flashfire": return type.equals("Fire");
      case "sapsipper": return type.equals("Grass");
      case "levitate": return type.equals("Ground") && !m.id.equals("thousandarrows") && !d.hasItem("ironball");
      case "eartheater": return type.equals("Ground");
      case "wellbakedbody": return type.equals("Fire");
      case "windrider": return m.has("wind") && !m.type.equals("Normal") && false;
      case "soundproof": return m.has("sound");
      case "bulletproof": return m.has("bullet");
      case "wonderguard": return false;
    }
    return false;
  }

  /** ¿Inmune por habilidad? Aplica efecto de absorción. */
  public boolean abilityImmune(Mon u, Mon d, Move m, String type) {
    if (Calc.ignoresAbility(u, m) || u == d) return false;
    String ab = d.abilityId();
    if (!absorbs(d, type, m)) {
      if (ab.equals("wonderguard") && m.cat != 'N' && Calc.typeEff(this, u, d, m, type) <= 1 && m.damage.isEmpty()) {
        log("¡" + d.name() + " es inmune gracias a Superguarda!"); return true;
      }
      if ((ab.equals("queenlymajesty") || ab.equals("dazzling") || ab.equals("armortail")) && prioOf(u, m) > 0) {
        log("¡" + d.name() + " no se deja afectar por movimientos prioritarios!"); return true;
      }
      return false;
    }
    switch (ab) {
      case "voltabsorb": case "waterabsorb": case "dryskin":
        if (d.hp < d.maxHp) heal(d, d.maxHp / 4, "¡" + d.name() + " absorbió el ataque y recuperó PS!"); else log("¡" + d.name() + " no se vio afectado!");
        return true;
      case "lightningrod": boost(d, 3, 1, d, true); return true;
      case "stormdrain": boost(d, 3, 1, d, true); return true;
      case "motordrive": boost(d, 5, 1, d, true); return true;
      case "sapsipper": boost(d, 1, 1, d, true); return true;
      case "eartheater": heal(d, d.maxHp / 4, "¡" + d.name() + " absorbió el ataque y recuperó PS!"); return true;
      case "wellbakedbody": boost(d, 2, 2, d, true); return true;
      case "flashfire":
        if (!d.flashFireOn) { d.flashFireOn = true; log("¡Absorber Fuego de " + d.name() + " potencia sus ataques de Fuego!"); } else log("¡" + d.name() + " no se vio afectado!");
        return true;
      case "levitate": log("¡" + d.name() + " flota y es inmune!"); return true;
      case "soundproof": case "bulletproof": log("¡" + d.name() + " es inmune gracias a su habilidad!"); return true;
    }
    return false;
  }

  public int prioOf(Mon u, Move m) {
    int p = m.prio;
    if (u.hasAbility("prankster") && m.cat == 'N') p += 1;
    if (u.hasAbility("galewings") && m.type.equals("Flying") && u.hp == u.maxHp) p += 1;
    if (u.hasAbility("triage") && (m.heal != null || m.drain != null)) p += 3;
    if (u.hasAbility("stall")) p += 0;
    return p;
  }

  // ------------------------------------------------------------------
  //  Mega y Dinamax
  // ------------------------------------------------------------------
  public boolean canMega(Side s) {
    return !s.transformed && s.active != null && s.active.megaForm() != null && !s.active.dynamaxed;
  }
  public boolean canDynamax(Side s) {
    return !s.transformed && s.active != null && s.active.canDynamax();
  }

  public void doMega(Side s) {
    Mon m = s.active; Species ms = m.megaForm();
    if (ms == null) return;
    s.transformed = true;
    log("¡" + m.name() + " está reaccionando a su piedra activadora!");
    String old = m.ability;
    m.megaed = true;
    m.origAbility = old;
    m.setSpecies(ms);
    m.ability = ms.abilities[0].isEmpty() ? old : ms.abilities[0];
    m.abilityOverride = null;
    m.recalc(false);
    log("¡" + m.name() + " Megaevolucionó en " + ms.name + "!", -1, "mega" + idxOf(m));
    onEnter(m);
  }

  public void doDynamax(Side s) {
    Mon m = s.active;
    if (!m.canDynamax()) return;
    s.transformed = true;
    m.dynamaxed = true; m.lockedMove = ""; m.lockTurns = 0;
    Species g = m.gmaxForm();
    m.recalc(false);          // los PS actuales suben en la misma proporción que los máximos
    if (g != null) { m.sp = g; m.type1 = g.t1; m.type2 = g.t2; }
    log("¡" + m.name() + " se ha " + (g != null ? "Gigamaxizado" : "Dinamaxizado") + "!", -1, "dmax" + idxOf(m));
  }

  /** Versión Max del movimiento (o Max Guard). */
  public Move maxVersion(Mon u, Move base) {
    if (base.cat == 'N') {
      Move g = Dex.mv("maxguard"); return g != null ? g : base;
    }
    String t = Calc.moveType(this, u, base);
    Move g = null;
    if (u.gmaxForm() != null) {
      Move gm = Dex.gmaxMoveOf.get(Dex.toId(u.normalSp.name));
      if (gm == null) gm = Dex.gmaxMoveOf.get(Dex.toId(u.normalSp.baseSpecies));
      if (gm != null && gm.type.equals(t)) g = gm;
    }
    Move m;
    if (g != null) { m = g.copy(); m.bp = 150; }
    else {
      String nm = "Max Strike";
      for (String[] r : MAXMOVES) if (r[0].equals(t)) nm = r[1];
      Move mm = Dex.mv(nm);
      m = mm != null ? mm.copy() : base.copy();
      m.type = t;
      int bp = base.maxBP > 0 ? base.maxBP : maxBpFor(base);
      m.bp = bp;
    }
    m.cat = base.cat; m.acc = 0; m.flags = "protect"; m.target = "normal"; m.multiMin = m.multiMax = 0; m.crit = 1;
    m.recoil = null; m.drain = null; m.heal = null; m.status = ""; m.secChance = 0; m.selfAfter = ""; m.damage = "";
    m.selfSwitch = false; m.forceSwitch = false; m.selfdestruct = false; m.ohko = false; m.id = Dex.toId(m.name);
    m.maxBP = 0; m.overrideOff = base.overrideOff; m.ignoreDef = false; m.willCrit = false; m.supported = true;
    return m;
  }

  public static int maxBpFor(Move b) {
    int p = b.bp; boolean low = b.type.equals("Fighting") || b.type.equals("Poison");
    if (low) return p <= 40 ? 70 : p <= 50 ? 75 : p <= 60 ? 80 : p <= 70 ? 85 : p <= 100 ? 90 : p <= 140 ? 95 : 100;
    return p <= 40 ? 90 : p <= 50 ? 100 : p <= 60 ? 110 : p <= 70 ? 120 : p <= 100 ? 130 : p <= 140 ? 140 : 150;
  }

  /** Movimiento realmente usado, según índice (aplica Dinamax). */
  public Move effectiveMove(Mon u, int idx) {
    if (idx < 0 || idx >= u.moves.length) return STRUGGLE;
    Move base = Dex.mv(u.moves[idx]);
    if (base == null) return STRUGGLE;
    return u.dynamaxed ? maxVersion(u, base) : base;
  }

  // ------------------------------------------------------------------
  //  Turno
  // ------------------------------------------------------------------
  public List<Integer> usableMoves(Mon u) {
    List<Integer> l = new ArrayList<Integer>();
    for (int i = 0; i < u.moves.length; i++) {
      if (u.pp[i] <= 0) continue;
      if (!u.lockedMove.isEmpty() && !u.dynamaxed && !u.moves[i].equals(u.lockedMove)) continue;
      if (u.hasItem("assaultvest") || u.tauntTurns > 0) { Move m = Dex.mv(u.moves[i]); if (m != null && m.cat == 'N') continue; }
      l.add(i);
    }
    return l;
  }

  public boolean mustStruggle(Mon u) { return usableMoves(u).isEmpty() && u.chargingMove.isEmpty() && u.recharge == 0 && u.lockTurns == 0; }

  public boolean canSwitch(Side s) {
    Mon m = s.active;
    if (m == null) return false;
    if (m.vol.contains("trapped") || m.vol.contains("bound")) return false;
    Mon f = foe(m);
    if (m.isType("Ghost")) return true;
    if (m.hasItem("shedshell")) return true;
    if (f != null && f.alive()) {
      if (f.hasAbility("shadowtag") && !m.hasAbility("shadowtag")) return false;
      if (f.hasAbility("arenatrap") && m.grounded()) return false;
      if (f.hasAbility("magnetpull") && m.isType("Steel")) return false;
    }
    return true;
  }

  public void turn(Action a0, Action a1) {
    if (over) return;
    turn++;
    Action[] acts = {a0, a1};
    for (Side s : sides) if (s.active != null) s.active.tookDamageThisTurn = false;
    // 1) cambios y objetos
    for (int pass = 0; pass < 2; pass++) {
      for (int i = 0; i < 2; i++) {
        Action a = acts[i];
        if (a == null || over) continue;
        if (a.type == Action.SWITCH) { doSwitch(sides[i], a.idx, false); acts[i] = null; }
        else if (a.type == Action.ITEM) { useBagItem(a); acts[i] = null; }
        else if (a.type == Action.RUN) { if (tryRun()) { acts[i] = null; escaped = true; over = true; winner = 2; return; } acts[i] = null; }
        else if (a.type == Action.BALL) { if (throwBall(a.item)) { over = true; winner = 2; caught = true; return; } acts[i] = null; }
      }
      break;
    }
    if (over) return;
    // 2) Mega / Dinamax (antes de los movimientos)
    for (int i = 0; i < 2; i++) {
      Action a = acts[i];
      if (a == null || a.type != Action.MOVE) continue;
      Side s = sides[i];
      if (s.active == null || !s.active.alive()) continue;
      if (a.mega && canMega(s)) doMega(s);
      else if (a.dyna && canDynamax(s)) doDynamax(s);
    }
    // 3) orden de ataque
    List<Integer> order = new ArrayList<Integer>();
    for (int i = 0; i < 2; i++) if (acts[i] != null && acts[i].type == Action.MOVE) order.add(i);
    final Move[] chosen = new Move[2];
    for (int i : order) {
      Mon u = sides[i].active;
      chosen[i] = pickMove(u, acts[i]);
    }
    Collections.sort(order, new Comparator<Integer>() {
      public int compare(Integer x, Integer y) {
        Mon ux = sides[x].active, uy = sides[y].active;
        int px = prioOf(ux, chosen[x]), py = prioOf(uy, chosen[y]);
        if (ux.hasItem("quickclaw") && rng.nextInt(5) == 0) px += 0;
        if (px != py) return py - px;
        double sx = Calc.speed(Battle.this, sides[x], ux), sy = Calc.speed(Battle.this, sides[y], uy);
        if (sx != sy) return (sx > sy) == (trickRoom <= 0) ? -1 : 1;
        return rng.nextBoolean() ? -1 : 1;
      }
    });
    for (int i : order) {
      if (over) break;
      Side us = sides[i], ts = sides[1 - i];
      Mon u = us.active;
      if (u == null || !u.alive() || u.vol.contains("fainted")) continue;
      if (needSwitch[0] || needSwitch[1]) { /* un Pokémon cayó antes de que alguien actuara: el rival ya no puede actuar contra nada */ }
      Mon t = ts.active;
      if (t == null || !t.alive()) {
        if (chosen[i] != null && !chosen[i].target.equals("self") && chosen[i].cat != 'N') continue;
      }
      runMove(us, ts, chosen[i], acts[i]);
      checkOver();
    }
    if (over) return;
    endOfTurn();
    checkOver();
  }

  public Move pickMove(Mon u, Action a) {
    if (!u.chargingMove.isEmpty()) return maybeMax(u, Dex.mv(u.chargingMove));
    if (u.recharge > 0) return STRUGGLE;
    if (u.lockTurns > 0 && !u.lockedMove.isEmpty() && !u.dynamaxed) return Dex.mv(u.lockedMove);
    int idx = a.idx;
    if (idx < 0 || idx >= u.moves.length || u.pp[idx] <= 0 || !usableMoves(u).contains(idx)) {
      List<Integer> us = usableMoves(u);
      if (us.isEmpty()) return STRUGGLE;
      idx = us.get(0);
    }
    return effectiveMove(u, idx);
  }

  public Move maybeMax(Mon u, Move m) { return u.dynamaxed ? maxVersion(u, m) : m; }

  public void checkOver() {
    if (over) return;
    boolean p = sides[0].alive() > 0, e = sides[1].alive() > 0;
    if (!p || !e) { over = true; winner = p ? 0 : 1; }
  }

  // ------------------------------------------------------------------
  //  cambios
  // ------------------------------------------------------------------
  public void doSwitch(Side s, int idx, boolean forced) {
    Mon out = s.active;
    Mon in = s.party.get(idx);
    if (out != null && out.alive()) {
      if (!forced) log("¡" + out.name() + ", vuelve!");
      if (out.hasAbility("naturalcure")) { out.status = ""; }
      if (out.hasAbility("regenerator")) out.hp = Math.min(out.maxHp, out.hp + out.maxHp / 3);
      if (out.normalSp.name.equals("Palafin") && out.sp.name.equals("Palafin")) { Species h = Dex.sp("Palafin-Hero"); if (h != null) out.setSpecies(h); }
      out.resetVolatile();
      if (out.dynamaxed || out.megaed) { /* se mantienen: duran todo el combate */ }
    }
    s.active = in;
    in.resetVolatile();
    in.vol.remove("fainted");
    log("¡Adelante, " + in.name() + "!", -1, "send" + (s == sides[0] ? 0 : 1));
    if (s == sides[0] || s == sides[1]) trackParticipants();
    onEnter(in);
    if (in.hasAbility("zerotohero") && false) { }
  }

  public void replace(int sideIdx, int partyIdx) {
    needSwitch[sideIdx] = false;
    Side s = sides[sideIdx];
    Mon out = s.active;
    s.active = s.party.get(partyIdx);
    s.active.resetVolatile();
    s.active.vol.remove("fainted");
    log("¡" + (sideIdx == 0 ? "Adelante, " : sides[1].name + " envía a ") + s.active.name() + "!", -1, "send" + sideIdx);
    trackParticipants();
    onEnter(s.active);
    checkOver();
  }

  // ------------------------------------------------------------------
  //  objetos de la mochila / captura / huida
  // ------------------------------------------------------------------
  public void useBagItem(Action a) {
    Side s = sides[0];
    Mon target = s.party.get(Math.max(0, Math.min(s.party.size() - 1, a.idx)));
    String msg = Items.useOnMon(this, target, a.item, true);
    log(msg);
    if (bag != null) bag.remove(a.item, 1);
  }

  public boolean tryRun() {
    if (!wild) { log("¡No puedes huir de un combate contra un entrenador!"); return false; }
    Mon p = sides[0].active, e = sides[1].active;
    runAttempts++;
    double a = Calc.speed(this, sides[0], p), b = Math.max(1, Calc.speed(this, sides[1], e));
    int f = (int) (a * 128 / b + 30 * runAttempts);
    if (f > 255 || rng.nextInt(256) < f) { log("¡Escapaste sin problemas!"); return true; }
    log("¡No puedes escapar!");
    return false;
  }

  public static int catchRate(Species s) {
    if (s.restricted()) return 3;
    if (s.legendary()) return 25;
    int bst = s.bst();
    return (int) Math.max(3, Math.min(255, 255 - (bst - 180) * 0.58));
  }

  public boolean throwBall(String ballId) {
    Mon e = sides[1].active;
    if (!wild) { log("¡No se puede capturar al Pokémon de otro entrenador!"); return false; }
    if (nuzlockeNoCatch) { log("Nuzlocke: ya usaste tu captura en esta zona."); return false; }
    if (bag != null) bag.remove(ballId, 1);
    ItemDef it = Dex.item(ballId);
    log("¡Lanzaste una " + (it != null ? it.dn() : "Poké Ball") + "!");
    double mult = Items.ballMult(this, e, ballId);
    if (mult >= 255) { finishCatch(e, ballId); return true; }
    double rate = catchRateOverride > 0 ? catchRateOverride : catchRate(e.normalSp);
    double st = e.status.equals("slp") || e.status.equals("frz") ? 2.0 : (!e.status.isEmpty() ? 1.5 : 1.0);
    double a = ((3.0 * e.maxHp - 2.0 * e.hp) * rate * mult) / (3.0 * e.maxHp) * st;
    if (hardMode) a *= 0.85;
    if (a >= 255) { finishCatch(e, ballId); return true; }
    double b = 1048560.0 / Math.sqrt(Math.sqrt(16711680.0 / Math.max(1, a)));
    int shakes = 0;
    for (int i = 0; i < 4; i++) { if (rng.nextInt(65536) < b) shakes++; else break; }
    if (shakes == 4) { finishCatch(e, ballId); return true; }
    log(shakes == 0 ? "¡Oh, no! ¡El Pokémon se escapó de la Poké Ball!" : shakes == 1 ? "¡Ya casi lo atrapas!" : shakes == 2 ? "¡Aaah! ¡Casi lo has atrapado!" : "¡Qué lástima! ¡Casi!");
    return false;
  }

  public void finishCatch(Mon e, String ballId) {
    log("¡Ya está! ¡" + e.name() + " fue atrapado!");
    e.ball = ballId; caughtMon = e;
  }

  // ------------------------------------------------------------------
  //  fin de turno
  // ------------------------------------------------------------------
  public void endOfTurn() {
    List<Side> order = new ArrayList<Side>(Arrays.asList(sides));
    if (Calc.speed(this, sides[1], sides[1].active) > Calc.speed(this, sides[0], sides[0].active)) Collections.reverse(order);
    // clima
    if (!weather.isEmpty()) {
      weatherTurns--;
      if (weatherTurns <= 0) {
        log(weather.equals("rain") ? "Dejó de llover." : weather.equals("sun") ? "Se acabó el sol." : weather.equals("sand") ? "La tormenta de arena amainó." : "Dejó de granizar.");
        weather = "";
        for (Side s : sides) if (s.active != null && s.active.hasAbility("forecast") && s.active.normalSp.name.equals("Castform")) castform(s.active);
      } else if (weatherNow().length() > 0) {
        for (Side s : order) {
          Mon m = s.active;
          if (m == null || !m.alive()) continue;
          boolean immune = m.hasAbility("magicguard") || m.hasAbility("overcoat") || m.hasItem("safetygoggles");
          if (weather.equals("sand") && !immune && !m.isType("Rock") && !m.isType("Ground") && !m.isType("Steel") && !m.hasAbility("sandveil") && !m.hasAbility("sandrush") && !m.hasAbility("sandforce")) {
            log("¡La tormenta de arena zarandea a " + m.name() + "!"); hurt(m, Math.max(1, m.maxHp / 16), null);
          }
          if (weather.equals("hail") && !immune && !m.isType("Ice") && !m.hasAbility("icebody") && !m.hasAbility("snowcloak") && !m.hasAbility("slushrush")) {
            log("¡El granizo golpea a " + m.name() + "!"); hurt(m, Math.max(1, m.maxHp / 16), null);
          }
        }
      }
    }
    for (Side s : order) {
      Mon m = s.active;
      if (m == null || !m.alive()) continue;
      String ab = m.abilityId();
      String w = weatherNow();
      // curaciones
      if (m.hasItem("leftovers")) heal(m, Math.max(1, m.maxHp / 16), "¡" + m.name() + " recuperó PS con Restos!");
      if (m.hasItem("blacksludge")) {
        if (m.isType("Poison")) heal(m, Math.max(1, m.maxHp / 16), "¡" + m.name() + " recuperó PS con el Lodo Negro!");
        else if (!ab.equals("magicguard")) { log("¡El Lodo Negro daña a " + m.name() + "!"); hurt(m, Math.max(1, m.maxHp / 8), null); }
      }
      if (ab.equals("raindish") && w.equals("rain")) heal(m, Math.max(1, m.maxHp / 16), "¡" + m.name() + " recuperó PS con Cura Lluvia!");
      if (ab.equals("icebody") && w.equals("hail")) heal(m, Math.max(1, m.maxHp / 16), "¡" + m.name() + " recuperó PS con Gélido Cuerpo!");
      if (ab.equals("dryskin")) { if (w.equals("rain")) heal(m, Math.max(1, m.maxHp / 8), "¡" + m.name() + " recuperó PS con Piel Seca!"); else if (w.equals("sun")) { log("¡Piel Seca de " + m.name() + " le daña!"); hurt(m, Math.max(1, m.maxHp / 8), null); } }
      if (ab.equals("solarpower") && w.equals("sun")) { log("¡El sol daña a " + m.name() + "!"); hurt(m, Math.max(1, m.maxHp / 8), null); }
      if (ab.equals("poisonheal") && (m.status.equals("psn") || m.status.equals("tox"))) heal(m, Math.max(1, m.maxHp / 8), "¡" + m.name() + " se cura con Antídoto!");
      if (terrain.equals("grassy") && m.grounded()) heal(m, Math.max(1, m.maxHp / 16), "¡La hierba cura a " + m.name() + "!");
      if (!m.alive()) continue;
      // estado
      if (!ab.equals("magicguard") && !(ab.equals("poisonheal"))) {
        if (m.status.equals("brn")) { log("¡" + m.name() + " sufre por la quemadura!"); hurt(m, Math.max(1, m.maxHp / (ab.equals("heatproof") ? 32 : 16)), null); }
        else if (m.status.equals("psn")) { log("¡" + m.name() + " sufre por el veneno!"); hurt(m, Math.max(1, m.maxHp / 8), null); }
        else if (m.status.equals("tox")) { m.toxicTurns++; log("¡" + m.name() + " sufre por el veneno!"); hurt(m, Math.max(1, m.maxHp * Math.min(15, m.toxicTurns) / 16), null); }
      } else if (m.status.equals("tox")) m.toxicTurns++;
      if (!m.alive()) continue;
      // Drenadoras
      if (m.vol.contains("leechseed") && !ab.equals("magicguard")) {
        Mon f = foe(m);
        if (f != null && f.alive()) {
          int d = hurt(m, Math.max(1, m.maxHp / 8), null);
          if (d > 0) heal(f, f.hasItem("bigroot") ? (int) (d * 1.3) : d, "¡Las drenadoras absorben PS de " + m.name() + "!");
        }
      }
      if (!m.alive()) continue;
      // Gigamax: daño residual
      if (s.dot > 0 && !m.isType(dotImmuneType(s.dotName)) && !ab.equals("magicguard")) {
        log("¡" + m.name() + " sufre por " + s.dotName + "!"); hurt(m, Math.max(1, m.maxHp / 6), null);
      }
      if (!m.alive()) continue;
      // habilidades
      if (ab.equals("speedboost") && m.turnsOut > 0) boost(m, 5, 1, m, true);
      if (ab.equals("moody")) { boost(m, 1 + rng.nextInt(5), 2, m, true); }
      if (ab.equals("shedskin") && !m.status.isEmpty() && rng.nextInt(3) == 0) cureStatus(m);
      if (ab.equals("hydration") && w.equals("rain") && !m.status.isEmpty()) cureStatus(m);
      if (ab.equals("baddreams")) { Mon f = foe(m); if (f != null && f.alive() && f.status.equals("slp")) { log("¡" + f.name() + " sufre pesadillas!"); hurt(f, Math.max(1, f.maxHp / 8), null); } }
      if (m.hasItem("flameorb") && m.status.isEmpty()) setStatus(m, m, "brn", true);
      if (m.hasItem("toxicorb") && m.status.isEmpty()) setStatus(m, m, "tox", true);
      checkBerries(m);
    }
    if (trickRoom > 0 && --trickRoom == 0) log("¡Las dimensiones volvieron a la normalidad!");
    // terreno, pantallas, etc.
    if (!terrain.isEmpty() && --terrainTurns <= 0) { log("El campo volvió a la normalidad."); terrain = ""; }
    for (Side s : sides) {
      if (s.reflect > 0 && --s.reflect == 0) log("¡Reflejo se disipó!");
      if (s.lightScreen > 0 && --s.lightScreen == 0) log("¡Pantalla de Luz se disipó!");
      if (s.auraVeil > 0 && --s.auraVeil == 0) log("¡Velo Aurora se disipó!");
      if (s.tailwind > 0 && --s.tailwind == 0) log("¡Se calmó el viento a favor!");
      if (s.dot > 0) { s.dot--; if (s.dot == 0) s.dotName = ""; }
      if (s.safeguard > 0 && --s.safeguard == 0) log("¡Velo Sagrado se disipó!");
      if (s.active != null && s.active.tauntTurns > 0) s.active.tauntTurns--;
      Mon m = s.active;
      if (m != null) { m.turnsOut++; m.vol.remove("flinch"); if (!m.vol.contains("protect")) m.protectCount = 0; m.vol.remove("protect"); m.vol.remove("endure"); }
    }
    // reemplazos / pivote
    for (int i = 0; i < 2; i++) {
      if (pivotPending[i] != null) {
        Mon pm = pivotPending[i]; pivotPending[i] = null;
        if (pm == sides[i].active && pm.alive() && sides[i].hasBackup() && !needSwitch[i]) needSwitch[i] = true;
      }
    }
  }

  public static String dotImmuneType(String name) {
    if (name.contains("Voraz") || name.contains("Wildfire") || name.contains("Centiferno")) return "Fire";
    if (name.contains("Vine") || name.contains("Lash")) return "Grass";
    if (name.contains("Cannonade")) return "Water";
    if (name.contains("Volcalith")) return "Rock";
    return "";
  }

  // ------------------------------------------------------------------
  //  después de un KO
  // ------------------------------------------------------------------
  /** Reparte experiencia entre los participantes del Pokémon enemigo caído. Devuelve mapa Mon -> exp. */
  public Map<Mon, Integer> expAwards(Mon fainted, double trainerMult, double luckyEgg) {
    Map<Mon, Integer> out = new LinkedHashMap<Mon, Integer>();
    Set<Mon> set = participants.get(fainted);
    if (set == null) return out;
    List<Mon> alive = new ArrayList<Mon>();
    for (Mon m : set) if (m.alive()) alive.add(m);
    if (alive.isEmpty()) return out;
    double base = fainted.normalSp.bst() * 0.45;
    double exp = base * fainted.level / 7.0 * trainerMult;
    for (Mon m : alive) {
      double e = exp / alive.size();
      if (m.hasItem("luckyegg")) e *= 1.5;
      out.put(m, Math.max(1, (int) e));
    }
    return out;
  }

  // ==================================================================
  //  EJECUCIÓN DE MOVIMIENTOS
  // ==================================================================
  public static final Set<String> PROTECT_MOVES = new HashSet<String>(Arrays.asList(
    "protect", "detect", "kingsshield", "spikyshield", "banefulbunker", "obstruct", "silktrap", "burningbulwark", "maxguard", "endure"));
  public static final Set<String> SEMI_INVULN = new HashSet<String>(Arrays.asList("fly", "bounce", "dig", "dive", "phantomforce", "shadowforce"));

  public void runMove(Side us, Side ts, Move m, Action act) {
    Mon u = us.active;
    int ui = idxOf(u);
    boolean cont = !u.chargingMove.isEmpty() || (u.lockTurns > 0 && !u.lockedMove.isEmpty() && !u.dynamaxed);
    if (!preMove(u, m)) return;
    Mon t = ts.active;
    // dos turnos
    boolean second = !u.chargingMove.isEmpty();
    if (second) { u.chargingMove = ""; u.vol.remove("invuln"); }
    else if (m.has("charge") && !chargeSkips(m) && !u.dynamaxed) {
      if (!cont) spendPP(u, m, act, ts);
      u.chargingMove = m.id;
      if (SEMI_INVULN.contains(m.id)) u.vol.add("invuln");
      log(chargeMsg(u, m));
      if (u.hasItem("powerherb")) { u.itemConsumed = true; log("¡" + u.name() + " usó la Hierba Única!"); u.chargingMove = ""; u.vol.remove("invuln"); }
      else return;
    }
    if (!cont && !second && m != STRUGGLE && !m.has("charge")) spendPP(u, m, act, ts);
    else if (m == STRUGGLE) { }
    // Elección: bloquea
    if (!u.dynamaxed && act != null && act.type == Action.MOVE && (u.hasItem("choiceband") || u.hasItem("choicespecs") || u.hasItem("choicescarf") || u.hasAbility("gorillatactics")) && act.idx >= 0 && act.idx < u.moves.length && !cont) {
      u.lockedMove = u.moves[act.idx];
    }
    if (m.id.equals("struggle")) log(u.name() + " no tiene movimientos que usar...");
    log("¡" + u.name() + " usó " + m.dn() + "!");
    u.lastMove = m.id;
    if (u.hasAbility("truant")) u.truantSkip = true;
    if ((u.hasAbility("protean") || u.hasAbility("libero")) && !m.id.equals("struggle") && !(u.type1.equals(m.type) && u.type2.isEmpty()) && !u.vol.contains("proteaned")) {
      String ty = Calc.moveType(this, u, m);
      u.type1 = ty; u.type2 = ""; u.vol.add("proteaned"); log("¡" + u.name() + " cambió a tipo " + ty + "!");
    }
    boolean ok;
    if (m.cat == 'N' && m.damage.isEmpty() && m.bp == 0) ok = statusMove(us, ts, u, t, m);
    else ok = attackMove(us, ts, u, t, m, cont || second);
    // bloqueo por furia
    if (m.vol.equals("lockedmove") && !u.dynamaxed) {
      if (!ok || !u.alive()) { u.lockedMove = ""; u.lockTurns = 0; }
      else if (!cont) { u.lockedMove = m.id; u.lockTurns = 1 + rng.nextInt(2); }
      else {
        u.lockTurns--;
        if (u.lockTurns <= 0) { u.lockedMove = ""; if (u.alive()) setVolatile(u, null, "confusion"); }
      }
    }
    if (ok && m.has("recharge") && u.alive() && !u.dynamaxed) u.recharge = 1;
    if (!u.alive()) return;
    // hazard clearing moves
    if (ok && (m.id.equals("rapidspin") || m.id.equals("mortalspin"))) {
      boolean any = us.stealthRock > 0 || us.spikes > 0 || us.toxicSpikes > 0 || u.vol.contains("leechseed");
      us.stealthRock = us.spikes = us.toxicSpikes = 0; u.vol.remove("leechseed");
      if (any) log("¡" + u.name() + " se deshizo de las trampas!");
      if (m.id.equals("rapidspin")) boost(u, 5, 1, u, true);
    }
    if (ok && m.id.equals("defog")) {
      for (Side s : sides) { s.stealthRock = s.spikes = s.toxicSpikes = 0; }
      ts.reflect = ts.lightScreen = ts.auraVeil = 0;
      log("¡El viento despejó las trampas!");
    }
    if (ok && m.id.equals("knockoff") && t != null && t.alive() && !t.itemId().isEmpty() && !Items.unremovable(t)) {
      ItemDef idf = Dex.item(t.item); log("¡" + u.name() + " tiró " + (idf != null ? idf.dn() : "el objeto") + " de " + t.name() + "!"); t.itemConsumed = true;
    }
    if (ok && m.selfSwitch && sides[ui].hasBackup() && u.alive()) pivotPending[ui] = u;
  }

  public boolean chargeSkips(Move m) {
    String w = weatherNow();
    if ((m.id.equals("solarbeam") || m.id.equals("solarblade")) && w.equals("sun")) return true;
    return false;
  }

  public String chargeMsg(Mon u, Move m) {
    switch (m.id) {
      case "fly": return "¡" + u.name() + " voló muy alto!";
      case "bounce": return "¡" + u.name() + " dio un gran salto!";
      case "dig": return "¡" + u.name() + " se escondió bajo tierra!";
      case "dive": return "¡" + u.name() + " se sumergió!";
      case "phantomforce": case "shadowforce": return "¡" + u.name() + " desapareció!";
      case "solarbeam": case "solarblade": return "¡" + u.name() + " está absorbiendo luz solar!";
      case "skullbash": return "¡" + u.name() + " bajó la cabeza!";
    }
    return "¡" + u.name() + " está cargando energía!";
  }

  public void spendPP(Mon u, Move m, Action act, Side ts) {
    if (act == null || act.type != Action.MOVE) return;
    int i = act.idx;
    if (i < 0 || i >= u.pp.length) return;
    int cost = 1;
    Mon t = ts.active;
    if (t != null && t.hasAbility("pressure") && m.cat != 'N') cost = 2;
    u.pp[i] = Math.max(0, u.pp[i] - cost);
    if (u.pp[i] == 0 && u.hasItem("leppaberry")) checkBerries(u);
  }

  /** Comprobaciones antes de mover. false = no se mueve. */
  public boolean preMove(Mon u, Move m) {
    if (u.hasAbility("truant") && u.truantSkip && u.chargingMove.isEmpty()) { u.truantSkip = false; log("¡" + u.name() + " está holgazaneando!"); return false; }
    if (u.recharge > 0) { u.recharge--; log("¡" + u.name() + " debe recargar energía!"); return false; }
    if (u.status.equals("slp")) {
      if (u.sleepTurns > 0) {
        u.sleepTurns--;
        log("¡" + u.name() + " está profundamente dormido!");
        if (!m.id.equals("sleeptalk") && !m.id.equals("snore")) return false;
      } else { u.status = ""; log("¡" + u.name() + " se despertó!"); }
    } else if (u.status.equals("frz")) {
      if (rng.nextInt(5) == 0 || m.has("defrost")) { u.status = ""; log("¡" + u.name() + " se descongeló!"); }
      else { log("¡" + u.name() + " está congelado!"); return false; }
    }
    if (u.vol.contains("flinch")) { u.vol.remove("flinch"); log("¡" + u.name() + " se amedrentó!"); return false; }
    if (u.vol.contains("confusion")) {
      u.confusionTurns--;
      if (u.confusionTurns <= 0) { u.vol.remove("confusion"); log("¡" + u.name() + " ya no está confuso!"); }
      else {
        log("¡" + u.name() + " está confuso!");
        if (rng.nextInt(3) == 0) {
          double dmg = Math.floor(Math.floor(Math.floor(2.0 * u.level / 5 + 2) * 40 * Calc.stat(this, u, 1, true, true) / Math.max(1, Calc.stat(this, u, 2, true, false))) / 50) + 2;
          dmg *= (85 + rng.nextInt(16)) / 100.0;
          log("¡Se hirió a sí mismo en su confusión!", idxOf(u), "");
          int d = (int) Math.max(1, dmg); d = Math.min(d, u.hp); u.hp -= d; log("", idxOf(u), "");
          if (u.fainted()) faintCheck(u); else checkBerries(u);
          return false;
        }
      }
    }
    if (u.status.equals("par") && rng.nextInt(4) == 0) { log("¡" + u.name() + " está paralizado! ¡No se puede mover!"); return false; }
    return true;
  }

  // ------------------------------------------------------------------
  //  movimientos de daño
  // ------------------------------------------------------------------
  public boolean accuracyCheck(Mon u, Mon t, Move m) {
    if (m.acc == 0 || u.hasAbility("noguard") || t.hasAbility("noguard")) return true;
    if (m.has("charge") && false) return true;
    double acc = m.acc;
    if (m.ohko) {
      if (t.level > u.level) return false;
      acc = 30 + u.level - t.level;
      return rng.nextInt(100) < acc;
    }
    String w = weatherNow();
    if (m.id.equals("thunder") || m.id.equals("hurricane")) { if (w.equals("rain")) return true; if (w.equals("sun")) acc = 50; }
    if (m.id.equals("blizzard") && w.equals("hail")) return true;
    int stage = u.stage[6] - t.stage[7];
    if (t.hasAbility("unaware") && !Calc.ignoresAbility(u, m)) stage = u.stage[6];
    if (u.hasAbility("unaware")) stage = -t.stage[7] * 0;
    stage = Math.max(-6, Math.min(6, stage));
    acc *= Calc.accMult(stage);
    if (u.hasAbility("compoundeyes")) acc *= 1.3;
    if (u.hasAbility("hustle") && m.cat == 'P') acc *= 0.8;
    if (u.hasItem("widelens")) acc *= 1.1;
    if (t.hasItem("brightpowder") || t.hasItem("laxincense")) acc *= 0.9;
    if (!Calc.ignoresAbility(u, m)) {
      if (t.hasAbility("sandveil") && w.equals("sand")) acc *= 0.8;
      if (t.hasAbility("snowcloak") && w.equals("hail")) acc *= 0.8;
      if (t.hasAbility("wonderskin") && m.cat == 'N') acc = Math.min(acc, 50);
    }
    return rng.nextInt(100) < acc;
  }

  public boolean isProtected(Mon u, Mon t, Move m) {
    if (!t.vol.contains("protect") && !t.vol.contains("endure_dummy")) return false;
    if (!m.has("protect") || m.breaksProtect) return false;
    if (m.cat == 'N' && m.target.equals("self")) return false;
    return true;
  }

  public boolean attackMove(Side us, Side ts, Mon u, Mon t, Move m, boolean cont) {
    if (t == null || !t.alive()) { log("¡Pero no tuvo efecto!"); return false; }
    // protección
    if (isProtected(u, t, m)) {
      log("¡" + t.name() + " se protegió!");
      if (m.contact()) {
        if (t.vol.contains("kingsshield")) boost(u, 1, -1, t, false);
        else if (t.vol.contains("spikyshield")) hurt(u, Math.max(1, u.maxHp / 8), null);
        else if (t.vol.contains("banefulbunker")) setStatus(u, t, "psn", true);
      }
      if (m.id.equals("highjumpkick") || m.id.equals("jumpkick")) { log("¡" + u.name() + " se estrelló!"); hurt(u, u.maxHp / 2, null); }
      return false;
    }
    String type = Calc.moveType(this, u, m);
    if (t.vol.contains("invuln") && !hitsInvuln(t, m)) { log("¡" + t.name() + " esquivó el ataque!"); return false; }
    if (!accuracyCheck(u, t, m)) {
      log("¡El ataque de " + u.name() + " falló!");
      if (m.id.equals("highjumpkick") || m.id.equals("jumpkick")) { log("¡" + u.name() + " se estrelló!"); hurt(u, u.maxHp / 2, null); }
      return false;
    }
    if (abilityImmune(u, t, m, type)) return false;
    // inmunidades: polvo, balón helio, tipo
    if (m.has("powder") && (t.isType("Grass") || t.hasAbility("overcoat") || t.hasItem("safetygoggles"))) { log("¡No afecta a " + t.name() + "!"); return false; }
    // movimientos con daño especial
    String id = m.id;
    boolean special = false; int fixed = 0;
    if (id.equals("counter") || id.equals("mirrorcoat") || id.equals("metalburst")) {
      boolean okc = id.equals("counter") ? u.lastDamageCat.equals("P") : id.equals("mirrorcoat") ? u.lastDamageCat.equals("S") : !u.lastDamageCat.isEmpty();
      if (!okc || u.lastDamageTaken <= 0) { log("¡Pero falló!"); return false; }
      fixed = id.equals("metalburst") ? u.lastDamageTaken * 3 / 2 : u.lastDamageTaken * 2; special = true;
    } else if (id.equals("superfang") || id.equals("naturesmadness") || id.equals("ruination")) { fixed = Math.max(1, t.hp / 2); special = true; }
    else if (id.equals("endeavor")) { if (t.hp <= u.hp) { log("¡Pero falló!"); return false; } fixed = t.hp - u.hp; special = true; }
    else if (id.equals("finalgambit")) { fixed = u.hp; u.hp = 0; special = true; faintCheck(u); }
    if (m.ohko) {
      if (t.level > u.level || (id.equals("sheercold") && t.isType("Ice")) || t.dynamaxed) { log("¡No afecta a " + t.name() + "!"); return false; }
      special = true; fixed = t.hp; log("¡KO de un golpe!");
    }
    int hits = 1;
    if (m.multiMax > 1 && !u.dynamaxed) {
      if (m.multiMin == m.multiMax) hits = m.multiMax;
      else if (u.hasAbility("skilllink")) hits = m.multiMax;
      else { int r = rng.nextInt(100); hits = r < 35 ? 2 : r < 70 ? 3 : r < 85 ? 4 : 5; if (m.multiMax < 5) hits = m.multiMin + rng.nextInt(m.multiMax - m.multiMin + 1); }
      if (u.hasItem("loadeddice") && hits < 4) hits = 4 + rng.nextInt(2);
    } else if (u.hasAbility("parentalbond") && m.cat != 'N' && m.multiMax <= 1 && !m.selfdestruct && !u.dynamaxed) hits = 2;
    int total = 0, done = 0; boolean anyCrit = false; double effFinal = 1;
    int startHp = t.hp;
    boolean sturdyUsed = false;
    for (int h = 0; h < hits && t.alive() && u.alive(); h++) {
      int hitBp = -1;
      if (id.equals("tripleaxel") || id.equals("triplekick")) {
        if (h > 0 && !accuracyCheck(u, t, m)) break;
      }
      Move mm = m;
      if (id.equals("tripleaxel") || id.equals("triplekick")) { mm = m.copy(); mm.bp = (id.equals("tripleaxel") ? 20 : 10) * (h + 1); }
      boolean crit = rollCrit(u, t, m);
      Calc.Res r = special ? new Calc.Res() : Calc.damage(this, us, u, ts, t, mm, crit, true);
      if (special) { r.dmg = fixed; r.eff = 1; if (m.ohko || m.cat != 'N') { } }
      effFinal = r.eff;
      if (r.eff == 0 && !special) { log("No afecta a " + t.name() + "..."); return h > 0; }
      if (r.dmg <= 0 && m.bp > 0 && !special) { log("¡Pero no tuvo efecto!"); return false; }
      int dmg = r.dmg;
      if (hits == 2 && u.hasAbility("parentalbond") && h == 1) dmg = Math.max(1, dmg / 4);
      if (r.crit) anyCrit = true;
      // Disfraz / Cara de Hielo
      if (!Calc.ignoresAbility(u, m) && t.hasAbility("disguise") && !t.disguiseBroken && (t.normalSp.name.equals("Mimikyu") || t.normalSp.baseSpecies.equals("Mimikyu"))) {
        t.disguiseBroken = true; Species b = Dex.sp("Mimikyu-Busted"); if (b != null) t.setSpecies(b);
        log("¡El disfraz de " + t.name() + " lo protegió!"); hurt(t, Math.max(1, t.maxHp / 8), null); done++; continue;
      }
      if (!Calc.ignoresAbility(u, m) && t.hasAbility("iceface") && m.cat == 'P' && t.normalSp.name.equals("Eiscue") && t.sp.name.equals("Eiscue")) {
        Species b = Dex.sp("Eiscue-Noice"); if (b != null) t.setSpecies(b);
        log("¡La Cara de Hielo de " + t.name() + " absorbió el golpe!"); done++; continue;
      }
      // sobrevivir con 1 PS
      boolean survive = false;
      if (dmg >= t.hp && t.hp == t.maxHp && h == 0 && !Calc.ignoresAbility(u, m) && t.hasAbility("sturdy") && !m.ohko) { survive = true; log("¡" + t.name() + " aguantó con Robustez!"); }
      else if (dmg >= t.hp && t.hp == t.maxHp && t.hasItem("focussash") && !sturdyUsed) { survive = true; t.itemConsumed = true; log("¡" + t.name() + " aguantó con la Banda Focus!"); sturdyUsed = true; }
      else if (dmg >= t.hp && t.hasItem("focusband") && rng.nextInt(10) == 0) { survive = true; t.itemConsumed = true; log("¡" + t.name() + " aguantó con la Cinta Focus!"); }
      else if (dmg >= t.hp && t.vol.contains("endure")) { survive = true; log("¡" + t.name() + " aguantó el golpe!"); }
      if (survive) dmg = t.hp - 1;
      dmg = Math.min(dmg, t.hp);
      int before = t.hp;
      t.hp -= dmg;
      total += dmg; done++;
      t.lastDamageTaken = dmg; t.lastDamageCat = m.cat == 'P' ? "P" : "S"; t.tookDamageThisTurn = true;
      if (r.berry) { log("¡La baya de " + t.name() + " redujo el daño!"); t.itemConsumed = true; }
      log(r.crit ? "¡Un golpe crítico!" : "", idxOf(t), "hit");
      if (h == 0 || hits == 1) {
        if (r.eff > 1) log("¡Es muy eficaz!"); else if (r.eff < 1 && r.eff > 0) log("No es muy eficaz...");
      }
      onHitReactions(u, t, m, dmg, r);
      if (t.fainted()) { faintCheck(t); onKO(u, t, m); break; }
      else checkBerries(t);
      if (u.hasItem("shellbell")) heal(u, Math.max(1, dmg / 8), "¡" + u.name() + " recuperó PS con la Campana Concha!");
      if (m.drain != null && !u.vol.contains("fainted")) {
        int amt = Math.max(1, dmg * m.drain[0] / m.drain[1]);
        if (u.hasItem("bigroot")) amt = (int) (amt * 1.3);
        if (t.hasAbility("liquidooze") && !Calc.ignoresAbility(u, m)) { log("¡" + u.name() + " absorbió lodo líquido!"); hurt(u, amt, null); }
        else heal(u, amt, "¡" + t.name() + " fue drenado!");
      }
      if (!u.alive()) break;
    }
    if (done > 1) log("¡Golpeó " + done + " veces!");
    if (total > 0 || special) {
      // retroceso
      if (m.recoil != null && u.alive() && !u.hasAbility("rockhead") && !u.hasAbility("magicguard")) {
        int rec = id.equals("struggle") ? Math.max(1, u.maxHp / 4) : Math.max(1, total * m.recoil[0] / m.recoil[1]);
        log("¡" + u.name() + " resultó herido por el retroceso!"); hurt(u, rec, null);
      }
      if (u.hasItem("lifeorb") && u.alive() && !u.hasAbility("magicguard") && !(u.hasAbility("sheerforce") && m.secChance > 0)) { log("¡" + u.name() + " perdió PS por la Vidasfera!"); hurt(u, Math.max(1, u.maxHp / 10), null); }
      // efectos secundarios
      if (t.alive() && u.alive() || t.alive()) secondary(u, t, m, ts);
      if (u.alive() && !m.selfAfter.isEmpty() && rng.nextInt(100) < m.selfAfterChance) applyBoostString(u, m.selfAfter, u, true);
      if (m.forceSwitch && t.alive()) forceOut(ts, t, u);
      if (m.isMax.length() > 0) maxEffect(us, ts, u, t, m);
    }
    if (m.selfdestruct && u.alive()) { u.hp = 0; faintCheck(u); }
    return total > 0 || special;
  }

  public boolean hitsInvuln(Mon t, Move m) {
    String id = m.id;
    if (t.chargingMove.equals("dig")) return id.equals("earthquake") || id.equals("magnitude") || id.equals("fissure");
    if (t.chargingMove.equals("fly") || t.chargingMove.equals("bounce")) return id.equals("thunder") || id.equals("gust") || id.equals("twister") || id.equals("hurricane") || id.equals("skyuppercut") || id.equals("smackdown") || id.equals("thousandarrows");
    if (t.chargingMove.equals("dive")) return id.equals("surf") || id.equals("whirlpool");
    return false;
  }

  public boolean rollCrit(Mon u, Mon t, Move m) {
    if (m.cat == 'N') return false;
    if (!Calc.ignoresAbility(u, m) && (t.hasAbility("battlearmor") || t.hasAbility("shellarmor"))) return false;
    if (t.hasItem("luckyegg_dummy")) return false;
    if (m.willCrit) return true;
    if (u.hasAbility("merciless") && (t.status.equals("psn") || t.status.equals("tox"))) return true;
    int stage = m.crit - 1;
    if (u.vol.contains("focusenergy")) stage += 2;
    if (u.hasAbility("superluck")) stage++;
    if (u.hasItem("scopelens") || u.hasItem("razorclaw")) stage++;
    if ((u.hasItem("luckypunch") && u.normalSp.name.equals("Chansey")) || (u.hasItem("leek") && (u.normalSp.name.equals("Farfetch'd") || u.normalSp.name.equals("Sirfetch'd")))) stage += 2;
    int den = stage <= 0 ? 24 : stage == 1 ? 8 : stage == 2 ? 2 : 1;
    return rng.nextInt(den) == 0;
  }

  /** Efectos al recibir un golpe (contacto, habilidades, objetos). */
  public void onHitReactions(Mon u, Mon t, Move m, int dmg, Calc.Res r) {
    boolean ign = Calc.ignoresAbility(u, m);
    String ta = ign ? "" : t.abilityId();
    boolean contact = m.contact() && !u.hasAbility("longreach") && !u.hasItem("protectivepads");
    if (contact && u.alive()) {
      if (ta.equals("roughskin") || ta.equals("ironbarbs")) { log("¡" + u.name() + " fue herido por la piel de " + t.name() + "!"); hurt(u, Math.max(1, u.maxHp / 8), null); }
      if (t.hasItem("rockyhelmet") && u.alive()) { log("¡" + u.name() + " fue herido por el Casco Dentado!"); hurt(u, Math.max(1, u.maxHp / 6), null); }
      if (u.alive() && !u.hasAbility("magicguard") && !u.hasAbility("longreach") && t.alive() || t.fainted() && ta.equals("aftermath")) {
        if (ta.equals("static") && rng.nextInt(10) < 3) setStatus(u, t, "par", true);
        else if (ta.equals("flamebody") && rng.nextInt(10) < 3) setStatus(u, t, "brn", true);
        else if (ta.equals("poisonpoint") && rng.nextInt(10) < 3) setStatus(u, t, "psn", true);
        else if (ta.equals("effectspore") && rng.nextInt(10) < 3) setStatus(u, t, new String[]{"psn", "par", "slp"}[rng.nextInt(3)], true);
        else if (ta.equals("poisontouch") && false) { }
        else if (ta.equals("gooey") || ta.equals("tanglinghair")) boost(u, 5, -1, t, false);
        else if (ta.equals("mummy") && !u.hasAbility("mummy")) { u.abilityOverride = "Mummy"; log("¡La habilidad de " + u.name() + " se convirtió en Momia!"); }
        else if (ta.equals("aftermath") && t.fainted()) { log("¡" + u.name() + " fue herido por Resquicio!"); hurt(u, Math.max(1, u.maxHp / 4), null); }
      }
    }
    if (u.alive() && u.hasAbility("poisontouch") && contact && rng.nextInt(10) < 3) setStatus(t, u, "psn", true);
    if (t.alive()) {
      if (ta.equals("stamina")) boost(t, 2, 1, t, true);
      else if (ta.equals("weakarmor") && m.cat == 'P') { boost(t, 2, -1, t, true); boost(t, 5, 2, t, true); }
      else if (ta.equals("watercompaction") && r.type.equals("Water")) boost(t, 2, 2, t, true);
      else if (ta.equals("justified") && r.type.equals("Dark")) boost(t, 1, 1, t, true);
      else if (ta.equals("rattled") && (r.type.equals("Dark") || r.type.equals("Bug") || r.type.equals("Ghost"))) boost(t, 5, 1, t, true);
      else if (ta.equals("angerpoint") && r.crit) { t.stage[1] = 6; log("¡" + t.name() + " se enfureció y maximizó su Ataque!"); }
      else if (ta.equals("steamengine") && (r.type.equals("Water") || r.type.equals("Fire"))) boost(t, 5, 6, t, true);
      else if (ta.equals("berserk") && t.hp * 2 <= t.maxHp && t.hp + dmg * 1 > t.maxHp / 2) boost(t, 3, 1, t, true);
      else if (ta.equals("cursedbody") && false) { }
      if (t.hasItem("weaknesspolicy") && r.eff > 1) { t.itemConsumed = true; log("¡" + t.name() + " usó la Política de Debilidad!"); boost(t, 1, 2, t, true); boost(t, 3, 2, t, true); }
      else if (t.hasItem("absorbbulb") && r.type.equals("Water")) { t.itemConsumed = true; boost(t, 3, 1, t, true); }
      else if (t.hasItem("cellbattery") && r.type.equals("Electric")) { t.itemConsumed = true; boost(t, 1, 1, t, true); }
      else if (t.hasItem("snowball") && r.type.equals("Ice")) { t.itemConsumed = true; boost(t, 1, 1, t, true); }
      else if (t.hasItem("luminousmoss") && r.type.equals("Water")) { t.itemConsumed = true; boost(t, 4, 1, t, true); }
      else if (t.hasItem("airballoon")) { t.itemConsumed = true; log("¡El Globo Helio de " + t.name() + " explotó!"); }
      else if (t.hasItem("redcard") && foeSide(t).hasBackup() && !u.dynamaxed) { t.itemConsumed = true; log("¡" + t.name() + " sacó la Tarjeta Roja!"); forceOut(foeSide(t), u, t); }
      else if (t.hasItem("ejectbutton") && sideOf(t).hasBackup()) { t.itemConsumed = true; pivotPending[idxOf(t)] = t; log("¡" + t.name() + " usó el Botón Escape!"); }
    }
    if (!t.alive() && t.vol.contains("fainted")) { }
    // Dinamax: Tipo Cinta Focus etc. no aplica
    if (u.alive() && (u.hasItem("kingsrock") || u.hasItem("razorfang")) && t.alive() && m.secVol.isEmpty() && rng.nextInt(10) == 0) setVolatile(t, u, "flinch");
  }

  public void onKO(Mon u, Mon t, Move m) {
    if (!u.alive()) return;
    String ab = u.abilityId();
    if (ab.equals("moxie") || ab.equals("chillingneigh") || ab.equals("asoneglastrier")) boost(u, 1, 1, u, true);
    else if (ab.equals("grimneigh") || ab.equals("asonespectrier")) boost(u, 3, 1, u, true);
    else if (ab.equals("beastboost")) {
      int best = 1; for (int i = 2; i < 6; i++) if (u.stat[i] > u.stat[best]) best = i;
      boost(u, best, 1, u, true);
    } else if (ab.equals("soulheart")) boost(u, 3, 1, u, true);
    else if (ab.equals("battlebond") && false) { }
  }

  public void secondary(Mon u, Mon t, Move m, Side ts) {
    if (!t.alive()) return;
    if (u.hasAbility("sheerforce") && m.secChance > 0) return;
    if (t.hasAbility("shielddust") && !Calc.ignoresAbility(u, m)) return;
    if (t.hasItem("covertcloak")) return;
    if (m.secChance <= 0) return;
    int chance = m.secChance * (u.hasAbility("serenegrace") ? 2 : 1);
    if (rng.nextInt(100) >= chance) return;
    if (!m.secStatus.isEmpty()) setStatus(t, u, m.secStatus, true);
    if (!m.secVol.isEmpty()) setVolatile(t, u, m.secVol);
    if (!m.secBoost.isEmpty()) applyBoostString(t, m.secBoost, u, false);
  }

  public void forceOut(Side ts, Mon t, Mon src) {
    if (t.dynamaxed || !t.alive() || t.hasAbility("suctioncups")) { log("¡Pero no tuvo efecto!"); return; }
    if (wild && ts == sides[1]) {
      log("¡" + t.name() + " huyó!"); sides[1].active.hp = 0; over = true; winner = 2; escaped = true; return;
    }
    List<Integer> opts = new ArrayList<Integer>();
    for (int i = 0; i < ts.party.size(); i++) if (ts.party.get(i).alive() && ts.party.get(i) != ts.active) opts.add(i);
    if (opts.isEmpty()) { log("¡Pero no tuvo efecto!"); return; }
    log("¡" + t.name() + " fue arrastrado fuera!");
    doSwitch(ts, opts.get(rng.nextInt(opts.size())), true);
  }

  // ------------------------------------------------------------------
  //  efectos de movimientos Max / G-Max
  // ------------------------------------------------------------------
  public void maxEffect(Side us, Side ts, Mon u, Mon t, Move m) {
    String n = m.name;
    switch (n) {
      case "Max Flare": setWeather("sun", u, 5); break;
      case "Max Geyser": setWeather("rain", u, 5); break;
      case "Max Rockfall": setWeather("sand", u, 5); break;
      case "Max Hailstorm": setWeather("hail", u, 5); break;
      case "Max Lightning": setTerrain("electric", u); break;
      case "Max Overgrowth": setTerrain("grassy", u); break;
      case "Max Starfall": setTerrain("misty", u); break;
      case "Max Mindstorm": setTerrain("psychic", u); break;
      case "Max Knuckle": boost(u, 1, 1, u, true); break;
      case "Max Steelspike": boost(u, 2, 1, u, true); break;
      case "Max Ooze": boost(u, 3, 1, u, true); break;
      case "Max Quake": boost(u, 4, 1, u, true); break;
      case "Max Airstream": boost(u, 5, 1, u, true); break;
      case "Max Wyrmwind": if (t.alive()) boost(t, 1, -1, u, false); break;
      case "Max Phantasm": if (t.alive()) boost(t, 2, -1, u, false); break;
      case "Max Flutterby": if (t.alive()) boost(t, 3, -1, u, false); break;
      case "Max Darkness": if (t.alive()) boost(t, 4, -1, u, false); break;
      case "Max Strike": if (t.alive()) boost(t, 5, -1, u, false); break;
      case "G-Max Wildfire": case "G-Max Vine Lash": case "G-Max Cannonade": case "G-Max Volcalith": case "G-Max Centiferno":
        ts.dot = 4; ts.dotName = n; log("¡Los rivales fueron atrapados por " + n + "!"); break;
      case "G-Max Steelsurge": case "G-Max Stonesurge": if (ts.stealthRock == 0) { ts.stealthRock = 1; log("¡Hay piedras flotando alrededor del rival!"); } break;
      case "G-Max Stun Shock": if (t.alive() && rng.nextBoolean()) setStatus(t, u, rng.nextBoolean() ? "par" : "psn", true); break;
      case "G-Max Volt Crash": if (t.alive()) setStatus(t, u, "par", true); break;
      case "G-Max Malodor": if (t.alive()) setStatus(t, u, "psn", true); break;
      case "G-Max Befuddle": if (t.alive()) setStatus(t, u, new String[]{"par", "psn", "slp"}[rng.nextInt(3)], true); break;
      case "G-Max Snooze": if (t.alive() && rng.nextInt(2) == 0) setStatus(t, u, "slp", true); break;
      case "G-Max Smite": if (t.alive()) setVolatile(t, u, "confusion"); break;
      case "G-Max Foam Burst": if (t.alive()) boost(t, 5, -2, u, false); break;
      case "G-Max Tartness": if (t.alive()) boost(t, 7, -1, u, false); break;
      case "G-Max Finale": heal(u, Math.max(1, u.maxHp / 6), "¡" + u.name() + " recuperó PS!"); break;
      case "G-Max Sweetness": case "G-Max Replenish": if (!u.status.isEmpty()) cureStatus(u); break;
      case "G-Max Resonance": us.auraVeil = 5; log("¡Un velo aurora cubre al equipo!"); break;
      case "G-Max Chi Strike": u.vol.add("focusenergy"); log("¡" + u.name() + " se concentra!"); break;
      case "G-Max Depletion": if (t.alive()) for (int i = 0; i < t.pp.length; i++) if (t.moves[i].equals(t.lastMove)) t.pp[i] = Math.max(0, t.pp[i] - 2); break;
      case "G-Max Gravitas": case "G-Max Terror": case "G-Max Meltdown": case "G-Max Cuddle": case "G-Max Sandblast": case "G-Max Hydrosnipe": case "G-Max Rapid Flow": case "G-Max One Blow":
      case "G-Max Gold Rush": case "G-Max Fireball": case "G-Max Drum Solo": case "G-Max Wind Rage": case "G-Max Foam": case "G-Max Resonance2": break;
    }
  }

  // ------------------------------------------------------------------
  //  movimientos de estado
  // ------------------------------------------------------------------
  public boolean statusMove(Side us, Side ts, Mon u, Mon t, Move m) {
    String id = m.id;
    boolean selfTarget = m.target.equals("self") || m.target.equals("allySide") || m.target.equals("allyTeam") || m.target.equals("all") || m.target.equals("foeSide") && false;
    if (PROTECT_MOVES.contains(id)) {
      boolean ok = id.equals("endure") || true;
      double chance = Math.pow(1.0 / 3, Math.min(u.protectCount, 6));
      if (u.protectCount > 0 && rng.nextDouble() > chance) { log("¡Pero falló!"); u.protectCount = 0; return false; }
      u.protectCount++;
      if (id.equals("endure")) { u.vol.add("endure"); log("¡" + u.name() + " se prepara para aguantar!"); return true; }
      u.vol.add("protect"); if (id.equals("kingsshield") || id.equals("spikyshield") || id.equals("banefulbunker")) u.vol.add(id);
      log("¡" + u.name() + " se está protegiendo!");
      return true;
    }
    // ataques que afectan al rival: protección, precisión, inmunidades
    if (!selfTarget && t != null) {
      if (!t.alive()) { log("¡Pero falló!"); return false; }
      if (isProtected(u, t, m)) { log("¡" + t.name() + " se protegió!"); return false; }
      if (t.vol.contains("invuln")) { log("¡" + t.name() + " esquivó el ataque!"); return false; }
      if (!accuracyCheck(u, t, m)) { log("¡El ataque de " + u.name() + " falló!"); return false; }
      if (abilityImmune(u, t, m, Calc.moveType(this, u, m))) return false;
      if (m.has("powder") && (t.isType("Grass") || t.hasAbility("overcoat"))) { log("¡No afecta a " + t.name() + "!"); return false; }
      if (id.equals("thunderwave") && t.isType("Ground")) { log("No afecta a " + t.name() + "..."); return false; }
      if (t.hasAbility("prankster_dummy")) return false;
      if (u.hasAbility("prankster") && t.isType("Dark") && !m.target.equals("self")) { log("No afecta a " + t.name() + "..."); return false; }
    }
    switch (id) {
      case "rest": {
        if (u.hp == u.maxHp || u.status.equals("slp")) { log("¡Pero falló!"); return false; }
        u.status = ""; u.hp = u.maxHp; u.status = "slp"; u.sleepTurns = 2; log("¡" + u.name() + " se durmió y recuperó la salud!"); return true; }
      case "haze": for (Side s : sides) if (s.active != null) Arrays.fill(s.active.stage, 0); log("¡Los cambios de estadísticas se eliminaron!"); return true;
      case "bellydrum": if (u.hp * 2 <= u.maxHp || u.stage[1] >= 6) { log("¡Pero falló!"); return false; } u.hp -= u.maxHp / 2; u.stage[1] = 6; log("¡" + u.name() + " recortó sus PS y maximizó su Ataque!"); return true;
      case "healbell": case "aromatherapy": for (Mon p : us.party) { if (!p.status.isEmpty() && p.alive()) { p.status = ""; } } log("¡Se curaron los estados del equipo!"); return true;
      case "painsplit": { int avg = (u.hp + t.hp) / 2; u.hp = Math.min(u.maxHp, avg); t.hp = Math.min(t.maxHp, avg); log("¡Los dos compartieron sus PS!"); return true; }
      case "sleeptalk": {
        if (!u.status.equals("slp")) { log("¡Pero falló!"); return false; }
        List<Integer> c = new ArrayList<Integer>();
        for (int i = 0; i < u.moves.length; i++) if (!u.moves[i].equals("sleeptalk") && u.pp[i] > 0) c.add(i);
        if (c.isEmpty()) { log("¡Pero falló!"); return false; }
        Move mv = effectiveMove(u, c.get(rng.nextInt(c.size())));
        String keep = u.status; u.status = "";
        log("¡" + u.name() + " usó " + mv.dn() + " dormido!");
        boolean r = (mv.cat == 'N' && mv.bp == 0) ? statusMove(us, ts, u, t, mv) : attackMove(us, ts, u, t, mv, true);
        if (u.status.isEmpty()) u.status = keep;
        return r; }
      case "metronome": {
        List<Move> all = new ArrayList<Move>();
        for (Move x : Dex.moves.values()) if (x.supported && x.isMax.isEmpty() && !x.id.equals("metronome") && !x.id.equals("sleeptalk") && x.pp > 0) all.add(x);
        Move mv = all.get(rng.nextInt(all.size()));
        log("¡" + u.name() + " agitó un dedo y usó " + mv.dn() + "!");
        return (mv.cat == 'N' && mv.bp == 0) ? statusMove(us, ts, u, t, mv) : attackMove(us, ts, u, t, mv, true); }
      case "splash": case "celebrate": case "holdhands": log("¡Pero no pasó nada!"); return true;
      case "teleport": case "batonpass": case "partingshot": break;
      case "taunt": { if (t.tauntTurns > 0) { log("¡Pero falló!"); return false; } t.tauntTurns = 3; log("¡" + t.name() + " fue provocado!"); return true; }
      case "trickroom": { if (trickRoom > 0) { trickRoom = 0; log("¡Las dimensiones volvieron a la normalidad!"); } else { trickRoom = 5; log("¡" + u.name() + " retorció las dimensiones!"); } return true; }
      case "growth": boost(u, 1, weatherNow().equals("sun") ? 2 : 1, u, true); boost(u, 3, weatherNow().equals("sun") ? 2 : 1, u, true); return true;
    }
    boolean did = false;
    // clima / terreno
    if (!m.weather.isEmpty()) {
      String w = m.weather.toLowerCase();
      String nw = w.contains("rain") ? "rain" : w.contains("sun") ? "sun" : w.contains("sand") ? "sand" : (w.contains("hail") || w.contains("snow")) ? "hail" : "";
      if (!nw.isEmpty()) { if (weather.equals(nw)) { log("¡Pero falló!"); return false; } setWeather(nw, u, 5); did = true; }
    }
    if (!m.terrain.isEmpty()) {
      String tr = m.terrain.toLowerCase();
      String nt = tr.contains("electric") ? "electric" : tr.contains("grassy") ? "grassy" : tr.contains("misty") ? "misty" : tr.contains("psychic") ? "psychic" : "";
      if (!nt.isEmpty()) { if (terrain.equals(nt)) { log("¡Pero falló!"); return false; } setTerrain(nt, u); did = true; }
    }
    // condiciones de lado
    if (!m.sideCond.isEmpty()) {
      String sc = m.sideCond.toLowerCase();
      if (sc.equals("stealthrock")) { if (ts.stealthRock > 0) { log("¡Pero falló!"); return false; } ts.stealthRock = 1; log("¡Rocas puntiagudas flotan alrededor del rival!"); did = true; }
      else if (sc.equals("spikes")) { if (ts.spikes >= 3) { log("¡Pero falló!"); return false; } ts.spikes++; log("¡Se esparcieron púas alrededor del rival!"); did = true; }
      else if (sc.equals("toxicspikes")) { if (ts.toxicSpikes >= 2) { log("¡Pero falló!"); return false; } ts.toxicSpikes++; log("¡Se esparcieron púas tóxicas!"); did = true; }
      else if (sc.equals("reflect")) { if (us.reflect > 0) { log("¡Pero falló!"); return false; } us.reflect = u.hasItem("lightclay") ? 8 : 5; log("¡Reflejo protege a tu equipo!"); did = true; }
      else if (sc.equals("lightscreen")) { if (us.lightScreen > 0) { log("¡Pero falló!"); return false; } us.lightScreen = u.hasItem("lightclay") ? 8 : 5; log("¡Pantalla de Luz protege a tu equipo!"); did = true; }
      else if (sc.equals("auroraveil")) { if (us.auraVeil > 0 || !weather.equals("hail")) { log("¡Pero falló!"); return false; } us.auraVeil = u.hasItem("lightclay") ? 8 : 5; log("¡Velo Aurora protege a tu equipo!"); did = true; }
      else if (sc.equals("stickyweb")) { if (ts.stickyWeb) { log("¡Pero falló!"); return false; } ts.stickyWeb = true; log("¡Una red viscosa cubre el campo rival!"); did = true; }
      else if (sc.equals("safeguard")) { if (us.safeguard > 0) { log("¡Pero falló!"); return false; } us.safeguard = 5; log("¡Velo Sagrado protege a tu equipo!"); did = true; }
      else if (sc.equals("tailwind")) { if (us.tailwind > 0) { log("¡Pero falló!"); return false; } us.tailwind = 4; log("¡Sopla un viento a favor!"); did = true; }
    }
    // curación
    boolean climateHeal = id.equals("synthesis") || id.equals("moonlight") || id.equals("morningsun") || id.equals("shoreup");
    if ((m.heal != null || climateHeal) && (m.target.equals("self") || m.target.equals("allies") )) {
      int num = m.heal != null ? m.heal[0] : 1, den = m.heal != null ? m.heal[1] : 2;
      if (id.equals("synthesis") || id.equals("moonlight") || id.equals("morningsun")) {
        String w = weatherNow(); if (w.equals("sun")) { num = 2; den = 3; } else if (!w.isEmpty()) { num = 1; den = 4; }
      } else if (id.equals("shoreup") && weatherNow().equals("sand")) { num = 2; den = 3; }
      if (u.hp >= u.maxHp) { log("¡Pero falló!"); return false; }
      heal(u, Math.max(1, u.maxHp * num / den), "¡" + u.name() + " recuperó PS!");
      did = true;
    }
    // mejoras propias
    if (!m.selfBoost.isEmpty()) { applyBoostString(u, m.selfBoost, u, true); did = true; }
    // efectos sobre el rival
    if (!selfTarget && t != null && t.alive()) {
      if (!m.status.isEmpty()) { if (!setStatus(t, u, m.status, false)) return did; did = true; }
      if (!m.tgtBoost.isEmpty()) { applyBoostString(t, m.tgtBoost, u, false); did = true; }
      if (!m.vol.isEmpty() && (m.vol.equals("confusion") || m.vol.equals("leechseed"))) { if (!setVolatile(t, u, m.vol)) return did; did = true; }
      if (m.forceSwitch) { forceOut(ts, t, u); did = true; }
    }
    if (m.vol.equals("focusenergy")) { setVolatile(u, u, "focusenergy"); did = true; }
    if (!did) { log("¡Pero no tuvo efecto!"); return false; }
    return true;
  }
}
