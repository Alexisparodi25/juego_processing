#!/bin/bash
# Compila el sketch como lo hace Processing (para comprobar errores sin abrir el IDE).
# Uso: tools/build_sketch.sh <ruta a core.jar> <directorio de salida>
set -e
cd "$(dirname "$0")/.."
CORE="$1"; OUT="$2"
mkdir -p "$OUT/src"
{
  echo "import pokefuego.engine.*; import processing.core.*; import processing.data.*; import processing.event.*; import java.util.*; import java.io.*;"
  echo "public class Sketch extends PApplet {"
  cat pokefuego/pokefuego.pde pokefuego/BattleScene.pde pokefuego/Screens.pde | grep -v "^import pokefuego.engine"
  echo "}"
} | sed -E 's/^void (setup|draw|settings|keyPressed|keyReleased|mousePressed)\(/public void \1(/' > "$OUT/src/Sketch.java"
javac -nowarn -cp "$CORE:pokefuego/code/pokefuego-engine.jar" -d "$OUT/classes" "$OUT"/src/*.java 2>&1 | grep -v '^Picked up' || true
