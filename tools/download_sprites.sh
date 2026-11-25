#!/bin/bash
# Descarga sprites opcionales (frontal y espalda) desde el repositorio público PokeAPI/sprites.
# El juego funciona sin ellos (dibuja criaturas procedurales por tipo); si existen, los usa.
# Uso: tools/download_sprites.sh [numero_maximo_de_dex]   (por defecto 1025)
MAX="${1:-1025}"
DIR="$(dirname "$0")/../pokefuego/data/sprites"
mkdir -p "$DIR/back"
BASE="https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon"
for i in $(seq 1 "$MAX"); do
  [ -f "$DIR/$i.png" ] || curl -sfL "$BASE/$i.png" -o "$DIR/$i.png" || rm -f "$DIR/$i.png"
  [ -f "$DIR/back/$i.png" ] || curl -sfL "$BASE/back/$i.png" -o "$DIR/back/$i.png" || rm -f "$DIR/back/$i.png"
done
echo "Listo. Sprites en $DIR"
