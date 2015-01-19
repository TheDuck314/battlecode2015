package anatid18;

import battlecode.common.*;

class SafetyPolicyHarass extends Bot implements NavSafetyPolicy {
    RobotInfo[] nearbyEnemies;

    public SafetyPolicyHarass(RobotInfo[] nearbyEnemies) {
        this.nearbyEnemies = nearbyEnemies;
    }

    public boolean isSafeToMoveTo(MapLocation loc) {
        return Harass.isSafeToMoveTo(loc, nearbyEnemies);
    }
}

public class Harass extends Bot {

    private static MapLocation[] enemyTowers;

    private static boolean canWin1v1(RobotInfo enemy) {
        int numAttacksAfterFirstToKillEnemy = (int) ((enemy.health - 0.001) / rc.getType().attackPower);
        int turnsTillWeCanAttack;
        int effectiveAttackDelay;
        if (rc.getSupplyLevel() > 0) {
            turnsTillWeCanAttack = (int) rc.getWeaponDelay();
            effectiveAttackDelay = rc.getType().attackDelay;
        } else {
            turnsTillWeCanAttack = (int) Math.max(0, (rc.getWeaponDelay() - 0.5) / 0.5);
            effectiveAttackDelay = 2 * rc.getType().attackDelay;
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
        double loadingDelay = rc.getType().loadingDelay;

        int numAttacksAfterFirstToKillEnemy = (int) ((enemy.health - 0.001) / rc.getType().attackPower);
        int turnsTillWeCanAttack;
        int effectiveAttackDelay;
        if (rc.getSupplyLevel() > 0) {
            double weaponDelayAfterMoving = Math.max(loadingDelay, rc.getWeaponDelay()) - 1.0;
            turnsTillWeCanAttack = 1 + (int) weaponDelayAfterMoving;
            effectiveAttackDelay = rc.getType().attackDelay;
        } else {
            double weaponDelayAfterMoving = Math.max(loadingDelay, rc.getWeaponDelay()) - 0.5;
            turnsTillWeCanAttack = 1 + (int) Math.max(0, (weaponDelayAfterMoving - 0.5) / 0.5);
            effectiveAttackDelay = 2 * rc.getType().attackDelay;
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

        // stay if we can fire and then retreat before they can fire once
        if (rc.getSupplyLevel() > 0 && rc.getType().cooldownDelay <= 1 && loneAttacker.weaponDelay >= 2 && rc.getWeaponDelay() <= loneAttacker.weaponDelay - 1) {
            return true;
        }

        if (canWin1v1(loneAttacker)) return true;

        return false;
    }

    public static boolean isSafeToMoveTo(MapLocation loc, RobotInfo[] nearbyEnemies) {
        if (inEnemyTowerOrHQRange(loc, enemyTowers)) return false;

        RobotInfo loneAttacker = null;
        int numAttackers = 0;
        for (RobotInfo enemy : nearbyEnemies) {
            switch (enemy.type) {
                case LAUNCHER:
                    if (loc.distanceSquaredTo(enemy.location) <= 35) {
                        return false;
                    }
                    break;

                case MISSILE:
                    if (loc.distanceSquaredTo(enemy.location) <= 12) {
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

        return Harass.canWin1v1AfterMovingTo(loc, loneAttacker);
    }

    private static boolean retreat() throws GameActionException {
        RobotInfo[] tooCloseEnemies = rc.senseNearbyRobots(15, them);
        if (tooCloseEnemies.length == 0) return false;

        RobotInfo[] nearbyEnemies = rc.senseNearbyRobots(35, them);

        Direction bestRetreatDir = null;
        RobotInfo currentClosestEnemy = Util.closest(nearbyEnemies, here);

        boolean mustMoveOrthogonally = false;
        if (rc.getType() == RobotType.DRONE && rc.getCoreDelay() >= 0.6 && currentClosestEnemy.type == RobotType.MISSILE) mustMoveOrthogonally = true;

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

        RobotType myType = rc.getType();
        int myAttackRadiusSquared = myType.attackRadiusSquared;

        for (RobotInfo enemy : nearbyEnemies) {
            if (enemy.type == RobotType.BEAVER || enemy.type == RobotType.MINER) {
                boolean canReach = true;
                MapLocation loc = here;
                while (loc.distanceSquaredTo(enemy.location) > myAttackRadiusSquared) {
                    Direction dir = loc.directionTo(enemy.location);
                    MapLocation newLoc = loc.add(dir);
                    if (rc.isPathable(myType, newLoc) && isSafeToMoveTo(newLoc, nearbyEnemies)) {
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

    private static boolean isHarasser(RobotType rt) {
        switch (rt) {
            case SOLDIER:
            case BASHER:
            case DRONE:
            case TANK:
                return true;

            default:
                return false;
        }
    }

    private static boolean tryStickToEnemyHarassers() throws GameActionException {
        RobotInfo[] nearbyEnemies = rc.senseNearbyRobots(80, them);

        RobotInfo closestEnemyHarasser = null;
        int minDistSq = 999999;
        for (RobotInfo enemy : nearbyEnemies) {
            if (isHarasser(enemy.type)) {
                int distSq = here.distanceSquaredTo(enemy.location);
                if (distSq < minDistSq) {
                    minDistSq = distSq;
                    closestEnemyHarasser = enemy;
                }
            }
        }

        if (closestEnemyHarasser == null) return false;

        // TODO: this analysis is probably screwed up if the closest enemy harasser has longer range than us

        Direction toEnemy = here.directionTo(closestEnemyHarasser.location);
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
                if (enemy.type.attackRadiusSquared >= loc.distanceSquaredTo(enemy.location)) {
                    numAttackers++;
                    if (numAttackers >= 2) break;
                    loneAttacker = enemy;
                }
            }

            if (numAttackers >= 2) {
                continue;
            }
            if (numAttackers == 1 && !canWin1v1AfterMovingTo(loc, loneAttacker)) continue;

            rc.move(dir);
            break;
        }

        return true;
    }

    private static boolean tryToAttack() throws GameActionException {
        RobotInfo[] attackableEnemies = rc.senseNearbyRobots(rc.getType().attackRadiusSquared, them);
        if (attackableEnemies.length == 0) return false;

        if (rc.isWeaponReady()) {
            RobotInfo target = Util.leastHealth(attackableEnemies);
            rc.attackLocation(target.location);
        }
        return true;
    }

    static boolean firstTurn = true;

    public static void doHarass() throws GameActionException {
        if (firstTurn) {
            NewNav.bugWallSide = (rc.getID() % 2 == 0 ? NewNav.WallSide.LEFT : NewNav.WallSide.RIGHT);
            firstTurn = false;
        }

        Supply.shareSupply();
        Supply.requestResupplyIfNecessary();

        enemyTowers = rc.senseEnemyTowerLocations();

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
                if (tryStickToEnemyHarassers()) return;
            }
            if (tryMoveTowardUndefendedHelplessEnemy()) return;

            RobotInfo[] nearbyEnemies = rc.senseNearbyRobots(35, them);
            NavSafetyPolicy safetyPolicy = new SafetyPolicyHarass(nearbyEnemies);
            NewNav.goTo(theirHQ, safetyPolicy);
        }
    }
}
