package framework6_tanks;

import battlecode.common.*;

public class BotLauncher extends Bot {
    public static void loop(RobotController theRC) throws GameActionException {
        Bot.init(theRC);
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
        }

        if (rc.isCoreReady()) {
            MapLocation rallyLoc = MessageBoard.RALLY_LOC.readMapLocation();
            if (here.distanceSquaredTo(rallyLoc) > 36) {
                Nav.goTo(rallyLoc);
            }
        }

        Supply.shareSupply();
    }

    static void tryLaunchMissile() throws GameActionException {
        RobotInfo[] nearbyEnemies = rc.senseNearbyRobots(36, them);

        if (nearbyEnemies.length == 0) return;

        for (RobotInfo enemy : nearbyEnemies) {
            boolean clearPath = true;
            MapLocation enemyLoc = enemy.location;
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
                Direction dir = here.directionTo(enemyLoc);
                rc.launchMissile(dir);
                MissileGuidance.setMissileTarget(here.add(dir), enemyLoc);
                return;
            }
        }
    }
}
