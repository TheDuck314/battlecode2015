package anatid16_ultimateharrass;

import battlecode.common.*;

public class BotGenericProducer extends Bot {
    public static void loop(RobotController theRC) throws Exception {
        Bot.init(theRC);
        Debug.init("status");
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
        if (rc.isCoreReady()) {
            trySpawn();
        } else {
            // green dot for currently in production cooldown
//            rc.setIndicatorDot(rc.getLocation(), 0, 255, 0);
//            Debug.indicate("status", 0, "in production cooldown");
        }
    }

    private static void trySpawn() throws GameActionException {
        boolean noOrders = true;
        for (RobotType spawnType : spawnTypes) {
            if (!MessageBoard.CONSTRUCTION_ORDERS.readConstructionOrder(spawnType)) {
                continue;
            } else {
                noOrders = false;
            }

            if (rc.getTeamOre() < spawnType.oreCost) {
                // red dot for production halted due to insufficient ore
//                rc.setIndicatorDot(here, 255, 0, 0);
//                Debug.indicate("status", 0, "not enough ore to make " + spawnType.toString());
                continue;
            }

            for (Direction dir : Direction.values()) {
                if (rc.canSpawn(dir, spawnType)) {
                    rc.spawn(dir, spawnType);
                    return;
                }
            }
        }
        if(noOrders) {
            // blue dot for production halted by HQ order
//            rc.setIndicatorDot(rc.getLocation(), 0, 0, 255);
//            Debug.indicate("status", 0, "no orders");
        }
    }
}
