package framework8;

import battlecode.common.*;

public class BotMissile extends Bot {
    public static void loop(RobotController theRC) throws GameActionException {
        rc = theRC;
        target = MissileGuidance.getMissileTarget(rc.getLocation());
        rc.setIndicatorString(0, target.toString());
        while (true) {
            try {
                turn();
            } catch (Exception e) {
                e.printStackTrace();
            }
            rc.yield();
        }
    }

    static MapLocation target;

    private static void turn() throws GameActionException {
        // if (rc.senseNearbyRobots(2, rc.getTeam().opponent()).length > 0) rc.explode();
        here = rc.getLocation();
        if (here.equals(target)) {
            rc.explode();
            return;
        }

        Direction dir = here.directionTo(target);

        RobotInfo blockage = rc.senseRobotAtLocation(here.add(dir));
        if (blockage != null && !blockage.team.equals(rc.getTeam()) && blockage.type != RobotType.MISSILE) {
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
