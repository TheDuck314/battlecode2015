package anatid13;

import battlecode.common.*;

public class DroneHarrass extends Bot {

    private static boolean canStay(MapLocation loc) {
        if (inEnemyTowerOrHQRange(loc, DroneNav.enemyTowers)) return false;

        RobotInfo[] potentialAttackers = rc.senseNearbyRobots(loc, 36/* 15 */, them);
        RobotInfo attacker = null;
        int numAttackers = 0;
        for (RobotInfo enemy : potentialAttackers) {
            if (enemy.type == RobotType.LAUNCHER) {
                int safeDist;
                if (enemy.missileCount > 0) safeDist = 4;
                else safeDist = 5 - Math.max(1, (int) enemy.weaponDelay);
                if (Math.abs(loc.x - enemy.location.x) < safeDist && Math.abs(loc.y - enemy.location.y) < safeDist) {
                    return false;
                }
            } else if (enemy.type == RobotType.MISSILE) {
                int distSq = loc.distanceSquaredTo(enemy.location);
                if (distSq <= 8) {
                    return false;
                }
            } else if (enemy.type.attackRadiusSquared >= loc.distanceSquaredTo(enemy.location)) {
                numAttackers++;
                if (numAttackers >= 2) return false;
                attacker = enemy;
            }
        }

        if (numAttackers == 0) return true;

        if (attacker.type.oreCost < RobotType.DRONE.oreCost) return false;

        if (attacker.health * RobotType.DRONE.attackDelay / RobotType.DRONE.attackPower + rc.getWeaponDelay() <= rc.getHealth() * attacker.type.attackDelay
                / attacker.type.attackPower + Math.max(0, attacker.weaponDelay - 1)) {
            return true;
        } else {
            return false;
        }
    }

    private static void retreat() throws GameActionException {
        RobotInfo[] tooCloseEnemies = rc.senseNearbyRobots(15, them);
        if (tooCloseEnemies.length == 0) return;

        RobotInfo[] nearbyEnemies = rc.senseNearbyRobots(35, them);

        Direction bestRetreatDir = null;
        RobotInfo currentClosestEnemy = Util.closest(nearbyEnemies, here);

        boolean mustMoveOrthogonally = false;
        if (rc.getCoreDelay() >= 0.6 && currentClosestEnemy.type == RobotType.MISSILE) mustMoveOrthogonally = true;

        int bestDistSq = here.distanceSquaredTo(currentClosestEnemy.location);
        for (Direction dir : Direction.values()) {
            if (!rc.canMove(dir)) continue;
            if (mustMoveOrthogonally && dir.isDiagonal()) continue;

            MapLocation retreatLoc = here.add(dir);
            if (inEnemyTowerOrHQRange(retreatLoc, DroneNav.enemyTowers)) continue;

            RobotInfo closestEnemy = Util.closest(nearbyEnemies, retreatLoc);
            int distSq = retreatLoc.distanceSquaredTo(closestEnemy.location);
            if (distSq > bestDistSq) {
                bestDistSq = distSq;
                bestRetreatDir = dir;
            }
        }

        if (bestRetreatDir != null) rc.move(bestRetreatDir);
    }

    private static boolean tryToAttack() throws GameActionException {
        RobotInfo[] targets = rc.senseNearbyRobots(RobotType.DRONE.attackRadiusSquared, them);
        for (RobotInfo target : targets) {
            if (target.type == RobotType.MISSILE && target.health < 1.5) {
                if (here.isAdjacentTo(target.location) || rc.senseNearbyRobots(target.location, 2, us).length > 0) {
                    continue;
                }
            }
            if (rc.isWeaponReady()) {
                rc.attackLocation(target.location);
                // rc.setIndicatorLine(here, target.location, 255, 255, 0);
            }
            // Debug.indicate("harrass", 2, "have target at " + target.location);
            return true;
        }
        return false;
    }

    private static boolean tryMoveTowardUndefendedHelplessEnemy() throws GameActionException {
        RobotInfo[] nearbyEnemies = rc.senseNearbyRobots(35, them);

        for (RobotInfo enemy : nearbyEnemies) {
            if (enemy.type.attackRadiusSquared < RobotType.DRONE.attackRadiusSquared && enemy.type != RobotType.MISSILE) {
                Direction dir = here.directionTo(enemy.location);
                if (rc.canMove(dir) && DroneNav.isSafe(here.add(dir))) {
                    // Debug.indicate("harrass", 1, "moving toward helpless enemy at " + enemy.location.toString());
                    rc.move(dir);
                    return true;
                }
            }
        }

        return false;
    }

    public static void doHarrass() throws GameActionException {
        Supply.shareSupply();
        Supply.requestResupplyIfNecessary();

        if (MessageBoard.FINAL_ATTACK_SIGNAL.readBoolean()) {
            tryToAttack();

            MapLocation rallyLoc = MessageBoard.RALLY_LOC.readMapLocation();

            DroneNav.fearEnemyUnits = false;
            DroneNav.fearEnemyHQ = rallyLoc.distanceSquaredTo(theirHQ) >= 25;
//            rc.setIndicatorString(0, "dist = " + rallyLoc.distanceSquaredTo(theirHQ) + "; fearEnemyHQ = " + DroneNav.fearEnemyHQ);

            DroneNav.enemyTowers = rc.senseEnemyTowerLocations();

            MapLocation[] enemyTowersToAvoidTemp = new MapLocation[DroneNav.enemyTowers.length];
            int n = 0;
            for (MapLocation enemyTower : DroneNav.enemyTowers) {
                if (enemyTower.distanceSquaredTo(rallyLoc) > 10) enemyTowersToAvoidTemp[n++] = enemyTower;
            }
            DroneNav.enemyTowersToAvoid = new MapLocation[n];
            for (int i = 0; i < n; i++) {
                DroneNav.enemyTowersToAvoid[i] = enemyTowersToAvoidTemp[i];
            }
            DroneNav.goTo(rallyLoc);

            return;
        }

        DroneNav.fearEnemyUnits = true;
        DroneNav.fearEnemyHQ = true;
        DroneNav.enemyTowers = DroneNav.enemyTowersToAvoid = rc.senseEnemyTowerLocations();

        if (rc.isCoreReady()) {
            if (!canStay(here)) {
                // Debug.indicate("harrass", 0, "retreating!");
                retreat();
            } else {
                // Debug.indicate("harrass", 0, "safe!");
            }
        }

        // Even if our weapon is on cooldown, we should stay put if we have a target
        boolean haveTarget = tryToAttack();

        if (!haveTarget && rc.isCoreReady()) {
            if (tryMoveTowardUndefendedHelplessEnemy()) return;

            DroneNav.goTo(theirHQ);
        }
    }

}
