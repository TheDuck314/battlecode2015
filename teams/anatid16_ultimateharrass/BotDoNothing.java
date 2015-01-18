package anatid16_ultimateharrass;

import battlecode.common.*;

public class BotDoNothing extends Bot {
    public static void loop(RobotController rc) throws GameActionException {
        while (true) {
            rc.yield();
        }
    }
}
