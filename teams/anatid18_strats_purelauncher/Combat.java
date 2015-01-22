package anatid18_strats_purelauncher;

import battlecode.common.*;

public class Combat extends Bot {
    public static void shootAtNearbyEnemies() throws GameActionException {
        int attackRadiusSq = rc.getType().attackRadiusSquared;
        RobotInfo[] enemies = rc.senseNearbyRobots(attackRadiusSq, them);

        RobotInfo target = null;
        double minHealth = 999999;
        for (RobotInfo enemy : enemies) {
            if (enemy.health < minHealth) {
                minHealth = enemy.health;
                target = enemy;
            }
        }

        if (target != null) {
            rc.attackLocation(target.location);
        }
    }

    public static boolean isSafe(MapLocation loc, MapLocation[] enemyTowers) {
        if (loc.distanceSquaredTo(theirHQ) <= 52) {
            if (enemyTowers.length >= 5) {
                // enemy HQ has range of 35 and splash, so effective range 52
                return false;
            } else if (enemyTowers.length >= 2) {
                // enemy HQ has range of 35 and no splash
                if (loc.distanceSquaredTo(theirHQ) <= 35) return false;
            } else {
                // enemyHQ has range of 24;
                if (loc.distanceSquaredTo(theirHQ) <= 24) return false;
            }
        }

        for (MapLocation tower : enemyTowers) {
            if (loc.distanceSquaredTo(tower) <= RobotType.TOWER.attackRadiusSquared) return false;
        }

        RobotInfo[] potentialAttackers = rc.senseNearbyRobots(loc, 15, them);
        for (RobotInfo enemy : potentialAttackers) {
            if (enemy.type.attackRadiusSquared >= loc.distanceSquaredTo(enemy.location)) {
                return false;
            }
        }

        return true;
    }

    public static void retreat(MapLocation[] enemyTowers) throws GameActionException {
        Direction[] retreatDirs = { Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST, Direction.NORTH_EAST, Direction.SOUTH_EAST,
                Direction.SOUTH_WEST, Direction.NORTH_WEST };

        for (Direction dir : retreatDirs) {
            if (rc.canMove(dir) && isSafe(here.add(dir), enemyTowers)) {
                rc.move(dir);
                return;
            }
        }
    }
}
