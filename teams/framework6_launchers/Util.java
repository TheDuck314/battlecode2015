package framework6_launchers;

import battlecode.common.*;

public class Util extends Bot {
    public static int indexFromCoords(int x, int y) {
        x %= GameConstants.MAP_MAX_WIDTH;
        if (x < 0) x += GameConstants.MAP_MAX_WIDTH;
        y %= GameConstants.MAP_MAX_HEIGHT;
        if (y < 0) y += GameConstants.MAP_MAX_HEIGHT;
        return y * GameConstants.MAP_MAX_WIDTH + x;
    }

    public static int numAlliedBuildingsAdjacent(MapLocation loc) {
        int ret = 0;
        for (RobotInfo ally : rc.senseNearbyRobots(loc, 2, us)) {
            if (ally.type.isBuilding) ret++;
        }
        return ret;
    }
}
