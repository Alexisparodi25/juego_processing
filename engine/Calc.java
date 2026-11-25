package pokefuego.engine;
import java.util.*;
import java.util.regex.*;

public class Calc {
  public static final String[] HP_TYPES = {"Fighting","Flying","Poison","Ground","Rock","Bug","Ghost","Steel","Fire","Water","Grass","Electric","Psychic","Ice","Dragon","Dark"};
  public static final Pattern BOOST_PAT = Pattern.compile("(\\w+)-type attacks? have 1\\.2");
  public static final Pattern RESIST_PAT = Pattern.compile("[Hh]alves damage taken from a (supereffective )?(\\w+)-type");
  public static Map<String, String> boostType = new HashMap<String, String>();
  public static Map<String, String[]> resistBerry = new HashMap<String, String[]>();

  public static double stageMult(int s) { return s >= 0 ? (2 + s) / 2.0 : 2.0 / (2 - s); }
  public static double accMult(int s) { return s >= 0 ? (3 + s) / 3.0 : 3.0 / (3 - s); }

  /** Tipo que potencia un objeto ("" si no es de potenciar tipo). */
  public static String itemBoostType(String itemId) {
    if (itemId.isEmpty()) return "";
    String c = boostType.get(itemId);
    if (c != null) return c;
    ItemDef it = Dex.items.get(itemId);
    c = "";
    if (it != null) {
      if (!it.plateType.isEmpty()) c = it.plateType;
      else {
        Matcher m = BOOST_PAT.matcher(it.desc);
        if (m.find() && !itemId.equals("adamantorb") && !itemId.equals("lustrousorb") && !itemId.equals("griseousorb") && !itemId.equals("griseouscore") && !itemId.equals("lustrousglobe") && !itemId.equals("adamantcrystal")) c = m.group(1);
      }
    }
    boostType.put(itemId, c);
    return c;
  }

  /** {tipo, "se"/"any"} si es una baya de resistencia. */
  public static String[] resistInfo(String itemId) {
    if (itemId.isEmpty()) return null;
    if (resistBerry.containsKey(itemId)) return resistBerry.get(itemId);
    String[] r = null;
    ItemDef it = Dex.items.get(itemId);
    if (it != null && it.berry) {
      Matcher m = RESIST_PAT.matcher(it.desc);
      if (m.find()) r = new String[]{m.group(2), m.group(1) == null ? "any" : "se"};
    }
    resistBerry.put(itemId, r);
    return r;
  }

  public static boolean ignoresAbility(Mon a, Move m) {
    return a.hasAbility("moldbreaker") || a.hasAbility("teravolt") || a.hasAbility("turboblaze") || m.id.equals("moongeistbeam") || m.id.equals("sunsteelstrike") || m.id.equals("photongeyser") || m.id.equals("lightthatburnsthesky");
  }

  // ------------------------------------------------------------
  //  Tipo del movimiento
  // ------------------------------------------------------------
  public static String moveType(Battle b, Mon a, Move m) {
    String t = m.type;
    if (m.id.equals("weatherball")) {
      String w = b.weatherNow();
      if (w.equals("sun")) t = "Fire"; else if (w.equals("rain")) t = "Water"; else if (w.equals("sand")) t = "Rock"; else if (w.equals("hail")) t = "Ice";
    } else if (m.id.equals("hiddenpower")) {
      int v = 0; int[] ord = {0, 1, 2, 5, 3, 4};
      for (int i = 0; i < 6; i++) v += (a.iv[ord[i]] & 1) << i;
      t = HP_TYPES[v * 15 / 63];
    } else if (m.id.equals("judgment") || m.id.equals("multiattack") || m.id.equals("technoblast")) {
      ItemDef it = Dex.item(a.item);
      if (it != null && !a.itemConsumed) {
        if (m.id.equals("judgment") && !it.plateType.isEmpty()) t = it.plateType;
        if (m.id.equals("multiattack") && !it.memoryType.isEmpty()) t = it.memoryType;
        if (m.id.equals("technoblast") && !it.driveType.isEmpty()) t = it.driveType;
      }
    } else if (m.id.equals("naturalgift")) {
      ItemDef it = Dex.item(a.item); if (it != null && !it.ngType.isEmpty()) t = it.ngType;
    }
    if (m.isMax.isEmpty()) {
      if (a.hasAbility("normalize")) t = "Normal";
      else if (t.equals("Normal")) {
        if (a.hasAbility("pixilate")) t = "Fairy"; else if (a.hasAbility("aerilate")) t = "Flying";
        else if (a.hasAbility("refrigerate")) t = "Ice"; else if (a.hasAbility("galvanize")) t = "Electric";
        else if (a.hasAbility("dragonize")) t = "Dragon";
      }
    }
    return t;
  }

  // ------------------------------------------------------------
  //  Estadísticas efectivas
  // ------------------------------------------------------------
  public static double stat(Battle b, Mon m, int idx, boolean useStage, boolean physForHustle) {
    double v = m.stat[idx];
    if (useStage) {
      int st = m.stage[idx];
      v *= stageMult(st);
    }
    String ab = m.abilityId(), it = m.itemId();
    String w = b.weatherNow();
    switch (idx) {
      case 1:
        if (ab.equals("hugepower") || ab.equals("purepower")) v *= 2;
        else if (ab.equals("hustle")) v *= 1.5;
        else if (ab.equals("gorillatactics")) v *= 1.5;
        else if (ab.equals("guts") && !m.status.isEmpty()) v *= 1.5;
        else if (ab.equals("defeatist") && m.hpFrac() <= 0.5) v *= 0.5;
        else if (ab.equals("slowstart") && m.turnsOut < 5) v *= 0.5;
        else if (ab.equals("orichalcumpulse") && w.equals("sun")) v *= 1.3333;
        else if (ab.equals("flowergift") && w.equals("sun")) v *= 1.5;
        if (it.equals("choiceband")) v *= 1.5;
        else if (it.equals("thickclub") && (m.normalSp.name.equals("Cubone") || m.normalSp.name.equals("Marowak") || m.normalSp.baseSpecies.equals("Marowak"))) v *= 2;
        else if (it.equals("lightball") && m.normalSp.baseSpecies.equals("Pikachu")) v *= 2;
        break;
      case 2:
        if (ab.equals("marvelscale") && !m.status.isEmpty()) v *= 1.5;
        else if (ab.equals("furcoat")) v *= 2;
        else if (ab.equals("grasspelt") && b.terrain.equals("grassy")) v *= 1.5;
          if (it.equals("eviolite") && !m.normalSp.evos.isEmpty()) v *= 1.5;
        else if (it.equals("metalpowder") && m.normalSp.name.equals("Ditto")) v *= 2;
        break;
      case 3:
        if (ab.equals("solarpower") && w.equals("sun")) v *= 1.5;
        else if (ab.equals("defeatist") && m.hpFrac() <= 0.5) v *= 0.5;
        else if (ab.equals("hadronengine") && b.terrain.equals("electric")) v *= 1.3333;
        if (it.equals("choicespecs")) v *= 1.5;
        else if (it.equals("lightball") && m.normalSp.baseSpecies.equals("Pikachu")) v *= 2;
        else if (it.equals("souldew") && (m.normalSp.baseSpecies.equals("Latios") || m.normalSp.baseSpecies.equals("Latias")) && false) v *= 1.5;
        else if (it.equals("deepseatooth") && m.normalSp.name.equals("Clamperl")) v *= 2;
        break;
      case 4:
        if (w.equals("sand") && m.isType("Rock")) v *= 1.5;
        if (it.equals("assaultvest")) v *= 1.5;
        else if (it.equals("eviolite") && !m.normalSp.evos.isEmpty()) v *= 1.5;
        else if (it.equals("deepseascale") && m.normalSp.name.equals("Clamperl")) v *= 2;
        break;
      case 5:
        if (m.status.equals("par") && !ab.equals("quickfeet")) v *= 0.5;
        if (ab.equals("quickfeet") && !m.status.isEmpty()) v *= 1.5;
        else if (ab.equals("swiftswim") && w.equals("rain")) v *= 2;
        else if (ab.equals("chlorophyll") && w.equals("sun")) v *= 2;
        else if (ab.equals("sandrush") && w.equals("sand")) v *= 2;
        else if (ab.equals("slushrush") && w.equals("hail")) v *= 2;
        else if (ab.equals("surgesurfer") && b.terrain.equals("electric")) v *= 2;
        else if (ab.equals("slowstart") && m.turnsOut < 5) v *= 0.5;
        if (it.equals("choicescarf")) v *= 1.5;
        else if (it.equals("ironball") || it.equals("machobrace") || it.startsWith("power") && it.length() <= 11 && !it.equals("powerherb")) v *= 0.5;
        else if (it.equals("quickpowder") && m.normalSp.name.equals("Ditto")) v *= 2;
        break;
    }
    return v;
  }

  public static double speed(Battle b, Side s, Mon m) {
    double v = stat(b, m, 5, true, false);
    if (s.tailwind > 0) v *= 2;
    return v;
  }

  // ------------------------------------------------------------
  //  Efectividad
  // ------------------------------------------------------------
  public static double typeEff(Battle b, Mon a, Mon d, Move m, String type) {
    if (type.isEmpty() || type.equals("Typeless")) return 1;
    double e1 = Dex.chart[Dex.ti(type)][Dex.ti(d.type1)];
    double e2 = d.type2 == null || d.type2.isEmpty() ? 1 : Dex.chart[Dex.ti(type)][Dex.ti(d.type2)];
    boolean scrappy = a.hasAbility("scrappy") || a.hasAbility("mindseye");
    if (scrappy && (type.equals("Normal") || type.equals("Fighting"))) {
      if (d.type1.equals("Ghost")) e1 = 1;
      if (d.type2 != null && d.type2.equals("Ghost")) e2 = 1;
    }
    if (type.equals("Ground")) {
      if (m.id.equals("thousandarrows")) {
        if (d.type1.equals("Flying")) e1 = 1;
        if (d.type2 != null && d.type2.equals("Flying")) e2 = 1;
      } else if (!d.grounded() && !d.hasItem("ironball")) return 0;
    }
    double e = e1 * e2;
    if (m.id.equals("freezedry") && d.isType("Water")) {
      double w = (d.type1.equals("Water") ? 2.0 / 0.5 : 1) * (d.type2 != null && d.type2.equals("Water") ? 2.0 / 0.5 : 1);
      e *= w;
    }
    if (m.id.equals("flyingpress")) e *= Dex.eff("Flying", d.type1, d.type2);
    if (e > 0 && (d.hasItem("ringtarget"))) e = Math.max(e, 1);
    return e;
  }

  // ------------------------------------------------------------
  //  Poder base
  // ------------------------------------------------------------
  public static int basePower(Battle b, Mon a, Mon d, Move m) {
    double bp = m.bp;
    String id = m.id;
    double ratio;
    switch (id) {
      case "lowkick": case "grassknot": {
        double w = d.sp.weight; bp = w >= 200 ? 120 : w >= 100 ? 100 : w >= 50 ? 80 : w >= 25 ? 60 : w >= 10 ? 40 : 20; break; }
      case "heavyslam": case "heatcrash": {
        double r = a.sp.weight / Math.max(0.1, d.sp.weight); bp = r >= 5 ? 120 : r >= 4 ? 100 : r >= 3 ? 80 : r >= 2 ? 60 : 40; break; }
      case "gyroball": bp = Math.min(150, 25 * speed(b, b.sideOf(d), d) / Math.max(1, speed(b, b.sideOf(a), a)) + 1); break;
      case "electroball": {
        double r = speed(b, b.sideOf(a), a) / Math.max(1, speed(b, b.sideOf(d), d)); bp = r >= 4 ? 150 : r >= 3 ? 120 : r >= 2 ? 80 : r >= 1 ? 60 : 40; break; }
      case "flail": case "reversal": {
        int p = 48 * a.hp / Math.max(1, a.maxHp); bp = p <= 1 ? 200 : p <= 4 ? 150 : p <= 9 ? 100 : p <= 16 ? 80 : p <= 32 ? 40 : 20; break; }
      case "eruption": case "waterspout": case "dragonenergy": bp = Math.max(1, 150 * a.hp / Math.max(1, a.maxHp)); break;
      case "wringout": case "crushgrip": bp = Math.max(1, 120 * d.hp / Math.max(1, d.maxHp)); break;
      case "hex": if (!d.status.isEmpty()) bp *= 2; break;
      case "venoshock": if (d.status.equals("psn") || d.status.equals("tox")) bp *= 2; break;
      case "facade": if (!a.status.isEmpty()) bp *= 2; break;
      case "brine": if (d.hpFrac() <= 0.5) bp *= 2; break;
      case "acrobatics": if (a.itemId().isEmpty()) bp *= 2; break;
      case "knockoff": if (!d.itemId().isEmpty()) bp *= 1.5; break;
      case "smellingsalts": if (d.status.equals("par")) bp *= 2; break;
      case "wakeupslap": if (d.status.equals("slp")) bp *= 2; break;
      case "assurance": if (d.tookDamageThisTurn) bp *= 2; break;
      case "revenge": case "avalanche": if (a.tookDamageThisTurn) bp *= 2; break;
      case "storedpower": case "powertrip": { int s = 0; for (int i = 1; i < 6; i++) if (a.stage[i] > 0) s += a.stage[i]; bp = 20 + 20 * s; break; }
      case "return": bp = 102; break;
      case "frustration": bp = 1; break;
      case "weatherball": if (!b.weatherNow().isEmpty()) bp = 100; break;
      case "solarbeam": case "solarblade": { String w = b.weatherNow(); if (w.equals("rain") || w.equals("sand") || w.equals("hail")) bp *= 0.5; break; }
      case "naturalgift": { ItemDef it = Dex.item(a.item); bp = it != null && it.ngBP > 0 ? it.ngBP : 1; break; }
      case "payback": break;
      case "trumpcard": bp = 40; break;
      case "terrainpulse": if (!b.terrain.isEmpty()) bp *= 2; break;
      case "expandingforce": if (b.terrain.equals("psychic")) bp *= 1.5; break;
      case "risingvoltage": if (b.terrain.equals("electric") && d.grounded()) bp *= 2; break;
    }
    if (bp <= 0) return 0;
    String ab = a.abilityId();
    String type = moveType(b, a, m);
    // habilidades de poder base
    if (ab.equals("technician") && bp <= 60) bp *= 1.5;
    if (ab.equals("ironfist") && m.has("punch")) bp *= 1.2;
    if (ab.equals("stronjaw") || ab.equals("strongjaw")) { if (m.has("bite")) bp *= 1.5; }
    if (ab.equals("megalauncher") && m.has("pulse")) bp *= 1.5;
    if (ab.equals("sharpness") && m.has("slicing")) bp *= 1.5;
    if (ab.equals("toughclaws") && m.contact()) bp *= 1.3;
    if (ab.equals("reckless") && (m.recoil != null || m.id.equals("jumpkick") || m.id.equals("highjumpkick"))) bp *= 1.2;
    if (ab.equals("punkrock") && m.has("sound")) bp *= 1.3;
    if (ab.equals("sandforce") && b.weatherNow().equals("sand") && (type.equals("Rock") || type.equals("Ground") || type.equals("Steel"))) bp *= 1.3;
    if (ab.equals("steelworker") && type.equals("Steel")) bp *= 1.5;
    if (ab.equals("transistor") && type.equals("Electric")) bp *= 1.5;
    if (ab.equals("dragonsmaw") && type.equals("Dragon")) bp *= 1.5;
    if (ab.equals("rockypayload") && type.equals("Rock")) bp *= 1.5;
    if (ab.equals("waterbubble") && type.equals("Water")) bp *= 2;
    if ((ab.equals("pixilate") || ab.equals("aerilate") || ab.equals("refrigerate") || ab.equals("galvanize") || ab.equals("normalize") || ab.equals("dragonize")) && m.type.equals("Normal") && !type.equals("Normal") || ab.equals("normalize") && !m.type.equals("Normal")) bp *= 1.2;
    double pinch = a.hpFrac() <= 1.0 / 3 ? 1.5 : 1;
    if (ab.equals("overgrow") && type.equals("Grass")) bp *= pinch;
    if (ab.equals("blaze") && type.equals("Fire")) bp *= pinch;
    if (ab.equals("torrent") && type.equals("Water")) bp *= pinch;
    if (ab.equals("swarm") && type.equals("Bug")) bp *= pinch;
    if (ab.equals("sheerforce") && m.secChance > 0) bp *= 1.3;
    if (ab.equals("flareboost") && a.status.equals("brn") && m.cat == 'S') bp *= 1.5;
    if (ab.equals("toxicboost") && (a.status.equals("psn") || a.status.equals("tox")) && m.cat == 'P') bp *= 1.5;
    if (ab.equals("supremeoverlord")) bp *= 1.1;
    // terreno
    if (a.grounded()) {
      if (b.terrain.equals("electric") && type.equals("Electric")) bp *= 1.3;
      if (b.terrain.equals("grassy") && type.equals("Grass")) bp *= 1.3;
      if (b.terrain.equals("psychic") && type.equals("Psychic")) bp *= 1.3;
    }
    if (d.grounded() && b.terrain.equals("misty") && type.equals("Dragon")) bp *= 0.5;
    if (b.terrain.equals("grassy") && (id.equals("earthquake") || id.equals("bulldoze") || id.equals("magnitude")) && d.grounded()) bp *= 0.5;
    // objetos
    String it = a.itemId();
    if (!it.isEmpty()) {
      String bt = itemBoostType(it);
      if (!bt.isEmpty() && bt.equals(type)) bp *= 1.2;
      if (it.equals("musclemand") || it.equals("muscleband")) { if (m.cat == 'P') bp *= 1.1; }
      else if (it.equals("wiseglasses")) { if (m.cat == 'S') bp *= 1.1; }
      else if (it.equals("adamantorb") && a.normalSp.baseSpecies.equals("Dialga") && (type.equals("Steel") || type.equals("Dragon"))) bp *= 1.2;
      else if (it.equals("lustrousorb") && a.normalSp.baseSpecies.equals("Palkia") && (type.equals("Water") || type.equals("Dragon"))) bp *= 1.2;
      else if (it.equals("griseousorb") && a.normalSp.baseSpecies.equals("Giratina") && (type.equals("Ghost") || type.equals("Dragon"))) bp *= 1.2;
      else if (it.equals("souldew") && (a.normalSp.baseSpecies.equals("Latios") || a.normalSp.baseSpecies.equals("Latias")) && (type.equals("Psychic") || type.equals("Dragon"))) bp *= 1.2;
      else if (it.equals("punchingglove") && m.has("punch")) bp *= 1.1;
    }
    // defensas por habilidad que modifican el poder (Heatproof etc. se hacen en el daño final)
    return (int) Math.max(1, Math.round(bp));
  }

  // ------------------------------------------------------------
  //  Daño
  // ------------------------------------------------------------
  public static class Res { public int dmg; public double eff = 1; public boolean crit, berry; public int bp; public String type; }

  /** crit: forzar crítico. rand: aplicar aleatoriedad 85-100% (false = promedio, para IA). */
  public static Res damage(Battle b, Side as, Mon a, Side ds, Mon d, Move m, boolean crit, boolean rand) {
    Res r = new Res();
    String type = moveType(b, a, m);
    r.type = type;
    r.eff = typeEff(b, a, d, m, type);
    if (m.bp <= 0 && m.damage.isEmpty()) return r;
    boolean ignoreAb = ignoresAbility(a, m);
    // daño fijo
    if (!m.damage.isEmpty()) {
      if (r.eff == 0 && !m.damage.equals("level")) return r;
      if (m.damage.equals("level")) r.dmg = a.level; else r.dmg = Dex.num(m.damage);
      return r;
    }
    int bp = basePower(b, a, d, m);
    r.bp = bp;
    r.crit = crit;
    if (r.eff == 0) return r;
    boolean phys = m.cat == 'P';
    int atkIdx = phys ? 1 : 3;
    Mon atkMon = a;
    if (m.id.equals("foulplay")) atkMon = d;
    if (m.overrideOff.equals("def")) atkIdx = 2;
    int defIdx = phys ? 2 : 4;
    if ((m.id.equals("psyshock") || m.id.equals("psystrike") || m.id.equals("secretsword"))) defIdx = 2;
    boolean unawareDef = d.hasAbility("unaware") && !ignoreAb;
    boolean unawareAtk = a.hasAbility("unaware");
    int aSt = atkMon.stage[atkIdx], dSt = d.stage[defIdx];
    if (a.hasAbility("simple") || (atkMon == d && false)) aSt *= 2;
    if (crit && aSt < 0) aSt = 0;
    if (crit && dSt > 0) dSt = 0;
    if (unawareDef || m.ignoreDef) aSt = 0;
    if (unawareAtk || m.ignoreDef) dSt = 0;
    double A = atkMon.stat[atkIdx] * stageMult(aSt);
    double D = d.stat[defIdx] * stageMult(dSt);
    // modificadores de estadística (sin etapas)
    A *= statMod(b, atkMon, atkIdx);
    D *= statMod(b, d, defIdx);
    A = Math.max(1, A); D = Math.max(1, D);
    double dmg = Math.floor(Math.floor(Math.floor(2.0 * a.level / 5 + 2) * bp * A / D) / 50) + 2;
    // clima
    String w = b.weatherNow();
    if (w.equals("sun")) { if (type.equals("Fire")) dmg *= 1.5; else if (type.equals("Water") && !m.id.equals("hydrosteam")) dmg *= 0.5; }
    else if (w.equals("rain")) { if (type.equals("Water")) dmg *= 1.5; else if (type.equals("Fire")) dmg *= 0.5; }
    if (crit) dmg *= a.hasAbility("sniper") ? 2.25 : 1.5;
    if (rand) dmg *= (85 + b.rng.nextInt(16)) / 100.0; else dmg *= 0.925;
    // STAB
    boolean stab = a.isType(type) || (m.isMax.length() > 0 && false);
    if (stab) dmg *= a.hasAbility("adaptability") ? 2 : 1.5;
    dmg = Math.floor(dmg * r.eff);
    if (a.status.equals("brn") && phys && !a.hasAbility("guts") && !m.id.equals("facade")) dmg *= 0.5;
    // pantallas
    if (!crit && !a.hasAbility("infiltrator") && !m.id.equals("brickbreak") && !m.id.equals("psychicfangs") && !m.id.equals("ragingbull")) {
      if (ds.auraVeil > 0) dmg *= 0.5;
      else if (phys && ds.reflect > 0) dmg *= 0.5;
      else if (!phys && ds.lightScreen > 0) dmg *= 0.5;
    }
    // habilidades del defensor
    if (!ignoreAb) {
      String da = d.abilityId();
      if (da.equals("multiscale") || da.equals("shadowshield")) { if (d.hp == d.maxHp) dmg *= 0.5; }
      if (r.eff > 1 && (da.equals("solidrock") || da.equals("filter") || da.equals("prismarmor"))) dmg *= 0.75;
      if (da.equals("thickfat") && (type.equals("Fire") || type.equals("Ice"))) dmg *= 0.5;
      if (da.equals("heatproof") && type.equals("Fire")) dmg *= 0.5;
      if (da.equals("waterbubble") && type.equals("Fire")) dmg *= 0.5;
      if (da.equals("dryskin") && type.equals("Fire")) dmg *= 1.25;
      if (da.equals("fluffy")) { if (m.contact()) dmg *= 0.5; if (type.equals("Fire")) dmg *= 2; }
      if (da.equals("icescales") && !phys) dmg *= 0.5;
      if (da.equals("punkrock") && m.has("sound")) dmg *= 0.5;
      if (da.equals("purifyingsalt") && type.equals("Ghost")) dmg *= 0.5;
      if (da.equals("friendguard")) dmg *= 1;
      if (da.equals("fairyaura") || da.equals("darkaura")) dmg *= 1;
    }
    if (a.hasAbility("tintedlens") && r.eff < 1) dmg *= 2;
    if (a.hasAbility("neuroforce") && r.eff > 1) dmg *= 1.25;
    if (a.hasAbility("sheerforce")) { /* ya incluido en el poder base */ }
    // objetos
    String it = a.itemId();
    if (it.equals("lifeorb")) dmg *= 1.3;
    else if (it.equals("expertbelt") && r.eff > 1) dmg *= 1.2;
    String di = d.itemId();
    String[] ri = resistInfo(di);
    if (ri != null && ri[0].equals(type) && (r.eff > 1 || (ri[1].equals("any") && type.equals("Normal")))) { dmg *= 0.5; r.berry = true; }
    if (d.dynamaxed && m.id.equals("dynamaxcannon")) dmg *= 2;
    r.dmg = (int) Math.max(1, Math.floor(dmg));
    return r;
  }

  /** Modificadores de estadística que no son etapas (se aplican al ataque/defensa). */
  public static double statMod(Battle b, Mon m, int idx) {
    double base = m.stat[idx];
    if (base <= 0) return 1;
    double eff = stat(b, m, idx, false, false);
    return eff / base;
  }

  /** Daño esperado en % del HP máximo del defensor (para la IA). */
  public static double expectedPercent(Battle b, Side as, Mon a, Side ds, Mon d, Move m) {
    if (m.cat == 'N' && m.damage.isEmpty()) return 0;
    Res r = damage(b, as, a, ds, d, m, false, false);
    int dmg = r.dmg;
    if (m.multiMax > 1) dmg *= a.hasAbility("skilllink") ? m.multiMax : (m.multiMin == m.multiMax ? m.multiMax : 3);
    if (a.hasAbility("parentalbond")) dmg = (int) (dmg * 1.25);
    // inmunidades por habilidad
    if (!ignoresAbility(a, m) && Battle.absorbs(d, r.type, m)) return 0;
    if (m.acc > 0) return Math.min(dmg, d.hp) * m.acc / 100.0 / Math.max(1, d.maxHp);
    return Math.min(dmg, d.hp) / (double) Math.max(1, d.maxHp);
  }
}
