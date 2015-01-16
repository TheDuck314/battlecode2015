package anatid16_puredrone;

import battlecode.common.*;

class SafetyPolicyDroneHarrass extends Bot implements NavSafetyPolicy {
    RobotInfo[] nearbyEnemies;

    public SafetyPolicyDroneHarrass(RobotInfo[] nearbyEnemies) {
        this.nearbyEnemies = nearbyEnemies;
    }

    public boolean isSafeToMoveTo(MapLocation loc) {
        return DroneHarrass.isSafeToMoveTo(loc, nearbyEnemies);
    }
}

public class DroneHarrass extends Bot {

    // Here's how a reasonable range-5 drone micro can work:
    //
    // We want to take all the 1v1 fights we can win.
    //
    // We want to avoid all 1v2s

    private static MapLocation[] enemyTowers;

    private static boolean canWin1v1(RobotInfo enemy) {
        int attacksToKillEnemy = (int) (enemy.health / RobotType.DRONE.attackPower);
        int turnsToKillEnemy = (int) rc.getWeaponDelay() + RobotType.DRONE.attackDelay * (attacksToKillEnemy - 1);

        int attacksForEnemyToKillUs = (int) (rc.getHealth() / enemy.type.attackPower);
        int turnsForEnemyToKillUs = Math.max(0, (int) enemy.weaponDelay - 1) + enemy.type.attackDelay * (attacksForEnemyToKillUs - 1);

        return turnsToKillEnemy <= turnsForEnemyToKillUs;
    }

    public static boolean canWin1v1AfterMovingTo(MapLocation loc, RobotInfo enemy) {
        int attacksToKillEnemy = (int) (enemy.health / RobotType.DRONE.attackPower);

        double loadingDelay = rc.senseTerrainTile(loc) == TerrainTile.VOID ? GameConstants.DRONE_VOID_DELAY_MULTIPLIER * RobotType.DRONE.loadingDelay
                : RobotType.DRONE.loadingDelay;
        int weaponDelayAfterMoving = (int) Math.max(loadingDelay, rc.getWeaponDelay() - 1);
        int turnsToKillEnemy = 1 + weaponDelayAfterMoving + RobotType.DRONE.attackDelay * (attacksToKillEnemy - 1);

        int attacksForEnemyToKillUs = (int) (rc.getHealth() / enemy.type.attackPower);
        int turnsForEnemyToKillUs = Math.max(0, (int) enemy.weaponDelay - 1) + enemy.type.attackDelay * (attacksForEnemyToKillUs - 1);

        return turnsToKillEnemy <= turnsForEnemyToKillUs;
    }

    private static boolean canStay(MapLocation loc) {
        if (inEnemyTowerOrHQRange(loc, enemyTowers)) return false;

        RobotInfo[] potentialAttackers = rc.senseNearbyRobots(loc, 35, them);
        RobotInfo loneAttacker = null;
        int numAttackers = 0;
        for (RobotInfo enemy : potentialAttackers) {
            switch (enemy.type) {
                case LAUNCHER:
                    int safeDist;
                    if (enemy.missileCount > 0) safeDist = 4;
                    else safeDist = 5 - Math.max(1, (int) enemy.weaponDelay);
                    if (Math.abs(loc.x - enemy.location.x) < safeDist && Math.abs(loc.y - enemy.location.y) < safeDist) {
                        return false;
                    }
                    break;

                case MISSILE:
                    if (loc.distanceSquaredTo(enemy.location) <= 8) {
                        return false;
                    }
                    break;

                default:
                    if (enemy.type.attackRadiusSquared >= loc.distanceSquaredTo(enemy.location)) {
                        numAttackers++;
                        if (numAttackers >= 2) return false;
                        loneAttacker = enemy;
                    }
                    break;
            }
        }

        if (numAttackers == 0) return true;

        return canWin1v1(loneAttacker);
    }

    public static boolean isSafeToMoveTo(MapLocation loc, RobotInfo[] nearbyEnemies) {
        if (inEnemyTowerOrHQRange(loc, enemyTowers)) return false;

        RobotInfo loneAttacker = null;
        int numAttackers = 0;
        for (RobotInfo enemy : nearbyEnemies) {
            switch (enemy.type) {
                case LAUNCHER:
                    int safeDist;
                    if (enemy.missileCount > 0) safeDist = 4;
                    else safeDist = 5 - Math.max(1, (int) enemy.weaponDelay);
                    if (Math.abs(loc.x - enemy.location.x) < safeDist && Math.abs(loc.y - enemy.location.y) < safeDist) {
                        return false;
                    }
                    break;

                case MISSILE:
                    if (loc.distanceSquaredTo(enemy.location) <= 8) {
                        return false;
                    }
                    break;

                default:
                    if (enemy.type.attackRadiusSquared >= loc.distanceSquaredTo(enemy.location)) {
                        numAttackers++;
                        if (numAttackers >= 2) return false;
                        loneAttacker = enemy;
                    }
                    break;
            }
        }

        if (numAttackers == 0) return true;

        return DroneHarrass.canWin1v1AfterMovingTo(loc, loneAttacker);
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
            if (inEnemyTowerOrHQRange(retreatLoc, enemyTowers)) continue;

            RobotInfo closestEnemy = Util.closest(nearbyEnemies, retreatLoc);
            int distSq = retreatLoc.distanceSquaredTo(closestEnemy.location);
            if (distSq > bestDistSq) {
                bestDistSq = distSq;
                bestRetreatDir = dir;
            }
        }

        if (bestRetreatDir != null) rc.move(bestRetreatDir);
    }

    private static boolean tryMoveTowardUndefendedHelplessEnemy() throws GameActionException {
        RobotInfo[] nearbyEnemies = rc.senseNearbyRobots(35, them);

        for (RobotInfo enemy : nearbyEnemies) {
            if (enemy.type == RobotType.BEAVER || enemy.type == RobotType.MINER) {
                boolean canReach = true;
                MapLocation loc = here;
                while (loc.distanceSquaredTo(enemy.location) > RobotType.DRONE.attackRadiusSquared) {
                    Direction dir = loc.directionTo(enemy.location);
                    MapLocation newLoc = loc.add(dir);
                    if (rc.isPathable(RobotType.DRONE, newLoc) && isSafeToMoveTo(newLoc, nearbyEnemies)) {
                        loc = newLoc;
                    } else {
                        canReach = false;
                        break;
                    }
                }
                if (canReach) {
                    Direction moveDir = here.directionTo(enemy.location);
                    if (canWin1v1AfterMovingTo(here.add(moveDir), enemy)) {
                        rc.move(moveDir);
                        return true;
                    }
                }
            }
        }

        return false;
    }

    private static boolean tryToAttack() throws GameActionException {
        RobotInfo[] attackableEnemies = rc.senseNearbyRobots(RobotType.DRONE.attackRadiusSquared, them);
        if (attackableEnemies.length == 0) return false;

        if (rc.isWeaponReady()) {
            RobotInfo target = Util.leastHealth(attackableEnemies);
            rc.attackLocation(target.location);
        }
        return true;
    }

    public static void doHarrass() throws GameActionException {
        Supply.shareSupply();
        Supply.requestResupplyIfNecessary();

        enemyTowers = rc.senseEnemyTowerLocations();

        if (rc.isCoreReady()) {
            if (!canStay(here)) {
                retreat();
                return;
            }
        }

        if (tryToAttack()) {
            return;
        }

        if (rc.isCoreReady()) {
            if (tryMoveTowardUndefendedHelplessEnemy()) return;

            RobotInfo[] nearbyEnemies = rc.senseNearbyRobots(35, them);
            NavSafetyPolicy safetyPolicy = new SafetyPolicyDroneHarrass(nearbyEnemies);
            NewNav.goTo(theirHQ, safetyPolicy);
        }
    }
}
