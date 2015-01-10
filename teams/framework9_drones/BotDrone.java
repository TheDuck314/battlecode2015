package framework9_drones;

import battlecode.common.*;

public class BotDrone extends Bot {
    public static void loop(RobotController theRC) throws GameActionException {
        Bot.init(theRC);
        Debug.init("harrassbug");

        init();
        
        while (true) {
            try {
                turn();
            } catch (Exception e) {
                e.printStackTrace();
            }
            rc.yield();
        }
    }

    static boolean amResupplyDrone = false;

    static void init() throws GameActionException {
        if (Clock.getRoundNum() > 250 && MessageBoard.RESUPPLY_DRONE_CHECKIN.readInt() < Clock.getRoundNum() - 5) {
            amResupplyDrone = true;
        }
    }

    private static void turn() throws GameActionException {
        here = rc.getLocation();

        MeasureMapSize.checkForMapEdges();

        if (amResupplyDrone) {
            if(rc.isCoreReady()) Supply.runSupplies();
        } else {
            DroneHarrass.doHarrass();
        }
    }
}
