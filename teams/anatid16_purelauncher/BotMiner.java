package anatid16_purelauncher;

import battlecode.common.*;

public class BotMiner extends Bot {
    public static void loop(RobotController theRC) throws GameActionException {
        Bot.init(theRC);
        Debug.init("supply");
        while (true) {
            try {
                turn();
            } catch (Exception e) {
                e.printStackTrace();
            }
            rc.yield();
        }
    }

    private static void turn() throws GameActionException {
        here = rc.getLocation();

        MeasureMapSize.checkForMapEdges();
        Supply.shareSupply();
        Supply.requestResupplyIfNecessary();

        if (rc.isWeaponReady()) Combat.shootAtNearbyEnemies();

        if (rc.isCoreReady()) doMining();

        if (Clock.getBytecodesLeft() > 3000) {
            // Do pathing with spare bytecodes
            MapLocation rallyLoc = MessageBoard.RALLY_LOC.readMapLocation();
            BfsDistributed.work(rallyLoc);
        }
    }

    static MapLocation mineLoc = null;

    static final double ORE_EXHAUSTED = 3.0;

    static MapLocation[] enemyTowers;

    static int mapMinX;
    static int mapMaxX;
    static int mapMinY;
    static int mapMaxY;

    public static void doMining() throws GameActionException {
        enemyTowers = rc.senseEnemyTowerLocations();
        mapMinX = MessageBoard.MAP_MIN_X.readInt();
        mapMaxX = MessageBoard.MAP_MAX_X.readInt();
        mapMinY = MessageBoard.MAP_MIN_Y.readInt();
        mapMaxY = MessageBoard.MAP_MAX_Y.readInt();

        if (!isSafeToMine(here)) {
            runAway();
            mineLoc = null;
            return;
        }

        if (mineLoc != null) {
            if (here.equals(mineLoc)) {
                // We are at the spot we want to mine. Decide whether to mine
                // or whether to move on because the spot is exhausted or we are
                // blocking the way
                if (rc.senseOre(here) >= ORE_EXHAUSTED) {
                    if (weAreBlockingTheWay()) {
                        if (tryMoveToUnblockTheWay()) {
                            Debug.indicate("mine", 0, "unblocking the way");
                            return;
                        } else {
                            Debug.indicate("mine", 0, "blocking but no better adjacent ore; mining; ore left = " + rc.senseOre(here));
                        }
                    } else {
                        Debug.indicate("mine", 0, "not blocking the way, mining; ore left = " + rc.senseOre(here));
                    }
                    rc.mine();
                    return;
                } else {
                    // ore here has been exhausted. choose a new mineLoc
                    mineLoc = null;
                    Debug.indicate("mine", 0, "ore here exhausted, going to look for a new mine loc");
                }
            } else {
                // we are not at our preferred mineLoc
                if (rc.senseOre(here) >= ORE_EXHAUSTED) {
                    // we happened upon a fine spot to mine that was different from our mineLoc
                    mineLoc = here;
                    Debug.indicate("mine", 2, "found opportunistic mineLoc here at " + mineLoc.toString() + "; mining; ore left = " + rc.senseOre(here));
                    rc.mine();
                    return;
                } else if (!isValidMineLoc(mineLoc)) {
                    // somehow our mineLoc is no longer suitable :( choose a new one
                    Debug.indicate("mine", 2, mineLoc.toString() + " is no longer valid :(");
                    mineLoc = null;
                }
            }
        }

        if (mineLoc == null) {
            mineLoc = chooseNewMineLoc();
            Debug.indicate("mine", 2, "chose new mineLoc: " + mineLoc.toString());
            return; // choosing new mine loc can take several turns
        }

        Debug.indicate("mine", 1, "going to " + mineLoc.toString());
        RobotInfo[] nearbyEnemies = rc.senseNearbyRobots(35, them);
        NavSafetyPolicy safetyPolicy = new SafetyPolicyAvoidAllUnits(enemyTowers, nearbyEnemies);
        NewNav.goTo(mineLoc, safetyPolicy);
    }

    private static boolean weAreBlockingTheWay() {
        int numAdjacentNonBuildingAllies = 0;
        RobotInfo[] adjacentAllies = rc.senseNearbyRobots(2, us);
        for(RobotInfo ally : adjacentAllies) {
            if(!ally.type.isBuilding) numAdjacentNonBuildingAllies++;
        }
        return numAdjacentNonBuildingAllies >= 3;
    }

    private static boolean tryMoveToUnblockTheWay() throws GameActionException {
        double oreHere = rc.senseOre(here);

        Direction bestDir = null;
        int fewestNeighbors = 999999;
        for (Direction dir : Direction.values()) {
            if (rc.canMove(dir)) {
                MapLocation loc = here.add(dir);
                if (rc.senseOre(loc) > oreHere && isSafeToMine(loc)) {
                    int numNeighbors = rc.senseNearbyRobots(loc, 2, us).length;
                    if (numNeighbors < fewestNeighbors) {
                        fewestNeighbors = numNeighbors;
                        bestDir = dir;
                    }
                }
            }
        }

        if (bestDir != null) {
            rc.move(bestDir);
            mineLoc = here.add(bestDir);
            return true;
        }
        return false;
    }

    private static int[] legDX = { 0, 0, 0, -1, 0, 0, 0, 1 };
    private static int[] legDY = { 0, 1, 0, 0, 0, -1, 0, 0 };

    private static MapLocation chooseNewMineLoc() throws GameActionException {
        MapLocation searchCenter = here;

        Direction startDiag = here.directionTo(ourHQ); 
        if (!startDiag.isDiagonal()) startDiag = startDiag.rotateLeft();

        for (int radius = 1;; radius++) {
            MapLocation bestLoc = null;
            int bestDistSq = 999999;
            
            MapLocation loc = searchCenter.add(startDiag, radius);
            int diag = startDiag.ordinal();
            for (int leg = 0; leg < 4; leg++) {
                int dx = legDX[diag];
                int dy = legDY[diag];

                for (int i = 0; i < 2 * radius; i++) {
                    if (isValidMineLoc(loc)) {
                        int distSq = ourHQ.distanceSquaredTo(loc);
                        if(distSq < bestDistSq) {
                            bestDistSq = distSq;
                            bestLoc = loc;
                        }
                    }

                    loc = loc.add(dx, dy);
                }

                diag = (diag + 2) % 8;
            }
            
            if(bestLoc != null) {
                return bestLoc;
            }
        }
    }

    private static boolean isValidMineLoc(MapLocation loc) throws GameActionException {
        if (rc.senseOre(loc) <= ORE_EXHAUSTED) {
            if (rc.senseTerrainTile(loc) == TerrainTile.UNKNOWN) {
                // an unknown tile is fine because it might have ore, unless it is definitely off the map
                return loc.x >= mapMinX && loc.x <= mapMaxX && loc.y >= mapMinY && loc.y <= mapMaxY;
            } else {
                return false;
            }
        }

        if (locIsOccupied(loc)) return false;

        if (!isSafeToMine(loc)) return false;

        return true;
    }

    private static boolean isSafeToMine(MapLocation loc) {
        if (inEnemyTowerOrHQRange(loc, enemyTowers)) return false;

        RobotInfo[] potentialAttackers = rc.senseNearbyRobots(loc, 24, them);
        int numEnemyWorkers = 0;
        for (RobotInfo enemy : potentialAttackers) {
            switch (enemy.type) {
                case DRONE:
                case TANK:
                case LAUNCHER:
                case SOLDIER:
                case BASHER:
                case COMMANDER:
                    return false;

                case BEAVER:
                case MINER:
                    numEnemyWorkers++;
                    break;

                default:
                    break;
            }
        }

        return numEnemyWorkers <= 3;
    }

    private static void runAway() throws GameActionException {
        RobotInfo[] visibleEnemies = rc.senseNearbyRobots(here, 24, them);
        RobotInfo nearestEnemy = null;
        int smallestDistSq = 999999;
        for (RobotInfo enemy : visibleEnemies) {
            if (enemy.type.attackPower > 0 || enemy.type == RobotType.LAUNCHER) {
                int distSq = here.distanceSquaredTo(enemy.location);
                if (distSq < smallestDistSq) {
                    smallestDistSq = distSq;
                    nearestEnemy = enemy;
                }
            }
        }
        if (nearestEnemy == null) return;

        Direction away = nearestEnemy.location.directionTo(here);
        Direction[] dirs = new Direction[] { away, away.rotateLeft(), away.rotateRight(), away.rotateLeft().rotateLeft(), away.rotateRight().rotateRight() };
        Direction flightDir = null;
        for (Direction dir : dirs) {
            if (rc.canMove(dir)) {
                if (isSafeToMine(here.add(dir))) {
                    rc.move(dir);
                    return;
                } else if (flightDir == null) {
                    flightDir = dir;
                }
            }
        }
        if (flightDir != null) {
            rc.move(flightDir);
        }
    }

    private static boolean locIsOffMap(MapLocation loc) throws GameActionException {
        if (loc.x < mapMinX) return true;
        if (loc.x > mapMaxX) return true;
        if (loc.y < mapMinX) return true;
        if (loc.y > mapMaxX) return true;
        return false;
    }
    
    private static boolean locIsOccupied(MapLocation loc) throws GameActionException {
        return rc.senseNearbyRobots(loc, 0, null).length > 0;
    }
}
