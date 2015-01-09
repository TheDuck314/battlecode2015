package framework8;

import battlecode.common.*;

public class BotLauncher extends Bot {
    public static void loop(RobotController theRC) throws GameActionException {
        Bot.init(theRC);
        Debug.init("bfsdist");
        while (true) {
            try {
                turn();
            } catch (Exception e) {
                e.printStackTrace();
            }
            rc.yield();
        }
    }

    private static void turn() throws GameActionException {
        here = rc.getLocation();

        Supply.shareSupply();
        Supply.requestResupplyIfNecessary();

        MeasureMapSize.checkForMapEdges();
        
        if (rc.getMissileCount() > 0) {
            if (tryLaunchMissile()) return;
        }

        
        if (rc.senseNearbyRobots(36, them).length == 0) {
            MapLocation rallyLoc = MessageBoard.RALLY_LOC.readMapLocation();
            Debug.indicate("bfsdist", 2, "rallyLoc = " + rallyLoc.toString());
            if (rc.isCoreReady()) {
                if (here.distanceSquaredTo(rallyLoc) > 0) {
                    Nav.goTo(rallyLoc);
                }
            }
            //Direction bfsDir = BfsDistributed.readResult(here, rallyLoc);
            //Debug.indicate("bfsdist", 0, "dir = " + (bfsDir == null ? "null" : bfsDir.toString()));
        }
    }

    static boolean tryLaunchMissile() throws GameActionException {
        RobotInfo[] nearbyEnemies = rc.senseNearbyRobots(36, them);

        for (RobotInfo enemy : nearbyEnemies) {
            if (tryLaunchAt(enemy.location)) return true;
        }

        for (MapLocation enemyTower : rc.senseEnemyTowerLocations()) {
            if (here.distanceSquaredTo(enemyTower) <= 36) {
                if (tryLaunchAt(enemyTower)) return true;
            }
        }

        if (here.distanceSquaredTo(theirHQ) <= 36) {
            if (tryLaunchAt(theirHQ)) return true;
        }

        return false;
    }

    static boolean tryLaunchAt(MapLocation enemyLoc) throws GameActionException {
        boolean clearPath = true;
        MapLocation loc = here;
        while (true) {
            loc = loc.add(loc.directionTo(enemyLoc));
            RobotInfo[] robotInWay = rc.senseNearbyRobots(loc, 0, null);
            if (robotInWay.length > 0 && robotInWay[0].type != RobotType.MISSILE) {
                clearPath = false;
                break;
            }
            if (loc.isAdjacentTo(enemyLoc)) {
                RobotInfo[] alliesInFriendlyFire = rc.senseNearbyRobots(loc, 2, us);
                for (RobotInfo ally : alliesInFriendlyFire) {
                    if (ally.type != RobotType.MISSILE) {
                        clearPath = false;
                        break;
                    }
                }
                break;
            }
        }

        if (clearPath) {
            Direction dir = here.directionTo(enemyLoc);
            if (rc.canLaunch(dir)) {
                rc.launchMissile(dir);
                MissileGuidance.setMissileTarget(here.add(dir), enemyLoc);
                return true;
            }
        }
        return false;
    }
}
