package framework6_tanks;

import battlecode.common.*;

public class RobotPlayer {
    public static void run(RobotController theRC) throws Exception {
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
                BotSupplyRunner.loop(theRC);
                break;

            case LAUNCHER:
                BotLauncher.loop(theRC);
                break;

            case MISSILE:
                BotMissile.loop(theRC);
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
