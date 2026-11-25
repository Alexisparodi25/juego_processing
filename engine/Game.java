package pokefuego.engine;
import java.util.*;

public class Game {
  public Rules rules = new Rules();
  public Random rng = new Random();
  public long seed;
  public List<Place> places = new ArrayList<Place>();
  public int placeIdx = 0;
  public List<Mon> party = new ArrayList<Mon>();
  public List<Mon> box = new ArrayList<Mon>();
  public List<Mon> graveyard = new ArrayList<Mon>();
  public Bag bag = new Bag();
  public int money = 3000, badges = 0;
  public boolean gameOver, champion;
  public String starterId = "";
  public Set<String> owned = new HashSet<String>();      // especies base capturadas / vistas (Nuzlocke duplicados)
  public Set<String> dex = new HashSet<String>();        // Pokédex: especies vistas/capturadas
  public int battlesWon, deaths, steps;

  // ------------------------------------------------------------
  //  nueva partida
  // ------------------------------------------------------------
  public Game(Rules r, long seed) {
    this.rules = r; this.seed = seed; this.rng = new Random(seed);
    buildWorld();
    bag.add("pokeball", 5); bag.add("potion", 3);
    money = 3000;
  }

  public List<String> starterChoices() {
    List<String> out = new ArrayList<String>();
    if (!rules.random) { out.add("bulbasaur"); out.add("charmander"); out.add("squirtle"); return out; }
    List<Species> cand = new ArrayList<Species>();
    for (Species s : Dex.basePool) if (s.prevo.isEmpty() && !s.evos.isEmpty() && s.bst() <= 340 && !s.legendary() && !s.past) cand.add(s);
    while (out.size() < 3) { Species s = cand.get(rng.nextInt(cand.size())); if (!out.contains(s.id)) out.add(s.id); }
    return out;
  }

  public void chooseStarter(String id) {
    starterId = id;
    Mon m = Mon.create(Dex.sp(id), 5, rng);
    m.pickAbility(rng, false);
    m.item = "";
    m.origin = places.get(0).name;
    party.add(m);
    own(m);
  }

  public void own(Mon m) { owned.add(Dex.toId(rootOf(m.normalSp).id)); dex.add(m.normalSp.id); }

  public static Species rootOf(Species s) {
    int g = 0;
    while (!s.prevo.isEmpty() && g++ < 5) { Species p = Dex.sp(s.prevo); if (p == null) break; s = p; }
    return s;
  }

  // ------------------------------------------------------------
  //  mundo
  // ------------------------------------------------------------
  public Place place() { return places.get(placeIdx); }

  public void buildWorld() {
    places.clear();
    Place p;
    p = new Place("Pueblo Paleta", "Tu pueblo natal. Tu rival te espera en el laboratorio.", 2, 4, "Normal", "Flying");
    p.center = true; p.mapX = 70; p.mapY = 390; p.boss = rival("Rival Azul", 0); places.add(p);
    p = new Place("Ruta 1", "Hierba alta entre Paleta y Ciudad Verde.", 2, 5, "Normal", "Flying");
    p.mapX = 70; p.mapY = 330; p.trainers.add(route("Cría", "Lucas", p, 0)); places.add(p);
    p = new Place("Ciudad Verde", "Centro Pokémon y tienda. El gimnasio está cerrado.", 3, 6, "Normal", "Poison");
    p.center = true; p.shop = true; p.mapX = 70; p.mapY = 270; places.add(p);
    p = new Place("Bosque Verde", "Un bosque lleno de Pokémon bicho.", 4, 8, "Bug", "Grass", "Poison");
    p.mapX = 70; p.mapY = 200; p.trainers.add(route("Cazabichos", "Pedro", p, 1)); p.trainers.add(route("Cazabichos", "Samuel", p, 2));
    p.trainers.add(route("Cazabichos", "Rodrigo", p, 3)); places.add(p);
    p = new Place("Ciudad Plateada", "Gimnasio de Roca. Líder: Brock.", 6, 10, "Rock", "Ground");
    p.center = true; p.shop = true; p.mapX = 70; p.mapY = 130; p.boss = leader("Brock", "Medalla Roca", 0); p.trainers.add(route("Excursionista", "Tomás", p, 4)); places.add(p);
    p = new Place("Monte Luna", "Una cueva con Clefairy y mucha roca.", 9, 14, "Rock", "Fairy", "Ground", "Normal");
    p.mapX = 140; p.mapY = 90; p.trainers.add(route("Montañero", "Alberto", p, 5)); p.trainers.add(route("Rocket", "Grunt", p, 6)); places.add(p);
    p = new Place("Ciudad Celeste", "Gimnasio de Agua. Líder: Misty.", 12, 18, "Water", "Ice");
    p.center = true; p.shop = true; p.mapX = 220; p.mapY = 90; p.boss = leader("Misty", "Medalla Cascada", 1); p.trainers.add(route("Nadadora", "Marina", p, 7)); places.add(p);
    p = new Place("Ruta 5-8 y Subterráneo", "El camino hacia Ciudad Carmín.", 14, 20, "Normal", "Psychic", "Electric", "Dark");
    p.mapX = 220; p.mapY = 170; p.trainers.add(route("Lanzador", "Iván", p, 8)); p.trainers.add(route("Mago", "Elías", p, 9)); places.add(p);
    p = new Place("Ciudad Carmín", "Gimnasio Eléctrico. Líder: Lt. Surge.", 16, 22, "Electric", "Steel");
    p.center = true; p.shop = true; p.mapX = 220; p.mapY = 250; p.boss = leader("Lt. Surge", "Medalla Trueno", 2); p.trainers.add(route("Marinero", "Hugo", p, 10)); places.add(p);
    p = new Place("Cueva Roca", "Oscuridad total. Muchos entrenadores.", 18, 24, "Rock", "Ground", "Fighting", "Dark");
    p.mapX = 300; p.mapY = 210; p.trainers.add(route("Excursionista", "Bruno", p, 11)); p.trainers.add(route("Pícnic", "Lola", p, 12)); places.add(p);
    p = new Place("Pueblo Lavanda y Torre Pokémon", "Fantasmas por todas partes.", 20, 28, "Ghost", "Psychic", "Poison");
    p.center = true; p.mapX = 320; p.mapY = 150; p.boss = rival("Rival Azul", 1); p.trainers.add(route("Médium", "Carmen", p, 13)); p.trainers.add(route("Médium", "Olga", p, 14)); places.add(p);
    p = new Place("Ciudad Azulona", "Gran Almacén con TODOS los objetos. Gimnasio de Planta: Erika.", 22, 30, "Grass", "Fairy", "Bug");
    p.center = true; p.shop = true; p.bigShop = true; p.megaShop = true; p.mapX = 300; p.mapY = 270; p.boss = leader("Erika", "Medalla Arcoíris", 3);
    p.trainers.add(route("Chica", "Rosa", p, 15)); places.add(p);
    p = new Place("Ciudad Fucsia", "Gimnasio Veneno: Koga.", 26, 34, "Poison", "Bug", "Water");
    p.center = true; p.shop = true; p.mapX = 300; p.mapY = 350; p.boss = leader("Koga", "Medalla Alma", 4); p.trainers.add(route("Ninja", "Hanzo", p, 16)); places.add(p);
    p = new Place("Ciudad Azafrán", "Gimnasio Psíquico: Sabrina. Silph S.A.", 30, 38, "Psychic", "Normal", "Steel");
    p.center = true; p.shop = true; p.mapX = 220; p.mapY = 330; p.boss = leader("Sabrina", "Medalla Pantano", 5); p.trainers.add(route("Científico", "Nico", p, 17)); p.trainers.add(route("Rocket", "Ejecutivo", p, 18)); places.add(p);
    p = new Place("Isla Canela", "Gimnasio de Fuego: Blaine. Mansión Pokémon.", 34, 42, "Fire", "Poison", "Steel");
    p.center = true; p.shop = true; p.mapX = 120; p.mapY = 440; p.boss = leader("Blaine", "Medalla Volcán", 6); p.trainers.add(route("Quemador", "Dante", p, 19)); places.add(p);
    p = new Place("Gimnasio de Ciudad Verde", "El último gimnasio: Giovanni, líder del Team Rocket.", 40, 48, "Ground", "Dark", "Normal");
    p.center = true; p.shop = true; p.mapX = 70; p.mapY = 270; p.boss = leader("Giovanni", "Medalla Tierra", 7); p.trainers.add(route("Rocket", "Comandante", p, 20)); places.add(p);
    p = new Place("Calle Victoria", "El último tramo antes de la Liga.", 44, 52, "Rock", "Fighting", "Dragon", "Ground");
    p.mapX = 20; p.mapY = 180; p.boss = rival("Rival Azul", 2); p.trainers.add(route("Entrenador", "Cinta Negra", p, 21)); p.trainers.add(route("Entrenador", "Cinturón", p, 22)); places.add(p);
    p = new Place("Liga Pokémon", "Alto Mando y Campeón. ¡Sin curar entre combates!", 52, 66, "Dragon", "Ice", "Fighting");
    p.center = false; p.shop = true; p.mapX = 20; p.mapY = 90; p.bossChain = eliteFour(); p.boss = p.bossChain.get(0); places.add(p);
    // trazado en serpiente (sin cruces) para dibujar el mapa
    int[][] pos = {{80,540},{80,485},{80,430},{80,375},{80,320},{150,268},{225,228},{225,288},{225,348},{300,392},{375,350},{375,290},{450,240},{450,300},{450,360},{450,420},{450,480},{450,540}};
    for (int i = 0; i < places.size() && i < pos.length; i++) { places.get(i).mapX = pos[i][0]; places.get(i).mapY = pos[i][1]; }
  }

  // ------------------------------------------------------------
  //  generación de especies
  // ------------------------------------------------------------
  public static Species evolveFor(Species s, int level, Random r) {
    for (int guard = 0; guard < 4; guard++) {
      List<Species> opts = new ArrayList<Species>();
      for (Evo e : s.evos) {
        Species to = Dex.sp(e.to);
        if (to == null || to.mega || to.gmax) continue;
        int need = Mon.evoLevel(e);
        if (e.type.equals("useItem")) need = 30;
        if (need > 0 && level >= need) opts.add(to);
      }
      if (opts.isEmpty()) break;
      s = opts.get(r.nextInt(opts.size()));
    }
    return s;
  }

  public static double targetBst(int level) { return 200 + level * 5.6; }

  public Species randomSpecies(int level, String[] theme, boolean fullRandom, int refBst) {
    double target = refBst > 0 ? refBst : targetBst(level);
    double tol = refBst > 0 ? Math.max(40, refBst * 0.12) : 75;
    for (int attempt = 0; attempt < 900; attempt++) {
      Species s;
      if (fullRandom) {
        s = Dex.basePool.get(rng.nextInt(Dex.basePool.size()));
        if (s.restricted() && level < 45) continue;
        if (s.legendary() && level < 30) continue;
        double b = s.bst();
        if (Math.abs(b - target) > tol + attempt / 10.0) continue;
        if (s.past && s.num > 10000) continue;
        return s;
      }
      s = Dex.basePool.get(rng.nextInt(Dex.basePool.size()));
      if (!s.prevo.isEmpty() || s.legendary() || s.past) continue;
      Species f = evolveFor(s, level, rng);
      if (f.mega || f.gmax) continue;
      boolean themed = theme == null || theme.length == 0;
      if (!themed) for (String t : theme) if (f.t1.equals(t) || f.t2.equals(t)) themed = true;
      if (!themed && attempt < 600) continue;
      if (Math.abs(f.bst() - target) > tol + attempt / 8.0) continue;
      return f;
    }
    return Dex.sp("rattata");
  }

  /** Crea un Pokémon con el estilo de dificultad actual. */
  public Mon makeMon(Species s, int level, boolean trainerMon, boolean smart) {
    Mon m = Mon.create(s, level, rng);
    if (trainerMon) {
      int ev = Math.min(84, badges * 11);
      for (int i = 0; i < 6; i++) { m.iv[i] = badges >= 2 ? 31 : 22 + rng.nextInt(10); m.ev[i] = ev; }
      if (badges >= 1) m.pickAbility(rng, true);
      m.recalc(true);
    }
    if (smart || rules.random) m.setMoves(MoveGen.smartSet(m.normalSp, level, rng));
    return m;
  }

  public static final String[] ITEMS_PHYS = {"choiceband", "lifeorb", "leftovers", "sitrusberry", "focussash", "expertbelt", "muscleband"};
  public static final String[] ITEMS_SPEC = {"choicespecs", "lifeorb", "leftovers", "sitrusberry", "focussash", "expertbelt", "wiseglasses"};
  public static final String[] ITEMS_BULKY = {"leftovers", "sitrusberry", "rockyhelmet", "assaultvest", "eviolite", "lumberry", "blacksludge"};

  public String autoItem(Mon m) {
    Species s = m.normalSp;
    if (!s.evos.isEmpty() && m.level < 50 && rng.nextInt(2) == 0) return "eviolite";
    if (s.base[1] >= s.base[3] + 10) return ITEMS_PHYS[rng.nextInt(ITEMS_PHYS.length)];
    if (s.base[3] >= s.base[1] + 10) return ITEMS_SPEC[rng.nextInt(ITEMS_SPEC.length)];
    if (s.base[2] + s.base[4] + s.base[0] > 240) return ITEMS_BULKY[rng.nextInt(ITEMS_BULKY.length)];
    return rng.nextBoolean() ? "lifeorb" : "leftovers";
  }

  public Mon buildTrainerMon(TMon t, boolean aceMegaCapable) {
    Species s = Dex.sp(t.species);
    if (s == null) s = Dex.sp("rattata");
    Mon m = makeMon(s, t.level, true, true);
    m.item = t.item.equals("none") ? "" : (t.item.isEmpty() ? (badges >= 1 ? autoItem(m) : "") : t.item);
    if (aceMegaCapable) {
      List<Species> ml = Dex.megasOf.get(m.normalSp.id);
      if (ml != null) {
        Species mg = ml.get(rng.nextInt(ml.size()));
        if (mg.reqItems.length > 0) m.item = mg.reqItems[0];
        else if (!mg.reqMove.isEmpty()) { List<String> mv = new ArrayList<String>(Arrays.asList(m.moves)); if (!mv.contains(Dex.toId(mg.reqMove))) { mv.set(Math.min(3, mv.size() - 1), Dex.toId(mg.reqMove)); m.setMoves(mv); } }
      }
    }
    m.checkItemForme();
    return m;
  }

  // ------------------------------------------------------------
  //  entrenadores
  // ------------------------------------------------------------
  public static final String[] NAMES = {"Alex","Beto","Carla","Dani","Edu","Fer","Gabi","Hugo","Iñaki","Javi","Kike","Lara","Marta","Nacho","Olaya","Paco","Quique","Rosa","Saúl","Tere","Uri","Vera","Willy","Xavi","Yago","Zoe"};

  public Trainer route(String cls, String nm, Place p, int id) {
    Trainer t = new Trainer(cls, nm);
    t.key = "r" + id;
    t.reward = 0;
    return t;     // el equipo se genera al combatir (para que el modo aleatorio y el nivel actual influyan)
  }

  public Trainer leader(String name, String badge, int n) {
    Trainer t = new Trainer("Líder", name); t.boss = true; t.badge = badge; t.key = "g" + n;
    return t;
  }

  public Trainer rival(String name, int n) { Trainer t = new Trainer("", name); t.boss = true; t.rival = true; t.key = "v" + n; return t; }

  public List<Trainer> eliteFour() {
    List<Trainer> l = new ArrayList<Trainer>();
    String[] nm = {"Lorelei", "Bruno", "Agatha", "Lance"};
    for (int i = 0; i < 4; i++) { Trainer t = new Trainer("Alto Mando", nm[i]); t.boss = true; t.key = "e" + i; l.add(t); }
    Trainer c = new Trainer("Campeón", "Azul"); c.boss = true; c.rival = true; c.key = "c0"; l.add(c);
    return l;
  }

  /** Genera (o regenera) el equipo del entrenador. */
  public void prepare(Trainer t, Place pl) {
    if (!t.team.isEmpty()) return;
    List<TMon> team = t.team;
    if (t.key.startsWith("g")) team.addAll(leaderTeam(Integer.parseInt(t.key.substring(1))));
    else if (t.key.startsWith("e")) team.addAll(e4Team(Integer.parseInt(t.key.substring(1))));
    else if (t.key.startsWith("c")) team.addAll(rivalTeam(3));
    else if (t.key.startsWith("v")) team.addAll(rivalTeam(Integer.parseInt(t.key.substring(1))));
    else {
      int n = 2 + rng.nextInt(2) + (pl.minLv > 15 ? 1 : 0);
      String[] themeMon = routeTheme(t.cls, pl);
      for (int i = 0; i < n; i++) {
        int lv = Math.min(pl.maxLv + 2, pl.minLv + 1 + rng.nextInt(Math.max(1, pl.maxLv - pl.minLv + 2)) + (hardBonus()));
        Species s = randomSpecies(lv, themeMon, rules.random, 0);
        team.add(new TMon(s.id, lv, "", i == n - 1));
      }
      t.reward = team.get(team.size() - 1).level * 35;
    }
    if (t.boss) t.reward = t.maxLevel() * (t.key.startsWith("c") ? 300 : 150);
    // modo aleatorio: especies reemplazadas
    if (rules.random && (t.boss)) {
      for (TMon tm : team) {
        Species o = Dex.sp(tm.species);
        Species s = randomSpecies(tm.level, null, true, o == null ? 0 : o.bst());
        tm.species = s.id;
      }
    }
  }

  public int hardBonus() { return 1; }

  public String[] routeTheme(String cls, Place p) {
    switch (cls) {
      case "Cazabichos": return new String[]{"Bug"};
      case "Excursionista": case "Montañero": return new String[]{"Rock", "Ground", "Fighting"};
      case "Nadadora": case "Marinero": return new String[]{"Water"};
      case "Médium": return new String[]{"Ghost", "Psychic"};
      case "Científico": return new String[]{"Electric", "Steel", "Psychic"};
      case "Rocket": return new String[]{"Poison", "Dark", "Normal"};
      case "Ninja": return new String[]{"Poison", "Dark"};
      case "Quemador": return new String[]{"Fire"};
      case "Chica": return new String[]{"Grass", "Fairy", "Normal"};
      case "Mago": return new String[]{"Psychic"};
    }
    return p.theme;
  }

  public Species starterStage(int starterFam, int level) {
    String[][] fams = {{"bulbasaur", "ivysaur", "venusaur"}, {"charmander", "charmeleon", "charizard"}, {"squirtle", "wartortle", "blastoise"}};
    int s = level < 16 ? 0 : level < 36 ? 1 : 2;
    return Dex.sp(fams[starterFam][s]);
  }

  /** Equipo del rival: usa el inicial que gana al tuyo (o uno aleatorio). */
  public List<TMon> rivalTeam(int n) {
    List<TMon> t = new ArrayList<TMon>();
    int fam = 0;
    String me = Dex.toId(starterId);
    if (me.equals("bulbasaur")) fam = 1; else if (me.equals("charmander")) fam = 2; else if (me.equals("squirtle")) fam = 0; else fam = rng.nextInt(3);
    Species st;
    switch (n) {
      case 0: t.add(new TMon(starterStage(fam, 5).id, 5, "none", true)); break;
      case 1: t.add(new TMon("pidgeotto", 25, "", false)); t.add(new TMon("kadabra", 23, "", false)); t.add(new TMon("growlithe", 24, "", false)); t.add(new TMon(starterStage(fam, 25).id, 27, "", true)); break;
      case 2: t.add(new TMon("pidgeot", 47, "pidgeotite", false)); t.add(new TMon("alakazam", 45, "", false)); t.add(new TMon("rhydon", 46, "", false)); t.add(new TMon("exeggutor", 46, "", false)); t.add(new TMon("gyarados", 47, "", false)); t.add(new TMon(starterStage(fam, 50).id, 53, "", true)); break;
      default: t.add(new TMon("pidgeot", 59, "pidgeotite", false)); t.add(new TMon("alakazam", 57, "", false)); t.add(new TMon("rhydon", 59, "", false)); t.add(new TMon("gyarados", 61, "", false)); t.add(new TMon("exeggutor", 63, "", false)); t.add(new TMon(starterStage(fam, 65).id, 65, "", true)); break;
    }
    return t;
  }

  public List<TMon> leaderTeam(int n) {
    List<TMon> t = new ArrayList<TMon>();
    switch (n) {
      case 0: t.add(new TMon("geodude", 12, "", false)); t.add(new TMon("rhyhorn", 14, "", false)); t.add(new TMon("onix", 16, "", true)); break;
      case 1: t.add(new TMon("staryu", 18, "", false)); t.add(new TMon("golduck", 20, "", false)); t.add(new TMon("starmie", 23, "", true)); break;
      case 2: t.add(new TMon("voltorb", 21, "", false)); t.add(new TMon("magneton", 22, "", false)); t.add(new TMon("raichu", 26, "", true)); break;
      case 3: t.add(new TMon("tangela", 26, "", false)); t.add(new TMon("vileplume", 29, "", false)); t.add(new TMon("victreebel", 32, "", true)); break;
      case 4: t.add(new TMon("koffing", 37, "", false)); t.add(new TMon("muk", 38, "", false)); t.add(new TMon("venomoth", 39, "", false)); t.add(new TMon("weezing", 43, "", true)); break;
      case 5: t.add(new TMon("kadabra", 38, "", false)); t.add(new TMon("mrmime", 38, "", false)); t.add(new TMon("venomoth", 38, "", false)); t.add(new TMon("alakazam", 44, "", true)); break;
      case 6: t.add(new TMon("growlithe", 42, "", false)); t.add(new TMon("ponyta", 41, "", false)); t.add(new TMon("rapidash", 43, "", false)); t.add(new TMon("magmar", 44, "", false)); t.add(new TMon("arcanine", 48, "", true)); break;
      default: t.add(new TMon("dugtrio", 50, "", false)); t.add(new TMon("persian", 51, "", false)); t.add(new TMon("nidoqueen", 53, "", false)); t.add(new TMon("nidoking", 53, "", false)); t.add(new TMon("rhyperior", 55, "", false)); t.add(new TMon("rhydon", 57, "", true)); break;
    }
    return t;
  }

  public List<TMon> e4Team(int n) {
    List<TMon> t = new ArrayList<TMon>();
    switch (n) {
      case 0: t.add(new TMon("dewgong", 54, "", false)); t.add(new TMon("cloyster", 53, "", false)); t.add(new TMon("slowbro", 54, "", false)); t.add(new TMon("jynx", 56, "", false)); t.add(new TMon("lapras", 59, "", true)); break;
      case 1: t.add(new TMon("onix", 53, "", false)); t.add(new TMon("hitmonchan", 55, "", false)); t.add(new TMon("hitmonlee", 55, "", false)); t.add(new TMon("steelix", 56, "", false)); t.add(new TMon("machamp", 60, "", true)); break;
      case 2: t.add(new TMon("arbok", 56, "", false)); t.add(new TMon("golbat", 54, "", false)); t.add(new TMon("haunter", 53, "", false)); t.add(new TMon("misdreavus", 56, "", false)); t.add(new TMon("gengar", 60, "", true)); break;
      default: t.add(new TMon("gyarados", 56, "", false)); t.add(new TMon("dragonair", 54, "", false)); t.add(new TMon("aerodactyl", 58, "", false)); t.add(new TMon("charizard", 58, "", false)); t.add(new TMon("dragonite", 62, "", true)); break;
    }
    return t;
  }

  /** Tope de nivel: el as del próximo jefe (se resta un poco para dificultad). */
  public int levelCap() {
    for (int i = placeIdx; i < places.size(); i++) {
      Place p = places.get(i);
      if (p.boss != null && !p.boss.done) {
        prepare(p.boss, p);
        int mx = p.boss.maxLevel();
        for (Trainer t : p.bossChain) { prepare(t, p); mx = Math.max(mx, t.maxLevel()); }
        if (p.boss != null && p.boss.key.equals("v0")) mx += 4;
        return Math.min(100, mx);
      }
    }
    return 100;
  }

  // ------------------------------------------------------------
  //  combates
  // ------------------------------------------------------------
  public Side playerSide() {
    Side s = new Side(); s.name = "Tú"; s.human = true;
    s.party.addAll(party);
    return s;
  }

  public Mon firstAlive() { for (Mon m : party) if (m.alive()) return m; return null; }

  /** Pokémon salvaje de la zona actual. */
  public Mon wildMon(boolean firstEncounter) {
    Place p = place();
    int lv = p.minLv + rng.nextInt(Math.max(1, p.maxLv - p.minLv + 1));
    Species s = null;
    for (int tries = 0; tries < 40; tries++) {
      s = randomSpecies(lv, rules.random ? null : p.theme, rules.random, 0);
      if (rules.nuzlocke && firstEncounter && owned.contains(Dex.toId(rootOf(s).id))) { s = null; continue; }   // cláusula de duplicados
      break;
    }
    if (s == null) s = randomSpecies(lv, p.theme, rules.random, 0);
    Mon m = makeMon(s, lv, false, rules.random);
    m.pickAbility(rng, rng.nextInt(10) == 0);
    m.origin = p.name;
    return m;
  }

  public Battle wildBattle() {
    Place p = place();
    boolean first = rules.nuzlocke && !p.encounterUsed;
    Mon w = wildMon(first);
    if (rules.nuzlocke) p.encounterUsed = true;
    Side e = new Side(); e.name = "salvaje"; e.party.add(w);
    Battle b = new Battle(playerSide(), e, true, new Random(rng.nextLong()));
    b.bag = bag; b.owned = owned; b.nuzlockeNoCatch = rules.nuzlocke && !first;
    b.catchEnc = first;
    dex.add(w.normalSp.id);
    return b;
  }

  public Battle trainerBattle(Trainer t) {
    Place p = place();
    prepare(t, p);
    Side e = new Side(); e.name = t.full(); e.trainer = true;
    int aceIdx = t.team.size() - 1;
    boolean canT = (t.boss && !t.key.equals("v0") && (badges >= 2 || !t.key.startsWith("g"))) || p.minLv >= 20;
    for (int i = 0; i < t.team.size(); i++) {
      TMon tm = t.team.get(i);
      boolean ace = canT && i == aceIdx;
      Mon m = buildTrainerMon(tm, ace);
      if (!canT && !m.item.isEmpty()) { ItemDef d = Dex.item(m.item); if (d != null && !d.megaStone.isEmpty()) m.item = autoItem(m); }
      e.party.add(m);
      dex.add(m.normalSp.id);
    }
    e.aiAce = e.party.size() - 1;
    e.canTransform = canT;
    // los jefes pueden transformar a su as; en rutas normales sólo si el as tiene piedra o nivel alto
    Battle b = new Battle(playerSide(), e, false, new Random(rng.nextLong()));
    b.bag = bag; b.owned = owned; b.currentTrainer = t;
    return b;
  }

  // ------------------------------------------------------------
  //  resultado de un combate
  // ------------------------------------------------------------
  public static class LevelUp { public Mon mon; public int newLevel; public List<String> newMoves = new ArrayList<String>(); }
  public static class Result {
    public List<String> msgs = new ArrayList<String>();
    public List<Object[]> learn = new ArrayList<Object[]>();       // {Mon, moveId}
    public List<Object[]> evolve = new ArrayList<Object[]>();      // {Mon, Species}
    public boolean won, lost, fled, captured, gameOver;
    public Mon capturedMon; String capturedWhere = "";
    public int money;
  }

  /** Procesa experiencia, muertes, dinero, capturas, etc. Se llama una vez al terminar el combate. */
  public Result finish(Battle b, Map<Mon, Integer> expLog) {
    Result r = new Result();
    int cap = levelCap();
    // sincronizar de vuelta
    for (Mon m : b.sides[0].party) {
      if (m.fainted() && rules.nuzlocke && !m.dead) { m.dead = true; }
    }
    for (Mon m : b.sides[0].party) { m.endBattle(); }
    if (rules.nuzlocke) moveDeadToGraveyard();
    if (b.winner == 2 && b.caught && b.caughtMon != null) {
      Mon c = b.caughtMon;
      c.endBattle();
      r.captured = true; r.capturedMon = c; r.won = true;
      own(c);
      if (party.size() < 6) { party.add(c); r.capturedWhere = "equipo"; } else { box.add(c); r.capturedWhere = "caja"; }
      r.msgs.add("¡" + c.name() + " se unió a tu " + r.capturedWhere + "!");
      return finishExp(b, r, cap);
    }
    if (b.escaped) { r.fled = true; return r; }
    if (b.winner == 0) {
      r.won = true; battlesWon++;
      Trainer t = b.currentTrainer;
      if (t != null) {
        t.done = true;
        int reward = t.reward;
        money += reward; r.money = reward; r.msgs.add("¡Ganaste $" + reward + "!");
        if (!t.badge.isEmpty()) { badges++; r.msgs.add("¡Obtuviste la " + t.badge + "!"); }
        if (rng.nextInt(100) < (t.boss ? 100 : 25)) giveDrop(r, t.boss);
      }
      finishExp(b, r, cap);
    } else {
      r.lost = true;
      r.msgs.add("Todo tu equipo se debilitó...");
      Place cur = place();
      if (!cur.bossChain.isEmpty()) { for (Trainer x : cur.bossChain) x.done = false; r.msgs.add("Tendrás que volver a empezar la Liga desde el primer miembro del Alto Mando."); }
      handleWhiteout(r);
    }
    return r;
  }

  public Result finishExp(Battle b, Result r, int cap) {
    // experiencia por cada rival caído
    for (Mon foe : b.sides[1].party) {
      if (!foe.fainted() && !(foe == b.caughtMon)) continue;
      if (foe == b.caughtMon) continue;
      Map<Mon, Integer> aw = b.expAwards(foe, b.wild ? 1.0 : 1.5, 1.0);
      for (Map.Entry<Mon, Integer> e : aw.entrySet()) {
        Mon m = e.getKey();
        if (!party.contains(m)) continue;
        if (m.level >= cap) continue;
        int pr = m.level;
        int gained = m.gainExp(e.getValue(), cap);
        r.msgs.add(m.name() + " ganó " + e.getValue() + " Pts. de EXP.");
        // EVs
        int best = 0; for (int i = 1; i < 6; i++) if (foe.normalSp.base[i] > foe.normalSp.base[best]) best = i;
        int tot = 0; for (int v : m.ev) tot += v;
        if (tot < 510 && m.ev[best] < 252) { m.ev[best] += foe.normalSp.bst() > 450 ? 2 : 1; }
        for (int lv = pr + 1; lv <= m.level; lv++) {
          r.msgs.add("¡" + m.name() + " subió al nivel " + lv + "!");
          for (String mv : m.newMovesAt(lv)) r.learn.add(new Object[]{m, mv});
        }
        if (gained > 0 && !m.hasItem("everstone")) {
          Evo e2 = m.checkEvolution(true);
          if (e2 != null) { Species to = Dex.sp(e2.to); if (to != null) r.evolve.add(new Object[]{m, to}); }
        }
      }
    }
    for (Mon m : party) m.recalc(false);
    return r;
  }

  public void giveDrop(Result r, boolean boss) {
    List<ItemDef> pool = new ArrayList<ItemDef>();
    for (ItemDef d : Dex.items.values()) {
      if (d.ball) continue;
      if (!d.megaStone.isEmpty() && !boss) continue;
      pool.add(d);
    }
    // jefes: una piedra mega para algún Pokémon del equipo si es posible
    if (boss) {
      List<ItemDef> stones = new ArrayList<ItemDef>();
      for (Mon m : party) {
        List<Species> ml = Dex.megasOf.get(m.normalSp.id);
        if (ml == null) continue;
        for (Species s : ml) for (String ri : s.reqItems) { ItemDef d = Dex.item(ri); if (d != null && bag.count(d.id) == 0 && !holding(d.id)) stones.add(d); }
      }
      if (!stones.isEmpty() && rng.nextInt(100) < 70) { ItemDef d = stones.get(rng.nextInt(stones.size())); bag.add(d.id, 1); r.msgs.add("¡Obtuviste " + d.name + "!"); return; }
    }
    ItemDef d = pool.get(rng.nextInt(pool.size()));
    bag.add(d.id, 1); r.msgs.add("¡Encontraste " + d.name + "!");
    int bonus = 1 + rng.nextInt(3);
    String[] cons = {"potion", "superpotion", "pokeball", "greatball", "revive", "xattack", "fullheal"};
    String c = cons[rng.nextInt(cons.length)];
    bag.add(c, bonus); r.msgs.add("¡Encontraste " + bonus + " × " + Items.name(c) + "!");
  }

  public boolean holding(String id) { for (Mon m : party) if (Dex.toId(m.item).equals(id)) return true; for (Mon m : box) if (Dex.toId(m.item).equals(id)) return true; return false; }

  public void handleWhiteout(Result r) {
    if (rules.nuzlocke) {
      boolean anyAlive = false;
      for (Mon m : party) if (m.alive()) anyAlive = true;
      for (Mon m : box) if (m.alive()) anyAlive = true;
      moveDeadToGraveyard();
      if (!anyAlive) { gameOver = true; r.gameOver = true; r.msgs.add("Nuzlocke fallido: no queda ningún Pokémon vivo."); return; }
      // equipo vacío: rellenar con la caja
      refillFromBox();
    }
    int lost = money / 2; money -= lost;
    r.msgs.add("Perdiste $" + lost + " y volviste al Centro Pokémon.");
    healAll();
    placeIdx = Math.max(0, lastCenter());
  }

  public int lastCenter() { for (int i = placeIdx; i >= 0; i--) if (places.get(i).center) return i; return 0; }

  public void moveDeadToGraveyard() {
    for (Iterator<Mon> it = party.iterator(); it.hasNext();) { Mon m = it.next(); if (m.dead) { graveyard.add(m); it.remove(); deaths++; } }
    for (Iterator<Mon> it = box.iterator(); it.hasNext();) { Mon m = it.next(); if (m.dead) { graveyard.add(m); it.remove(); } }
  }

  public void refillFromBox() {
    while (party.size() < 1 && !box.isEmpty()) { for (Mon m : box) if (m.alive()) { box.remove(m); party.add(m); break; } if (party.isEmpty()) break; }
  }

  public void healAll() { for (Mon m : party) m.fullHeal(); }

  // ------------------------------------------------------------
  //  tienda
  // ------------------------------------------------------------
  public List<String> shopStock(int kind) {
    List<String> l = new ArrayList<String>();
    if (kind == 0) {      // médico + balls
      l.addAll(Arrays.asList("pokeball", "greatball", "potion", "antidote", "paralyzeheal", "awakening", "burnheal", "iceheal"));
      if (badges >= 1) l.addAll(Arrays.asList("superpotion", "revive", "xattack", "xdefense", "xspeed", "xspatk", "xspdef", "ether"));
      if (badges >= 3) l.addAll(Arrays.asList("ultraball", "hyperpotion", "fullheal", "elixir"));
      if (badges >= 5) l.addAll(Arrays.asList("maxpotion", "maxrevive", "maxether", "quickball", "netball", "nestball", "timerball", "fastball", "repeatball", "diveball", "duskball"));
      if (badges >= 7) l.addAll(Arrays.asList("fullrestore", "maxelixir"));
    } else if (kind == 1) {   // piedras evolutivas
      for (ItemDef d : Dex.items.values()) if (d.id.endsWith("stone") && !d.id.equals("everstone") && !d.id.equals("floatstone")) l.add(d.id);
      l.addAll(Arrays.asList("hpup", "protein", "iron", "calcium", "zinc", "carbos", "rarecandy"));
    } else if (kind == 2) {   // todos los objetos equipables (sin megapiedras)
      List<ItemDef> all = new ArrayList<ItemDef>(Dex.items.values());
      Collections.sort(all, new Comparator<ItemDef>() { public int compare(ItemDef a, ItemDef b) { return a.name.compareTo(b.name); } });
      for (ItemDef d : all) if (!d.ball && d.megaStone.isEmpty()) l.add(d.id);
    } else {                  // piedras mega
      List<ItemDef> all = new ArrayList<ItemDef>(Dex.items.values());
      Collections.sort(all, new Comparator<ItemDef>() { public int compare(ItemDef a, ItemDef b) { return a.name.compareTo(b.name); } });
      for (ItemDef d : all) if (!d.megaStone.isEmpty()) l.add(d.id);
      for (String id : new String[]{"redorb", "blueorb"}) if (Dex.items.containsKey(id)) l.add(id);
    }
    return l;
  }

  public int priceOf(String id) {
    int p = Items.price(id);
    ItemDef d = Dex.items.get(id);
    if (d != null && !d.megaStone.isEmpty()) p = 12000;
    if (p <= 0) p = id.equals("rarecandy") ? 5000 : 4000;
    return p;
  }

  public boolean buy(String id, int qty) {
    int cost = priceOf(id) * qty;
    if (money < cost) return false;
    money -= cost; bag.add(id, qty); return true;
  }

  // ------------------------------------------------------------
  //  avance
  // ------------------------------------------------------------
  public boolean canAdvance() {
    Place p = place();
    if (placeIdx >= places.size() - 1) return false;
    if (p.boss != null && !allBossDone(p)) return false;
    return true;
  }

  public boolean allBossDone(Place p) {
    if (p.bossChain.isEmpty()) return p.boss == null || p.boss.done;
    for (Trainer t : p.bossChain) if (!t.done) return false;
    return true;
  }

  public Trainer nextBoss() {
    Place p = place();
    if (!p.bossChain.isEmpty()) { for (Trainer t : p.bossChain) if (!t.done) return t; return null; }
    return p.boss != null && !p.boss.done ? p.boss : null;
  }

  public void advance() {
    if (canAdvance()) { placeIdx++; }
  }

  public boolean won() { Place p = places.get(places.size() - 1); return allBossDone(p) && !p.bossChain.isEmpty(); }

  /** Después de ganar el último combate de la Liga. */
  public void checkChampion() { if (won()) champion = true; }

  // ------------------------------------------------------------
  //  guardado
  // ------------------------------------------------------------
  public static String monLine(Mon m) {
    StringBuilder sb = new StringBuilder("M|");
    sb.append(m.normalSp.id).append('|').append(m.nick).append('|').append(m.level).append('|').append(m.exp).append('|').append(m.nature).append('|');
    for (int v : m.iv) sb.append(v).append(','); sb.append('|');
    for (int v : m.ev) sb.append(v).append(','); sb.append('|');
    sb.append(m.ability).append('|').append(m.item).append('|').append(m.hp).append('|').append(m.status).append('|').append(m.dead ? 1 : 0).append('|');
    for (String s : m.moves) sb.append(s).append(','); sb.append('|');
    for (int s : m.pp) sb.append(s).append(','); sb.append('|');
    sb.append(m.origin);
    return sb.toString();
  }

  public static Mon parseMon(String line) {
    String[] p = line.split("\\|", -1);
    Species s = Dex.sp(p[1]);
    Mon m = new Mon(s, Integer.parseInt(p[3]));
    m.nick = p[2]; m.exp = Integer.parseInt(p[4]); m.nature = Integer.parseInt(p[5]);
    String[] iv = p[6].split(","), ev = p[7].split(",");
    for (int i = 0; i < 6; i++) { m.iv[i] = Integer.parseInt(iv[i]); m.ev[i] = Integer.parseInt(ev[i]); }
    m.ability = p[8]; m.item = p[9];
    m.recalc(true);
    m.hp = Integer.parseInt(p[10]); m.status = p[11]; m.dead = p[12].equals("1");
    List<String> mv = new ArrayList<String>();
    for (String x : p[13].split(",")) if (!x.isEmpty()) mv.add(x);
    m.setMoves(mv);
    String[] pp = p[14].split(",");
    for (int i = 0; i < m.pp.length && i < pp.length; i++) if (!pp[i].isEmpty()) m.pp[i] = Integer.parseInt(pp[i]);
    m.origin = p.length > 15 ? p[15] : "";
    m.checkItemForme();
    return m;
  }

  public List<String> serialize() {
    List<String> o = new ArrayList<String>();
    o.add("G|" + (rules.random ? 1 : 0) + "|" + (rules.nuzlocke ? 1 : 0) + "|" + placeIdx + "|" + money + "|" + badges + "|" + seed + "|" + starterId + "|" + (gameOver ? 1 : 0) + "|" + battlesWon + "|" + deaths);
    StringBuilder sb = new StringBuilder("BAG|");
    for (String id : bag.ids()) sb.append(id).append(':').append(bag.count(id)).append(',');
    o.add(sb.toString());
    for (Mon m : party) o.add("P" + monLine(m));
    for (Mon m : box) o.add("B" + monLine(m));
    for (Mon m : graveyard) o.add("D" + monLine(m));
    sb = new StringBuilder("PL|");
    for (Place p : places) sb.append(p.encounterUsed ? 1 : 0).append(p.boss != null && p.boss.done ? 1 : 0).append(trainerBits(p)).append(',');
    o.add(sb.toString());
    sb = new StringBuilder("E4|");
    Place last = places.get(places.size() - 1);
    for (Trainer t : last.bossChain) sb.append(t.done ? 1 : 0);
    o.add(sb.toString());
    sb = new StringBuilder("OWN|"); for (String s : owned) sb.append(s).append(','); o.add(sb.toString());
    sb = new StringBuilder("DEX|"); for (String s : dex) sb.append(s).append(','); o.add(sb.toString());
    return o;
  }

  public String trainerBits(Place p) { StringBuilder sb = new StringBuilder(); for (Trainer t : p.trainers) sb.append(t.done ? 1 : 0); return sb.toString(); }

  public static Game deserialize(List<String> lines) {
    Game g = null;
    for (String l : lines) {
      String[] p = l.split("\\|", -1);
      if (p[0].equals("G")) {
        Rules r = new Rules(); r.random = p[1].equals("1"); r.nuzlocke = p[2].equals("1");
        g = new Game(r, Long.parseLong(p[6]));
        g.placeIdx = Integer.parseInt(p[3]); g.money = Integer.parseInt(p[4]); g.badges = Integer.parseInt(p[5]);
        g.starterId = p[7]; g.gameOver = p[8].equals("1"); g.battlesWon = Integer.parseInt(p[9]); g.deaths = Integer.parseInt(p[10]);
        g.bag = new Bag();
      } else if (g == null) continue;
      else if (p[0].equals("BAG")) { for (String e : p[1].split(",")) if (e.contains(":")) { String[] kv = e.split(":"); g.bag.add(kv[0], Integer.parseInt(kv[1])); } }
      else if (p[0].equals("PM")) g.party.add(parseMon(l.substring(1)));
      else if (p[0].equals("BM")) g.box.add(parseMon(l.substring(1)));
      else if (p[0].equals("DM")) g.graveyard.add(parseMon(l.substring(1)));
      else if (p[0].equals("PL")) {
        String[] ps = p[1].split(",");
        for (int i = 0; i < ps.length && i < g.places.size(); i++) {
          Place pl = g.places.get(i);
          if (ps[i].length() >= 2) { pl.encounterUsed = ps[i].charAt(0) == '1'; if (pl.boss != null) pl.boss.done = ps[i].charAt(1) == '1';
            for (int k = 0; k < pl.trainers.size() && k + 2 < ps[i].length(); k++) pl.trainers.get(k).done = ps[i].charAt(k + 2) == '1'; }
        }
      }
      else if (p[0].equals("E4")) { Place last = g.places.get(g.places.size() - 1); for (int i = 0; i < p[1].length() && i < last.bossChain.size(); i++) last.bossChain.get(i).done = p[1].charAt(i) == '1'; }
      else if (p[0].equals("OWN")) { for (String s : p[1].split(",")) if (!s.isEmpty()) g.owned.add(s); }
      else if (p[0].equals("DEX")) { for (String s : p[1].split(",")) if (!s.isEmpty()) g.dex.add(s); }
    }
    return g;
  }
}
