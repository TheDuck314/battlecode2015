package anatid16_puredrone;

import battlecode.common.*;

public class BotTower extends Bot {
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
        if (rc.isWeaponReady()) Combat.shootAtNearbyEnemies();

        Supply.shareSupply();
    }

}
