#!/bin/bash
# Compila el motor (Java puro) y corre simulaciones de combate para detectar errores.
set -e
cd "$(dirname "$0")/.."
OUT=$(mktemp -d)
javac -nowarn -d "$OUT" pokefuego/*.java tools/SimTest.java 2>&1 | grep -v '^Picked up' || true
java -cp "$OUT" SimTest pokefuego/data "${1:-2000}" "${2:-1}" 2>&1 | grep -v '^Picked up'
