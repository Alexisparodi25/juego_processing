#!/bin/bash
# Compila el motor (Java puro) y corre simulaciones de combate IA vs IA para detectar errores.
set -e
cd "$(dirname "$0")/.."
OUT=$(mktemp -d)
javac --release 8 -nowarn -encoding UTF-8 -d "$OUT" engine/*.java 2>&1 | grep -v '^Picked up' || true
javac -nowarn -cp "$OUT" -d "$OUT" tools/SimTest.java 2>&1 | grep -v '^Picked up' || true
java -Dfile.encoding=UTF-8 -cp "$OUT" SimTest pokefuego/data "${1:-2000}" "${2:-1}" 2>&1 | grep -v '^Picked up'
