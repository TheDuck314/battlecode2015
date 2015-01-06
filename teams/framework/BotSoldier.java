package framework;

import battlecode.common.*;

public class BotSoldier extends Bot {
    public static void loop(RobotController theRC) throws GameActionException {
        Bot.init(theRC);
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

        if (rc.isWeaponReady()) Combat.shootAtNearbyEnemies();
        else return;
        
        if (rc.isCoreReady()) {
            MapLocation rallyLoc = MessageBoard.RALLY_LOC.readMapLocation();
            Nav.goTo(rallyLoc);
        }

        Supply.shareSupply();
    }
}
