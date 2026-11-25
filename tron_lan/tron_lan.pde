import processing.net.*;
import java.net.*;

// =====================================================
//  TRON LAN - hasta 5 jugadores
//
//  Host = Jugador 1 (rojo). Los clientes entran por IP.
//  Controles (todos): WASD o flechas.
//  Duración de partida configurable: por PUNTOS o por TIEMPO (1 a 5 min).
//  Si dos motos chocan de frente: ambas mueren y NADIE suma puntos.
//  Se suma 1 punto cuando una ronda termina con un único sobreviviente.
//
//  Protocolo (texto, una línea por mensaje). Las posiciones viajan
//  NORMALIZADAS (0..10000 = 0..100% del campo), así cliente y host
//  pueden tener cualquier resolución / pantalla completa.
// =====================================================

// ==========================================
// 1. CONFIG & SETUP
// ==========================================
final int W = 800;     // tamaño LÓGICO de la pantalla (se escala a fullscreen)
final int H = 600;

// campo de juego (dentro de la pantalla lógica). Sus bordes se dibujan y matan.
final int FX = 10;
final int FY = 50;
final int FW = 780;
final int FH = 540;

final int MAXJ = 5;    // máximo de jugadores (host + 4 clientes)

// estados de la aplicación
final int MENU = 0;
final int CONFIG = 1;
final int SERVIDOR = 2;
final int CLIENTE = 3;
final int CONECTANDO = 4;
int estadoApp = MENU;

// fases de la partida (host y cliente)
final int LOBBY = 0;
final int JUGANDO = 1;
final int FIN = 2;
int fase = LOBBY;

// modos de duración
final int PUNTOS = 0;
final int TIEMPO = 1;
int modoJuego = PUNTOS;
int valorJuego = 10;                       // puntos objetivo o minutos
final int[] opcionesPuntos = {3, 5, 10, 15, 20, 30};
final int[] opcionesMin = {1, 2, 3, 4, 5};
int idxPuntos = 2;
int idxMin = 2;

final int[] COLORES = {0xFFFF1E1E, 0xFFF0F0FF, 0xFF1EC8FF, 0xFFFFD21E, 0xFF3CFF6E};
final String[] NOMBRES = {"Rojo", "Blanco", "Cian", "Amarillo", "Verde"};

// posiciones iniciales (normalizadas) y direcciones; con N jugadores se usan los primeros N
final float[] SPX = {0.10f, 0.90f, 0.50f, 0.50f, 0.10f};
final float[] SPY = {0.50f, 0.50f, 0.10f, 0.90f, 0.90f};
final int[] SDX = {1, -1, 0, 0, 1};
final int[] SDY = {0, 0, 1, -1, 0};

Server s;
Client c;
int puerto = 12345;
String[] ipsServidor = {"10.66.88.234", "10.120.2.147", "10.160.4.36"};
int indiceIp = 0;
String ipServidor = ipsServidor[indiceIp];

int pasosPorFrame = 4;

// estado de la partida (lo usan host y cliente para dibujar)
float[] px = new float[MAXJ];     // coordenadas dentro del campo (0..FW, 0..FH)
float[] py = new float[MAXJ];
int[] est = new int[MAXJ];        // 0 = ausente, 1 = vivo, 2 = muerto
int[] puntos = new int[MAXJ];
int tRestMs = 0;                  // tiempo restante (modo TIEMPO)
int ganadorFinal = -1;
String msgRonda = "";
int msgHasta = 0;

// solo servidor
int[] dx = new int[MAXJ];
int[] dy = new int[MAXJ];
int[] ndx = new int[MAXJ];
int[] ndy = new int[MAXJ];
boolean[] participa = new boolean[MAXJ];
boolean[][] ocupado;
boolean rondaTerminada = false;
int pausaHasta = 0;
int ultimoMs = 0;
Client[] clientes = new Client[MAXJ - 1];       // clientes[k] es el jugador k+1
ArrayList<Client> pendientes = new ArrayList<Client>();

// solo cliente
int miId = -1;
int cMask = 0;
float[] qx = new float[MAXJ];
float[] qy = new float[MAXJ];
int[] prevEst = new int[MAXJ];
String errorConexion = "";

// render
PGraphics rastro;                       // estelas del campo (FW x FH)
ArrayList<Seg> segs = new ArrayList<Seg>();   // segmentos pendientes de dibujar (se vuelcan 1 vez por frame)
ArrayList<Particula> sistemaParticulas;

class Seg {
  int col;
  float x0, y0, x1, y1;
  Seg(int col, float x0, float y0, float x1, float y1) {
    this.col = col; this.x0 = x0; this.y0 = y0; this.x1 = x1; this.y1 = y1;
  }
}

void setup() {
  fullScreen();     // pantalla completa; el juego se escala desde W x H
  frameRate(60);
  ocupado = new boolean[FW][FH];
  rastro = createGraphics(FW, FH);
  sistemaParticulas = new ArrayList<Particula>();
  aplicarConfig();
}

void draw() {
  background(10, 10, 15);

  // todo se dibuja en coordenadas lógicas W x H y se escala (con barras) para entrar en la pantalla
  float esc = min(width / (float) W, height / (float) H);
  pushMatrix();
  translate((width - W * esc) / 2, (height - H * esc) / 2);
  scale(esc);

  if (estadoApp == MENU) {
    dibujarMenu();
  } else if (estadoApp == CONFIG) {
    dibujarConfig();
  } else if (estadoApp == SERVIDOR) {
    ejecutarServidor();
  } else if (estadoApp == CONECTANDO) {
    dibujarMensajeCentro("Conectando a " + ipServidor + " ...");
  } else if (estadoApp == CLIENTE) {
    ejecutarCliente();
  }

  popMatrix();
}

void aplicarConfig() {
  valorJuego = (modoJuego == PUNTOS) ? opcionesPuntos[idxPuntos] : opcionesMin[idxMin];
}


// ==========================================
// 2. MENU, CONFIG & INPUT
// ==========================================
void dibujarMenu() {
  textAlign(CENTER, CENTER);
  textSize(48);
  fill(255, 30, 30);
  text("TRON: RED GRID LAN", W/2, H/2 - 100);

  textSize(20);
  fill(255);
  text("Presiona [ 1 ] para crear Servidor (Host / Jugador Rojo)", W/2, H/2 - 20);
  text("Presiona [ 2 ] para conectar como Cliente", W/2, H/2 + 20);

  textSize(14);
  fill(150, 150, 150);
  text("IP actual configurada para cliente: " + ipServidor + "  (Presiona [ 3 ] para cambiar IP)", W/2, H/2 + 80);
  text("Hasta 5 jugadores  |  ESC para salir", W/2, H/2 + 105);
}

void dibujarConfig() {
  textAlign(CENTER, CENTER);
  textSize(36);
  fill(0, 220, 255);
  text("CONFIGURAR PARTIDA", W/2, 110);

  textSize(22);
  fill(255);
  String a = (modoJuego == PUNTOS) ? "[ PUNTOS ]    TIEMPO" : "PUNTOS    [ TIEMPO ]";
  text("Modo:  " + a, W/2, 230);
  textSize(14);
  fill(150);
  text("TAB para cambiar de modo", W/2, 260);

  textSize(22);
  fill(255);
  String v = (modoJuego == PUNTOS) ? ("Primero a " + valorJuego + " puntos") : ("Duración: " + valorJuego + (valorJuego == 1 ? " minuto" : " minutos"));
  text("<   " + v + "   >", W/2, 330);
  textSize(14);
  fill(150);
  text("Flechas IZQ / DER para cambiar", W/2, 360);

  textSize(20);
  fill(255, 220, 60);
  text("[ ENTER ] crear sala", W/2, 450);
  textSize(14);
  fill(150);
  text("[ BACKSPACE ] volver", W/2, 480);
}

void dibujarMensajeCentro(String msg) {
  textAlign(CENTER, CENTER);
  textSize(20);
  fill(255);
  text(msg, W/2, H/2);
}

// devuelve "UP", "DOWN", "LEFT", "RIGHT" según WASD o flechas; null si no es una dirección
String teclaADireccion() {
  if (key == CODED) {
    if (keyCode == UP) return "UP";
    if (keyCode == DOWN) return "DOWN";
    if (keyCode == LEFT) return "LEFT";
    if (keyCode == RIGHT) return "RIGHT";
    return null;
  }
  char k = Character.toLowerCase(key);
  if (k == 'w') return "UP";
  if (k == 's') return "DOWN";
  if (k == 'a') return "LEFT";
  if (k == 'd') return "RIGHT";
  return null;
}

boolean esEnter() {
  return key == ENTER || key == RETURN;
}

void keyPressed() {
  if (estadoApp == MENU) {
    if (key == '1') estadoApp = CONFIG;
    if (key == '2') iniciarCliente();
    if (key == '3') {
      indiceIp = (indiceIp + 1) % ipsServidor.length;
      ipServidor = ipsServidor[indiceIp];
    }
  }
  else if (estadoApp == CONFIG) {
    if (key == TAB) {
      modoJuego = (modoJuego == PUNTOS) ? TIEMPO : PUNTOS;
      aplicarConfig();
    } else if (key == CODED && (keyCode == LEFT || keyCode == RIGHT)) {
      int d = (keyCode == RIGHT) ? 1 : -1;
      if (modoJuego == PUNTOS) idxPuntos = constrain(idxPuntos + d, 0, opcionesPuntos.length - 1);
      else idxMin = constrain(idxMin + d, 0, opcionesMin.length - 1);
      aplicarConfig();
    } else if (esEnter()) {
      iniciarServidor();
    } else if (key == BACKSPACE) {
      estadoApp = MENU;
    }
  }
  else if (estadoApp == SERVIDOR) {
    if (fase == LOBBY && esEnter()) iniciarPartida();
    else if (fase == FIN && esEnter()) volverALobby();
    else if (fase == JUGANDO) {
      String d = teclaADireccion();
      if (d != null) procesarComando(0, "KEY_" + d);
    }
  }
  else if (estadoApp == CLIENTE) {
    if (c != null && c.active()) {
      String d = teclaADireccion();
      if (d != null) c.write("KEY_" + d + "\n");
    } else if (key == 'r' || key == 'R') {
      iniciarCliente();
    }
  }
}


// ==========================================
// 3. SERVER LOGIC
// ==========================================
void iniciarServidor() {
  aplicarConfig();
  s = new Server(this, puerto);
  volverALobby();
  estadoApp = SERVIDOR;
  miId = 0;
  ultimoMs = millis();
}

// llamado por Processing (en otro hilo) cuando alguien se conecta
void serverEvent(Server srv, Client cl) {
  synchronized (pendientes) {
    pendientes.add(cl);
  }
}

void volverALobby() {
  fase = LOBBY;
  for (int i = 0; i < MAXJ; i++) {
    puntos[i] = 0;
    est[i] = 0;
    participa[i] = false;
  }
  rondaTerminada = false;
  limpiarRastro();
  sistemaParticulas.clear();
}

boolean presente(int i) {
  return i == 0 || clientes[i - 1] != null;
}

int mascaraPresentes() {
  int m = 0;
  for (int i = 0; i < MAXJ; i++) if (presente(i)) m |= (1 << i);
  return m;
}

void aceptarNuevos() {
  ArrayList<Client> nuevos;
  synchronized (pendientes) {
    nuevos = new ArrayList<Client>(pendientes);
    pendientes.clear();
  }
  for (Client cl : nuevos) {
    int slot = -1;
    if (fase == LOBBY) {
      for (int k = 0; k < clientes.length; k++) {
        if (clientes[k] == null) { slot = k; break; }
      }
    }
    if (slot < 0) {
      cl.write("LLENO\n");
      cl.stop();
    } else {
      clientes[slot] = cl;
      cl.write("WELCOME," + (slot + 1) + "\n");
    }
  }
}

void leerInputsRemotos() {
  for (int k = 0; k < clientes.length; k++) {
    Client cl = clientes[k];
    if (cl == null) continue;
    while (cl.available() > 0) {
      String mensaje = cl.readStringUntil('\n');
      if (mensaje == null) break;          // línea incompleta: seguir en el próximo frame
      procesarComando(k + 1, trim(mensaje));
    }
  }
}

void revisarDesconexiones() {
  for (int k = 0; k < clientes.length; k++) {
    if (clientes[k] != null && !clientes[k].active()) {
      clientes[k] = null;
      participa[k + 1] = false;
      est[k + 1] = 0;
    }
  }
}

void procesarComando(int id, String cmd) {
  if (cmd.equals("KEY_UP")) girar(id, 0, -1);
  else if (cmd.equals("KEY_DOWN")) girar(id, 0, 1);
  else if (cmd.equals("KEY_LEFT")) girar(id, -1, 0);
  else if (cmd.equals("KEY_RIGHT")) girar(id, 1, 0);
}

// se compara contra dx/dy (la dirección en la que realmente te moviste) y el giro se aplica
// al inicio del próximo frame: evita girar 2 veces en un frame y chocarte con tu propia estela
void girar(int i, int ddx, int ddy) {
  if (est[i] != 1) return;
  if (ddy != 0 && dy[i] == 0) { ndx[i] = 0; ndy[i] = ddy; }
  if (ddx != 0 && dx[i] == 0) { ndx[i] = ddx; ndy[i] = 0; }
}

void ejecutarServidor() {
  int ahora = millis();
  int dt = min(ahora - ultimoMs, 100);
  ultimoMs = ahora;

  aceptarNuevos();
  leerInputsRemotos();
  revisarDesconexiones();

  if (fase == LOBBY) {
    int m = mascaraPresentes();
    s.write("LOBBY," + modoJuego + "," + valorJuego + "," + m + "\n");
    dibujarLobby(m, true);
  }
  else if (fase == JUGANDO) {
    avanzarJuego(dt);
    if (fase == JUGANDO) enviarEstado();
    dibujarEscena();
  }
  else {
    s.write("FIN," + ganadorFinal + "," + puntos[0] + "," + puntos[1] + "," + puntos[2] + "," + puntos[3] + "," + puntos[4] + "\n");
    dibujarEscena();
    dibujarFin(true);
  }
}

void iniciarPartida() {
  int n = 0;
  for (int i = 0; i < MAXJ; i++) {
    participa[i] = presente(i);
    if (participa[i]) n++;
    puntos[i] = 0;
  }
  if (n < 2) return;                     // hacen falta al menos 2
  tRestMs = (modoJuego == TIEMPO) ? valorJuego * 60000 : 0;
  s.write("START\n");
  fase = JUGANDO;
  nuevaRonda();
}

void nuevaRonda() {
  for (int x = 0; x < FW; x++) {
    for (int y = 0; y < FH; y++) {
      ocupado[x][y] = false;
    }
  }
  limpiarRastro();
  for (int i = 0; i < MAXJ; i++) {
    if (participa[i]) {
      est[i] = 1;
      px[i] = round(SPX[i] * FW);
      py[i] = round(SPY[i] * FH);
      dx[i] = SDX[i]; dy[i] = SDY[i];
      ndx[i] = SDX[i]; ndy[i] = SDY[i];
    } else {
      est[i] = 0;
    }
  }
  rondaTerminada = false;
  s.write("RESET\n");
}

int contarVivos() {
  int n = 0;
  for (int i = 0; i < MAXJ; i++) if (est[i] == 1) n++;
  return n;
}

int contarParticipantes() {
  int n = 0;
  for (int i = 0; i < MAXJ; i++) if (participa[i]) n++;
  return n;
}

void avanzarJuego(int dt) {
  // aplicar giros pendientes
  for (int i = 0; i < MAXJ; i++) {
    if (est[i] == 1) { dx[i] = ndx[i]; dy[i] = ndy[i]; }
  }

  if (contarParticipantes() < 2) { terminarPartida(); return; }

  // pausa entre rondas
  if (rondaTerminada) {
    if (millis() >= pausaHasta) {
      boolean fin = false;
      if (modoJuego == PUNTOS) {
        for (int i = 0; i < MAXJ; i++) if (participa[i] && puntos[i] >= valorJuego) fin = true;
      }
      if (fin) terminarPartida(); else nuevaRonda();
    }
    return;
  }

  // reloj (solo corre mientras se juega, no durante la pausa)
  if (modoJuego == TIEMPO) {
    tRestMs -= dt;
    if (tRestMs <= 0) { tRestMs = 0; terminarPartida(); return; }
  }

  for (int p = 0; p < pasosPorFrame; p++) {
    int[] cx = new int[MAXJ];
    int[] cy = new int[MAXJ];
    boolean[] muere = new boolean[MAXJ];

    for (int i = 0; i < MAXJ; i++) {
      if (est[i] != 1) continue;
      float ox = px[i], oy = py[i];
      px[i] += dx[i];
      py[i] += dy[i];
      segmento(COLORES[i], ox, oy, px[i], py[i]);
      cx[i] = int(px[i]);
      cy[i] = int(py[i]);
      if (cx[i] < 0 || cx[i] >= FW || cy[i] < 0 || cy[i] >= FH || ocupado[cx[i]][cy[i]]) muere[i] = true;
    }

    // choque frontal: mueren los dos, sin puntos para nadie
    for (int i = 0; i < MAXJ; i++) {
      if (est[i] != 1) continue;
      for (int j = i + 1; j < MAXJ; j++) {
        if (est[j] == 1 && cx[i] == cx[j] && cy[i] == cy[j]) { muere[i] = true; muere[j] = true; }
      }
    }

    for (int i = 0; i < MAXJ; i++) {
      if (est[i] != 1) continue;
      if (muere[i]) {
        est[i] = 2;
        crearExplosion(FX + px[i], FY + py[i]);
        s.write("BOOM," + norm(px[i], FW) + "," + norm(py[i], FH) + "\n");
      } else {
        ocupado[cx[i]][cy[i]] = true;
      }
    }

    if (contarVivos() <= 1) break;
  }

  // la ronda termina cuando queda 1 o ninguno con vida
  if (contarVivos() <= 1) finalizarRonda();
}

void finalizarRonda() {
  int ganador = -1;
  if (contarVivos() == 1) {
    for (int i = 0; i < MAXJ; i++) {
      if (est[i] == 1) { ganador = i; puntos[i]++; }
    }
  }
  rondaTerminada = true;
  pausaHasta = millis() + 2000;
  mostrarMensajeRonda(ganador);
  s.write("RONDA," + ganador + "\n");
}

void mostrarMensajeRonda(int ganador) {
  msgRonda = (ganador >= 0) ? ("Punto para " + NOMBRES[ganador]) : "Choque: nadie suma";
  msgHasta = millis() + 2000;
}

void terminarPartida() {
  int mejor = -1, mejorPts = -1;
  boolean empate = false;
  for (int i = 0; i < MAXJ; i++) {
    if (!participa[i]) continue;
    if (puntos[i] > mejorPts) { mejorPts = puntos[i]; mejor = i; empate = false; }
    else if (puntos[i] == mejorPts) empate = true;
  }
  ganadorFinal = empate ? -1 : mejor;
  fase = FIN;
}

int norm(float v, int tam) {
  return round(v / tam * 10000);
}

void enviarEstado() {
  StringBuilder sb = new StringBuilder();
  sb.append("STATE,").append(tRestMs).append(",").append(modoJuego).append(",").append(valorJuego);
  for (int i = 0; i < MAXJ; i++) {
    sb.append(",").append(norm(px[i], FW)).append(",").append(norm(py[i], FH));
    sb.append(",").append(est[i]).append(",").append(puntos[i]);
  }
  sb.append("\n");
  s.write(sb.toString());
}


// ==========================================
// 4. CLIENT LOGIC
// ==========================================
void iniciarCliente() {
  limpiarRastro();
  for (int i = 0; i < MAXJ; i++) { est[i] = 0; prevEst[i] = 0; puntos[i] = 0; }
  fase = LOBBY;
  miId = -1;
  cMask = 0;
  estadoApp = CONECTANDO;
  thread("conectarCliente");     // la conexión corre en otro hilo: no congela la ventana
}

// conecta con timeout de 3 s; devuelve null si no se pudo
Client conectarConTimeout(String ip) {
  try {
    Socket sk = new Socket();
    sk.setTcpNoDelay(true);      // no juntar mensajes chicos de teclas: menos latencia de input
    sk.connect(new InetSocketAddress(ip, puerto), 3000);
    return new Client(this, sk);
  } catch (Exception e) {
    println("No se pudo conectar a " + ip + ": " + e);
    errorConexion += ip + " -> " + e.getClass().getSimpleName() + ": " + e.getMessage() + "\n";
    return null;
  }
}

void conectarCliente() {
  errorConexion = "";
  c = conectarConTimeout(ipServidor);
  if (c == null && ipsServidor.length > 1) {
    int alternativa = (indiceIp + 1) % ipsServidor.length;
    Client intentoAlternativo = conectarConTimeout(ipsServidor[alternativa]);
    if (intentoAlternativo != null) {
      c = intentoAlternativo;
      indiceIp = alternativa;
      ipServidor = ipsServidor[indiceIp];
    }
  }
  estadoApp = CLIENTE;
}

void ejecutarCliente() {
  if (c == null || !c.active()) {
    dibujarMensajeCentro("Sin conexión con " + ipServidor + "  -  [ R ] para reintentar");
    textSize(14);
    textAlign(CENTER, TOP);
    fill(255, 120, 120);
    text(errorConexion, W/2, H/2 + 30);
    return;
  }

  while (c.available() > 0) {
    String data = c.readStringUntil('\n');
    if (data == null) break;             // línea incompleta: seguir en el próximo frame
    String[] p = split(trim(data), ',');

    if (p[0].equals("WELCOME") && p.length >= 2) {
      miId = int(p[1]);
    }
    else if (p[0].equals("LLENO")) {
      errorConexion = "La sala está llena o la partida ya empezó.\n";
      c.stop();
      return;
    }
    else if (p[0].equals("LOBBY") && p.length >= 4) {
      if (fase != LOBBY) {
        for (int i = 0; i < MAXJ; i++) { est[i] = 0; prevEst[i] = 0; puntos[i] = 0; }
        limpiarRastro();
      }
      fase = LOBBY;
      modoJuego = int(p[1]);
      valorJuego = int(p[2]);
      cMask = int(p[3]);
    }
    else if (p[0].equals("START")) {
      fase = JUGANDO;
      for (int i = 0; i < MAXJ; i++) { prevEst[i] = 0; puntos[i] = 0; }
      limpiarRastro();
    }
    else if (p[0].equals("STATE") && p.length >= 4 + MAXJ * 4) {
      fase = JUGANDO;
      tRestMs = int(p[1]);
      modoJuego = int(p[2]);
      valorJuego = int(p[3]);
      for (int i = 0; i < MAXJ; i++) {
        int b = 4 + i * 4;
        px[i] = int(p[b]) / 10000.0 * FW;         // normalizado -> coordenadas del campo
        py[i] = int(p[b + 1]) / 10000.0 * FH;
        est[i] = int(p[b + 2]);
        puntos[i] = int(p[b + 3]);
        if (prevEst[i] == 1 && (est[i] == 1 || est[i] == 2)) {
          segmento(COLORES[i], qx[i], qy[i], px[i], py[i]);
        }
        qx[i] = px[i];
        qy[i] = py[i];
        prevEst[i] = est[i];
      }
    }
    else if (p[0].equals("RESET")) {
      limpiarRastro();
      for (int i = 0; i < MAXJ; i++) prevEst[i] = 0;
    }
    else if (p[0].equals("BOOM") && p.length >= 3) {
      crearExplosion(FX + int(p[1]) / 10000.0 * FW, FY + int(p[2]) / 10000.0 * FH);
    }
    else if (p[0].equals("RONDA") && p.length >= 2) {
      mostrarMensajeRonda(int(p[1]));
    }
    else if (p[0].equals("FIN") && p.length >= 2 + MAXJ) {
      fase = FIN;
      ganadorFinal = int(p[1]);
      for (int i = 0; i < MAXJ; i++) puntos[i] = int(p[2 + i]);
    }
  }

  if (fase == LOBBY) {
    dibujarLobby(cMask, false);
  } else {
    dibujarEscena();
    if (fase == FIN) dibujarFin(false);
  }
}


// ==========================================
// 5. RENDER & PARTICLES
// ==========================================
void segmento(int col, float x0, float y0, float x1, float y1) {
  segs.add(new Seg(col, x0, y0, x1, y1));
}

// vuelca todos los segmentos pendientes al buffer de estelas en un solo beginDraw/endDraw
void vaciarSegmentos() {
  if (segs.size() == 0) return;
  rastro.beginDraw();
  rastro.strokeWeight(3);
  for (Seg sg : segs) {
    rastro.stroke(sg.col);
    rastro.line(sg.x0, sg.y0, sg.x1, sg.y1);
  }
  rastro.endDraw();
  segs.clear();
}

void limpiarRastro() {
  segs.clear();
  rastro.beginDraw();
  rastro.clear();
  rastro.endDraw();
}

void dibujarHud() {
  noStroke();
  fill(10, 10, 15, 220);
  rect(0, 0, W, FY - 6);

  int n = 0;
  for (int i = 0; i < MAXJ; i++) if (est[i] != 0) n++;
  int k = 0;
  textAlign(CENTER, CENTER);
  textSize(18);
  for (int i = 0; i < MAXJ; i++) {
    if (est[i] == 0) continue;
    fill(COLORES[i], est[i] == 2 ? 110 : 255);
    String t = NOMBRES[i] + ": " + puntos[i] + (i == miId ? " *" : "");
    text(t, (k + 0.5f) * W / n, 14);
    k++;
  }

  textSize(14);
  fill(200);
  String info;
  if (modoJuego == TIEMPO) {
    int tot = (tRestMs + 999) / 1000;
    info = "Tiempo  " + nf(tot / 60, 2) + ":" + nf(tot % 60, 2);
  } else {
    info = "Primero a " + valorJuego + " puntos";
  }
  text(info, W/2, 34);
}

void dibujarEscena() {
  vaciarSegmentos();

  // fondo del campo + estelas
  noStroke();
  fill(6, 6, 14);
  rect(FX, FY, FW, FH);
  image(rastro, FX, FY);

  // bordes visibles del campo
  noFill();
  stroke(0, 200, 255, 70);
  strokeWeight(8);
  rect(FX - 3, FY - 3, FW + 6, FH + 6);
  stroke(0, 220, 255);
  strokeWeight(3);
  rect(FX - 2, FY - 2, FW + 4, FH + 4);

  actualizarDibujarParticulas();

  noStroke();
  for (int i = 0; i < MAXJ; i++) {
    if (est[i] == 1) {
      fill(COLORES[i]);
      ellipse(FX + px[i], FY + py[i], 6, 6);
    }
  }

  dibujarHud();

  if (millis() < msgHasta) {
    textAlign(CENTER, CENTER);
    textSize(28);
    fill(255, 230, 80);
    text(msgRonda, W/2, FY + FH/2);
  }
}

void dibujarLobby(int mask, boolean esHost) {
  textAlign(CENTER, CENTER);
  textSize(36);
  fill(0, 220, 255);
  text("SALA DE ESPERA", W/2, 55);

  textSize(18);
  fill(255);
  String cfg = (modoJuego == PUNTOS) ? ("Primero a " + valorJuego + " puntos") : ("Duración: " + valorJuego + (valorJuego == 1 ? " minuto" : " minutos"));
  text(cfg, W/2, 100);

  int n = 0;
  for (int i = 0; i < MAXJ; i++) {
    float y = 170 + i * 50;
    boolean pres = ((mask >> i) & 1) == 1;
    if (pres) n++;
    noStroke();
    fill(COLORES[i], pres ? 255 : 60);
    ellipse(W/2 - 160, y, 18, 18);
    textAlign(LEFT, CENTER);
    textSize(22);
    fill(255, pres ? 255 : 90);
    String t = NOMBRES[i] + (i == 0 ? " (Host)" : "") + (pres ? "" : "  -  esperando...");
    if (i == miId) t += "   <- vos";
    text(t, W/2 - 130, y);
  }

  textAlign(CENTER, CENTER);
  textSize(16);
  fill(255, 220, 60);
  if (esHost) {
    if (n >= 2) text("[ ENTER ] para empezar la partida", W/2, 455);
    else text("Se necesitan al menos 2 jugadores", W/2, 455);
    fill(150);
    textSize(14);
    text("Tu IP: " + Server.ip() + "  |  puerto " + puerto, W/2, 495);
  } else {
    text("Esperando que el host inicie la partida...", W/2, 455);
  }
  fill(150);
  textSize(14);
  text("Controles: WASD o flechas", W/2, 525);
}

void dibujarFin(boolean esHost) {
  noStroke();
  fill(0, 0, 0, 180);
  rect(FX, FY, FW, FH);

  textAlign(CENTER, CENTER);
  textSize(44);
  if (ganadorFinal >= 0) {
    fill(COLORES[ganadorFinal]);
    text("GANA " + NOMBRES[ganadorFinal].toUpperCase() + "!", W/2, 170);
  } else {
    fill(255, 230, 80);
    text("EMPATE", W/2, 170);
  }

  textSize(22);
  int k = 0;
  for (int i = 0; i < MAXJ; i++) {
    if (est[i] == 0) continue;
    fill(COLORES[i]);
    text(NOMBRES[i] + ": " + puntos[i] + " pts", W/2, 250 + k * 36);
    k++;
  }

  textSize(16);
  fill(255, 220, 60);
  text(esHost ? "[ ENTER ] volver a la sala" : "Esperando al host...", W/2, 520);
}

class Particula {
  PVector pos;
  PVector vel;
  color col;
  float vida;

  Particula(float x, float y) {
    pos = new PVector(x, y);
    vel = PVector.random2D().mult(random(1, 5));
    col = color(random(200, 255), random(50, 150), 0);
    vida = 255;
  }

  void actualizar() {
    pos.add(vel);
    vida -= 8;
  }

  void mostrar() {
    noStroke();
    fill(col, vida);
    ellipse(pos.x, pos.y, 4, 4);
  }
}

void crearExplosion(float x, float y) {
  for (int i = 0; i < 30; i++) {
    sistemaParticulas.add(new Particula(x, y));
  }
}

void actualizarDibujarParticulas() {
  for (int i = sistemaParticulas.size() - 1; i >= 0; i--) {
    Particula p = sistemaParticulas.get(i);
    p.actualizar();
    p.mostrar();
    if (p.vida <= 0) {
      sistemaParticulas.remove(i);
    }
  }
}
