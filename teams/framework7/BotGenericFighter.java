package framework7;

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

        if (rc.isWeaponReady()) {
            if (rc.getType() != RobotType.BASHER) {
                Combat.shootAtNearbyEnemies();
            }

            MapLocation rallyLoc = MessageBoard.RALLY_LOC.readMapLocation();
            if (rc.isCoreReady()) {
                Nav.goTo(rallyLoc);
            }
            if (Bfs.readResult(here, rallyLoc) == null) Bfs.work(rallyLoc);
        }

    }
}
