package anatid16_puredrone;

import battlecode.common.*;

public class BotMiner extends Bot {
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

        MeasureMapSize.checkForMapEdges();
        Supply.shareSupply();
        Supply.requestResupplyIfNecessary();

        if (doObligatoryMicro()) {
            mineLoc = null;
            return;
        }

        doMining();

        if (Clock.getBytecodesLeft() > 3000) {
            // Do pathing with spare bytecodes
            MapLocation rallyLoc = MessageBoard.RALLY_LOC.readMapLocation();
            BfsDistributed.work(rallyLoc);
        }
    }

    static MapLocation mineLoc = null;

    static final double ORE_EXHAUSTED = 3.0;

    static MapLocation[] enemyTowers;

    static int mapMinX;
    static int mapMaxX;
    static int mapMinY;
    static int mapMaxY;

    public static void doMining() throws GameActionException {
        enemyTowers = rc.senseEnemyTowerLocations();
        mapMinX = MessageBoard.MAP_MIN_X.readInt();
        mapMaxX = MessageBoard.MAP_MAX_X.readInt();
        mapMinY = MessageBoard.MAP_MIN_Y.readInt();
        mapMaxY = MessageBoard.MAP_MAX_Y.readInt();

        if (mineLoc != null) {
            if (here.equals(mineLoc)) {
                // We are at the spot we want to mine. Decide whether to mine
                // or whether to move on because the spot is exhausted or we are
                // blocking the way
                if (rc.senseOre(here) >= ORE_EXHAUSTED) {
                    if (weAreBlockingTheWay()) {
                        if (tryMoveToUnblockTheWay()) {
                            return;
                        }
                    }
                    if (rc.isCoreReady()) rc.mine();
                    return;
                } else {
                    // ore here has been exhausted. choose a new mineLoc
                    mineLoc = null;
                }
            } else {
                // we are not at our preferred mineLoc
                if (rc.senseOre(here) >= ORE_EXHAUSTED) {
                    // we happened upon a fine spot to mine that was different from our mineLoc
                    mineLoc = here;
                    if (rc.isCoreReady()) rc.mine();
                    return;
                } else if (!isValidMineLoc(mineLoc)) {
                    // somehow our mineLoc is no longer suitable :( choose a new one
                    mineLoc = null;
                }
            }
        }

        if (mineLoc == null) {
            mineLoc = chooseNewMineLoc();
            return; // choosing new mine loc can take several turns
        }

        RobotInfo[] nearbyEnemies = rc.senseNearbyRobots(35, them);
        NavSafetyPolicy safetyPolicy = new SafetyPolicyAvoidAllUnits(enemyTowers, nearbyEnemies);
        if (rc.isCoreReady()) NewNav.goTo(mineLoc, safetyPolicy);
    }

    private static boolean weAreBlockingTheWay() {
        int numAdjacentNonBuildingAllies = 0;
        RobotInfo[] adjacentAllies = rc.senseNearbyRobots(2, us);
        for (RobotInfo ally : adjacentAllies) {
            if (!ally.type.isBuilding) numAdjacentNonBuildingAllies++;
        }
        return numAdjacentNonBuildingAllies >= 3;
    }

    private static boolean tryMoveToUnblockTheWay() throws GameActionException {
        double oreHere = rc.senseOre(here);

        Direction bestDir = null;
        int fewestNeighbors = 999999;
        for (Direction dir : Direction.values()) {
            if (rc.canMove(dir)) {
                MapLocation loc = here.add(dir);
                if (rc.senseOre(loc) > oreHere && isSafeToMine(loc)) {
                    int numNeighbors = rc.senseNearbyRobots(loc, 2, us).length;
                    if (numNeighbors < fewestNeighbors) {
                        fewestNeighbors = numNeighbors;
                        bestDir = dir;
                    }
                }
            }
        }

        if (bestDir != null) {
            rc.move(bestDir);
            mineLoc = here.add(bestDir);
            return true;
        }
        return false;
    }

    private static int[] legDX = { 0, 0, 0, -1, 0, 0, 0, 1 };
    private static int[] legDY = { 0, 1, 0, 0, 0, -1, 0, 0 };

    private static MapLocation chooseNewMineLoc() throws GameActionException {
        MapLocation searchCenter = here;

        Direction startDiag = here.directionTo(ourHQ);
        if (!startDiag.isDiagonal()) startDiag = startDiag.rotateLeft();

        for (int radius = 1;; radius++) {
            MapLocation bestLoc = null;
            int bestDistSq = 999999;

            MapLocation loc = searchCenter.add(startDiag, radius);
            int diag = startDiag.ordinal();
            for (int leg = 0; leg < 4; leg++) {
                int dx = legDX[diag];
                int dy = legDY[diag];

                for (int i = 0; i < 2 * radius; i++) {
                    if (isValidMineLoc(loc)) {
                        int distSq = ourHQ.distanceSquaredTo(loc);
                        if (distSq < bestDistSq) {
                            bestDistSq = distSq;
                            bestLoc = loc;
                        }
                    }

                    loc = loc.add(dx, dy);
                }

                diag = (diag + 2) % 8;
            }

            if (bestLoc != null) {
                return bestLoc;
            }
        }
    }

    private static boolean isValidMineLoc(MapLocation loc) throws GameActionException {
        if (rc.senseOre(loc) <= ORE_EXHAUSTED) {
            if (rc.senseTerrainTile(loc) == TerrainTile.UNKNOWN) {
                // an unknown tile is fine because it might have ore, unless it is definitely off the map
                return loc.x >= mapMinX && loc.x <= mapMaxX && loc.y >= mapMinY && loc.y <= mapMaxY;
            } else {
                return false;
            }
        }

        if (locIsOccupied(loc)) return false;

        if (!isSafeToMine(loc)) return false;

        return true;
    }

    // Miner micro should work like this:
    //
    // If we are attacked by anything but a beaver, miner, or drone, always run away
    //
    // If we are attacked by a beaver, miner or drone:
    // - If we are a 1v1, stay and shoot if we can win it or if our health is above some threshold
    // - If multiple enemies are attacking us, stay if we have at least as many allies attacking at least one enemy, otherwise run away
    //
    // Find the closest enemy. if we can move to be in attack range of that enemy,
    // and doing so wouldn't expose us to more enemies than we have allies currently attacking the enemy, move to engage

    private static boolean doObligatoryMicro() throws GameActionException {
        RobotInfo[] visibleEnemies = rc.senseNearbyRobots(24, them);

        boolean strongEnemyNearby = false;
        RobotInfo[] weakEnemiesAttackingUs = new RobotInfo[99];
        int numWeakEnemiesAttackingUs = 0;

        visibleEnemiesLoop: for (RobotInfo visibleEnemy : visibleEnemies) {
            switch (visibleEnemy.type) {
                case TANK:
                case LAUNCHER:
                case SOLDIER:
                case BASHER:
                case COMMANDER:
                    strongEnemyNearby = true;
                    break visibleEnemiesLoop;

                case BEAVER:
                case MINER:
                case DRONE:
                    if (visibleEnemy.type.attackRadiusSquared >= here.distanceSquaredTo(visibleEnemy.location)) {
                        weakEnemiesAttackingUs[numWeakEnemiesAttackingUs++] = visibleEnemy;
                    }
                    break;

                default:
                    break;
            }
        }

        if (strongEnemyNearby) {
            runAway();
            return true;
        }

        if (numWeakEnemiesAttackingUs > 0) {
            int maxAlliesAttackingEnemy = 0;
            for (int i = 0; i < numWeakEnemiesAttackingUs; i++) {
                int numAlliesAttackingEnemy = 1 + numOtherAlliedUnitsInAttackRange(weakEnemiesAttackingUs[i].location);
                if (numAlliesAttackingEnemy > maxAlliesAttackingEnemy) maxAlliesAttackingEnemy = numAlliesAttackingEnemy;
            }

            if (numWeakEnemiesAttackingUs == 1) {
                if (maxAlliesAttackingEnemy == 1) {
                    // we are in a 1v1
                    boolean shouldStayIn1v1;
                    if (rc.getHealth() > 30) {
                        shouldStayIn1v1 = true;
                    } else {
                        shouldStayIn1v1 = canWin1v1(weakEnemiesAttackingUs[0]);
                    }
                    if (shouldStayIn1v1) {
                        if (rc.isWeaponReady()) rc.attackLocation(weakEnemiesAttackingUs[0].location);
                        return true;
                    } else {
                        runAway();
                        return true;
                    }
                } else {
                    // we outnumber the lone enemy
                    if (rc.isWeaponReady()) rc.attackLocation(weakEnemiesAttackingUs[0].location);
                    return true;
                }
            } else {
                // multiple enemies are attacking us
                if (maxAlliesAttackingEnemy >= numWeakEnemiesAttackingUs) {
                    // we have enough allies in the fight to stay
                    double minHealth = 1e99;
                    RobotInfo target = null;
                    for (int i = 0; i < numWeakEnemiesAttackingUs; i++) {
                        if (weakEnemiesAttackingUs[i].health < minHealth) {
                            minHealth = weakEnemiesAttackingUs[i].health;
                            target = weakEnemiesAttackingUs[i];
                        }
                    }
                    rc.attackLocation(target.location);
                    return true;
                } else {
                    // we are outnumbered, disengage
                    runAway();
                    return true;
                }
            }
        } else {
            // no one is attacking us. only obligatory micro is to move to engage enemies
            // that an ally is already fighting, if possible
            MapLocation closestWeakEnemy = null;
            int minDistSq = 999999;
            for (RobotInfo visibleEnemy : visibleEnemies) {
                switch (visibleEnemy.type) {
                    case DRONE:
                    case BEAVER:
                    case MINER:
                        int distSq = here.distanceSquaredTo(visibleEnemy.location);
                        if (distSq < minDistSq) {
                            minDistSq = distSq;
                            closestWeakEnemy = visibleEnemy.location;
                        }
                        break;

                    default:
                        break;
                }
            }

            if (closestWeakEnemy == null) {
                return false;
            }

            int numAlliesAlreadyEngaged = numOtherAlliedUnitsInAttackRange(closestWeakEnemy);
            if(numAlliesAlreadyEngaged == 0) {
                return false;
            }
            
            Direction dir = here.directionTo(closestWeakEnemy);
            if (rc.canMove(dir)) {
                MapLocation loc = here.add(dir);
                if (loc.distanceSquaredTo(closestWeakEnemy) <= RobotType.MINER.attackRadiusSquared) {
                    int enemyExposure = 0;
                    RobotInfo[] enemiesEngaged = rc.senseNearbyRobots(loc, 15, them);
                    for (RobotInfo enemy : enemiesEngaged) {
                        if (enemy.type.attackRadiusSquared >= loc.distanceSquaredTo(enemy.location)) {
                            switch (enemy.type) {
                                case TANK:
                                case LAUNCHER:
                                case SOLDIER:
                                case BASHER:
                                case COMMANDER:
                                case TOWER:
                                case HQ:
                                    enemyExposure = 99;
                                    break;

                                case DRONE:
                                case MINER:
                                case BEAVER:
                                    enemyExposure += 1;
                                    break;

                                default:
                                    break;
                            }
                        }
                    }
                    
                    if(numAlliesAlreadyEngaged >= enemyExposure) {
                        rc.move(dir);
                        return true;
                    }
                }
            }
            return false;
        }
    }

    private static int numOtherAlliedUnitsInAttackRange(MapLocation loc) {
        RobotInfo[] nearbyAllies = rc.senseNearbyRobots(15, us);
        int ret = 0;
        for (RobotInfo ally : nearbyAllies) {
            if (ally.type.attackRadiusSquared >= loc.distanceSquaredTo(ally.location)) {
                ret++;
            }
        }
        return ret;
    }

    private static boolean canWin1v1(RobotInfo enemy) {
        int attacksToKillEnemy = (int) (enemy.health / RobotType.MINER.attackPower);
        int turnsToKillEnemy = (int) rc.getWeaponDelay() + RobotType.MINER.attackDelay * (attacksToKillEnemy - 1);

        int attacksForEnemyToKillUs = (int) (rc.getHealth() / enemy.type.attackPower);
        int turnsForEnemyToKillUs = Math.max(0, (int) enemy.weaponDelay - 1) + enemy.type.attackDelay * (attacksForEnemyToKillUs - 1);

        return turnsToKillEnemy <= turnsForEnemyToKillUs;
    }

    private static boolean isSafeToMine(MapLocation loc) {
        if (inEnemyTowerOrHQRange(loc, enemyTowers)) return false;

        RobotInfo[] potentialAttackers = rc.senseNearbyRobots(loc, 24, them);
        int numAttackers = 0;
        RobotInfo loneAttacker = null;
        for (RobotInfo enemy : potentialAttackers) {
            switch (enemy.type) {
                case TANK:
                case LAUNCHER:
                case SOLDIER:
                case BASHER:
                case COMMANDER:
                    return false;

                case BEAVER:
                case MINER:
                case DRONE:
                    if (enemy.type.attackRadiusSquared >= loc.distanceSquaredTo(enemy.location)) {
                        numAttackers++;
                        loneAttacker = enemy;
                    }
                    break;

                default:
                    break;
            }
        }

        if (numAttackers == 0) return true;

        if (numAttackers >= 2) return false;

        if (rc.getHealth() >= 20) return true;

        if (canWin1v1(loneAttacker)) return true;

        return false;
    }

    private static void runAway() throws GameActionException {
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
                if (isSafeToMine(here.add(dir))) {
                    rc.move(dir);
                    return;
                } else if (flightDir == null) {
                    flightDir = dir;
                }
            }
        }
        if (flightDir != null) {
            rc.move(flightDir);
        }
    }

    private static boolean locIsOffMap(MapLocation loc) throws GameActionException {
        if (loc.x < mapMinX) return true;
        if (loc.x > mapMaxX) return true;
        if (loc.y < mapMinX) return true;
        if (loc.y > mapMaxX) return true;
        return false;
    }

    private static boolean locIsOccupied(MapLocation loc) throws GameActionException {
        return rc.senseNearbyRobots(loc, 0, null).length > 0;
    }
}
