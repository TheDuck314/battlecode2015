package anatid16_dronedefender;

import battlecode.common.*;

public class BotBeaver extends Bot {
    public static void loop(RobotController theRC) throws GameActionException {
        Bot.init(theRC);
//        Debug.init("build");
        init();
        while (true) {
            try {
                turn();
            } catch (Exception e) {
                e.printStackTrace();
            }
            rc.yield();
        }
    }

    static MapLocation[] preferredBuildLocs = new MapLocation[100];
    static int numPreferredBuildLocs = 0;

    static MapLocation buildLoc = null;
    static boolean buildLocIsPreferred = false;

    static boolean urgent = false;

    private static void init() {
        computePreferredBuildLocs();
    }

    private static void turn() throws GameActionException {
        here = rc.getLocation();

        if (rc.isWeaponReady()) Combat.shootAtNearbyEnemies();

        if (buildLoc == null || !okToBuildAt(buildLoc)) {
            buildLoc = chooseNewBuildLoc();
            return; // choosing a new build loc can take a long time, so start a new turn
        }

        if (here.isAdjacentTo(buildLoc)) {
            tryBuildSomethingAtBuildLoc();
        } else {
            if (!buildLocIsPreferred) {
                if(tryFindOpportunisticBuildLoc()) {
                    return;
                }
            }
            goToBuildLoc();
        }
    }

    private static void addPreferredBuildLoc(MapLocation loc) {
        preferredBuildLocs[numPreferredBuildLocs++] = loc;
    }

    private static void computePreferredBuildLocs() {
        MapLocation[] hqAdjacentLocs = { ourHQ.add(1, -1), ourHQ.add(1, 1), ourHQ.add(-1, 1), ourHQ.add(-1, -1), ourHQ.add(0, -2), ourHQ.add(2, 0),
                ourHQ.add(0, 2), ourHQ.add(-2, 0) };

        for (MapLocation loc : hqAdjacentLocs) {
            if (okToBuildAt(loc)) {
                addPreferredBuildLoc(loc);
            }
        }
    }

    private static int[] legDX = { 0, 0, 0, -1, 0, 0, 0, 1 };
    private static int[] legDY = { 0, 1, 0, 0, 0, -1, 0, 0 };

    private static MapLocation chooseNewBuildLoc() {
        for (int i = 0; i < numPreferredBuildLocs; i++) {
            if (okToBuildAt(preferredBuildLocs[i])) {
                buildLocIsPreferred = true;
                return preferredBuildLocs[i];
            }
        }

        buildLocIsPreferred = false;

        MapLocation searchCenter = here;

        Direction startDiag = here.directionTo(ourHQ);
        if (!startDiag.isDiagonal()) startDiag = startDiag.rotateLeft();

        for (int radius = 1;; radius++) {
            MapLocation loc = searchCenter.add(startDiag, radius);
            int diag = startDiag.ordinal();
            for (int leg = 0; leg < 4; leg++) {
                int dx = legDX[diag];
                int dy = legDY[diag];

                for (int i = 0; i < 2 * radius; i++) {
                    if (okToBuildAt(loc)) return loc;

                    loc = loc.add(dx, dy);
                }

                diag = (diag + 2) % 8;
            }
        }
    }

    private static boolean okToBuildAt(MapLocation loc) {
        TerrainTile tt = rc.senseTerrainTile(loc);
        if (Util.isImpassable(tt)) return false;

        RobotInfo[] orthogonallyAdjacentAllies = rc.senseNearbyRobots(loc, 1, us);
        for (RobotInfo ally : orthogonallyAdjacentAllies) {
            if (ally.type.isBuilding) return false;
            if (urgent && ally.location.equals(loc)) return false;
        }

        if (buildingHereBlocksPassage(loc)) return false;

        return true;
    }

    private static void tryBuildSomethingAtBuildLoc() throws GameActionException {
        if (!rc.isCoreReady()) return;

        RobotType buildingType = MessageBoard.DESIRED_BUILDING.readRobotType();
        if (buildingType == RobotType.HQ) return; // HQ means don't build anything

        if (rc.getTeamOre() < buildingType.oreCost) return;

        // if we go to build something and the location is blocked then we
        // have to choose another build location :(
        if (rc.senseRobotAtLocation(buildLoc) != null) {
            buildLoc = null;
            urgent = true;
            return;
        }

        Direction buildDir = here.directionTo(buildLoc);

        if (rc.canBuild(buildDir, buildingType)) {
            rc.build(buildDir, buildingType);
            urgent = false;
            buildLoc = null;
        }
    }

    private static boolean tryFindOpportunisticBuildLoc() throws GameActionException {
        Direction buildDir = here.directionTo(ourHQ);
        for (int i = 0; i < 8; i++) {
            if (rc.canMove(buildDir) && okToBuildAt(here.add(buildDir))) {
                buildLoc = here.add(buildDir);
                return true;
            }
            buildDir = buildDir.rotateLeft();
        }
        
        return false;
    }

    private static void goToBuildLoc() throws GameActionException {
        if (!rc.isCoreReady()) return;

        // if we are standing on the build location we need to get off it
        if (here.equals(buildLoc)) {
            getOffThisSquare();
        } else {
            MapLocation[] enemyTowers = rc.senseEnemyTowerLocations();
            RobotInfo[] nearbyEnemies = rc.senseNearbyRobots(35, them);
            NavSafetyPolicy safetyPolicy = new SafetyPolicyAvoidAllUnits(enemyTowers, nearbyEnemies);
            NewNav.goTo(buildLoc, safetyPolicy);
        }
    }

    private static void getOffThisSquare() throws GameActionException {
        Direction dir = here.directionTo(ourHQ);
        for (int i = 0; i < 8; i++) {
            if (rc.canMove(dir)) {
                rc.move(dir);
                return;
            }
            dir.rotateRight();
        }
    }

    private static boolean buildingHereBlocksPassage(MapLocation loc) {
        boolean northVoid = Util.isImpassable(rc.senseTerrainTile(loc.add(Direction.NORTH)));
        boolean eastVoid = Util.isImpassable(rc.senseTerrainTile(loc.add(Direction.EAST)));
        boolean southVoid = Util.isImpassable(rc.senseTerrainTile(loc.add(Direction.SOUTH)));
        boolean westVoid = Util.isImpassable(rc.senseTerrainTile(loc.add(Direction.WEST)));

        // Blockages like this:
        // . . .
        // X b X
        // . . .
        if (northVoid && southVoid) return true;
        if (eastVoid && westVoid) return true;

        // Blockages like this:
        // . X .
        // . b X
        // . . .
        if (northVoid && eastVoid) return !Util.isImpassable(rc.senseTerrainTile(loc.add(Direction.NORTH_EAST)));
        if (southVoid && eastVoid) return !Util.isImpassable(rc.senseTerrainTile(loc.add(Direction.SOUTH_EAST)));
        if (southVoid && westVoid) return !Util.isImpassable(rc.senseTerrainTile(loc.add(Direction.SOUTH_WEST)));
        if (northVoid && westVoid) return !Util.isImpassable(rc.senseTerrainTile(loc.add(Direction.NORTH_WEST)));

        return false;
    }

}
