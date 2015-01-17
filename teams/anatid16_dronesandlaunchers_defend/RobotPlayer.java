package anatid16_dronesandlaunchers_defend;

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
            case BASHER:
            case TANK:
            case COMMANDER:
                BotGenericFighter.loop(theRC);
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

            case HANDWASHSTATION:
            case SUPPLYDEPOT:
            case COMPUTER:
                BotDoNothing.loop(theRC);
                break;

            default:
                throw new Exception("Unknown robot type!!! :(");
        }
    }
}
