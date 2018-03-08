# Game design

This is an odd little game, a bit like a head-to-head tower offense game (with no towers!) Opposing
armies charge at each other by placing units in lanes, to try to capture objectives, increasing
resource income.

 | - | - | - |
 |   |   |   |
 |   | * |   |
 |   | v |   |
 |   | ^ |   |
 |   |   | * |
 | v |   | v |
 | * |   |   |
 |   |   |   |
 |   |   | ^ |
 | - | - | - |

Space:
 - discrete lanes (x)
 - continuous position (y)
 - discrete time (dt)

Map:
 - objective positions
 - hills
 - walls?

Units:
 - simple micro AI
 - 3x circular counter units (e.g. swordsman, archer, horseman)

Controls:
 - unit/lane assignment
 - upgrades?

Resources:
 - objective points (control based)
