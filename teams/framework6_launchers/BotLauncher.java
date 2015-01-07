package framework6_launchers;

import battlecode.common.*;

public class BotLauncher extends Bot {
    public static void loop(RobotController theRC) throws GameActionException {
        Bot.init(theRC);
        Debug.init("launch");
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

        if (rc.getMissileCount() > 0) {
            tryLaunchMissile();
        } else {
            Debug.indicate("launch", 0, "no missiles!");
        }

        if (rc.isCoreReady()) {
            if (rc.senseNearbyRobots(36, them).length == 0) {
                MapLocation rallyLoc = MessageBoard.RALLY_LOC.readMapLocation();
                if (here.distanceSquaredTo(rallyLoc) > 36) {
                    Nav.goTo(rallyLoc);
                }
            }
        }

        Supply.shareSupply();
        Supply.requestResupplyIfNecessary();
    }

    static void tryLaunchMissile() throws GameActionException {
        RobotInfo[] nearbyEnemies = rc.senseNearbyRobots(36, them);

        for (RobotInfo enemy : nearbyEnemies) {
            if (tryLaunchAt(enemy.location)) return;
        }

        for (MapLocation enemyTower : rc.senseEnemyTowerLocations()) {
            if (here.distanceSquaredTo(enemyTower) <= 36) {
                if (tryLaunchAt(enemyTower)) return;
            }
        }

        if (here.distanceSquaredTo(theirHQ) <= 36) {
            if (tryLaunchAt(theirHQ)) return;
        }

        Debug.indicate("launch", 0, "no clear path to any enemy");
    }

    static boolean tryLaunchAt(MapLocation enemyLoc) throws GameActionException {
        boolean clearPath = true;
        MapLocation loc = here;
        while (true) {
            loc = loc.add(loc.directionTo(enemyLoc));
            if (rc.senseNearbyRobots(loc, 0, null).length > 0) {
                clearPath = false;
                break;
            }
            if (loc.isAdjacentTo(enemyLoc)) {
                clearPath = rc.senseNearbyRobots(loc, 2, us).length == 0;
                break;
            }
        }

        if (clearPath) {
            Debug.indicate("launch", 0, "launching at enemy at " + enemyLoc.toString());
            Direction dir = here.directionTo(enemyLoc);
            rc.launchMissile(dir);
            MissileGuidance.setMissileTarget(here.add(dir), enemyLoc);
            return true;
        } else {
            return false;
        }
    }
}
