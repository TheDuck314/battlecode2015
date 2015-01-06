package framework;

import battlecode.common.*;

public class RobotPlayer {
    public static void run(RobotController theRC) throws Exception {
        switch (theRC.getType()) {
            case HQ:
                BotHQ.loop(theRC);
                break;

            case BEAVER:
                BotBeaver.loop(theRC);
                break;

            case MINER:
                BotMiner.loop(theRC);
                break;

            case TOWER:
                BotTower.loop(theRC);
                break;

            case MINERFACTORY:
                BotMinerFactory.loop(theRC);
                break;

            case BARRACKS:
                BotBarracks.loop(theRC);
                break;

            case SOLDIER:
                BotSoldier.loop(theRC);
                break;

            case AEROSPACELAB:
                BotAerospaceLab.loop(theRC);
                break;

            case LAUNCHER:
                BotLauncher.loop(theRC);
                break;

            case MISSILE:
                BotMissile.loop(theRC);
                break;

            case HANDWASHSTATION:
            case HELIPAD:
            case SUPPLYDEPOT:
            case TANKFACTORY:
            case TECHNOLOGYINSTITUTE:
            case TRAININGFIELD:
            case BASHER:
            case COMMANDER:
            case COMPUTER:
            case DRONE:
            case TANK:
                BotDoNothing.loop(theRC);
                break;

            default:
                throw new Exception("Unknown robot type!!! :(");
        }
    }
}
