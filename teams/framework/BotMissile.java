package framework;

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
        if (rc.senseNearbyRobots(2, rc.getTeam().opponent()).length > 0) rc.explode();
        here = rc.getLocation();
        Direction dir = here.directionTo(target);
        if (rc.canMove(dir)) rc.move(dir);
    }
}
