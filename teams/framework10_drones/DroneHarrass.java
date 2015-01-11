package framework10_drones;

import javax.swing.text.html.HTMLDocument.HTMLReader.IsindexAction;

import battlecode.common.*;

public class DroneHarrass extends Bot {

    static MapLocation[] enemyTowers = null;

    private static boolean isSafe(MapLocation loc) {
        if (inEnemyTowerOrHQRange(loc, enemyTowers)) return false;

        RobotInfo[] potentialAttackers = rc.senseNearbyRobots(loc, 36/* 15 */, them);
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
                return false;
            }
        }

        return true;
    }

    private static boolean canStay(MapLocation loc) {
        if (inEnemyTowerOrHQRange(loc, enemyTowers)) return false;

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

    private static boolean canMove(Direction dir) {
        return rc.canMove(dir) && isSafe(here.add(dir));
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
                rc.setIndicatorLine(here, target.location, 255, 255, 0);
            }
            Debug.indicate("harrass", 2, "have target at " + target.location);
            return true;
        }
        return false;
    }

    private static boolean tryMoveTowardUndefendedHelplessEnemy() throws GameActionException {
        RobotInfo[] nearbyEnemies = rc.senseNearbyRobots(35, them);

        for (RobotInfo enemy : nearbyEnemies) {
            if (enemy.type.attackRadiusSquared < RobotType.DRONE.attackRadiusSquared && enemy.type != RobotType.MISSILE) {
                Direction dir = here.directionTo(enemy.location);
                if (rc.canMove(dir) && isSafe(here.add(dir))) {
                    Debug.indicate("harrass", 1, "moving toward helpless enemy at " + enemy.location.toString());
                    rc.move(dir);
                    return true;
                }
            }
        }

        return false;
    }

    // current failure modes:
    // - we fail to run away from a missile because we would still be in range of a lesser threat
    // - we shoot and kill a missile that is adjacent to an ally who could have fled
    // - we move diagonally, giving ourselves >= 2 core delay and then can't dodge an incoming missile

    public static void doHarrass() throws GameActionException {
        Supply.shareSupply();

        enemyTowers = rc.senseEnemyTowerLocations();

        if (rc.isCoreReady()) {
            if (!canStay(here)) {
                Debug.indicate("harrass", 0, "retreating!");
                retreat();
            } else {
                Debug.indicate("harrass", 0, "safe!");
            }
        }

        // Even if our weapon is on cooldown, we should stay put if we have a target
        boolean haveTarget = tryToAttack();

        if (!haveTarget && rc.isCoreReady()) {
            if (tryMoveTowardUndefendedHelplessEnemy()) return;

            if (dest == null) {
                dest = theirHQ;
                bugState = BugState.DIRECT;
                bugWallSide = rc.getID() % 2 == 0 ? WallSide.LEFT : WallSide.RIGHT;
            }
            bugMove();
        }
    }

    private static MapLocation dest;

    private enum BugState {
        DIRECT, BUG
    }

    private enum WallSide {
        LEFT, RIGHT
    }

    private static BugState bugState;
    private static WallSide bugWallSide = WallSide.LEFT;
    private static int bugStartDistSq;
    private static Direction bugLastMoveDir;
    private static Direction bugLookStartDir;
    private static int bugRotationCount;
    private static int bugMovesSinceSeenObstacle = 0;

    public static int minBfsInitRound = 0;

    private static boolean moveSafely(Direction dir) throws GameActionException {
        // if (dir.isDiagonal()) {
        if (rc.getCoreDelay() + 1.4 >= 2) {
            return false;
        }
        // }
        rc.move(dir);
        return true;
    }

    private static boolean tryMoveDirect() throws GameActionException {
        Direction toDest = here.directionTo(dest);

        if (canMove(toDest)) {
            moveSafely(toDest);
            return true;
        }

        Direction[] dirs = new Direction[2];
        Direction dirLeft = toDest.rotateLeft();
        Direction dirRight = toDest.rotateRight();
        if (here.add(dirLeft).distanceSquaredTo(dest) < here.add(dirRight).distanceSquaredTo(dest)) {
            dirs[0] = dirLeft;
            dirs[1] = dirRight;
        } else {
            dirs[0] = dirRight;
            dirs[1] = dirLeft;
        }
        for (Direction dir : dirs) {
            if (canMove(dir)) {
                moveSafely(dir);
                return true;
            }
        }
        return false;
    }

    private static void startBug() throws GameActionException {
        bugStartDistSq = here.distanceSquaredTo(dest);
        bugLastMoveDir = here.directionTo(dest);
        bugLookStartDir = here.directionTo(dest);
        bugRotationCount = 0;
        bugMovesSinceSeenObstacle = 0;

        // try to intelligently choose on which side we will keep the wall
        Direction leftTryDir = bugLastMoveDir.rotateLeft();
        for (int i = 0; i < 3; i++) {
            if (!canMove(leftTryDir)) leftTryDir = leftTryDir.rotateLeft();
            else break;
        }
        Direction rightTryDir = bugLastMoveDir.rotateRight();
        for (int i = 0; i < 3; i++) {
            if (!canMove(rightTryDir)) rightTryDir = rightTryDir.rotateRight();
            else break;
        }
        // if (dest.distanceSquaredTo(here.add(leftTryDir)) < dest.distanceSquaredTo(here.add(rightTryDir))) {
        // bugWallSide = WallSide.RIGHT;
        // } else {
        // bugWallSide = WallSide.LEFT;
        // }
    }

    private static Direction findBugMoveDir() throws GameActionException {
        bugMovesSinceSeenObstacle++;
        Direction dir = bugLookStartDir;
        for (int i = 8; i-- > 0;) {
            if (canMove(dir)) return dir;
            dir = (bugWallSide == WallSide.LEFT ? dir.rotateRight() : dir.rotateLeft());
            bugMovesSinceSeenObstacle = 0;
        }
        return null;
    }

    private static int numRightRotations(Direction start, Direction end) {
        return (end.ordinal() - start.ordinal() + 8) % 8;
    }

    private static int numLeftRotations(Direction start, Direction end) {
        return (-end.ordinal() + start.ordinal() + 8) % 8;
    }

    private static int calculateBugRotation(Direction moveDir) {
        if (bugWallSide == WallSide.LEFT) {
            return numRightRotations(bugLookStartDir, moveDir) - numRightRotations(bugLookStartDir, bugLastMoveDir);
        } else {
            return numLeftRotations(bugLookStartDir, moveDir) - numLeftRotations(bugLookStartDir, bugLastMoveDir);
        }
    }

    private static void bugMove(Direction dir) throws GameActionException {
        if (moveSafely(dir)) {
            bugRotationCount += calculateBugRotation(dir);
            bugLastMoveDir = dir;
            if (bugWallSide == WallSide.LEFT) bugLookStartDir = dir.rotateLeft().rotateLeft();
            else bugLookStartDir = dir.rotateRight().rotateRight();
        }
    }

    private static boolean detectBugIntoEdge() {
        if (bugWallSide == WallSide.LEFT) {
            return rc.senseTerrainTile(here.add(bugLastMoveDir.rotateLeft())) == TerrainTile.OFF_MAP;
        } else {
            return rc.senseTerrainTile(here.add(bugLastMoveDir.rotateRight())) == TerrainTile.OFF_MAP;
        }
    }

    private static void reverseBugWallFollowDir() throws GameActionException {
        bugWallSide = (bugWallSide == WallSide.LEFT ? WallSide.RIGHT : WallSide.LEFT);
        startBug();
    }

    private static void bugTurn() throws GameActionException {
        if (detectBugIntoEdge()) {
            reverseBugWallFollowDir();
        }
        Direction dir = findBugMoveDir();
        if (dir != null) {
            bugMove(dir);
        }
    }

    private static boolean canEndBug() {
        if (bugMovesSinceSeenObstacle >= 4) return true;
        return (bugRotationCount <= 0 || bugRotationCount >= 8) && here.distanceSquaredTo(dest) <= bugStartDistSq;
    }

    private static void bugMove() throws GameActionException {
        // Debug.indicate("harrass", 0, "bugging to " + dest.toString());
        // Debug.clear("nav");

        // Check if we can stop bugging at the *beginning* of the turn
        if (bugState == BugState.BUG) {
            if (canEndBug()) {
                // Debug.indicateAppend("nav", 1, "ending bug; ");
                bugState = BugState.DIRECT;
            }
        }

        // If DIRECT mode, try to go directly to target
        if (bugState == BugState.DIRECT) {
            if (!tryMoveDirect()) {
                // Debug.indicateAppend("nav", 1, "starting to bug; ");
                bugState = BugState.BUG;
                startBug();
            } else {
                // Debug.indicateAppend("nav", 1, "successful direct move; ");
            }
        }

        // If that failed, or if bugging, bug
        if (bugState == BugState.BUG) {
            // Debug.indicateAppend("nav", 1, "bugging; ");
            bugTurn();
        }
    }

}
