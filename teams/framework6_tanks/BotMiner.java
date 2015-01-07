package framework6_tanks;

import battlecode.common.*;

public class BotMiner extends Bot {
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

        if (rc.isWeaponReady()) Combat.shootAtNearbyEnemies();
        
        if (rc.isCoreReady()) Mining.tryMine();

        Supply.shareSupply();
        
        Supply.requestResupplyIfNecessary();
    }
}
