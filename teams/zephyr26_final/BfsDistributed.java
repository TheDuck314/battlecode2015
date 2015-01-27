package zephyr26_final;

import battlecode.common.*;

public class BfsDistributed extends Bot {
    // Set up the queue
    private static Direction[] dirs = null;
    private static int[] dirsX = null;
    private static int[] dirsY = null;

    private static int minX;
    private static int maxX;
    private static int minY;
    private static int maxY;

    private static int mapMinX = Integer.MIN_VALUE;
    private static int mapMaxX = Integer.MAX_VALUE;
    private static int mapMinY = Integer.MIN_VALUE;
    private static int mapMaxY = Integer.MAX_VALUE;

    static final int CHANNEL_INIT_ROUND = 15996;
    static final int CHANNEL_DEST = 15997;
    static final int CHANNEL_LOC_QUEUE_HEAD = 15998;
    static final int CHANNEL_LOC_QUEUE_TAIL = 15999;
    static final int CHANNEL_LOC_QUEUE_BASE = 16000;
    static final int CHANNEL_WAS_QUEUED_BASE = 32000;
    static final int CHANNEL_PATHS = 48000;

    static int initRound;
    static int locQueueHead;
    static int locQueueTail;

    private static void init() {
        dirs = new Direction[] { Direction.NORTH_WEST, Direction.SOUTH_WEST, Direction.SOUTH_EAST, Direction.NORTH_EAST, Direction.NORTH, Direction.WEST,
                Direction.SOUTH, Direction.EAST };
        dirsX = new int[] { 1, 1, -1, -1, 0, 1, 0, -1 };
        dirsY = new int[] { 1, -1, -1, 1, 1, 0, -1, 0 };

        minX = (ourHQ.x + theirHQ.x) / 2 - GameConstants.MAP_MAX_WIDTH / 2;
        maxX = (ourHQ.x + theirHQ.x) / 2 + GameConstants.MAP_MAX_WIDTH / 2 - 1;
        minY = (ourHQ.y + theirHQ.y) / 2 - GameConstants.MAP_MAX_HEIGHT / 2;
        maxY = (ourHQ.y + theirHQ.y) / 2 + GameConstants.MAP_MAX_HEIGHT / 2 - 1;
    }

    private static int intFromMapLocation(MapLocation loc) {
        return GameConstants.MAP_MAX_WIDTH * (loc.y - minY) + (loc.x - minX);
    }

    private static MapLocation mapLocationFromInt(int i) {
        int x = minX + i % GameConstants.MAP_MAX_WIDTH;
        int y = minY + i / GameConstants.MAP_MAX_WIDTH;
        return new MapLocation(x, y);
    }

    private static void pushOnQueue(MapLocation loc) throws GameActionException {
        int locInt = intFromMapLocation(loc);
        rc.broadcast(CHANNEL_LOC_QUEUE_BASE + locQueueTail, locInt);
        rc.broadcast(CHANNEL_WAS_QUEUED_BASE + locInt, Clock.getRoundNum());
        locQueueTail++;
    }

    private static MapLocation popFromQueue() throws GameActionException {
        MapLocation ret = mapLocationFromInt(rc.readBroadcast(CHANNEL_LOC_QUEUE_BASE + locQueueHead));
        locQueueHead++;
        return ret;
    }

    private static boolean checkWasQueued(MapLocation loc) throws GameActionException {
        return rc.readBroadcast(CHANNEL_WAS_QUEUED_BASE + intFromMapLocation(loc)) > initRound;
    }

    private static void publishPath(MapLocation start, Direction dir) throws GameActionException {
        int data = 10 * Clock.getRoundNum() + dir.ordinal() + 1;
        rc.broadcast(CHANNEL_PATHS + intFromMapLocation(start), data);
    }

    public static Direction readResult(MapLocation start, MapLocation dest, int minInitRound) throws GameActionException {
        if (dirs == null) init();

        int data = rc.readBroadcast(CHANNEL_PATHS + intFromMapLocation(start));
        if (data == 0) return null;

        int publishedRound = data / 10;
        if (publishedRound <= rc.readBroadcast(CHANNEL_INIT_ROUND)) return null; // old search
        if (publishedRound < minInitRound) return null; // search too old for user

        return Direction.values()[(data % 10) - 1];
    }

    public static boolean isSearchDest(MapLocation loc) throws GameActionException {
        if (dirs == null) init();
        return rc.readBroadcast(CHANNEL_DEST) == intFromMapLocation(loc);
    }

    // initialize the BFS algorithm
    public static void reinitQueue(MapLocation dest) throws GameActionException {
        if (dirs == null) init();

        // System.out.println("reiniting queue for dest " + dest.toString());
        rc.broadcast(CHANNEL_INIT_ROUND, Clock.getRoundNum());
        rc.broadcast(CHANNEL_DEST, intFromMapLocation(dest));
        rc.broadcast(CHANNEL_LOC_QUEUE_HEAD, 0);
        rc.broadcast(CHANNEL_LOC_QUEUE_TAIL, 1);
        locQueueHead = 0;
        locQueueTail = 0;
        pushOnQueue(dest);
    }

    public static void work(MapLocation dest) throws GameActionException {
        if (dirs == null) init();

        if (!isSearchDest(dest)) {
            reinitQueue(dest);
            return; // can't work on same turn as reinit
        }

        locQueueHead = rc.readBroadcast(CHANNEL_LOC_QUEUE_HEAD);
        locQueueTail = rc.readBroadcast(CHANNEL_LOC_QUEUE_TAIL);
        if (locQueueHead == locQueueTail) return; // search is finished

        // You can't work on a destination the same round it is chosen. This lets us discard
        // any work that was done in the same round but before the new destination was chosen.
        initRound = rc.readBroadcast(CHANNEL_INIT_ROUND);
        if (initRound == Clock.getRoundNum()) return;

        mapMinX = MessageBoard.MAP_MIN_X.readInt();
        mapMaxX = MessageBoard.MAP_MAX_X.readInt();
        mapMinY = MessageBoard.MAP_MIN_Y.readInt();
        mapMaxY = MessageBoard.MAP_MAX_Y.readInt();
//        Debug.indicate("bfsdist", 0, "symmetry = " + MeasureMapSize.mapSymmetry.toString() + "; " + mapMinX + " <= x <= " + mapMaxX + "; " + mapMinY
//                + " <= y <= " + mapMaxY);

        int bytecodeBuffer = 1500;

        while (locQueueHead != locQueueTail && Clock.getBytecodesLeft() > bytecodeBuffer) {
            // pop a location from the queue
            MapLocation loc = popFromQueue();
            if (loc.equals(Bot.ourHQ) && !loc.equals(dest)) continue; // can't path through our HQ unless HQ is the destination

            int locX = loc.x;
            int locY = loc.y;
            for (int i = 8; i-- > 0;) {
                int x = locX + dirsX[i];
                int y = locY + dirsY[i];
                if (x >= minX && y >= minY && x <= maxX && y <= maxY) {
                    if (x >= mapMinX && y >= mapMinY && x <= mapMaxX && y <= mapMaxY) {
                        MapLocation newLoc = new MapLocation(x, y);
                        if (!checkWasQueued(newLoc)) {
                            TerrainTile terrain = rc.senseTerrainTile(newLoc);
                            if (terrain != TerrainTile.VOID && terrain != TerrainTile.OFF_MAP) {
                                publishPath(newLoc, dirs[i]);
                                pushOnQueue(newLoc);
//                                rc.setIndicatorLine(newLoc, loc, 0, 255, 0);
                            }
                        }
                    }
                }
            }
        }

        rc.broadcast(CHANNEL_LOC_QUEUE_HEAD, locQueueHead);
        rc.broadcast(CHANNEL_LOC_QUEUE_TAIL, locQueueTail);
    }
}
