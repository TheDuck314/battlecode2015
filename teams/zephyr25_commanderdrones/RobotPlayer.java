package zephyr25_commanderdrones;

import battlecode.common.*;

public class RobotPlayer {
    public static void run(RobotController theRC) throws Exception {

        // I think putting the missile check out in front prevents us from loading any
        // extraneous classes and invoking their static initializers, saving some crucial
        // bytecodes on the missile's first turn.
        if (theRC.getType() == RobotType.MISSILE) {
            BotMissile.loop(theRC);
            return;
        }

        switch (theRC.getType()) {
            case HQ:
                BotHQ.loop(theRC);
                break;

            case TOWER:
                BotTower.loop(theRC);
                break;

            case BEAVER:
                BotBeaver.loop(theRC);
                break;

            case MINER:
                BotMiner.loop(theRC);
                break;

            case SOLDIER:
                BotSoldier.loop(theRC);
                break;

            case TANK:
                BotTank.loop(theRC);
                break;

            case COMMANDER:
                BotCommander.loop(theRC);
                break;

            case DRONE:
                BotDrone.loop(theRC);
                break;

            case LAUNCHER:
                BotLauncher.loop(theRC);
                break;

            case AEROSPACELAB:
            case BARRACKS:
            case HELIPAD:
            case MINERFACTORY:
            case TANKFACTORY:
            case TECHNOLOGYINSTITUTE:
            case TRAININGFIELD:
                BotGenericProducer.loop(theRC);
                break;

            case HANDWASHSTATION:
            case SUPPLYDEPOT:
            case COMPUTER:
            case BASHER:
                BotDoNothing.loop(theRC);
                break;

            default:
                throw new Exception("Unknown robot type!!! :(");
        }
    }
}
