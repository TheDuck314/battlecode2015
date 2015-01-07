package framework;

import battlecode.common.*;

public class BotGenericFighter extends Bot {
    public static void loop(RobotController theRC) throws GameActionException {
        Bot.init(theRC);
        Debug.init("supply");
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

        if (rc.isWeaponReady()) {
            if (rc.getType() != RobotType.BASHER) {
                Combat.shootAtNearbyEnemies();
            }

            if (rc.isCoreReady()) {
                MapLocation rallyLoc = MessageBoard.RALLY_LOC.readMapLocation();
                Nav.goTo(rallyLoc);
            }
        }

        Supply.shareSupply();
        Supply.requestResupplyIfNecessary();
    }
}
