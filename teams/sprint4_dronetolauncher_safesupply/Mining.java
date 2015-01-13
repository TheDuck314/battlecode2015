package sprint4_dronetolauncher_safesupply;

import battlecode.common.*;

public class Mining extends Bot {
    static MapLocation mineDest = null;

    static final double ORE_EXHAUSTED = 3.0;

    public static void tryMine() throws GameActionException {
        MapLocation[] enemyTowers = rc.senseEnemyTowerLocations();

        if (!isSafeToMine(here, enemyTowers)) {
            runAway(enemyTowers);
            mineDest = null;
            return;
        }

        if (mineDest != null) {
            if (here.equals(mineDest)) {
                if (rc.senseOre(here) >= ORE_EXHAUSTED) {
                    if (rc.senseNearbyRobots(2, us).length >= 3) {
                        Direction randDir = Direction.values()[(int) (8 * Math.random())];
                        if (rc.canMove(randDir) && rc.senseOre(here.add(randDir)) > rc.senseOre(here)) {
                            rc.move(randDir);
                            return;
                        }
                    }
                    rc.mine();
                    return;
                } else {
                    mineDest = null;
                }
            } else {
                if (rc.senseOre(here) >= ORE_EXHAUSTED) {
                    mineDest = here;
                    rc.mine();
                    return;
                } else if (rc.senseNearbyRobots(mineDest, 0, null).length != 0) {
                    mineDest = null;
                }
            }
        }

        if (mineDest == null) {
            for (int radius = 1;; radius++) {
                MapLocation[] locs = MapLocation.getAllMapLocationsWithinRadiusSq(here, radius * radius);

                MapLocation bestDest = null;
                double maxOre = ORE_EXHAUSTED;
                for (MapLocation loc : locs) {
                    double ore = rc.senseOre(loc);
                    if (ore == 0.0) {
                        if (rc.senseTerrainTile(loc) == TerrainTile.UNKNOWN) ore = ORE_EXHAUSTED + 0.1;
                    }
                    if (ore > maxOre && rc.senseNearbyRobots(loc, 0, us).length == 0) {
                        if (isSafeToMine(loc, enemyTowers)) {
                            maxOre = ore;
                            bestDest = loc;
                        }
                    }
                }

                if (bestDest != null) {
                    mineDest = bestDest;
                    rc.yield(); // this process can take a long time
//                    Debug.indicate("mine", 0, "chose new mineDest: " + mineDest.toString());
                    return;
                }
            }
        }

        // Debug.indicate("mine", 0, "going to mineDest = " + mineDest.toString());
        Nav.goTo(mineDest);
    }

    public static boolean isSafeToMine(MapLocation loc, MapLocation[] enemyTowers) {
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

        RobotInfo[] potentialAttackers = rc.senseNearbyRobots(loc, 24, them);
        int numEnemyWorkers = 0;
        for (RobotInfo enemy : potentialAttackers) {
            switch (enemy.type) {
                case DRONE:
                case TANK:
                case LAUNCHER:
                case SOLDIER:
                case BASHER:
                case COMMANDER:
                    return false;

                case BEAVER:
                case MINER:
                    numEnemyWorkers++;
                    break;

                default:
                    break;
            }
        }

        return numEnemyWorkers <= 3;
    }

    private static void runAway(MapLocation[] enemyTowers) throws GameActionException {
        RobotInfo[] visibleEnemies = rc.senseNearbyRobots(here, 24, them);
        RobotInfo nearestEnemy = null;
        int smallestDistSq = 999999;
        for (RobotInfo enemy : visibleEnemies) {
            if (enemy.type.attackPower > 0 || enemy.type == RobotType.LAUNCHER) {
                int distSq = here.distanceSquaredTo(enemy.location);
                if (distSq < smallestDistSq) {
                    smallestDistSq = distSq;
                    nearestEnemy = enemy;
                }
            }
        }
        if (nearestEnemy == null) return;

        Direction away = nearestEnemy.location.directionTo(here);
        Direction[] dirs = new Direction[] { away, away.rotateLeft(), away.rotateRight(), away.rotateLeft().rotateLeft(), away.rotateRight().rotateRight() };
        Direction flightDir = null;
        for (Direction dir : dirs) {
            if (rc.canMove(dir)) {
                if (isSafeToMine(here.add(dir), enemyTowers)) {
                    rc.move(dir);
                    return;
                } else if(flightDir == null) {
                    flightDir = dir;
                }
            }            
        }
        if(flightDir != null) {
            rc.move(flightDir);
        }
    }
}
