package framework8;

import battlecode.common.*;

public class BotSupplyRunner extends Bot {
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

    static boolean harrass;

    private static void turn() throws GameActionException {
        here = rc.getLocation();

        if (rc.isCoreReady()) Supply.runSupplies();
    }
}
