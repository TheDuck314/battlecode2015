package framework11_tankrush;

import battlecode.common.*;

public class Util extends Bot {
    public static int numAlliedBuildingsAdjacent(MapLocation loc) {
        int ret = 0;
        for (RobotInfo ally : rc.senseNearbyRobots(loc, 2, us)) {
            if (ally.type.isBuilding) ret++;
        }
        return ret;
    }
    
    public static int numAlliedBuildingsOrthogonallyAdjacent(MapLocation loc) {
        int ret = 0;
        for (RobotInfo ally : rc.senseNearbyRobots(loc, 1, us)) {
            if (ally.type.isBuilding) ret++;
        }
        return ret;
    }
    
    public static RobotInfo closest(RobotInfo[] robots, MapLocation toHere) {
        RobotInfo ret = null;
        int bestDistSq = 999999;
        for(int i = robots.length; i --> 0;) {
            int distSq = toHere.distanceSquaredTo(robots[i].location);
            if(distSq < bestDistSq) {
                bestDistSq = distSq;
                ret = robots[i];
            }
        }
        return ret;
    }
}
