// =====================================================
//  TRON - Motos de luz
//  Juego estilo Tron hecho en Processing (modo Java)
//
//  Jugador 1 (CIAN):    W A S D
//  Jugador 2 (NARANJA): Flechas  (en modo 1 jugador la
//                       moto naranja la maneja la CPU y
//                       las flechas también mueven al J1)
//  P = pausa   M = volver al menú
// =====================================================

// ---------- Configuración ----------
final int CELDA = 8;             // tamaño de cada celda en píxeles
final int HUD = 40;              // alto de la barra superior
final int FRAMES_POR_PASO = 3;   // cada cuántos frames avanzan las motos
final int PUNTOS_PARA_GANAR = 5;

// ---------- Estados ----------
final int MENU = 0;
final int CUENTA = 1;
final int JUGANDO = 2;
final int FIN_RONDA = 3;
final int FIN_PARTIDA = 4;

// Direcciones: 0 derecha, 1 abajo, 2 izquierda, 3 arriba
final int[] DX = {1, 0, -1, 0};
final int[] DY = {0, 1, 0, -1};

int cols, filas;
int[][] grid;                  // 0 = libre, 1 = estela J1, 2 = estela J2
Moto[] motos = new Moto[2];
int[] puntos = new int[2];
int estado = MENU;
boolean vsCPU = true;
boolean pausa = false;
int contadorFrames = 0;
int tiempoEstado = 0;
int ganadorRonda = -1;         // -1 empate, 0 J1, 1 J2

PGraphics capaBrillo;          // halo difuso de las estelas
PGraphics capaNucleo;          // línea brillante de las estelas
ArrayList<Particula> particulas = new ArrayList<Particula>();

// Para el relleno por inundación de la IA
int[][] visitado;
int marcaVisita = 0;
int[] pilaX, pilaY;

color COLOR_J1, COLOR_J2;
String[] NOMBRES = {"CIAN", "NARANJA"};
PFont fuente;

void setup() {
  size(800, 640);
  frameRate(60);
  cols = width / CELDA;
  filas = (height - HUD) / CELDA;
  grid = new int[cols][filas];
  visitado = new int[cols][filas];
  pilaX = new int[cols * filas];
  pilaY = new int[cols * filas];

  COLOR_J1 = color(0, 240, 255);
  COLOR_J2 = color(255, 140, 20);

  capaBrillo = createGraphics(width, height - HUD);
  capaNucleo = createGraphics(width, height - HUD);

  fuente = createFont("Monospaced", 32, true);
  textFont(fuente);
}

// =====================================================
//  Bucle principal
// =====================================================
void draw() {
  background(5, 5, 15);

  if (estado == MENU) {
    dibujarMenu();
    return;
  }

  // Lógica
  if (estado == CUENTA && millis() - tiempoEstado >= 3000) {
    estado = JUGANDO;
    contadorFrames = 0;
  }
  if (estado == JUGANDO && !pausa) {
    contadorFrames++;
    if (contadorFrames >= FRAMES_POR_PASO) {
      contadorFrames = 0;
      paso();
    }
  }
  if (estado == FIN_RONDA && millis() - tiempoEstado >= 2500) {
    if (puntos[0] >= PUNTOS_PARA_GANAR || puntos[1] >= PUNTOS_PARA_GANAR) {
      estado = FIN_PARTIDA;
    } else {
      nuevaRonda();
    }
  }

  // Dibujo
  dibujarArena();
  actualizarParticulas();
  dibujarHUD();
  dibujarMensajes();
}

// =====================================================
//  Partida y rondas
// =====================================================
void nuevaPartida() {
  puntos[0] = 0;
  puntos[1] = 0;
  nuevaRonda();
}

void nuevaRonda() {
  for (int x = 0; x < cols; x++) {
    for (int y = 0; y < filas; y++) {
      grid[x][y] = 0;
    }
  }
  particulas.clear();
  pausa = false;

  motos[0] = new Moto(0, cols / 4, filas / 2, 0, COLOR_J1);
  motos[1] = new Moto(1, cols * 3 / 4, filas / 2, 2, COLOR_J2);

  capaBrillo.beginDraw();
  capaBrillo.clear();
  capaBrillo.endDraw();
  capaNucleo.beginDraw();
  capaNucleo.clear();
  capaNucleo.endDraw();

  for (Moto m : motos) {
    grid[m.x][m.y] = m.id + 1;
    dibujarSegmento(m, m.x, m.y);
  }

  estado = CUENTA;
  tiempoEstado = millis();
}

// Avanza todas las motos una celda
void paso() {
  for (Moto m : motos) {
    m.aplicarGiro();
  }
  if (vsCPU) {
    motos[1].dir = decidirIA(motos[1], motos[0]);
  }

  int[] nx = new int[2];
  int[] ny = new int[2];
  boolean[] choca = new boolean[2];
  for (int i = 0; i < 2; i++) {
    nx[i] = envolverX(motos[i].x + DX[motos[i].dir]);
    ny[i] = envolverY(motos[i].y + DY[motos[i].dir]);
    choca[i] = !libre(nx[i], ny[i]);
  }
  // Choque de frente: las dos entran a la misma celda
  if (nx[0] == nx[1] && ny[0] == ny[1]) {
    choca[0] = true;
    choca[1] = true;
  }

  for (int i = 0; i < 2; i++) {
    Moto m = motos[i];
    if (choca[i]) {
      m.viva = false;
      explotar(m);
    } else {
      int viejoX = m.x;
      int viejoY = m.y;
      m.x = nx[i];
      m.y = ny[i];
      grid[m.x][m.y] = m.id + 1;
      dibujarSegmento(m, viejoX, viejoY);
    }
  }

  if (!motos[0].viva || !motos[1].viva) {
    if (!motos[0].viva && !motos[1].viva) {
      ganadorRonda = -1;
    } else {
      ganadorRonda = motos[0].viva ? 0 : 1;
      puntos[ganadorRonda]++;
    }
    estado = FIN_RONDA;
    tiempoEstado = millis();
  }
}

boolean libre(int x, int y) {
  return grid[envolverX(x)][envolverY(y)] == 0;
}

// Los bordes son portales: al salir por un lado se entra por el contrario
int envolverX(int x) {
  return (x % cols + cols) % cols;
}

int envolverY(int y) {
  return (y % filas + filas) % filas;
}

// Distancia teniendo en cuenta que los bordes se conectan
int distanciaEnvuelta(int x1, int y1, int x2, int y2) {
  int dx = abs(x1 - x2);
  int dy = abs(y1 - y2);
  return min(dx, cols - dx) + min(dy, filas - dy);
}

// =====================================================
//  Inteligencia artificial
// =====================================================
int decidirIA(Moto yo, Moto rival) {
  int[] opciones = {yo.dir, (yo.dir + 3) % 4, (yo.dir + 1) % 4}; // recto, izq, der
  int mejorDir = yo.dir;
  float mejorPuntaje = -1e9;

  for (int k = 0; k < opciones.length; k++) {
    int d = opciones[k];
    int x = envolverX(yo.x + DX[d]);
    int y = envolverY(yo.y + DY[d]);
    float puntaje;
    if (!libre(x, y)) {
      puntaje = -1e6;
    } else {
      // Espacio libre alcanzable desde esa celda
      puntaje = contarEspacio(x, y, cols * filas);
      // Preferir seguir recto (movimiento más natural)
      if (k == 0) puntaje += 8;
      // Evitar quedar pegado a la cabeza del rival (riesgo de choque de frente)
      if (distanciaEnvuelta(x, y, rival.x, rival.y) <= 1) puntaje -= 50;
      // Un poco de azar para que no sea predecible
      puntaje += random(0, 6);
    }
    if (puntaje > mejorPuntaje) {
      mejorPuntaje = puntaje;
      mejorDir = d;
    }
  }
  return mejorDir;
}

// Relleno por inundación limitado: cuántas celdas libres hay conectadas
int contarEspacio(int sx, int sy, int limite) {
  marcaVisita++;
  int tope = 0;
  int cuenta = 0;
  pilaX[tope] = sx;
  pilaY[tope] = sy;
  tope++;
  visitado[sx][sy] = marcaVisita;

  while (tope > 0 && cuenta < limite) {
    tope--;
    int x = pilaX[tope];
    int y = pilaY[tope];
    cuenta++;
    for (int d = 0; d < 4; d++) {
      int vx = envolverX(x + DX[d]);
      int vy = envolverY(y + DY[d]);
      if (libre(vx, vy) && visitado[vx][vy] != marcaVisita) {
        visitado[vx][vy] = marcaVisita;
        pilaX[tope] = vx;
        pilaY[tope] = vy;
        tope++;
      }
    }
  }
  return cuenta;
}

// =====================================================
//  Dibujo
// =====================================================
void dibujarSegmento(Moto m, int desdeX, int desdeY) {
  float x1 = desdeX * CELDA + CELDA / 2.0;
  float y1 = desdeY * CELDA + CELDA / 2.0;
  float x2 = m.x * CELDA + CELDA / 2.0;
  float y2 = m.y * CELDA + CELDA / 2.0;

  if (abs(m.x - desdeX) > 1 || abs(m.y - desdeY) > 1) {
    // Cruzó un borde: se dibuja media línea hasta el borde de salida
    // y otra media desde el borde de entrada, en vez de cruzar la pantalla
    float medio = CELDA / 2.0;
    dibujarLinea(m.col, x1, y1, x1 + DX[m.dir] * medio, y1 + DY[m.dir] * medio);
    dibujarLinea(m.col, x2 - DX[m.dir] * medio, y2 - DY[m.dir] * medio, x2, y2);
  } else {
    dibujarLinea(m.col, x1, y1, x2, y2);
  }
}

void dibujarLinea(color c, float x1, float y1, float x2, float y2) {
  capaBrillo.beginDraw();
  capaBrillo.strokeCap(ROUND);
  capaBrillo.stroke(c, 45);
  capaBrillo.strokeWeight(CELDA * 1.6);
  capaBrillo.line(x1, y1, x2, y2);
  capaBrillo.endDraw();

  capaNucleo.beginDraw();
  capaNucleo.strokeCap(ROUND);
  capaNucleo.stroke(c);
  capaNucleo.strokeWeight(CELDA * 0.5);
  capaNucleo.line(x1, y1, x2, y2);
  capaNucleo.stroke(255, 160);
  capaNucleo.strokeWeight(1.5);
  capaNucleo.line(x1, y1, x2, y2);
  capaNucleo.endDraw();
}

void dibujarArena() {
  pushMatrix();
  translate(0, HUD);

  // Cuadrícula de fondo
  stroke(0, 90, 140, 45);
  strokeWeight(1);
  for (int x = 0; x <= cols; x += 5) {
    line(x * CELDA, 0, x * CELDA, filas * CELDA);
  }
  for (int y = 0; y <= filas; y += 5) {
    line(0, y * CELDA, cols * CELDA, y * CELDA);
  }

  // Estelas
  image(capaBrillo, 0, 0);
  image(capaNucleo, 0, 0);

  // Cabezas de las motos
  float pulso = 0.5 + 0.5 * sin(millis() * 0.02);
  rectMode(CENTER);
  for (Moto m : motos) {
    if (!m.viva) continue;
    float cx = m.x * CELDA + CELDA / 2.0;
    float cy = m.y * CELDA + CELDA / 2.0;
    noStroke();
    fill(m.col, 70 + 60 * pulso);
    ellipse(cx, cy, CELDA * 3, CELDA * 3);
    fill(255);
    rect(cx, cy, CELDA, CELDA);
  }
  rectMode(CORNER);

  // Borde de la arena: línea que titila para indicar que es un portal
  noFill();
  stroke(0, 200, 255, 60 + 50 * pulso);
  strokeWeight(2);
  rect(1, 1, cols * CELDA - 2, filas * CELDA - 2);

  popMatrix();
}

void dibujarHUD() {
  noStroke();
  fill(0, 20, 35);
  rect(0, 0, width, HUD);
  stroke(0, 200, 255, 120);
  line(0, HUD - 1, width, HUD - 1);

  textSize(20);
  textAlign(LEFT, CENTER);
  fill(COLOR_J1);
  text(NOMBRES[0] + "  " + puntos[0], 15, HUD / 2);

  textAlign(RIGHT, CENTER);
  fill(COLOR_J2);
  String nombre2 = vsCPU ? "CPU" : NOMBRES[1];
  text(puntos[1] + "  " + nombre2, width - 15, HUD / 2);

  textAlign(CENTER, CENTER);
  fill(180);
  textSize(14);
  text("Primero a " + PUNTOS_PARA_GANAR + "   |   P pausa   M menú", width / 2, HUD / 2);
}

void dibujarMensajes() {
  textAlign(CENTER, CENTER);
  float cy = HUD + (height - HUD) / 2.0;

  if (estado == CUENTA) {
    int restante = 3 - (millis() - tiempoEstado) / 1000;
    textoBrillante(str(max(restante, 1)), width / 2, cy - 60, 72, color(255));
  } else if (estado == JUGANDO && pausa) {
    capaOscura();
    textoBrillante("PAUSA", width / 2, cy, 64, color(0, 240, 255));
  } else if (estado == FIN_RONDA) {
    if (ganadorRonda == -1) {
      textoBrillante("¡EMPATE!", width / 2, cy, 56, color(255));
    } else {
      String nombre = (ganadorRonda == 1 && vsCPU) ? "CPU" : NOMBRES[ganadorRonda];
      textoBrillante("¡PUNTO PARA " + nombre + "!", width / 2, cy, 44, motos[ganadorRonda].col);
    }
  } else if (estado == FIN_PARTIDA) {
    capaOscura();
    int g = puntos[0] >= PUNTOS_PARA_GANAR ? 0 : 1;
    String nombre = (g == 1 && vsCPU) ? "LA CPU" : NOMBRES[g];
    textoBrillante("¡GANA " + nombre + "!", width / 2, cy - 30, 56, motos[g].col);
    fill(220);
    textSize(18);
    text(puntos[0] + " - " + puntos[1], width / 2, cy + 25);
    text("ENTER para volver al menú", width / 2, cy + 60);
  }
}

void dibujarMenu() {
  // Fondo animado: cuadrícula que se desplaza
  float desplaz = (millis() * 0.05) % (CELDA * 5);
  stroke(0, 90, 140, 50);
  strokeWeight(1);
  for (float x = -CELDA * 5 + desplaz; x < width; x += CELDA * 5) {
    line(x, 0, x, height);
  }
  for (float y = -CELDA * 5 + desplaz; y < height; y += CELDA * 5) {
    line(0, y, width, y);
  }

  textAlign(CENTER, CENTER);
  textoBrillante("TRON", width / 2, 150, 110, color(0, 240, 255));
  fill(255, 140, 20);
  textSize(20);
  text("MOTOS DE LUZ", width / 2, 225);

  fill(230);
  textSize(24);
  text("1 - Un jugador (vs CPU)", width / 2, 320);
  text("2 - Dos jugadores", width / 2, 365);

  textSize(16);
  fill(COLOR_J1);
  text("Jugador 1 (CIAN): W A S D", width / 2, 450);
  fill(COLOR_J2);
  text("Jugador 2 (NARANJA): Flechas", width / 2, 478);
  fill(160);
  text("No choques con ninguna estela. Los bordes te llevan al lado contrario.", width / 2, 530);
  text("Gana el primero en llegar a " + PUNTOS_PARA_GANAR + " puntos.", width / 2, 555);
}

void textoBrillante(String s, float x, float y, float tam, color c) {
  textSize(tam);
  textAlign(CENTER, CENTER);
  fill(c, 40);
  for (int i = 0; i < 8; i++) {
    float a = TWO_PI * i / 8;
    text(s, x + cos(a) * 4, y + sin(a) * 4);
  }
  fill(c);
  text(s, x, y);
  fill(255, 180);
  text(s, x, y - 1);
}

void capaOscura() {
  noStroke();
  fill(0, 150);
  rect(0, HUD, width, height - HUD);
}

// =====================================================
//  Partículas de explosión
// =====================================================
void explotar(Moto m) {
  float cx = m.x * CELDA + CELDA / 2.0;
  float cy = m.y * CELDA + CELDA / 2.0 + HUD;
  for (int i = 0; i < 80; i++) {
    particulas.add(new Particula(cx, cy, i % 3 == 0 ? color(255) : m.col));
  }
}

void actualizarParticulas() {
  noStroke();
  for (int i = particulas.size() - 1; i >= 0; i--) {
    Particula p = particulas.get(i);
    p.actualizar();
    p.dibujar();
    if (p.vida <= 0) particulas.remove(i);
  }
}

class Particula {
  float x, y, vx, vy, vida;
  color col;

  Particula(float x, float y, color col) {
    this.x = x;
    this.y = y;
    this.col = col;
    float ang = random(TWO_PI);
    float vel = random(1, 6);
    vx = cos(ang) * vel;
    vy = sin(ang) * vel;
    vida = random(30, 70);
  }

  void actualizar() {
    x += vx;
    y += vy;
    vx *= 0.95;
    vy *= 0.95;
    vida--;
  }

  void dibujar() {
    fill(col, map(vida, 0, 70, 0, 255));
    rect(x - 1.5, y - 1.5, 3, 3);
  }
}

// =====================================================
//  Motos
// =====================================================
class Moto {
  int id, x, y, dir;
  color col;
  boolean viva = true;
  IntList giros = new IntList();  // giros pendientes (buffer de teclas)

  Moto(int id, int x, int y, int dir, color col) {
    this.id = id;
    this.x = x;
    this.y = y;
    this.dir = dir;
    this.col = col;
  }

  // Encola un giro; ignora repetir la misma dirección o dar media vuelta
  void girar(int nuevaDir) {
    int ultima = giros.size() > 0 ? giros.get(giros.size() - 1) : dir;
    if (nuevaDir == ultima || nuevaDir == (ultima + 2) % 4) return;
    if (giros.size() < 3) giros.append(nuevaDir);
  }

  void aplicarGiro() {
    if (giros.size() > 0) dir = giros.remove(0);
  }
}

// =====================================================
//  Teclado
// =====================================================
void keyPressed() {
  if (key == ESC) {
    key = 0;          // evita que ESC cierre el sketch
    estado = MENU;
    return;
  }

  if (estado == MENU) {
    if (key == '1') {
      vsCPU = true;
      nuevaPartida();
    } else if (key == '2') {
      vsCPU = false;
      nuevaPartida();
    }
    return;
  }

  if (key == 'm' || key == 'M') {
    estado = MENU;
    return;
  }
  if ((key == 'p' || key == 'P') && estado == JUGANDO) {
    pausa = !pausa;
    return;
  }
  if (estado == FIN_PARTIDA && (key == ENTER || key == RETURN)) {
    estado = MENU;
    return;
  }

  if ((estado != CUENTA && estado != JUGANDO) || pausa) return;

  // Jugador 1: WASD
  char k = Character.toLowerCase(key);
  if (k == 'd') motos[0].girar(0);
  if (k == 's') motos[0].girar(1);
  if (k == 'a') motos[0].girar(2);
  if (k == 'w') motos[0].girar(3);

  // Flechas: J2 en modo 2 jugadores, J1 en modo vs CPU
  if (key == CODED) {
    Moto m = vsCPU ? motos[0] : motos[1];
    if (keyCode == RIGHT) m.girar(0);
    if (keyCode == DOWN)  m.girar(1);
    if (keyCode == LEFT)  m.girar(2);
    if (keyCode == UP)    m.girar(3);
  }
}
