package pokefuego.engine;
import java.util.*;

public class Items {
  public static LinkedHashMap<String, Consumable> CONS = new LinkedHashMap<String, Consumable>();
  static {
    add("potion", "Poción", "Restaura 20 PS.", 300, "heal");
    add("superpotion", "Superpoción", "Restaura 60 PS.", 700, "heal");
    add("hyperpotion", "Hiperpoción", "Restaura 120 PS.", 1200, "heal");
    add("maxpotion", "Poción Máxima", "Restaura todos los PS.", 2500, "heal");
    add("fullrestore", "Restaurar Todo", "Restaura PS y cura cualquier estado.", 3000, "heal");
    add("revive", "Revivir", "Revive a un Pokémon con la mitad de sus PS.", 1500, "revive");
    add("maxrevive", "Revivir Máximo", "Revive a un Pokémon con todos sus PS.", 4000, "revive");
    add("antidote", "Antídoto", "Cura el envenenamiento.", 100, "status");
    add("paralyzeheal", "Antiparalizador", "Cura la parálisis.", 200, "status");
    add("awakening", "Despertar", "Despierta a un Pokémon dormido.", 250, "status");
    add("burnheal", "Antiquemar", "Cura las quemaduras.", 250, "status");
    add("iceheal", "Antihielo", "Descongela a un Pokémon.", 250, "status");
    add("fullheal", "Cura Total", "Cura cualquier estado.", 600, "status");
    add("ether", "Éter", "Restaura 10 PP de un movimiento.", 1200, "pp");
    add("maxether", "Éter Máximo", "Restaura los PP de un movimiento.", 2000, "pp");
    add("elixir", "Elixir", "Restaura 10 PP de todos los movimientos.", 3000, "pp");
    add("maxelixir", "Elixir Máximo", "Restaura todos los PP.", 4500, "pp");
    add("rarecandy", "Caramelo Raro", "Sube un nivel (sin pasar del tope).", 0, "candy");
    add("xattack", "Ataque X", "+2 Ataque en combate.", 500, "x");
    add("xdefense", "Defensa X", "+2 Defensa en combate.", 550, "x");
    add("xspeed", "Velocidad X", "+2 Velocidad en combate.", 350, "x");
    add("xspatk", "At. Esp. X", "+2 At. Esp. en combate.", 350, "x");
    add("xspdef", "Def. Esp. X", "+2 Def. Esp. en combate.", 350, "x");
    add("hpup", "Más PS", "Sube las PS base de forma permanente (EVs).", 0, "ev");
    add("protein", "Proteína", "Sube el Ataque base (EVs).", 0, "ev");
    add("iron", "Hierro", "Sube la Defensa base (EVs).", 0, "ev");
    add("calcium", "Calcio", "Sube el At. Esp. base (EVs).", 0, "ev");
    add("zinc", "Cinc", "Sube la Def. Esp. base (EVs).", 0, "ev");
    add("carbos", "Carburante", "Sube la Velocidad base (EVs).", 0, "ev");
  }
  public static void add(String id, String name, String desc, int price, String kind) { CONS.put(id, new Consumable(id, name, desc, price, kind)); }

  public static String name(String id) {
    id = Dex.toId(id);
    Consumable c = CONS.get(id);
    if (c != null) return c.name;
    ItemDef d = Dex.items.get(id);
    return d != null ? d.dn() : id;
  }
  public static String desc(String id) {
    id = Dex.toId(id);
    Consumable c = CONS.get(id);
    if (c != null) return c.desc;
    ItemDef d = Dex.items.get(id);
    return d != null ? d.dd() : "";
  }
  public static boolean usableInBattle(String id) {
    Consumable c = CONS.get(Dex.toId(id));
    return c != null && !c.kind.equals("candy") && !c.kind.equals("ev");
  }
  public static boolean isBall(String id) { ItemDef d = Dex.items.get(Dex.toId(id)); return d != null && d.ball; }
  public static int price(String id) {
    id = Dex.toId(id);
    Consumable c = CONS.get(id);
    if (c != null) return c.price;
    if (isBall(id)) return id.equals("pokeball") ? 200 : id.equals("greatball") ? 600 : id.equals("ultraball") ? 1200 : 1000;
    ItemDef d = Dex.items.get(id);
    if (d == null) return 1000;
    return d.berry ? 300 : (d.megaStone.isEmpty() ? 2000 : 8000);
  }

  /** Objetos que pasan a gastarse (no regresan al terminar el combate). */
  public static boolean isConsumable(String id) {
    ItemDef d = Dex.items.get(Dex.toId(id));
    if (d == null) return false;
    if (d.berry) return true;
    switch (d.id) {
      case "focussash": case "whiteherb": case "weaknesspolicy": case "airballoon": case "absorbbulb": case "cellbattery": case "snowball":
      case "luminousmoss": case "redcard": case "ejectbutton": case "powerherb": case "mentalherb": case "berryjuice": return true;
    }
    return false;
  }

  /** El objeto no se puede quitar (piedras mega de su dueño, orbes/formas). */
  public static boolean unremovable(Mon m) {
    ItemDef d = Dex.item(m.item);
    if (d == null) return false;
    if (!d.megaStone.isEmpty() && m.normalSp != null && Dex.megasOf.containsKey(m.normalSp.id)) return true;
    if (Dex.itemForme.containsKey(m.normalSp.id + "|" + d.id)) return true;
    for (String u : d.users) if (Dex.toId(u).equals(Dex.toId(m.normalSp.baseSpecies))) return true;
    return false;
  }

  public static double ballMult(Battle b, Mon e, String id) {
    id = Dex.toId(id);
    switch (id) {
      case "masterball": return 255;
      case "ultraball": return 2;
      case "greatball": case "safariball": case "sportball": return 1.5;
      case "netball": return (e.isType("Water") || e.isType("Bug")) ? 3.5 : 1;
      case "diveball": return e.isType("Water") ? 3.5 : 1;
      case "nestball": return e.level < 30 ? Math.max(1, (41 - e.level) / 10.0) : 1;
      case "quickball": return b.turn <= 1 ? 5 : 1;
      case "timerball": return Math.min(4, 1 + b.turn * 0.3);
      case "fastball": return e.normalSp.base[5] >= 100 ? 4 : 1;
      case "repeatball": return b.owned != null && b.owned.contains(e.normalSp.baseSpecies) ? 3.5 : 1;
      case "duskball": return 1;
      case "beastball": return e.normalSp.tags.contains("Mythical") ? 0.5 : 1;
      case "levelball": return 1;
      case "heavyball": return 1;
    }
    return 1;
  }

  /** Usa un objeto de curación sobre un Pokémon. Devuelve el mensaje. */
  public static String useOnMon(Battle b, Mon m, String id, boolean inBattle) {
    id = Dex.toId(id);
    Consumable c = CONS.get(id);
    if (c == null) return "No se puede usar ahora.";
    String n = m.name();
    int missing = m.maxHp - m.hp;
    switch (id) {
      case "potion": case "superpotion": case "hyperpotion": case "maxpotion": {
        if (m.fainted() || missing <= 0) return "No tendrá ningún efecto.";
        int amt = id.equals("potion") ? 20 : id.equals("superpotion") ? 60 : id.equals("hyperpotion") ? 120 : 9999;
        int a = Math.min(amt, missing); m.hp += a; return "¡" + n + " recuperó " + a + " PS!"; }
      case "fullrestore": {
        if (m.fainted() || (missing <= 0 && m.status.isEmpty())) return "No tendrá ningún efecto.";
        m.hp = m.maxHp; m.status = ""; m.vol.remove("confusion"); return "¡" + n + " se recuperó por completo!"; }
      case "revive": case "maxrevive": {
        if (!m.fainted() || m.dead) return "No tendrá ningún efecto.";
        m.hp = id.equals("revive") ? Math.max(1, m.maxHp / 2) : m.maxHp; m.status = ""; return "¡" + n + " fue revivido!"; }
      case "antidote": if (m.status.equals("psn") || m.status.equals("tox")) { m.status = ""; return "¡" + n + " se curó del veneno!"; } return "No tendrá ningún efecto.";
      case "paralyzeheal": if (m.status.equals("par")) { m.status = ""; return "¡" + n + " se curó de la parálisis!"; } return "No tendrá ningún efecto.";
      case "awakening": if (m.status.equals("slp")) { m.status = ""; return "¡" + n + " se despertó!"; } return "No tendrá ningún efecto.";
      case "burnheal": if (m.status.equals("brn")) { m.status = ""; return "¡" + n + " se curó la quemadura!"; } return "No tendrá ningún efecto.";
      case "iceheal": if (m.status.equals("frz")) { m.status = ""; return "¡" + n + " se descongeló!"; } return "No tendrá ningún efecto.";
      case "fullheal": if (!m.status.isEmpty() && !m.fainted()) { m.status = ""; return "¡" + n + " se curó!"; } return "No tendrá ningún efecto.";
      case "ether": case "maxether": {
        for (int i = 0; i < m.pp.length; i++) if (m.pp[i] < m.maxPp[i]) { m.pp[i] = Math.min(m.maxPp[i], m.pp[i] + (id.equals("ether") ? 10 : 99)); return "¡Se restauraron los PP de " + Dex.mv(m.moves[i]).dn() + "!"; }
        return "No tendrá ningún efecto."; }
      case "elixir": case "maxelixir": {
        boolean any = false;
        for (int i = 0; i < m.pp.length; i++) if (m.pp[i] < m.maxPp[i]) { m.pp[i] = Math.min(m.maxPp[i], m.pp[i] + (id.equals("elixir") ? 10 : 99)); any = true; }
        return any ? "¡Se restauraron los PP de " + n + "!" : "No tendrá ningún efecto."; }
      case "xattack": if (b != null && inBattle) { b.boost(m, 1, 2, m, true); return "¡Usaste Ataque X!"; } return "No tendrá ningún efecto.";
      case "xdefense": if (b != null && inBattle) { b.boost(m, 2, 2, m, true); return "¡Usaste Defensa X!"; } return "No tendrá ningún efecto.";
      case "xspatk": if (b != null && inBattle) { b.boost(m, 3, 2, m, true); return "¡Usaste At. Esp. X!"; } return "No tendrá ningún efecto.";
      case "xspdef": if (b != null && inBattle) { b.boost(m, 4, 2, m, true); return "¡Usaste Def. Esp. X!"; } return "No tendrá ningún efecto.";
      case "xspeed": if (b != null && inBattle) { b.boost(m, 5, 2, m, true); return "¡Usaste Velocidad X!"; } return "No tendrá ningún efecto.";
      case "hpup": case "protein": case "iron": case "calcium": case "zinc": case "carbos": {
        int idx = id.equals("hpup") ? 0 : id.equals("protein") ? 1 : id.equals("iron") ? 2 : id.equals("calcium") ? 3 : id.equals("zinc") ? 4 : 5;
        int tot = 0; for (int e : m.ev) tot += e;
        if (m.ev[idx] >= 100 || tot >= 510) return "No tendrá ningún efecto.";
        m.ev[idx] = Math.min(100, m.ev[idx] + 10); m.recalc(false); return "¡Las estadísticas base de " + n + " subieron!"; }
    }
    return "No tendrá ningún efecto.";
  }
}
