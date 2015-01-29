package bloomberg;

import battlecode.common.*;

public class BotExplorer extends Bot {
    public static void loop(RobotController theRC) throws GameActionException {
        Bot.init(theRC);

        while (true) {
            try {
                turn();
            } catch (Exception e) {
                e.printStackTrace();
            }
            rc.yield();
        }
    }

    private static int minX;
    private static int maxX;
    private static int minY;
    private static int maxY;

    private static int mapMinX = Integer.MIN_VALUE;
    private static int mapMaxX = Integer.MAX_VALUE;
    private static int mapMinY = Integer.MIN_VALUE;
    private static int mapMaxY = Integer.MAX_VALUE;

    static MapLocation exploreDest = null;

    private static void turn() throws GameActionException {
        here = rc.getLocation();

        MeasureMapSize.checkForMapEdges();

        Supply.shareSupply();

        if (rc.isCoreReady()) {
            MapLocation rallyLoc = MessageBoard.RALLY_LOC.readMapLocation();
            RobotInfo[] nearbyEnemies = rc.senseNearbyRobots(35, them);
            MapLocation[] enemyTowers = rc.senseEnemyTowerLocations();
            NavSafetyPolicy safetyPolicy = new SafetyPolicyAvoidAllUnits(enemyTowers, nearbyEnemies);
            Nav.goTo(rallyLoc, safetyPolicy);
        }

        // if (exploreDest != null) {
        // if (here.equals(exploreDest) || rc.senseTerrainTile(exploreDest) == TerrainTile.VOID || rc.senseTerrainTile(exploreDest) == TerrainTile.OFF_MAP) {
        // exploreDest = null;
        // }
        // }
        //
        // while (exploreDest == null) {
        // exploreDest = findNearestUnknownTile();
        // if (exploreDest == null) {
        // if (rc.senseNearbyRobots(1, us).length > 0) {
        // rc.disintegrate();
        // }
        // }
        // }
        //
        // rc.setIndicatorDot(exploreDest, 255, 0, 255);
        //
        // Direction moveDir = getMoveDir();
        // if (moveDir != null) {
        // rc.move(moveDir);
        // } else {
        // bfsPathTo(exploreDest);
        // }
    }

    private static int intFromMapLocation(MapLocation loc) {
        return GameConstants.MAP_MAX_WIDTH * (loc.y - minY) + (loc.x - minX);
    }

    private static MapLocation mapLocationFromInt(int i) {
        int x = minX + i % GameConstants.MAP_MAX_WIDTH;
        int y = minY + i / GameConstants.MAP_MAX_WIDTH;
        return new MapLocation(x, y);
    }

    static Direction[] dirs = new Direction[] { Direction.NORTH_WEST, Direction.SOUTH_WEST, Direction.SOUTH_EAST, Direction.NORTH_EAST, Direction.NORTH,
            Direction.WEST, Direction.SOUTH, Direction.EAST };
    static int[] dirsX = new int[] { 1, 1, -1, -1, 0, 1, 0, -1 };
    static int[] dirsY = new int[] { 1, -1, -1, 1, 1, 0, -1, 0 };

    static MapLocation[] locQueue;
    static boolean[] wasQueued;

    private static MapLocation findNearestUnknownTile() throws GameActionException {
        minX = (ourHQ.x + theirHQ.x) / 2 - GameConstants.MAP_MAX_WIDTH / 2;
        maxX = (ourHQ.x + theirHQ.x) / 2 + GameConstants.MAP_MAX_WIDTH / 2 - 1;
        minY = (ourHQ.y + theirHQ.y) / 2 - GameConstants.MAP_MAX_HEIGHT / 2;
        maxY = (ourHQ.y + theirHQ.y) / 2 + GameConstants.MAP_MAX_HEIGHT / 2 - 1;
        mapMinX = MessageBoard.MAP_MIN_X.readInt();
        mapMaxX = MessageBoard.MAP_MAX_X.readInt();
        mapMinY = MessageBoard.MAP_MIN_Y.readInt();
        mapMaxY = MessageBoard.MAP_MAX_Y.readInt();

        locQueue = new MapLocation[GameConstants.MAP_MAX_WIDTH * GameConstants.MAP_MAX_HEIGHT];
        wasQueued = new boolean[GameConstants.MAP_MAX_WIDTH * GameConstants.MAP_MAX_HEIGHT];

        path = new Direction[GameConstants.MAP_MAX_WIDTH * GameConstants.MAP_MAX_HEIGHT];

        int locQueueHead = 0;
        int locQueueTail = 0;
        locQueue[locQueueTail++] = here;
        wasQueued[intFromMapLocation(here)] = true;

        MapLocation[] enemyTowers = rc.senseEnemyTowerLocations();

        while (locQueueHead != locQueueTail) {
            MapLocation loc = locQueue[locQueueHead++];
            int locX = loc.x;
            int locY = loc.y;
            for (int i = 8; i-- > 0;) {
                int x = locX + dirsX[i];
                int y = locY + dirsY[i];
                if (x >= minX && y >= minY && x <= maxX && y <= maxY) {
                    if (x >= mapMinX && y >= mapMinY && x <= mapMaxX && y <= mapMaxY) {
                        MapLocation newLoc = new MapLocation(x, y);
                        int newLocInt = intFromMapLocation(newLoc);
                        if (!wasQueued[newLocInt]) {
                            TerrainTile terrain = rc.senseTerrainTile(newLoc);
                            if (!inEnemyTowerOrHQRange(newLoc, enemyTowers)) {
                                if (terrain == TerrainTile.UNKNOWN) {
                                    return newLoc;
                                }
                                if (terrain == TerrainTile.NORMAL) {
                                    locQueue[locQueueTail++] = newLoc;
                                    wasQueued[newLocInt] = true;
                                    rc.setIndicatorLine(newLoc, loc, 255, 0, 0);
                                }
                            }
                        }
                    }
                }
            }
        }

        return null;
    }

    static Direction[] path;

    private static void bfsPathTo(MapLocation dest) {
        locQueue = new MapLocation[GameConstants.MAP_MAX_WIDTH * GameConstants.MAP_MAX_HEIGHT];
        wasQueued = new boolean[GameConstants.MAP_MAX_WIDTH * GameConstants.MAP_MAX_HEIGHT];
        path = new Direction[GameConstants.MAP_MAX_WIDTH * GameConstants.MAP_MAX_HEIGHT];

        int locQueueHead = 0;
        int locQueueTail = 0;
        locQueue[locQueueTail++] = dest;
        wasQueued[intFromMapLocation(dest)] = true;

        MapLocation[] enemyTowers = rc.senseEnemyTowerLocations();

        while (locQueueHead != locQueueTail) {
            MapLocation loc = locQueue[locQueueHead++];
            int locX = loc.x;
            int locY = loc.y;
            for (int i = 8; i-- > 0;) {
                int x = locX + dirsX[i];
                int y = locY + dirsY[i];
                if (x >= minX && y >= minY && x <= maxX && y <= maxY) {
                    if (x >= mapMinX && y >= mapMinY && x <= mapMaxX && y <= mapMaxY) {
                        MapLocation newLoc = new MapLocation(x, y);
                        int newLocInt = intFromMapLocation(newLoc);
                        if (!wasQueued[newLocInt]) {
                            TerrainTile terrain = rc.senseTerrainTile(newLoc);
                            if (terrain == TerrainTile.NORMAL) {
                                if (!inEnemyTowerOrHQRange(newLoc, enemyTowers)) {
                                    path[newLocInt] = dirs[i];
                                    if (newLoc.equals(here)) return;
                                    locQueue[locQueueTail++] = newLoc;
                                    wasQueued[newLocInt] = true;
                                    rc.setIndicatorLine(newLoc, loc, 0, 0, 255);
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private static Direction getMoveDir() {
        Direction dir = path[intFromMapLocation(here)];
        if (dir == null) return null;
        if (rc.canMove(dir)) return dir;
        if (rc.canMove(dir.rotateLeft())) return dir.rotateLeft();
        if (rc.canMove(dir.rotateRight())) return dir.rotateRight();
        return null;
    }
}
