package anatid16_soldierharrass;

import battlecode.common.*;

class SafetyPolicySoldierHarrass extends Bot implements NavSafetyPolicy {
    RobotInfo[] nearbyEnemies;

    public SafetyPolicySoldierHarrass(RobotInfo[] nearbyEnemies) {
        this.nearbyEnemies = nearbyEnemies;
    }

    public boolean isSafeToMoveTo(MapLocation loc) {
        return SoldierHarrass.isSafeToMoveTo(loc, nearbyEnemies);
    }
}

public class SoldierHarrass extends Bot {

    private static MapLocation[] enemyTowers;

    private static boolean canWin1v1(RobotInfo enemy) {
        int numAttacksAfterFirstToKillEnemy = (int) ((enemy.health - 0.001) / RobotType.SOLDIER.attackPower);
        int turnsTillWeCanAttack;
        int effectiveAttackDelay;
        if (rc.getSupplyLevel() > 0) {
            turnsTillWeCanAttack = (int) rc.getWeaponDelay();
            effectiveAttackDelay = RobotType.SOLDIER.attackDelay;
        } else {
            turnsTillWeCanAttack = (int) Math.max(0, (rc.getWeaponDelay() - 0.5) / 0.5);
            effectiveAttackDelay = 2 * RobotType.SOLDIER.attackDelay;
        }
        int turnsToKillEnemy = turnsTillWeCanAttack + effectiveAttackDelay * numAttacksAfterFirstToKillEnemy;

        int numAttacksAfterFirstForEnemyToKillUs = (int) ((rc.getHealth() - 0.001) / enemy.type.attackPower);
        int turnsTillEnemyCanAttack;
        int effectiveEnemyAttackDelay;
        if (enemy.supplyLevel > 0) {
            turnsTillEnemyCanAttack = (int) Math.max(0, enemy.weaponDelay - 1);
            effectiveEnemyAttackDelay = enemy.type.attackDelay;
        } else {
            turnsTillEnemyCanAttack = (int) Math.max(0, (enemy.weaponDelay - 1.0) / 0.5);
            effectiveEnemyAttackDelay = 2 * enemy.type.attackDelay;
        }
        int turnsForEnemyToKillUs = turnsTillEnemyCanAttack + effectiveEnemyAttackDelay * numAttacksAfterFirstForEnemyToKillUs;

        return turnsToKillEnemy <= turnsForEnemyToKillUs;
    }

    public static boolean canWin1v1AfterMovingTo(MapLocation loc, RobotInfo enemy) {
        double loadingDelay = RobotType.SOLDIER.loadingDelay;

        int numAttacksAfterFirstToKillEnemy = (int) ((enemy.health - 0.001) / RobotType.SOLDIER.attackPower);
        int turnsTillWeCanAttack;
        int effectiveAttackDelay;
        if (rc.getSupplyLevel() > 0) {
            double weaponDelayAfterMoving = Math.max(loadingDelay, rc.getWeaponDelay()) - 1.0;
            turnsTillWeCanAttack = 1 + (int) weaponDelayAfterMoving;
            effectiveAttackDelay = RobotType.SOLDIER.attackDelay;
        } else {
            double weaponDelayAfterMoving = Math.max(loadingDelay, rc.getWeaponDelay()) - 0.5;
            turnsTillWeCanAttack = 1 + (int) Math.max(0, (weaponDelayAfterMoving - 0.5) / 0.5);
            effectiveAttackDelay = 2 * RobotType.SOLDIER.attackDelay;
        }
        int turnsToKillEnemy = turnsTillWeCanAttack + effectiveAttackDelay * numAttacksAfterFirstToKillEnemy;

        int numAttacksAfterFirstForEnemyToKillUs = (int) ((rc.getHealth() - 0.001) / enemy.type.attackPower);
        int turnsTillEnemyCanAttack;
        int effectiveEnemyAttackDelay;
        if (enemy.supplyLevel > 0) {
            turnsTillEnemyCanAttack = (int) Math.max(0, enemy.weaponDelay - 1);
            effectiveEnemyAttackDelay = enemy.type.attackDelay;
        } else {
            turnsTillEnemyCanAttack = (int) Math.max(0, (enemy.weaponDelay - 1.0) / 0.5);
            effectiveEnemyAttackDelay = 2 * enemy.type.attackDelay;
        }
        int turnsForEnemyToKillUs = turnsTillEnemyCanAttack + effectiveEnemyAttackDelay * numAttacksAfterFirstForEnemyToKillUs;

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

        return SoldierHarrass.canWin1v1AfterMovingTo(loc, loneAttacker);
    }

    private static boolean retreat() throws GameActionException {
        RobotInfo[] tooCloseEnemies = rc.senseNearbyRobots(15, them);
        if (tooCloseEnemies.length == 0) return false;

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

        if (bestRetreatDir != null) {
            rc.move(bestRetreatDir);
            return true;
        }
        return false;
    }

    private static boolean tryMoveTowardUndefendedHelplessEnemy() throws GameActionException {
        RobotInfo[] nearbyEnemies = rc.senseNearbyRobots(35, them);

        for (RobotInfo enemy : nearbyEnemies) {
            if (enemy.type == RobotType.BEAVER || enemy.type == RobotType.MINER) {
                boolean canReach = true;
                MapLocation loc = here;
                while (loc.distanceSquaredTo(enemy.location) > RobotType.SOLDIER.attackRadiusSquared) {
                    Direction dir = loc.directionTo(enemy.location);
                    MapLocation newLoc = loc.add(dir);
                    if (rc.isPathable(RobotType.SOLDIER, newLoc) && isSafeToMoveTo(newLoc, nearbyEnemies)) {
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

    private static boolean tryStickToEnemyDrones() throws GameActionException {
        RobotInfo[] nearbyEnemies = rc.senseNearbyRobots(80, them);

        RobotInfo closestEnemyDrone = null;
        int minDistSq = 999999;
        for (RobotInfo enemy : nearbyEnemies) {
            if (enemy.type == RobotType.DRONE) {
                int distSq = here.distanceSquaredTo(enemy.location);
                if (distSq < minDistSq) {
                    minDistSq = distSq;
                    closestEnemyDrone = enemy;
                }
            }
        }

        if (closestEnemyDrone == null) return false;

        Direction toEnemy = here.directionTo(closestEnemyDrone.location);
        Direction[] dirs = new Direction[] { toEnemy, toEnemy.rotateRight(), toEnemy.rotateLeft(), toEnemy.rotateRight().rotateRight(),
                toEnemy.rotateLeft().rotateLeft() };
        for (Direction dir : dirs) {
            if (!rc.canMove(dir)) continue;

            MapLocation loc = here.add(dir);
            if (inEnemyTowerOrHQRange(loc, enemyTowers)) continue;

            RobotInfo[] potentialAttackers = rc.senseNearbyRobots(loc, 15, them);

            int numAttackers = 0;
            RobotInfo loneAttacker = null;
            for (RobotInfo enemy : potentialAttackers) {
                if (enemy.type != RobotType.DRONE) {
                    if (enemy.type.attackRadiusSquared >= loc.distanceSquaredTo(enemy.location)) {
                        numAttackers = 99;
                        break;
                    }
                } else {
                    // drone
                    if (enemy.type.attackRadiusSquared >= loc.distanceSquaredTo(enemy.location)) {
                        numAttackers++;
                        if (numAttackers >= 2) break;
                        loneAttacker = enemy;
                    }
                }
            }

            if (numAttackers >= 2) continue;
            if (numAttackers == 1 && loneAttacker.weaponDelay < 3 && !canWin1v1AfterMovingTo(loc, loneAttacker)) continue;
            rc.move(dir);
            break;
        }

        return true;
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

    private static boolean tryToShootMissiles() throws GameActionException {
        if (!rc.isWeaponReady()) return false;

        RobotInfo[] attackableEnemies = rc.senseNearbyRobots(RobotType.DRONE.attackRadiusSquared, them);
        if (attackableEnemies.length == 0) return false;

        double minHealth = 1e99;
        RobotInfo target = null;
        for (RobotInfo enemy : attackableEnemies) {
            if (enemy.type == RobotType.MISSILE && enemy.health < minHealth) {
                minHealth = enemy.health;
                target = enemy;
            }
        }
        if (target != null) {
            rc.attackLocation(target.location);
            return true;
        }
        return false;
    }

    public static void doHarrass() throws GameActionException {
        Supply.shareSupply();
        Supply.requestResupplyIfNecessary();

        enemyTowers = rc.senseEnemyTowerLocations();

        if (tryToShootMissiles()) {
            return;
        }

        if (rc.isCoreReady()) {
            if (!canStay(here)) {
                if (retreat()) {
                    return;
                }
            }
        }

        if (tryToAttack()) {
            return;
        }

        if (rc.isCoreReady()) {
            if (here.distanceSquaredTo(ourHQ) < here.distanceSquaredTo(theirHQ)) {
                if (tryStickToEnemyDrones()) return;
            }
            if (tryMoveTowardUndefendedHelplessEnemy()) return;

            RobotInfo[] nearbyEnemies = rc.senseNearbyRobots(35, them);
            NavSafetyPolicy safetyPolicy = new SafetyPolicySoldierHarrass(nearbyEnemies);
            NewNav.goTo(theirHQ, safetyPolicy);
        }
    }
}
