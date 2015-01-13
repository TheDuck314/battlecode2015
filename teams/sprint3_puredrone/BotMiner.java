package sprint3_puredrone;

import battlecode.common.*;

public class BotMiner extends Bot {
    public static void loop(RobotController theRC) throws GameActionException {
        Bot.init(theRC);
        Debug.init("bfsdist");
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

        MeasureMapSize.checkForMapEdges();

        if (rc.isCoreReady()) Mining.tryMine();

        Supply.shareSupply();

        Supply.requestResupplyIfNecessary();

        MapLocation rallyLoc = MessageBoard.RALLY_LOC.readMapLocation();
        BfsDistributed.work(rallyLoc);
    }
}
