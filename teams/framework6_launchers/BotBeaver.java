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
                    || (here != hangOutLoc && rc.senseNearbyRobots(hangOutLoc, 0, us).length > 0) || rc.senseTerrainTile(hangOutLoc) == TerrainTile.VOID
                    || rc.senseTerrainTile(hangOutLoc) == TerrainTile.OFF_MAP) {
                MapLocation searchCenter = new MapLocation((3 * here.x + ourHQ.x) / 4, (3 * here.y + ourHQ.y) / 4);
                for (int radius = 1;; radius++) {
                    MapLocation[] locs = MapLocation.getAllMapLocationsWithinRadiusSq(searchCenter, radius * radius);

                    for (MapLocation loc : locs) {
                        if (Util.numAlliedBuildingsAdjacent(loc) == 0) {
                            TerrainTile terrain = rc.senseTerrainTile(loc);
                            if (terrain != TerrainTile.VOID && terrain != TerrainTile.OFF_MAP) {
                                hangOutLoc = loc;
                                return;
                            }
                        }
                    }

                }
            }

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
