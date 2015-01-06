package framework;

import battlecode.common.*;

public class BotBarracks extends Bot {
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
        if (rc.isCoreReady()) trySpawnSoldier();
    }

    private static void trySpawnSoldier() throws GameActionException {
        if (rc.getTeamOre() < RobotType.SOLDIER.oreCost) return;

        for (Direction dir : Direction.values()) {
            if (rc.canSpawn(dir, RobotType.SOLDIER)) {
                rc.spawn(dir, RobotType.SOLDIER);
                return;
            }
        }
    }
}
