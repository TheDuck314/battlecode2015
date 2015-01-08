package framework7;

import battlecode.common.*;

public class Nav extends Bot {

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

    private static boolean tryMoveDirect() throws GameActionException {
        Direction toDest = here.directionTo(dest);

        if (rc.canMove(toDest)) {
            rc.move(toDest);
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
            if (rc.canMove(dir)) {
                rc.move(dir);
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
            if (!rc.canMove(leftTryDir)) leftTryDir = leftTryDir.rotateLeft();
            else break;
        }
        Direction rightTryDir = bugLastMoveDir.rotateRight();
        for (int i = 0; i < 3; i++) {
            if (!rc.canMove(rightTryDir)) rightTryDir = rightTryDir.rotateRight();
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
            if (rc.canMove(dir)) return dir;
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
        rc.move(dir);
        bugRotationCount += calculateBugRotation(dir);
        bugLastMoveDir = dir;
        if (bugWallSide == WallSide.LEFT) bugLookStartDir = dir.rotateLeft().rotateLeft();
        else bugLookStartDir = dir.rotateRight().rotateRight();
    }

    private static boolean detectBugIntoEdge() {
        if (rc.senseTerrainTile(here.add(bugLastMoveDir)) != TerrainTile.OFF_MAP) return false;

        if (bugLastMoveDir.isDiagonal()) {
            if (bugWallSide == WallSide.LEFT) {
                return !rc.canMove(bugLastMoveDir.rotateLeft());
            } else {
                return !rc.canMove(bugLastMoveDir.rotateRight());
            }
        } else {
            return true;
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

    private static void bugTo(MapLocation theDest) throws GameActionException {
        Debug.clear("nav");

        // Check if we can stop bugging at the *beginning* of the turn
        if (bugState == BugState.BUG) {
            if (canEndBug()) {
                Debug.indicateAppend("nav", 1, "ending bug; ");
                bugState = BugState.DIRECT;
            }
        }

        // If DIRECT mode, try to go directly to target
        if (bugState == BugState.DIRECT) {
            if (!tryMoveDirect()) {
                Debug.indicateAppend("nav", 1, "starting to bug; ");
                bugState = BugState.BUG;
                startBug();
            } else {
                Debug.indicateAppend("nav", 1, "successful direct move; ");
            }
        }

        // If that failed, or if bugging, bug
        if (bugState == BugState.BUG) {
            Debug.indicateAppend("nav", 1, "bugging; ");
            bugTurn();
        }
    }

    private static boolean tryMoveBfs() throws GameActionException {
        Direction bfsDir = Bfs.readResult(here, dest);

        if (bfsDir == null) return false;

        if (rc.canMove(bfsDir)) {
            rc.move(bfsDir);
            return true;
        } else {
        }

        Direction[] dirs = new Direction[] { bfsDir.rotateLeft(), bfsDir.rotateRight() };
        for (Direction dir : dirs) {
            if (rc.canMove(dir)) {
                rc.move(dir);
                return true;
            }
        }

        // recompute path if we run into a previously unknown obstacle
        if (rc.senseTerrainTile(here.add(bfsDir)) == TerrainTile.VOID) {
            Bfs.reinitQueue(dest);
        }

        return false;
    }

    public static void goTo(MapLocation theDest) throws GameActionException {
        if (!theDest.equals(dest)) {
            dest = theDest;
            bugState = BugState.DIRECT;
        }

        if (here.equals(dest)) return;

        if (!tryMoveBfs()) {
            bugTo(dest);            
        }
    }
}
