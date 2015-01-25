package zephyr23_soldiers;

import battlecode.common.*;

public class BotMissile {
    static RobotController rc;

    public static void loop(RobotController theRC) throws GameActionException {
        rc = theRC;
        MissileGuidance.readMissileTarget(rc, rc.getLocation());
        // rc.setIndicatorString(0, MissileGuidance.receivedTargetLocation.toString() + " - " + MissileGuidance.receivedTargetID);
        // System.out.println("hello");
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
        RobotInfo[] adjacentEnemies = rc.senseNearbyRobots(2, rc.getTeam().opponent());
        for (int i = adjacentEnemies.length; i-- > 0;) {
            if (adjacentEnemies[i].type != RobotType.MISSILE) rc.explode();
        }

        if (rc.canSenseRobot(MissileGuidance.receivedTargetID)) {
            MissileGuidance.receivedTargetLocation = rc.senseRobot(MissileGuidance.receivedTargetID).location;
        }

        MapLocation here = rc.getLocation();
        Direction dir = here.directionTo(MissileGuidance.receivedTargetLocation);

        RobotInfo blockage = rc.senseRobotAtLocation(here.add(dir));
        if (blockage != null && !blockage.team.equals(rc.getTeam()) && blockage.type != RobotType.MISSILE) {
            // System.out.println("exploding at blockage");
            rc.explode();
            return;
        }

        if (!rc.isCoreReady()) return;

        if (rc.canMove(dir)) {
            rc.move(dir);
            return;
        }

        Direction left = dir.rotateLeft();
        if (rc.canMove(left)) {
            rc.move(left);
            return;
        }

        Direction right = dir.rotateRight();
        if (rc.canMove(right)) {
            rc.move(right);
            return;
        }
    }
}
