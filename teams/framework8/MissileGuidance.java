package framework8;

import battlecode.common.*;

public class MissileGuidance extends Bot {
    static final int baseChannel = 1000;

    public static void setMissileTarget(MapLocation start, MapLocation target) throws GameActionException {
        int dx = target.x - start.x;
        int dy = target.y - start.y;
        int data = 100*(100 + dx) + 50 + dy;
        rc.broadcast(Util.indexFromCoords(start.x, start.y), data);
    }

    public static MapLocation getMissileTarget(MapLocation start) throws GameActionException {
        int data = rc.readBroadcast(Util.indexFromCoords(start.x, start.y));
        int dx = (data / 100) - 100;
        int dy = data - 100*(100 + dx) - 50;
        return new MapLocation(start.x + dx, start.y + dy);
    }
}
