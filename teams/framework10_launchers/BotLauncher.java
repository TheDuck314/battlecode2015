package framework10_launchers;

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

    // Here is how far a missile can go in six turns, plus splash
    // ..........
    // ssssss....
    // OOOOOsss..
    // OOOOOOOs..
    // OOOOOOOss.
    // OOOOOOOOs.
    // OOOOOOOOs.
    // OOOOOOOOs.
    // OOOOOOOOs.
    // XOOOOOOOs.
    // The farthest distance it can travel is diagonally, for a squared distance of 72
    // Also, the displacement of (7, 4) gives a squared distance of 65
    // The closest distance it cannot reach is 8 squares away orthogonally, a squared distance of 64
    // If we neglect the few abnormally far reachable squares, we can say that the missile can
    // travel to any location with a squared distance of <= 63
    //
    // The closest non-splashable square is at a squared distance of 81. There are a few splashable
    // squares that are farther than 81, but to a good approximation the missile can splash those
    // squares with a squared distance of <= 80.

    // Here is a reasonable strategy:
    // - If there is an enemy within range 80, fire a missile
    //
    // - If there is an enemy within range 15, kite back
    // Else if there is an enemy within range 80, stay put
    // Else proceed to rally point.

    private static void turn() throws GameActionException {
        here = rc.getLocation();

        Supply.shareSupply();
        Supply.requestResupplyIfNecessary();

        MeasureMapSize.checkForMapEdges();

        if (rc.getMissileCount() > 0) {
            if (tryLaunchMissile()) return;
        }

        if (rc.isCoreReady()) {
            if (kiteBack()) {
                return;
            } else {
                MapLocation rallyLoc = MessageBoard.RALLY_LOC.readMapLocation();
                if (here.distanceSquaredTo(rallyLoc) > 49) { // stop short because we can shoot the target from long range
                    RobotInfo[] shootableEnemies = rc.senseNearbyRobots(49, them);
                    if (shootableEnemies.length == 0) { // stop while we kill the enemy
                        Nav.goTo(rallyLoc);
                    }
                }
            }
        }
    }

    private static boolean kiteBack() throws GameActionException {
        RobotInfo[] tooCloseEnemies = rc.senseNearbyRobots(15, them);
        if (tooCloseEnemies.length == 0) return false;

        RobotInfo[] nearbyEnemies = rc.senseNearbyRobots(35, them);

        Direction bestRetreatDir = null;
        RobotInfo currentClosestEnemy = Util.closest(nearbyEnemies, here);
        int bestDistSq = here.distanceSquaredTo(currentClosestEnemy.location);
        for (Direction dir : Direction.values()) {
            if (!rc.canMove(dir)) continue;

            MapLocation retreatLoc = here.add(dir);
            RobotInfo closestEnemy = Util.closest(nearbyEnemies, retreatLoc);
            int distSq = retreatLoc.distanceSquaredTo(closestEnemy.location);
            if (distSq > bestDistSq) {
                bestDistSq = distSq;
                bestRetreatDir = dir;
            }
        }

        if (bestRetreatDir == null) return false;

        rc.move(bestRetreatDir);
        return true;
    }

    static boolean tryLaunchMissile() throws GameActionException {
        RobotInfo[] nearbyEnemies = rc.senseNearbyRobots(80, them);

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
