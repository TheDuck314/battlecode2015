package framework6_launchers;

import battlecode.common.*;

public class BotBeaver extends Bot {
    public static void loop(RobotController theRC) throws GameActionException {
        Bot.init(theRC);
        Debug.init("hangout");
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
            if (hangOutLoc == null || Util.numAlliedBuildingsAdjacent(hangOutLoc) > 0
                    || (here != hangOutLoc && rc.senseNearbyRobots(hangOutLoc, 0, us).length > 0) || rc.senseTerrainTile(hangOutLoc) != TerrainTile.NORMAL) {
                MapLocation searchCenter = here; // new MapLocation((3 * here.x + ourHQ.x) / 4, (3 * here.y + ourHQ.y) / 4);
                for (int radius = 1;; radius++) {
                    MapLocation[] locs = MapLocation.getAllMapLocationsWithinRadiusSq(searchCenter, radius * radius);

                    for (MapLocation loc : locs) {
                        // if (loc.distanceSquaredTo(searchCenter) > (radius - 1) * (radius - 1)) {
                        TerrainTile terrain = rc.senseTerrainTile(loc);
                        if (terrain == TerrainTile.NORMAL) {
                            if (Util.numAlliedBuildingsAdjacent(loc) == 0) {
                                hangOutLoc = loc;
                                return;
                            }
                        }
                        // }
                    }

                }
            }

            Debug.indicate("hangout", 0, "going to hang out at " + hangOutLoc.toString());
            Nav.goTo(hangOutLoc);
        }

        Supply.shareSupply();
    }

    private static void tryBuildSomething() throws GameActionException {
        RobotType desiredBuilding = MessageBoard.DESIRED_BUILDING.readRobotType();

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
