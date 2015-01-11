package framework10_launchers;

import battlecode.common.*;

public class DroneHarrass extends Bot {

    static MapLocation[] enemyTowers = null;

    private static boolean isSafe(MapLocation loc) {
        if (loc.distanceSquaredTo(theirHQ) <= 52) {
            if (enemyTowers.length >= 5) {
                // enemy HQ has range of 35 and splash, so effective range 52
                return false;
            } else if (enemyTowers.length >= 2) {
                // enemy HQ has range of 35 and no splash
                if (loc.distanceSquaredTo(theirHQ) <= 35) return false;
            } else {
                // enemyHQ has range of 24;
                if (loc.distanceSquaredTo(theirHQ) <= 24) return false;
            }
        }

        for (MapLocation tower : enemyTowers) {
            if (loc.distanceSquaredTo(tower) <= RobotType.TOWER.attackRadiusSquared) return false;
        }

        RobotInfo[] potentialAttackers = rc.senseNearbyRobots(loc, 15, them);
        for (RobotInfo enemy : potentialAttackers) {
            if (enemy.type == RobotType.LAUNCHER) {
                if (loc.distanceSquaredTo(enemy.location) <= 8) {
                    if (enemy.missileCount > 0 || enemy.weaponDelay < 2) {
                        return false;
                    }
                }
            } else if (enemy.type.attackRadiusSquared >= loc.distanceSquaredTo(enemy.location)) {
                return false;
            }
        }

        return true;
    }

    private static boolean canStay(MapLocation loc) {
        if (loc.distanceSquaredTo(theirHQ) <= 52) {
            if (enemyTowers.length >= 5) {
                // enemy HQ has range of 35 and splash, so effective range 52
                return false;
            } else if (enemyTowers.length >= 2) {
                // enemy HQ has range of 35 and no splash
                if (loc.distanceSquaredTo(theirHQ) <= 35) return false;
            } else {
                // enemyHQ has range of 24;
                if (loc.distanceSquaredTo(theirHQ) <= 24) return false;
            }
        }

        for (MapLocation tower : enemyTowers) {
            if (loc.distanceSquaredTo(tower) <= RobotType.TOWER.attackRadiusSquared) return false;
        }

        RobotInfo[] potentialAttackers = rc.senseNearbyRobots(loc, 15, them);
        RobotInfo attacker = null;
        int numAttackers = 0;
        for (RobotInfo enemy : potentialAttackers) {
            if (enemy.type == RobotType.LAUNCHER) {
                if (loc.distanceSquaredTo(enemy.location) <= 8) {
                    if (enemy.missileCount > 0 || enemy.weaponDelay < 2) {
                        return false;
                    }
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
        Direction[] retreatDirs = { Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST, Direction.NORTH_EAST, Direction.SOUTH_EAST,
                Direction.SOUTH_WEST, Direction.NORTH_WEST };

        for (Direction dir : retreatDirs) {
            if (canMove(dir)) {
                rc.move(dir);
                return;
            }
        }
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
                retreat();
            }
        }

        RobotInfo[] targets = rc.senseNearbyRobots(10, them);
        for (RobotInfo target : targets) {
            if (target.type == RobotType.MISSILE) {
                if (rc.senseNearbyRobots(target.location, 2, us).length > 0) {
                    continue;
                }
            }
            if (rc.isWeaponReady()) {
                rc.attackLocation(targets[0].location);
            }
            return;
        }

        if (rc.isCoreReady()) {
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
        if (dir.isDiagonal()) {
            if (rc.getCoreDelay() + 1.4 >= 2) {
                return false;
            }
        }
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
