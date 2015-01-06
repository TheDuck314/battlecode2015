package framework;

import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.TerrainTile;

public class Mining extends Bot {
    static MapLocation mineDest = null;

    static final double ORE_EXHAUSTED = 0.1;

    public static void tryMine() throws GameActionException {
        if (mineDest != null) {
            if (here.equals(mineDest)) {
                rc.setIndicatorString(2, "" + rc.senseOre(here));
                if (rc.senseOre(here) >= ORE_EXHAUSTED) {
                    rc.mine();
                    return;
                } else {
                    mineDest = null;
                }
            } else {
                if (rc.senseNearbyRobots(mineDest, 0, null).length != 0) {
                    mineDest = null;
                }
            }
        }

        if (mineDest == null) {
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
                        maxOre = ore;
                        bestDest = loc;
                    }
                }

                if (bestDest != null) {
                    mineDest = bestDest;
                    break;
                }
            }
        }

        Nav.goTo(mineDest);
    }
}
