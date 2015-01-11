package framework10_launchers;

import battlecode.common.*;

public class BotMissile extends Bot {
    public static void loop(RobotController theRC) throws GameActionException {
        rc = theRC;
        MissileGuidance.readMissileTarget(rc.getLocation());
        rc.setIndicatorString(0, MissileGuidance.receivedTargetLocation.toString() + " - " + MissileGuidance.receivedTargetID);
//        System.out.println("hello");
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

        if (rc.senseNearbyRobots(2, rc.getTeam().opponent()).length > 0) {
//            System.out.println("exploding on contact");
            rc.explode();
            return;
        }
        
        if (rc.canSenseRobot(MissileGuidance.receivedTargetID)) {
            MissileGuidance.receivedTargetLocation = rc.senseRobot(MissileGuidance.receivedTargetID).location;
        }

        Direction dir = here.directionTo(MissileGuidance.receivedTargetLocation);

        RobotInfo blockage = rc.senseRobotAtLocation(here.add(dir));
        if (blockage != null && !blockage.team.equals(rc.getTeam()) && blockage.type != RobotType.MISSILE) {
            System.out.println("exploding at blockage");
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
