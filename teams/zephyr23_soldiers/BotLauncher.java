package zephyr23_soldiers;

import battlecode.common.*;

public class BotLauncher extends Bot {
    public static void loop(RobotController theRC) throws GameActionException {
        Bot.init(theRC);
//        Debug.init("supply");
        while (true) {
            try {
                turn();
            } catch (Exception e) {
                e.printStackTrace();
            }
            rc.yield();
        }
    }

    // Here is how far a missile can go in five turns, plus splash
    // ..........
    // ..........
    // ssssss....
    // OOOOOss...
    // OOOOOOss..
    // OOOOOOOs..
    // OOOOOOOs..
    // OOOOOOOs..
    // OOOOOOOs..
    // XOOOOOOs..
    // The farthest distance it can travel is (6, 4), for a squared distance of 52
    // Also, the diagonal displacement of (5, 5) gives a squared distance of 50
    // The closest distance it cannot reach is 7 squares away orthogonally, a squared distance of 49
    // If we neglect the few abnormally far reachable squares, we can say that the missile can
    // travel to any location with a squared distance of <= 48
    //
    // The closest non-splashable square is at a squared distance of 64. There are a few splashable
    // squares that are farther than 64, but to a good approximation the missile can splash those
    // squares with a squared distance of <= 63.

    private static MapLocation[] enemyTowers;
    
    private static void turn() throws GameActionException {
        here = rc.getLocation();

        Supply.shareSupply();
        Supply.requestResupplyIfNecessary();

        MeasureMapSize.checkForMapEdges();

        enemyTowers = rc.senseEnemyTowerLocations();
        
        if (rc.getMissileCount() > 0) {
            boolean launchedMissile = tryLaunchMissile();
            if (launchedMissile) return;
        }


        if (rc.isCoreReady()) {
            if (kiteBack()) {
                return;
            } else {
                MapLocation rallyLoc = MessageBoard.RALLY_LOC.readMapLocation();
                if (here.distanceSquaredTo(rallyLoc) > 35) { // stop short because we can shoot the target from long range
                    RobotInfo[] nearbyEnemies = rc.senseNearbyRobots(35, them);
                    if (nearbyEnemies.length == 0) { // stop while we kill the enemy
                        NavSafetyPolicy safetyPolicy = new SafetyPolicyAvoidAllUnits(enemyTowers, nearbyEnemies);
                        NewNav.goTo(rallyLoc, safetyPolicy);
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
            if(inEnemyTowerOrHQRange(retreatLoc, enemyTowers)) continue;
            
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
        RobotInfo[] nearbyEnemies = rc.senseNearbyRobots(63, them);

        for (RobotInfo enemy : nearbyEnemies) {
            if (enemy.type != RobotType.MISSILE) {
                if (tryLaunchAt(enemy.location)) return true;
            }
        }

        for (MapLocation enemyTower : rc.senseEnemyTowerLocations()) {
            if (here.distanceSquaredTo(enemyTower) <= 49) {
                if (tryLaunchAt(enemyTower)) return true;
            }
        }

        if (here.distanceSquaredTo(theirHQ) <= 49) {
            if (tryLaunchAt(theirHQ)) return true;
        }

        return false;
    }

    static boolean tryLaunchAt(MapLocation enemyLoc) throws GameActionException {
        if (here.isAdjacentTo(enemyLoc)) return false;

        boolean clearPath = true;
        MapLocation loc = here;
        while (true) {
            loc = loc.add(loc.directionTo(enemyLoc));
            RobotInfo robotInWay = rc.canSenseLocation(loc) ? rc.senseRobotAtLocation(loc) : null;
            if (robotInWay != null && robotInWay.type != RobotType.MISSILE) {
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
                MissileGuidance.setMissileTarget(rc, here.add(dir), enemyLoc);
                return true;
            }
        }
        return false;
    }
}
