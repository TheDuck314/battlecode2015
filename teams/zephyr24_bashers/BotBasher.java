package zephyr24_bashers;

import battlecode.common.*;

public class BotBasher extends Bot {
    public static void loop(RobotController theRC) throws GameActionException {
        Bot.init(theRC);
        // Debug.init("micro");

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

        MeasureMapSize.checkForMapEdges();
        Supply.shareSupply();
        Supply.requestResupplyIfNecessary();

        if (rc.isCoreReady()) {
            RobotInfo[] nearbyEnemies = rc.senseNearbyRobots(24, them);
            if (nearbyEnemies.length > 0) {
                Nav.goTo(Util.closest(nearbyEnemies, here).location, new SafetyPolicyYOLO());
            } else {
                MapLocation rallyLoc = MessageBoard.RALLY_LOC.readMapLocation();
                Nav.goTo(rallyLoc, new SafetyPolicyYOLO());
            }
        }
    }
}

class SafetyPolicyYOLO implements NavSafetyPolicy {
    public boolean isSafeToMoveTo(MapLocation loc) {
        return true;
    }
}