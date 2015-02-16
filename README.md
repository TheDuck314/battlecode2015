This is my code for my [Battlecode 2015](http://www.battlecode.org/) entry as "the other team." My final bot is in teams/zephyr26_final and won the main tournament. 

My submission to the Bloomberg tournament is in teams/bloomberg and won the Bloomberg pathfinding prize. 

#### Highlights

**Pathfinding:** Launchers used a breadth-first search for pathfinding. The BFS was computed in a distributed fashion by my miners using spare bytecodes, and is implemented in BfsDistributed.java. As a fallback there's a bug algorithm implemented in Nav.java. Everything besides launchers used the bug nav.

**Launchers:** Launcher/missile micro is in BotLauncher.java, BotMissile.java, and MissileGuidance.java

**Commander:** Commander micro is in CommanderHarass.java

**Strategy:** Overall strategy, including build order, is implemented in BotHQ.java.

**Supply:** My supply distribution system is implemented in Supply.java, including a resupply drone to ferry supply from the HQ to hungry bots.

**Mining:** Mining is implemented in BotMiner.java.

**Map analysis:** After the seeding tournament I wrote MapAnalysis.java to detect maps like frontlines.xml where there is a large amount of ore completely protected by allied towers. On such maps I would skip building a commander and just build launchers. Unfortunately the qualifying and final tournaments had zero such maps so the code was entirely useless :(

##### Unused code

BotSoldier.java, BotTank.java, and Harass.java are unused in my final bot and are just left over from previous versions.

#### Other versions

My sprint bot is in teams/sprint5_breakties. My seeding bot is in teams/seeding. The rest of the folders in teams/ are various intermediate or testing versions.

#### Other Battlecode 2015 repositories

* [anim0ls](https://bitbucket.org/jdshen/battlecode-2015) (3rd place)
* [Fox](https://github.com/dchoi2/BattleCode2015) (9th-12th place)
* [Ayyyyyyyylmao](https://github.com/awojnowski/Battlecode2015) (13th-16th place)
* [Fragments of Hologram Rose](https://github.com/3urningChrome/battlecode2015)
