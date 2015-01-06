package framework;

import battlecode.common.*;

public class BotAerospaceLab extends Bot {
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
        if (rc.isCoreReady()) trySpawnLauncher();
    }

    private static void trySpawnLauncher() throws GameActionException {
        if (rc.getTeamOre() < RobotType.LAUNCHER.oreCost) return;

        for (Direction dir : Direction.values()) {
            if (rc.canSpawn(dir, RobotType.LAUNCHER)) {
                rc.spawn(dir, RobotType.LAUNCHER);
                return;
            }
        }
    }
}
