package framework6_tanks;

import battlecode.common.*;

public class BotBeaver extends Bot {
    public static void loop(RobotController theRC) throws GameActionException {
        Bot.init(theRC);
        Debug.init("hangout");
        while (true) {
            try {
                turn();
            } catch (Exception e) {
                e.printStackTrace();
            }
            rc.yield();
        }
    }

    static MapLocation hangOutLoc = null;
    static boolean movedSinceBuild = true;

    private static void turn() throws GameActionException {
        here = rc.getLocation();

        if (rc.isWeaponReady()) Combat.shootAtNearbyEnemies();

        if (rc.isCoreReady() && movedSinceBuild) tryBuildSomething();

        if (rc.isCoreReady()) {
            hangOutLoc = ourHQ.add(10 - (int) (21 * Math.random()), 10 - (int) (21 * Math.random()));
            Nav.goTo(hangOutLoc);
            movedSinceBuild = true;
        }

        Supply.shareSupply();
    }

    private static void tryBuildSomething() throws GameActionException {
        RobotType desiredBuilding = MessageBoard.DESIRED_BUILDING.readRobotType();

        if (rc.getTeamOre() > desiredBuilding.oreCost) {
            for (Direction dir : Direction.values()) {
                if (rc.canBuild(dir, desiredBuilding)) {
                    rc.build(dir, desiredBuilding);
                    movedSinceBuild = false;
                    return;
                }
            }
        }
    }

    static MapLocation mineDest = null;
    static final double ORE_EXHAUSTED = 5.0;
}
