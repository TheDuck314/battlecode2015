package framework9_drones;

import battlecode.common.*;

public class BotMiner extends Bot {
    public static void loop(RobotController theRC) throws GameActionException {
        Bot.init(theRC);
        Debug.init("mine");
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

        if (rc.isCoreReady()) {
            MapLocation[] enemyTowers = rc.senseEnemyTowerLocations();
            if (!Combat.isSafe(here, enemyTowers)) {
                Combat.retreat(enemyTowers);
                Mining.mineDest = null;
            }
        }

        if (rc.isCoreReady()) Mining.tryMine();

        Supply.shareSupply();

        Supply.requestResupplyIfNecessary();

        MapLocation rallyLoc = MessageBoard.RALLY_LOC.readMapLocation();
        BfsDistributed.work(rallyLoc);
    }
}
