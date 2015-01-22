package anatid19_adaptive;

import battlecode.common.*;

public class Bfs extends Bot {
    // Set up the queue
    private static MapLocation[] locQueue = null;
    private static int locQueueHead = 0;
    private static int locQueueTail = 0;
    private static boolean[][] wasQueued = null;

    private static Direction[] dirs = null;
    private static int[] dirsX = null;
    private static int[] dirsY = null;

    private static MapLocation previousDest = null;

    private static int minX;
    private static int maxX;
    private static int minY;
    private static int maxY;

    private static Direction[][] paths = null;

    // initialize the BFS algorithm
    public static void reinitQueue(MapLocation dest) {
        if (locQueue == null) {
            locQueue = new MapLocation[GameConstants.MAP_MAX_WIDTH * GameConstants.MAP_MAX_HEIGHT];
            wasQueued = new boolean[GameConstants.MAP_MAX_WIDTH][GameConstants.MAP_MAX_HEIGHT];
            dirs = new Direction[] { Direction.NORTH_WEST, Direction.SOUTH_WEST, Direction.SOUTH_EAST, Direction.NORTH_EAST, Direction.NORTH, Direction.WEST,
                    Direction.SOUTH, Direction.EAST };
            dirsX = new int[] { 1, 1, -1, -1, 0, 1, 0, -1 };
            dirsY = new int[] { 1, -1, -1, 1, 1, 0, -1, 0 };

            minX = (ourHQ.x + theirHQ.x) / 2 - GameConstants.MAP_MAX_WIDTH / 2;
            maxX = (ourHQ.x + theirHQ.x) / 2 + GameConstants.MAP_MAX_WIDTH / 2 - 1;
            minY = (ourHQ.y + theirHQ.y) / 2 - GameConstants.MAP_MAX_HEIGHT / 2;
            maxY = (ourHQ.y + theirHQ.y) / 2 + GameConstants.MAP_MAX_HEIGHT / 2 - 1;
        }

        // System.out.println("reiniting queue for dest " + dest.toString());

        locQueueHead = 0;
        locQueueTail = 0;

        wasQueued = new boolean[GameConstants.MAP_MAX_WIDTH][GameConstants.MAP_MAX_HEIGHT];
        paths = new Direction[GameConstants.MAP_MAX_WIDTH][GameConstants.MAP_MAX_HEIGHT];

        // Push dest onto queue
        locQueue[locQueueTail] = dest;
        locQueueTail++;
        wasQueued[dest.x - minX][dest.y - minY] = true;
    }

    public static void work(MapLocation dest) throws GameActionException {
        if (!dest.equals(previousDest)) {
            reinitQueue(dest);
        }

        previousDest = dest;

        int bytecodeLimit = rc.getSupplyLevel() > rc.getType().supplyUpkeep ? 9500 : 4500;

        // Debug.indicate("bfs", 0, "dest = " + dest.toString() + ", locQueueTail = " + locQueueTail);

        while (locQueueHead != locQueueTail && Clock.getBytecodeNum() < bytecodeLimit) {
            // pop a location from the queue
            MapLocation loc = locQueue[locQueueHead++];
            if (loc.equals(Bot.ourHQ) && !loc.equals(dest)) continue; // can't path through our HQ unless HQ is the destination

            int locX = loc.x;
            int locY = loc.y;
            for (int i = 8; i-- > 0;) {
                int x = locX + dirsX[i];
                int y = locY + dirsY[i];
                if (x >= minX && y >= minY && x <= maxX && y <= maxY && !wasQueued[x - minX][y - minY]) {
                    MapLocation newLoc = new MapLocation(x, y);
                    TerrainTile terrain = rc.senseTerrainTile(newLoc);
                    if (terrain != TerrainTile.VOID && terrain != TerrainTile.OFF_MAP) {
                        paths[x - minX][y - minY] = dirs[i];

                        // push newLoc onto queue
                        locQueue[locQueueTail++] = newLoc;
                        wasQueued[x - minX][y - minY] = true;
                    }
                }
            }
        }
    }

    public static Direction readResult(MapLocation start, MapLocation dest) {
        if (!dest.equals(previousDest)) return null;

        return paths[start.x - minX][start.y - minY];
    }
}
