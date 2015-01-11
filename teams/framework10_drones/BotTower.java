package framework10_drones;

import battlecode.common.*;

public class BotTower extends Bot {
    public static void loop(RobotController theRC) throws GameActionException {
        Bot.init(theRC);
        while (true) {
            try {
                turn();
            } catch(Exception e) {
                e.printStackTrace();
            }
            rc.yield();
        }
    }
    
    private static void turn() throws GameActionException
    {
        if (rc.isWeaponReady()) attackEnemies();       

        Supply.shareSupply();
    }
    
    private static void attackEnemies() throws GameActionException {
        RobotInfo[] nearbyEnemies = rc.senseNearbyRobots(RobotType.TOWER.attackRadiusSquared, them);

        if (nearbyEnemies.length == 0) return;

        // TODO: prioritize targets by health and unit type
        rc.attackLocation(nearbyEnemies[0].location);
    }
}
