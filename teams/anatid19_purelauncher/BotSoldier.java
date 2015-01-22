package anatid19_purelauncher;

import battlecode.common.*;

public class BotSoldier extends Bot {
    public static void loop(RobotController theRC) throws GameActionException {
        Bot.init(theRC);
        Debug.init("micro");

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

        Harass.doHarass();
    }
}
