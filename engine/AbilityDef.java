package pokefuego.engine;
import java.util.*;

public class AbilityDef { String id, name, desc, es = "", descEs = ""; int gen; double rating;
  public String dn() { return es.isEmpty() ? name : es; }
  public String dd() { return descEs.isEmpty() ? desc : descEs; } }
