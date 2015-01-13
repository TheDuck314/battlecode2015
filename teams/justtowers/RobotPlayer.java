package justtowers;

import battlecode.common.*;

public class RobotPlayer {
    public static void run(RobotController rc) throws GameActionException {
        while (true) {
            
            if(rc.isWeaponReady()) {
                int attackRadiusSq = rc.getType().attackRadiusSquared;
                if(rc.getType() == RobotType.HQ && rc.senseTowerLocations().length >= 2) {
                    attackRadiusSq = GameConstants.HQ_BUFFED_ATTACK_RADIUS_SQUARED;
                }
                
                RobotInfo[] enemies = rc.senseNearbyRobots(attackRadiusSq, rc.getTeam().opponent());

                RobotInfo target = null;
                double minHealth = 999999;
                for(RobotInfo enemy : enemies) {
                    if(enemy.health < minHealth) {
                        minHealth = enemy.health;
                        target = enemy;
                    }
                }
                
                if(target != null) {
                    rc.attackLocation(target.location);
                }
            }
            
            rc.yield();
        }
    }
}
