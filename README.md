# TRON - Motos de luz (Processing)

Juego estilo **Tron** hecho en [Processing](https://processing.org/) (modo Java).
Cada moto deja una estela de luz detrás: si chocás contra cualquier estela
(incluida la tuya), perdés la ronda. Los bordes no matan: si salís por un lado
de la pantalla, aparecés por el lado contrario. Gana el primero en llegar a 5 puntos.

## Cómo jugar

1. Instalá Processing (3.x o 4.x).
2. Abrí `tron/tron.pde` y apretá **Run** (▶).

### Modos
- **1** - Un jugador contra la CPU
- **2** - Dos jugadores en el mismo teclado

### Controles
| Acción | Jugador 1 (cian) | Jugador 2 (naranja) |
|---|---|---|
| Girar | `W` `A` `S` `D` | Flechas |

En modo 1 jugador, las flechas también mueven al jugador 1.

- `P` - Pausa
- `M` / `ESC` - Volver al menú
- `ENTER` - Volver al menú al terminar la partida

## Características
- Estelas con efecto de brillo neón
- Cuenta regresiva antes de cada ronda
- Explosiones de partículas al chocar
- CPU que elige el camino con más espacio libre (relleno por inundación)
- Buffer de teclas para poder hacer giros rápidos sin perder pulsaciones
- Detección de choques de frente (empate)
- Bordes que funcionan como portales al lado contrario
