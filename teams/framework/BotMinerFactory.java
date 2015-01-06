package framework;

import battlecode.common.*;

public class BotMinerFactory extends Bot {
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
        if (MessageBoard.MAKE_MINERS.readBoolean()) {
            if (rc.isCoreReady()) trySpawnMiner();
        }

        Supply.shareSupply();
    }

    private static void trySpawnMiner() throws GameActionException {
        if (rc.getTeamOre() < RobotType.MINER.oreCost) return;

        Direction[] dirs = Direction.values();
        Direction bestDir = null;
        double maxOre = -1;
        for (Direction dir : dirs) {
            if (rc.canSpawn(dir, RobotType.MINER)) {
                double ore = rc.senseOre(here.add(dir));
                if (ore > maxOre) {
                    maxOre = ore;
                    bestDir = dir;
                }
            }
        }

        if (bestDir != null) {
            rc.spawn(bestDir, RobotType.MINER);
        }
    }
}
