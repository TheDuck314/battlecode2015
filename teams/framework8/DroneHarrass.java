package framework8;

import battlecode.common.*;

public class DroneHarrass extends Bot {

    private static boolean isSafe(MapLocation loc, MapLocation[] enemyTowers) {
        if (loc.distanceSquaredTo(theirHQ) <= 52) return false;

        for (MapLocation tower : enemyTowers) {
            if (loc.distanceSquaredTo(tower) <= 25) return false;
        }

        return true;
    }

    private static void retreat() throws GameActionException {
        for (Direction dir : Direction.values()) {
            if (!rc.canMove(dir)) continue;

            MapLocation loc = here.add(dir);
            RobotInfo[] enemies = rc.senseNearbyRobots(loc, 15, them);
            boolean safe = true;
            for (RobotInfo enemy : enemies) {
                if (enemy.type.attackRadiusSquared >= loc.distanceSquaredTo(enemy.location)) {
                    safe = false;
                    break;
                }
            }
            if (safe) {
                rc.move(dir);
                return;
            }
        }
    }

    public static void doHarrass() throws GameActionException {
        RobotInfo[] threats = rc.senseNearbyRobots(15, them);

        for (RobotInfo enemy : threats) {
            if (enemy.type.attackRadiusSquared >= here.distanceSquaredTo(enemy.location)) {
                if (rc.isCoreReady()) {
                    retreat();
                }
                return;
            }
        }

        RobotInfo[] targets = rc.senseNearbyRobots(10, them);
        if (targets.length > 0) {
            if (rc.isWeaponReady()) {
                rc.attackLocation(targets[0].location);
            }
            return;
        }

        MapLocation[] enemyTowers = rc.senseEnemyTowerLocations();

        if (rc.isCoreReady()) {
            Direction toHQ = here.directionTo(theirHQ);
            Direction[] dirs;
            if (Math.random() < 0.5) {
                dirs = new Direction[] { toHQ, toHQ.rotateLeft(), toHQ.rotateRight(), toHQ.rotateLeft().rotateLeft(), toHQ.rotateRight().rotateRight(),
                        toHQ.rotateLeft().rotateLeft().rotateLeft(), toHQ.rotateRight().rotateRight().rotateRight(), toHQ.opposite() };
            } else {
                dirs = new Direction[] { toHQ, toHQ.rotateRight(), toHQ.rotateLeft(), toHQ.rotateRight().rotateRight(), toHQ.rotateLeft().rotateLeft(),
                        toHQ.rotateRight().rotateRight().rotateRight(), toHQ.rotateLeft().rotateLeft().rotateLeft(), toHQ.opposite() };
            }

            for (Direction dir : dirs) {
                if (rc.canMove(dir) && isSafe(here.add(dir), enemyTowers)) {
                    rc.move(dir);
                    return;
                }
            }
        }
    }
}
