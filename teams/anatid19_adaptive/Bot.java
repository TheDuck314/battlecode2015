package anatid19_adaptive;

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
            switch (enemyTowers.length) {
                case 6:
                case 5:
                    // enemy HQ has range of 35 and splash
                    if (loc.add(loc.directionTo(theirHQ)).distanceSquaredTo(theirHQ) <= 35) return true;
                    break;

                case 4:
                case 3:
                case 2:
                    // enemy HQ has range of 35 and no splash
                    if (loc.distanceSquaredTo(theirHQ) <= 35) return true;
                    break;

                case 1:
                case 0:
                default:
                    // enemyHQ has range of 24;
                    if (loc.distanceSquaredTo(theirHQ) <= 24) return true;
                    break;
            }
        }

        for (MapLocation tower : enemyTowers) {
            if (loc.distanceSquaredTo(tower) <= RobotType.TOWER.attackRadiusSquared) return true;
        }

        return false;
    }
}
