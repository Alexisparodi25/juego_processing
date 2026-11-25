#!/bin/bash
# Compila el motor (engine/*.java) a pokefuego/code/pokefuego-engine.jar (Java 8, lo carga Processing solo).
set -e
cd "$(dirname "$0")/.."
OUT=$(mktemp -d)
javac --release 8 -nowarn -encoding UTF-8 -d "$OUT" engine/*.java 2>&1 | grep -v '^Picked up' || true
mkdir -p pokefuego/code
jar cf pokefuego/code/pokefuego-engine.jar -C "$OUT" .
echo "OK -> pokefuego/code/pokefuego-engine.jar"
