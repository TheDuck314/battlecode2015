package framework6_launchers;

import battlecode.common.*;

public class Combat extends Bot {
    public static void shootAtNearbyEnemies() throws GameActionException {
        int attackRangeSq = rc.getType().attackRadiusSquared;
        RobotInfo[] nearbyEnemies = rc.senseNearbyRobots(attackRangeSq, them);
        if(nearbyEnemies.length > 0) {
            rc.attackLocation(nearbyEnemies[0].location);
        }
    }
}
