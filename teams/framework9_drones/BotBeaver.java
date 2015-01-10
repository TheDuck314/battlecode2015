package framework9_drones;

import battlecode.common.*;

public class BotBeaver extends Bot {
    public static void loop(RobotController theRC) throws GameActionException {
        Bot.init(theRC);
        Debug.init("bfsdist");
        while (true) {
            try {
                turn();
            } catch (Exception e) {
                e.printStackTrace();
            }
            rc.yield();
        }
    }

    static MapLocation hangOutLoc = null;

    private static void turn() throws GameActionException {
        here = rc.getLocation();

        if (rc.isWeaponReady()) Combat.shootAtNearbyEnemies();

        if (rc.isCoreReady()) tryBuildSomething();

        if (rc.isCoreReady()) {
            if (hangOutLoc == null || Util.numAlliedBuildingsAdjacent(hangOutLoc) > 0 || rc.senseNearbyRobots(hangOutLoc, 0, us).length > 0
                    || rc.senseTerrainTile(hangOutLoc) != TerrainTile.NORMAL) {
                MapLocation searchCenter = here;
                for (int radius = 1;; radius++) {
                    MapLocation[] locs = MapLocation.getAllMapLocationsWithinRadiusSq(searchCenter, radius * radius);

                    for (MapLocation loc : locs) {
                        TerrainTile terrain = rc.senseTerrainTile(loc);
                        if (terrain == TerrainTile.NORMAL) {
                            if (Util.numAlliedBuildingsAdjacent(loc) == 0) {
                                hangOutLoc = loc;
                                return;
                            }
                        }
                    }

                }
            }

            if (here.equals(hangOutLoc)) {
                for (Direction dir : Direction.values()) {
                    if (rc.canMove(dir)) {
                        rc.move(dir);
                        return;
                    }
                }
            } else if (here.isAdjacentTo(hangOutLoc)) {
                return;
            } else {
                Nav.goTo(hangOutLoc);
            }
        }

//         MapLocation dest = new MapLocation((ourHQ.x + theirHQ.x) / 2, (ourHQ.y + theirHQ.y)/2);
//         if(rc.isCoreReady()) {
//             Nav.goTo(dest);
//         }
//         if(BfsDistributed.readResult(here, dest, Nav.minBfsInitRound) == null) BfsDistributed.work(dest);
    }

    private static void tryBuildSomething() throws GameActionException {
        RobotType desiredBuilding = MessageBoard.DESIRED_BUILDING.readRobotType();

        if (desiredBuilding == RobotType.HQ) return;

        if (rc.getTeamOre() > desiredBuilding.oreCost) {
            for (Direction dir : Direction.values()) {
                if (rc.canBuild(dir, desiredBuilding)) {
                    int numBuildingsAdjacent = 0;
                    RobotInfo[] adjacentAllies = rc.senseNearbyRobots(here.add(dir), 2, us);
                    for (RobotInfo ally : adjacentAllies) {
                        if (ally.type.isBuilding) numBuildingsAdjacent++;
                    }
                    if (numBuildingsAdjacent >= 1) continue;

                    rc.build(dir, desiredBuilding);
                    return;
                }
            }
        }
    }

    static MapLocation mineDest = null;
    static final double ORE_EXHAUSTED = 5.0;
}
