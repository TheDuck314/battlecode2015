package framework9_drones;

import battlecode.common.*;

public class Mining extends Bot {
    static MapLocation mineDest = null;

    static final double ORE_EXHAUSTED = 2.0;

    public static void tryMine() throws GameActionException {
        if (mineDest != null) {
            if (here.equals(mineDest)) {
                if (rc.senseOre(here) >= ORE_EXHAUSTED) {
                    if (rc.senseNearbyRobots(2, us).length >= 3) {
                        Direction randDir = Direction.values()[(int) (8 * Math.random())];
                        if (rc.canMove(randDir) && rc.senseOre(here.add(randDir)) > rc.senseOre(here)) {
                            rc.move(randDir);
                            return;
                        }
                    }
                    rc.mine();
                    return;
                } else {
                    mineDest = null;
                }
            } else {
                if (rc.senseOre(here) >= ORE_EXHAUSTED) {
                    mineDest = here;
                    rc.mine();
                    return;
                } else if (rc.senseNearbyRobots(mineDest, 0, null).length != 0) {
                    mineDest = null;
                }
            }
        }

        if (mineDest == null) {
            MapLocation[] enemyTowers = rc.senseEnemyTowerLocations();

            for (int radius = 1;; radius++) {
                MapLocation[] locs = MapLocation.getAllMapLocationsWithinRadiusSq(here, radius * radius);

                MapLocation bestDest = null;
                double maxOre = ORE_EXHAUSTED;
                for (MapLocation loc : locs) {
                    double ore = rc.senseOre(loc);
                    if (ore == 0.0) {
                        if (rc.senseTerrainTile(loc) == TerrainTile.UNKNOWN) ore = ORE_EXHAUSTED + 0.1;
                    }
                    if (ore > maxOre && rc.senseNearbyRobots(loc, 0, us).length == 0) {
                        if (Combat.isSafe(loc, enemyTowers)) {
                            maxOre = ore;
                            bestDest = loc;
                        }
                    }
                }

                if (bestDest != null) {
                    mineDest = bestDest;
                    rc.yield(); // this process can take a long time
                    break;
                }
            }
        }

        // Debug.indicate("mine", 0, "going to mineDest = " + mineDest.toString());
        Nav.goTo(mineDest);
    }
}
