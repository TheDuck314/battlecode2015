package framework8;

import battlecode.common.*;

public class BotGenericProducer extends Bot {
    public static void loop(RobotController theRC) throws Exception {
        Bot.init(theRC);
        init();
        while (true) {
            try {
                turn();
            } catch (Exception e) {
                e.printStackTrace();
            }
            rc.yield();
        }
    }

    private static RobotType[] spawnTypes;

    private static void init() throws Exception {
        switch (rc.getType()) {
            case MINERFACTORY:
                spawnTypes = new RobotType[] { RobotType.MINER };
                break;

            case AEROSPACELAB:
                spawnTypes = new RobotType[] { RobotType.LAUNCHER };
                break;

            case BARRACKS:
                spawnTypes = new RobotType[] { RobotType.SOLDIER, RobotType.BASHER };
                break;

            case HELIPAD:
                spawnTypes = new RobotType[] { RobotType.DRONE };
                break;

            case TANKFACTORY:
                spawnTypes = new RobotType[] { RobotType.TANK };
                break;

            case TECHNOLOGYINSTITUTE:
                spawnTypes = new RobotType[] { RobotType.COMPUTER };
                break;

            case TRAININGFIELD:
                spawnTypes = new RobotType[] { RobotType.COMMANDER };
                break;

            default:
                throw new Exception("unknown robot type!");
        }
    }

    private static void turn() throws GameActionException {
        if (rc.isCoreReady()) trySpawn();
    }

    private static void trySpawn() throws GameActionException {
        for (RobotType spawnType : spawnTypes) {
            if (!MessageBoard.CONSTRUCTION_ORDERS.readConstructionOrder(spawnType)) continue;

            if (rc.getTeamOre() < spawnType.oreCost) continue;

            for (Direction dir : Direction.values()) {
                if (rc.canSpawn(dir, spawnType)) {
                    rc.spawn(dir, spawnType);
                    return;
                }
            }
        }
    }
}
