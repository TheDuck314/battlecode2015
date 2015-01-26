package zephyr25_dronegroup;

import battlecode.common.*;

interface NavSafetyPolicy {
    public boolean isSafeToMoveTo(MapLocation loc);
}

class SafetyPolicyAvoidTowersAndHQ extends Bot implements NavSafetyPolicy {
    MapLocation[] enemyTowers;

    public SafetyPolicyAvoidTowersAndHQ(MapLocation[] enemyTowers) {
        this.enemyTowers = enemyTowers;
    }

    public boolean isSafeToMoveTo(MapLocation loc) {
        return !inEnemyTowerOrHQRange(loc, enemyTowers);
    }
}

class SafetyPolicyAvoidAllUnits extends Bot implements NavSafetyPolicy {
    MapLocation[] enemyTowers;
    RobotInfo[] nearbyEnemies;

    public SafetyPolicyAvoidAllUnits(MapLocation[] enemyTowers, RobotInfo[] nearbyEnemies) {
        this.enemyTowers = enemyTowers;
        this.nearbyEnemies = nearbyEnemies;
    }

    public boolean isSafeToMoveTo(MapLocation loc) {
        if (inEnemyTowerOrHQRange(loc, enemyTowers)) return false;

        for (RobotInfo enemy : nearbyEnemies) {
            switch (enemy.type) {
                case MISSILE:
                    if (loc.distanceSquaredTo(enemy.location) <= 15) return false;
                    break;

                case LAUNCHER:
                    if (loc.distanceSquaredTo(enemy.location) <= 24) return false;
                    break;

                default:
                    if (enemy.type.attackRadiusSquared >= loc.distanceSquaredTo(enemy.location)) return false;
                    break;
            }
        }

        return true;
    }
}

public class Nav extends Bot {

    private static MapLocation dest;
    private static NavSafetyPolicy safety;

    private enum BugState {
        DIRECT, BUG
    }

    public enum WallSide {
        LEFT, RIGHT
    }

    private static BugState bugState;
    public static WallSide bugWallSide = WallSide.LEFT;
    private static int bugStartDistSq;
    private static Direction bugLastMoveDir;
    private static Direction bugLookStartDir;
    private static int bugRotationCount;
    private static int bugMovesSinceSeenObstacle = 0;

    public static int minBfsInitRound = 0;

    private static boolean move(Direction dir) throws GameActionException {
        rc.move(dir);
        return true;
    }

    private static boolean canMove(Direction dir) {
        return rc.canMove(dir) && safety.isSafeToMoveTo(here.add(dir));
    }

    private static boolean tryMoveDirect() throws GameActionException {
        if (rc.getType() == RobotType.DRONE) return tryMoveDirectDrone();

        Direction toDest = here.directionTo(dest);

        if (canMove(toDest)) {
            move(toDest);
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
                move(dir);
                return true;
            }
        }
        return false;
    }

    private static boolean tryMoveDirectDrone() throws GameActionException {
        Direction dirAhead = here.directionTo(dest);
        MapLocation locAhead = here.add(dirAhead);
        if(canMove(dirAhead) && rc.senseTerrainTile(locAhead) != TerrainTile.VOID) {
            move(dirAhead);
            return true;
        }
        
        Direction dirLeft = dirAhead.rotateLeft();
        Direction dirRight = dirAhead.rotateRight();
        
        Direction[] dirs = new Direction[3];
        dirs[0] = dirAhead;
        if (here.add(dirLeft).distanceSquaredTo(dest) < here.add(dirRight).distanceSquaredTo(dest)) {
            dirs[1] = dirLeft;
            dirs[2] = dirRight;
        } else {
            dirs[1] = dirRight;
            dirs[2] = dirLeft;
        }

        Direction voidMove = null;
        for(Direction dir : dirs) {
            if(canMove(dir)) {
                if(rc.senseTerrainTile(here.add(dir)) != TerrainTile.VOID) {
                    move(dir);
                    return true;
                } else {
                    if(voidMove != null) {
                        voidMove = dir;
                    }
                }
            }
        }
        
        if(voidMove != null) {
            move(voidMove);
            return true;
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
        if (dest.distanceSquaredTo(here.add(leftTryDir)) < dest.distanceSquaredTo(here.add(rightTryDir))) {
            bugWallSide = WallSide.RIGHT;
        } else {
            bugWallSide = WallSide.LEFT;
        }
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
        if (move(dir)) {
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
        // Debug.clear("nav");
        // Debug.indicate("nav", 0, "bugMovesSinceSeenObstacle = " + bugMovesSinceSeenObstacle + "; bugRotatoinCount = " + bugRotationCount);

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

    private static boolean tryMoveBfs() throws GameActionException {
        if (!BfsDistributed.isSearchDest(dest)) {
            // BFS is not searching for our destination
            return false;
        }

        Direction bfsDir = BfsDistributed.readResult(here, dest, minBfsInitRound);

        if (bfsDir == null) {
            return false;
        }

        if (canMove(bfsDir)) {
            move(bfsDir);
            return true;
        }

        Direction[] dirs = new Direction[] { bfsDir.rotateLeft(), bfsDir.rotateRight() };
        for (Direction dir : dirs) {
            if (canMove(dir)) {
                move(dir);
                return true;
            }
        }

        // recompute path if we run into a previously unknown obstacle
        if (Util.isImpassable(rc.senseTerrainTile(here.add(bfsDir)))) {
            BfsDistributed.reinitQueue(dest);
            minBfsInitRound = Clock.getRoundNum();
        }

        return false;
    }

    public static void goTo(MapLocation theDest, NavSafetyPolicy theSafety) throws GameActionException {
        if (!theDest.equals(dest)) {
            dest = theDest;
            bugState = BugState.DIRECT;
        }

        if (here.equals(dest)) return;

        safety = theSafety;

        if (rc.getType() != RobotType.DRONE) { // BFS avoids voids, but drones need not
            if (tryMoveBfs()) {
                return;
            }
        }

        bugMove();
    }
}
