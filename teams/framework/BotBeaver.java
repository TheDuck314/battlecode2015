package framework;

import battlecode.common.*;

public class BotBeaver extends Bot {
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
        here = rc.getLocation();

        if (rc.isWeaponReady()) Combat.shootAtNearbyEnemies();
        
        if (rc.isCoreReady()) tryBuildSomething();

        if (rc.isCoreReady()) {
            if(here.distanceSquaredTo(ourHQ) > 25) Nav.goTo(ourHQ);
            else Mining.tryMine();
        }

        Supply.shareSupply();
    }

    private static void tryBuildSomething() throws GameActionException {
        RobotType desiredBuilding = MessageBoard.DESIRED_BUILDING.readRobotType();
        
        if (rc.getTeamOre() > desiredBuilding.oreCost) {
            for (Direction dir : Direction.values()) {
                if (rc.canBuild(dir, desiredBuilding)) {
                    rc.build(dir, desiredBuilding);
                    return;
                }
            }
        }
    }

    static MapLocation mineDest = null;
    static final double ORE_EXHAUSTED = 5.0;
}
