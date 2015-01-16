package anatid16_dronesandlaunchers;

import battlecode.common.*;

class SafetyPolicyDroneDefense extends Bot implements NavSafetyPolicy {
    RobotInfo[] nearbyEnemies;

    public SafetyPolicyDroneDefense(RobotInfo[] nearbyEnemies) {
        this.nearbyEnemies = nearbyEnemies;
    }

    public boolean isSafeToMoveTo(MapLocation loc) {
        return DroneDefense.isSafeToMoveTo(loc, nearbyEnemies);
    }
}

public class DroneDefense extends Bot {

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

        return DroneDefense.canWin1v1AfterMovingTo(loc, loneAttacker);
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
        for(Direction dir : dirs) {
            if(!rc.canMove(dir)) continue;

            MapLocation loc = here.add(dir);
            if(inEnemyTowerOrHQRange(loc, enemyTowers)) continue;

            RobotInfo[] potentialAttackers = rc.senseNearbyRobots(loc, 15, them);
            
            int numAttackers = 0;
            RobotInfo loneAttacker = null;
            for(RobotInfo enemy : potentialAttackers) {
                if(enemy.type != RobotType.DRONE) {
                    if(enemy.type.attackRadiusSquared >= loc.distanceSquaredTo(enemy.location)) {
                        numAttackers = 99;
                        break;
                    }
                } else {
                    // drone
                    if(enemy.type.attackRadiusSquared >= loc.distanceSquaredTo(enemy.location)) {
                        numAttackers++;
                        if(numAttackers >= 2) break;
                        loneAttacker = enemy;
                    }
                }
            }

            if(numAttackers >= 2) continue;
            if(numAttackers == 1 && loneAttacker.weaponDelay < 3 && !canWin1v1AfterMovingTo(loc, loneAttacker)) continue;
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

    public static void doDefense() throws GameActionException {
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
            if (tryStickToEnemyDrones()) return;

            RobotInfo[] nearbyEnemies = rc.senseNearbyRobots(35, them);
            NavSafetyPolicy safetyPolicy = new SafetyPolicyDroneDefense(nearbyEnemies);
            MapLocation midpoint = new MapLocation((ourHQ.x + theirHQ.x) / 2, (ourHQ.y + theirHQ.y) / 2);
            NewNav.goTo(midpoint, safetyPolicy);
        }
    }
}
