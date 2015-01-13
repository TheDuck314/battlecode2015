package framework11_tankrush;

import battlecode.common.*;

public class BotGenericFighter extends Bot {
    public static void loop(RobotController theRC) throws GameActionException {
        Bot.init(theRC);
        Debug.init("bfs");
        while (true) {
            try {
                turn();
            } catch (Exception e) {
                e.printStackTrace();
            }
            rc.yield();
        }
    }

    private static void turn() throws GameActionException {
        here = rc.getLocation();

        Supply.shareSupply();
        Supply.requestResupplyIfNecessary();

        MeasureMapSize.checkForMapEdges();

        // if (rc.isCoreReady()) {
        // int currentDistSqToHQ = here.distanceSquaredTo(theirHQ);
        // if (currentDistSqToHQ <= rc.getType().attackRadiusSquared) {
        // Direction toHQ = here.directionTo(theirHQ);
        // Direction[] dirs = new Direction[] { toHQ, toHQ.rotateLeft(), toHQ.rotateRight() };
        // for (Direction dir : dirs) {
        // if (rc.canMove(dir)) {
        // if (here.add(dir).distanceSquaredTo(theirHQ) < currentDistSqToHQ) {
        // rc.move(dir);
        // break;
        // }
        // }
        // }
        // }
        // }

        if (rc.isWeaponReady()) {
            if (rc.getType() != RobotType.BASHER) {
                Combat.shootAtNearbyEnemies();
            }

            if (rc.isCoreReady()) {
                MapLocation rallyLoc = MessageBoard.RALLY_LOC.readMapLocation();
                if (here.distanceSquaredTo(rallyLoc) > 2) {
                    Nav.goTo(rallyLoc);
                }
            }
        }
    }
}
