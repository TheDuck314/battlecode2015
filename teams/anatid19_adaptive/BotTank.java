package anatid19_adaptive;

import battlecode.common.*;

public class BotTank extends Bot {
    public static void loop(RobotController theRC) throws GameActionException {
        Bot.init(theRC);
        Debug.init("harrass");

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
