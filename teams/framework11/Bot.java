package framework11;

import battlecode.common.*;

public class Bot {
    public static RobotController rc;
    protected static Team us;
    protected static Team them;
    protected static MapLocation ourHQ;
    protected static MapLocation theirHQ;

    protected static MapLocation here; // bot classes are responsible for keeping this up to date
    
    protected static void init(RobotController theRC) throws GameActionException {
        rc = theRC;
        
        us = rc.getTeam();
        them = us.opponent();
        
        ourHQ = rc.senseHQLocation();
        theirHQ = rc.senseEnemyHQLocation();
        
        here = rc.getLocation();
    }
    
    
    public static boolean inEnemyTowerOrHQRange(MapLocation loc, MapLocation[] enemyTowers) {
        if (loc.distanceSquaredTo(theirHQ) <= 52) {
            if (enemyTowers.length >= 5) {
                // enemy HQ has range of 35 and splash, so effective range 52
                return true;
            } else if (enemyTowers.length >= 2) {
                // enemy HQ has range of 35 and no splash
                if (loc.distanceSquaredTo(theirHQ) <= 35) return true;
            } else {
                // enemyHQ has range of 24;
                if (loc.distanceSquaredTo(theirHQ) <= 24) return true;
            }
        }

        for (MapLocation tower : enemyTowers) {
            if (loc.distanceSquaredTo(tower) <= RobotType.TOWER.attackRadiusSquared) return true;
        }
        
        return false;
    }
}
