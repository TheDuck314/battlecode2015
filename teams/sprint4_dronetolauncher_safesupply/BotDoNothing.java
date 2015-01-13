package sprint4_dronetolauncher_safesupply;

import battlecode.common.*;

public class BotDoNothing extends Bot {
    public static void loop(RobotController rc) throws GameActionException {
        while (true) {
            rc.yield();
        }
    }
}
