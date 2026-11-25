# Juegos en Processing

Este repositorio contiene dos juegos hechos en [Processing](https://processing.org/) (modo Java):

| Carpeta | Juego |
|---|---|
| [`pokefuego/`](pokefuego/) | **Pokémon Rojo Fuego — Edición Total** (ver abajo) |
| [`tron/`](tron/) | TRON, motos de luz (ver [`tron/README.md`](tron/README.md)) |

---

# Pokémon Rojo Fuego — Edición Total

Un juego de Pokémon por turnos basado en **Rojo Fuego / Kanto**: 8 gimnasios, Alto Mando y Campeón.
Usa **todas las especies y formas de todas las generaciones, todas las megaevoluciones, movimientos,
habilidades y objetos**, pero con **únicamente dos mecánicas especiales: Megaevolución y Dinamax/Gigamax**,
y las dos **duran todo el combate** (no hay límite de 3 turnos).

## Cómo ejecutarlo

1. Instala Processing 3.5+ o 4.x.
2. Abre `pokefuego/pokefuego.pde` y pulsa **Run** (▶).
   (Los datos ya están en `pokefuego/data/`; no hace falta nada más.)

### Controles
| Acción | Teclas |
|---|---|
| Mover | Flechas o `W` `A` `S` `D` |
| Aceptar | `Z`, `Enter` o `Espacio` |
| Volver | `X`, `Retroceso` o `Esc` |
| **Megaevolución** (en combate, antes de elegir movimiento) | `M` |
| **Dinamax / Gigamax** (ídem) | `G` |

## Modos (se pueden combinar)
- **Normal**: Kanto con los equipos de Rojo Fuego (Brock, Misty, Surge...), especies salvajes por tema de zona.
- **Aleatorio**: iniciales, Pokémon salvajes y equipos de líderes/Alto Mando/rival aleatorios, de *todas* las generaciones
  (se respeta la fuerza aproximada de cada rival, no aparecen legendarios fuertes a nivel bajo).
- **Nuzlocke**: sólo capturas el primer Pokémon de cada zona (con cláusula de duplicados); si un Pokémon se debilita
  **muere para siempre** (va al Cementerio); si no queda ninguno vivo, la partida termina.
- **Dificultad: Difícil** (siempre):
  - IA que calcula el daño real de cada movimiento, prioriza KOs, usa preparación/estados/trampas y **cambia a counters**.
  - Jefes y entrenadores avanzados usan **Mega y Dinamax/Gigamax** con su as.
  - Entrenadores con IVs/EVs, objetos y movimientos competitivos que escalan con tus medallas.
  - **Tope de nivel** por jefe (tus Pokémon no ganan experiencia por encima del as del próximo jefe).
  - Captura más difícil, sin huida en combates de entrenador.

## Megaevolución y Dinamax
- **Mega**: el Pokémon debe llevar su **piedra mega** (las dan los jefes, los entrenadores y la tienda de Ciudad Azulona).
  Incluye las 48 megas clásicas, las 2 regresiones primigenias (Orbe Rojo/Azul) y las megas de Leyendas Z-A.
- **Dinamax / Gigamax**: disponible para cualquier Pokémon (salvo los que no pueden). Multiplica sus PS por 1,5
  y convierte sus movimientos en **Movimientos Max** (con efectos de clima/terreno/estadísticas) o **G-Max** si es de una de las 34 especies Gigamax.
  Los de estado se convierten en **Protección Max**.
- **Una transformación por combate y por bando** (Mega *o* Dinamax). Ambas duran hasta que termina el combate.

## Contenido
| | |
|---|---|
| Especies y formas | **1416** (todas las gens, formas regionales, Mega, Gigamax, formas por objeto: Arceus, Silvally, Genesect, Zacian...) |
| Megaevoluciones | **91** (+2 primigenias) |
| Gigamax | **34** |
| Movimientos | **888** + Movimientos Max/G-Max |
| Habilidades | **318** (todas existen y se muestran; ~210 tienen efecto en combate) |
| Objetos | **582** de combate + pociones, balls, piedras, vitaminas... (la tienda "Gran Almacén" de Ciudad Azulona vende **todos**) |

### Qué está implementado en combate (y qué no)
Todos los datos (estadísticas, tipos, habilidades, movimientos que aprende cada especie por nivel/MT/tutor/huevo, cadenas evolutivas) son reales.
El **motor de combate es propio** y no implementa el 100 % de las mecánicas de los juegos oficiales:
- **Movimientos de daño (570):** todos funcionan (poder, tipo, precisión, prioridad, críticos, multigolpe, retroceso, drenaje, efectos
  secundarios, cargas/recarga, furia, golpes fijos/por nivel, OHKO, poder variable por peso/PS/velocidad, etc.).
- **Movimientos de estado (266):** ~144 implementados (estados, potenciar/bajar estadísticas, curación, clima, terreno, trampas,
  pantallas, Protección, Mofa, Espacio Raro...). Los demás (Sustituto, Rencor, Tinieblas...) **no hacen nada** y la IA/generador de
  equipos los evita.
- **Habilidades:** todas se muestran con su descripción; ~210 de 318 tienen efecto real (las más usadas: Intimidación, climas,
  absorciones, Robustez, Mold Breaker, Técnico, Adaptable, Velo Arena, etc.). Faltan las que dependen de mecánicas de dobles/formas raras.
- **Objetos:** bayas, restos, vidasfera, bandas/gafas Elección, chalecos, placas, potenciadores de tipo, orbes, piedras mega, etc.
  Muchos objetos "de nicho" existen pero no tienen efecto en combate.
- **Combates 1 vs 1** (sin dobles).
- Los **nombres** de especies, movimientos y habilidades están en inglés (los de la fuente de datos); la interfaz está en español.
- **Sin sprites oficiales** (no se pueden incluir): el juego dibuja una criatura generada por tipo. Para usar sprites reales ejecuta
  `tools/download_sprites.sh` (descarga de PokeAPI/sprites); el juego los detecta solo en `pokefuego/data/sprites/`.

## Estructura
```
pokefuego/
  pokefuego.pde      sketch principal (bucle, entrada, utilidades de dibujo)
  Screens.pde        título, mapa de Kanto, equipo, mochila, tienda, cajas, Pokédex...
  BattleScene.pde    escena de combate
  Battle.java        motor de combate (Mega, Dinamax, estados, clima, trampas...)
  Calc.java          daño, estadísticas, tipos, modificadores de habilidades y objetos
  AI.java            IA de entrenadores
  Game.java          partida: Kanto, jefes, aleatorio, Nuzlocke, experiencia, guardado
  Data.java Mon.java Items.java Effects.java
  data/*.tsv         datos generados con tools/export_data.js
tools/
  export_data.js     regenera los datos desde @pkmn/dex (npm i && node export_data.js)
  check.sh           compila el motor y simula miles de combates IA vs IA
  PlaySim.java       bot que juega partidas completas (todos los modos)
  build_sketch.sh    compila el sketch contra core.jar sin abrir Processing
  UiTest*.java       pruebas de interfaz bajo Xvfb (teclas simuladas + capturas)
  download_sprites.sh  (opcional) descarga sprites reales
```

## Pruebas
```bash
tools/check.sh 2000      # 2000 combates aleatorios con Mega/Dinamax: debe terminar con "errores=0"
```

Los datos provienen de [@pkmn/dex](https://github.com/pkmn/ps) (Pokémon Showdown). Pokémon es marca de Nintendo/Game Freak/Creatures;
proyecto de fans sin fines de lucro.
