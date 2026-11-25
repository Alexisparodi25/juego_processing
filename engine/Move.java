package pokefuego.engine;
import java.util.*;

public class Move {
  public String id, name, type, target = "normal", flags = "";
  public char cat = 'S';           // P, S, N(status)
  public int bp, acc, pp, prio, crit = 1;
  public String status = "", vol = "";
  public String selfBoost = "", tgtBoost = "";
  public int secChance; public String secStatus = "", secVol = "", secBoost = "";
  public String selfAfter = ""; public int selfAfterChance = 100;
  public int[] drain, recoil, heal;
  public int multiMin, multiMax;
  public boolean selfdestruct, ohko, forceSwitch, selfSwitch, breaksProtect, ignoreDef, willCrit;
  public String weather = "", terrain = "", sideCond = "", damage = "", pseudo = "", slotCond = "";
  public String overrideOff = "", overrideDefPoke = "";
  public int maxBP; public String isMax = "";
  public String desc = "";
  public boolean supported = true;
  public String es = "", descEs = "";
  public String dn() { return es.isEmpty() ? name : es; }
  public String dd() { return descEs.isEmpty() ? desc : descEs; }
  public boolean has(String f) { return flags.indexOf(f) >= 0; }
  public boolean contact() { return has("contact"); }
  public boolean status() { return cat == 'N'; }
  public Move copy() {
    Move m = new Move();
    m.id = id; m.name = name; m.type = type; m.target = target; m.flags = flags; m.cat = cat; m.bp = bp; m.acc = acc;
    m.pp = pp; m.prio = prio; m.crit = crit; m.status = status; m.vol = vol; m.selfBoost = selfBoost; m.tgtBoost = tgtBoost;
    m.secChance = secChance; m.secStatus = secStatus; m.secVol = secVol; m.secBoost = secBoost; m.selfAfter = selfAfter;
    m.selfAfterChance = selfAfterChance; m.drain = drain; m.recoil = recoil; m.heal = heal; m.multiMin = multiMin;
    m.multiMax = multiMax; m.selfdestruct = selfdestruct; m.ohko = ohko; m.forceSwitch = forceSwitch;
    m.selfSwitch = selfSwitch; m.breaksProtect = breaksProtect; m.ignoreDef = ignoreDef; m.willCrit = willCrit;
    m.weather = weather; m.terrain = terrain; m.sideCond = sideCond; m.damage = damage; m.pseudo = pseudo;
    m.slotCond = slotCond; m.overrideOff = overrideOff; m.overrideDefPoke = overrideDefPoke; m.maxBP = maxBP;
    m.isMax = isMax; m.desc = desc; m.supported = supported; m.es = es; m.descEs = descEs;
    return m;
  }
  public String toString() { return name; }
}
