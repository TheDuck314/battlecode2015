package zephyr25_supply;

import battlecode.common.*;

class SafetyPolicyCommanderHarass extends Bot implements NavSafetyPolicy {
    RobotInfo[] nearbyEnemies;

    public SafetyPolicyCommanderHarass(RobotInfo[] nearbyEnemies) {
        this.nearbyEnemies = nearbyEnemies;
    }

    public boolean isSafeToMoveTo(MapLocation loc) {
        return CommanderHarass.isSafeToMoveTo(loc, nearbyEnemies);
    }
}

public class CommanderHarass extends Bot {

    private static MapLocation[] enemyTowers;

    private static boolean canWin1v1(RobotInfo enemy) {
        int numAttacksAfterFirstToKillEnemy = (int) ((enemy.health - 0.001) / rc.getType().attackPower);
        int turnsTillWeCanAttack;
        int effectiveAttackDelay;
        if (rc.getSupplyLevel() > 0) {
            turnsTillWeCanAttack = (int) rc.getWeaponDelay();
            effectiveAttackDelay = rc.getType().attackDelay;
        } else {
            turnsTillWeCanAttack = (int) Math.max(0, (rc.getWeaponDelay() - 0.5) / 0.5);
            effectiveAttackDelay = 2 * rc.getType().attackDelay;
        }
        int turnsToKillEnemy = turnsTillWeCanAttack + effectiveAttackDelay * numAttacksAfterFirstToKillEnemy;

        int numAttacksAfterFirstForEnemyToKillUs = (int) ((rc.getHealth() - 0.001) / enemy.type.attackPower);
        int turnsTillEnemyCanAttack;
        int effectiveEnemyAttackDelay;
        if (enemy.supplyLevel > 0) {
            turnsTillEnemyCanAttack = (int) Math.max(0, enemy.weaponDelay - 1);
            effectiveEnemyAttackDelay = enemy.type.attackDelay;
        } else {
            turnsTillEnemyCanAttack = (int) Math.max(0, (enemy.weaponDelay - 1.0) / 0.5);
            effectiveEnemyAttackDelay = 2 * enemy.type.attackDelay;
        }
        int turnsForEnemyToKillUs = turnsTillEnemyCanAttack + effectiveEnemyAttackDelay * numAttacksAfterFirstForEnemyToKillUs;

        return turnsToKillEnemy <= turnsForEnemyToKillUs;
    }

    public static boolean canWin1v1AfterMovingTo(MapLocation loc, RobotInfo enemy) {
        double loadingDelay = rc.getType().loadingDelay;

        int numAttacksAfterFirstToKillEnemy = (int) ((enemy.health - 0.001) / rc.getType().attackPower);
        int turnsTillWeCanAttack;
        int effectiveAttackDelay;
        if (rc.getSupplyLevel() > 0) {
            double weaponDelayAfterMoving = Math.max(loadingDelay, rc.getWeaponDelay()) - 1.0;
            turnsTillWeCanAttack = 1 + (int) weaponDelayAfterMoving;
            effectiveAttackDelay = rc.getType().attackDelay;
        } else {
            double weaponDelayAfterMoving = Math.max(loadingDelay, rc.getWeaponDelay()) - 0.5;
            turnsTillWeCanAttack = 1 + (int) Math.max(0, (weaponDelayAfterMoving - 0.5) / 0.5);
            effectiveAttackDelay = 2 * rc.getType().attackDelay;
        }
        int turnsToKillEnemy = turnsTillWeCanAttack + effectiveAttackDelay * numAttacksAfterFirstToKillEnemy;

        int numAttacksAfterFirstForEnemyToKillUs = (int) ((rc.getHealth() - 0.001) / enemy.type.attackPower);
        int turnsTillEnemyCanAttack;
        int effectiveEnemyAttackDelay;
        if (enemy.supplyLevel > 0) {
            turnsTillEnemyCanAttack = (int) Math.max(0, enemy.weaponDelay - 1);
            effectiveEnemyAttackDelay = enemy.type.attackDelay;
        } else {
            turnsTillEnemyCanAttack = (int) Math.max(0, (enemy.weaponDelay - 1.0) / 0.5);
            effectiveEnemyAttackDelay = 2 * enemy.type.attackDelay;
        }
        int turnsForEnemyToKillUs = turnsTillEnemyCanAttack + effectiveEnemyAttackDelay * numAttacksAfterFirstForEnemyToKillUs;

        return turnsToKillEnemy <= turnsForEnemyToKillUs;
    }

    public static boolean isSafeToMoveTo(MapLocation loc, RobotInfo[] nearbyEnemies) {
        if (inEnemyTowerOrHQRange(loc, enemyTowers)) return false;

        RobotInfo loneAttacker = null;
        int numAttackers = 0;
        for (RobotInfo enemy : nearbyEnemies) {
            switch (enemy.type) {
                case LAUNCHER:
                    if (loc.distanceSquaredTo(enemy.location) <= 24) {
                        return false;
                    }
                    break;

                case MISSILE:
                    if (loc.distanceSquaredTo(enemy.location) <= 10) {
                        return false;
                    }
                    break;

                default:
                    if (enemy.type.attackRadiusSquared >= loc.distanceSquaredTo(enemy.location)) {
                        numAttackers++;
                        if (numAttackers >= 2) return false;
                        loneAttacker = enemy;
                    }
                    break;
            }
        }

        if (numAttackers == 0) return true;

        return CommanderHarass.canWin1v1AfterMovingTo(loc, loneAttacker);
    }

    // commander flash range:
    // . . . . . . . . .
    // . . . f f f . . .
    // . . f f f f f . .
    // . f f f f f f f .
    // . f f f C f f f .
    // . f f f f f f f .
    // . . f f f f f . .
    // . . . f f f . . .
    // . . . . . . . . .
    static int[] flashMaxRangeDxs = new int[] { 0, 1, 2, 3, 3, 3, 2, 1, 0, -1, -2, -3, -3, -3, -2, -1 };
    static int[] flashMaxRangeDys = new int[] { 3, 3, 2, 1, 0, -1, -2, -3, -3, -3, -2, -1, 0, 1, 2, 3 };
    static final int numFlashMaxRangeSquares = 16;

    private static boolean tryToRetreat(RobotInfo[] nearbyEnemies) throws GameActionException {
        Direction bestRetreatDir = null;
        RobotInfo currentClosestEnemy = Util.closest(nearbyEnemies, here);

        int bestDistSq = here.distanceSquaredTo(currentClosestEnemy.location);
        for (Direction dir : Direction.values()) {
            if (!rc.canMove(dir)) continue;

            MapLocation retreatLoc = here.add(dir);
            if (inEnemyTowerOrHQRange(retreatLoc, enemyTowers)) continue;

            RobotInfo closestEnemy = Util.closest(nearbyEnemies, retreatLoc);
            int distSq = retreatLoc.distanceSquaredTo(closestEnemy.location);
            if (distSq > bestDistSq) {
                bestDistSq = distSq;
                bestRetreatDir = dir;
            }
        }

        if (rc.getFlashCooldown() == 0) {
            MapLocation bestFlashLoc = null;
            for (int i = 0; i < numFlashMaxRangeSquares; i++) {
                MapLocation flashLoc = here.add(flashMaxRangeDxs[i], flashMaxRangeDys[i]);
                if (!rc.isPathable(RobotType.COMMANDER, flashLoc)) continue;
                if (inEnemyTowerOrHQRange(flashLoc, enemyTowers)) continue;
                RobotInfo closestEnemy = Util.closest(nearbyEnemies, flashLoc);
                int distSq = flashLoc.distanceSquaredTo(closestEnemy.location);
                if (distSq > bestDistSq) {
                    bestDistSq = distSq;
                    bestFlashLoc = flashLoc;
                }
            }
            if (bestFlashLoc != null) {
                rc.castFlash(bestFlashLoc);
                return true;
            }
        }

        if (bestRetreatDir != null) {
            rc.move(bestRetreatDir);
            return true;
        }
        return false;
    }

    private static boolean canTakeMultipleEnemies(RobotInfo[] enemiesAttackingUs, int numEnemiesAttackingUs) {
        // suppose we kill the enemies in the reverse order they appear in the list.
        // then we can figure out how much damage we will take during that process

        double enemyDps = 0;
        double damageTakenWhileKillingAllEnemies = 0;
        for (int i = 0; i < numEnemiesAttackingUs; i++) {
            RobotInfo enemy = enemiesAttackingUs[i];
            enemyDps += enemy.type.attackPower / enemy.type.attackDelay;
            int turnsToKillEnemy = (int) Math.ceil(enemy.health / RobotType.COMMANDER.attackPower);
            damageTakenWhileKillingAllEnemies += enemyDps * turnsToKillEnemy;
        }

        return damageTakenWhileKillingAllEnemies < 0.7 * rc.getHealth();
    }

    private static boolean doMicro(RobotInfo[] nearbyEnemies, boolean shadowEnemyHarassers) throws GameActionException {
        if (nearbyEnemies.length == 0) {
            RobotInfo[] moreEnemies = rc.senseNearbyRobots(63, them);
            if (moreEnemies.length == 0) {
                Debug.indicate("micro", 0, "no enemies, no micro");
                return false;
            } else {
                RobotInfo closestEnemy = Util.closest(moreEnemies, here);
                if (closestEnemy != null && isHarasser(closestEnemy.type) && rc.getType().attackRadiusSquared >= closestEnemy.type.attackRadiusSquared) {
                    Debug.indicate("micro", 0, "no nearby enemies, shadowing an enemy at long range");
                    if (rc.isCoreReady()) {
                        shadowHarasser(closestEnemy, nearbyEnemies);
                    }
                    return true;
                }
            }
        }

        if (tryFleeMissiles(nearbyEnemies)) return true;

        int numEnemiesAttackingUs = 0;
        RobotInfo[] enemiesAttackingUs = new RobotInfo[99];
        RobotInfo bestEnemyToAttack = null;
        for (RobotInfo enemy : nearbyEnemies) {
            int distSq = here.distanceSquaredTo(enemy.location);
            if (enemy.type.attackRadiusSquared >= distSq) {
                enemiesAttackingUs[numEnemiesAttackingUs++] = enemy;
            }
            if (RobotType.COMMANDER.attackRadiusSquared >= distSq) {
                if (bestEnemyToAttack == null || enemy.health < bestEnemyToAttack.health) {
                    bestEnemyToAttack = enemy;
                }
            }
        }

        // we can fire every turn without compromising our ability to move. so if we have a target, shoot
        if (bestEnemyToAttack != null) {
            if (rc.isWeaponReady()) rc.attackLocation(bestEnemyToAttack.location);
        }

        if (!rc.isCoreReady()) return true; // can't do anything else

        if (numEnemiesAttackingUs > 0) {
            // people are attacking us
            if (canTakeMultipleEnemies(enemiesAttackingUs, numEnemiesAttackingUs)) {
                // we can take the guys attacking us. approach our target if we can
                if (tryMoveTowardLocationSafely(bestEnemyToAttack.location, nearbyEnemies)) {
                    Debug.indicate("micro", 0, "moved toward enemy we are attacking");
                } else {
                    Debug.indicate("micro", 0, "couldn't move toward");
                }
            } else {
                if (tryToRetreat(nearbyEnemies)) {
                    Debug.indicate("micro", 0, "retreated");
                } else {
                    Debug.indicate("micro", 0, "couldn't retreat!");
                }
            }
            return true;
        } else {
            // no one is shooting at us.

            // if we shot at someone, try to move toward them to prevent them from escaping
            if (bestEnemyToAttack != null) {
                if (tryMoveTowardLocationSafely(bestEnemyToAttack.location, nearbyEnemies)) {
                    Debug.indicate("micro", 0, "moved toward enemy we are attacking (no one is attacking us)");
                } else {
                    Debug.indicate("micro", 0, "couldn't move toward enemy we are attacking (no one is attacking us)");
                }
                return true;
            }

            // we can't shoot anyone

            // check if we can engage the closest enemy
            RobotInfo closestEnemy = Util.closest(nearbyEnemies, here);
            if (closestEnemy != null && closestEnemy.type != RobotType.MISSILE) {
                if (tryMoveToSafelyEngageEnemyAtLocationInOneTurn(closestEnemy.location, nearbyEnemies)) {
                    Debug.indicate("micro", 0, "moved to engage an enemy; we can take everyone near the dest");
                    return true;
                }
            }

            // try to move toward and kill an enemy worker
            if (tryMoveToEngageAnyUndefendedWorkerOrBuilding(nearbyEnemies)) {
                Debug.indicate("micro", 0, "moved to engage an undefended worker or building");
                return true;
            }

            if (shadowEnemyHarassers) {
                if (closestEnemy != null && isHarasser(closestEnemy.type) && rc.getType().attackRadiusSquared >= closestEnemy.type.attackRadiusSquared) {
                    Debug.indicate("micro", 0, "shadowing " + closestEnemy.location.toString());
                    shadowHarasser(closestEnemy, nearbyEnemies);
                    return true;
                }
            }

            // no required actions
            Debug.indicate("micro", 0, "no micro action though core is ready and there are nearby enemies");
            return false;
        }
    }

    private static boolean tryFleeMissiles(RobotInfo[] nearbyEnemies) throws GameActionException {
        boolean needToFlee = false;
        RobotInfo missileToShoot = null;
        for (RobotInfo enemy : nearbyEnemies) {
            if (enemy.type == RobotType.MISSILE && here.distanceSquaredTo(enemy.location) <= 10) {
                needToFlee = true;
                if (missileToShoot == null || enemy.health < missileToShoot.health) {
                    missileToShoot = enemy;
                }
                break;
            }
        }


        if (needToFlee) {
            Debug.indicate("micro", 1, "need to flee; ");
            if (rc.isWeaponReady()) {
                Debug.indicateAppend("micro", 1, "shot; ");
                rc.attackLocation(missileToShoot.location);
            }
            if (rc.isCoreReady()) {
                Debug.indicateAppend("micro", 1, "moved");
                tryToRetreat(nearbyEnemies);
            }
            return true;
        }

        Debug.indicate("micro", 1, "don't need to flee");

        return false;
    }

    private static boolean canTakeEnemiesAttackingLocation(MapLocation loc, RobotInfo[] nearbyEnemies) {
        RobotInfo[] enemiesAttackingLoc = new RobotInfo[99];
        int numEnemiesAttackingLoc = 0;
        for (RobotInfo enemy : nearbyEnemies) {
            if (enemy.type.attackRadiusSquared >= loc.distanceSquaredTo(enemy.location)) {
                enemiesAttackingLoc[numEnemiesAttackingLoc++] = enemy;
            }
        }

        return canTakeMultipleEnemies(enemiesAttackingLoc, numEnemiesAttackingLoc);
    }

    private static boolean tryMoveToSafelyEngageEnemyAtLocationInOneTurn(MapLocation loc, RobotInfo[] nearbyEnemies) throws GameActionException {

        if (rc.getFlashCooldown() == 0) {
            Direction locToHere = loc.directionTo(here);
            MapLocation[] flashLocs = new MapLocation[] { loc.add(locToHere), loc.add(locToHere.rotateLeft()), loc.add(locToHere.rotateRight()) };
            for (MapLocation flashLoc : flashLocs) {
                if (here.distanceSquaredTo(flashLoc) > GameConstants.FLASH_RANGE_SQUARED) continue;
                if (!rc.isPathable(RobotType.COMMANDER, flashLoc)) continue;
                if (inEnemyTowerOrHQRange(flashLoc, enemyTowers)) continue;
                if (!canTakeEnemiesAttackingLocation(flashLoc, nearbyEnemies)) continue;

                rc.castFlash(flashLoc);
                return true;
            }
        }

        Direction toLoc = here.directionTo(loc);
        Direction[] tryDirs = new Direction[] { toLoc, toLoc.rotateLeft(), toLoc.rotateRight() };
        for (Direction dir : tryDirs) {
            if (!rc.canMove(dir)) continue;
            MapLocation moveLoc = here.add(dir);
            if (rc.getType().attackRadiusSquared < moveLoc.distanceSquaredTo(loc)) continue; // must engage in one turn
            if (inEnemyTowerOrHQRange(moveLoc, enemyTowers)) continue;
            if (!canTakeEnemiesAttackingLocation(moveLoc, nearbyEnemies)) continue;

            rc.move(dir);
            return true;
        }

        return false;
    }

    private static boolean tryMoveTowardLocationSafely(MapLocation loc, RobotInfo[] nearbyEnemies) throws GameActionException {
        Direction toLoc = here.directionTo(loc);
        Direction[] tryDirs = new Direction[] { toLoc, toLoc.rotateLeft(), toLoc.rotateRight() };
        for (Direction dir : tryDirs) {
            if (!rc.canMove(dir)) continue;
            MapLocation moveLoc = here.add(dir);
            if (inEnemyTowerOrHQRange(moveLoc, enemyTowers)) continue;
            if (!canTakeEnemiesAttackingLocation(moveLoc, nearbyEnemies)) continue;

            rc.move(dir);
            return true;
        }

        return false;
    }

    private static boolean tryMoveToEngageAnyUndefendedWorkerOrBuilding(RobotInfo[] nearbyEnemies) throws GameActionException {
        for (RobotInfo enemy : nearbyEnemies) {
            if (isWorkerOrBuilding(enemy.type)) {
                if (canWin1v1(enemy)) {
                    boolean canReach = true;
                    MapLocation loc = here;
                    while (loc.distanceSquaredTo(enemy.location) > rc.getType().attackRadiusSquared) {
                        Direction dir = loc.directionTo(enemy.location);
                        MapLocation newLoc = loc.add(dir);

                        if (!rc.isPathable(rc.getType(), newLoc) || inEnemyTowerOrHQRange(newLoc, enemyTowers)) {
                            canReach = false;
                            break;
                        }

                        boolean noOtherEnemiesAttackNewLoc = true;
                        for (RobotInfo otherEnemy : nearbyEnemies) {
                            if (otherEnemy != enemy
                                    && (otherEnemy.type.attackRadiusSquared >= newLoc.distanceSquaredTo(otherEnemy.location) || (otherEnemy.type == RobotType.MISSILE && 15 >= newLoc
                                            .distanceSquaredTo(otherEnemy.location)))) {
                                noOtherEnemiesAttackNewLoc = false;
                                break;
                            }
                        }

                        if (noOtherEnemiesAttackNewLoc) {
                            loc = newLoc;
                        } else {
                            canReach = false;
                            break;
                        }
                    }
                    if (canReach) {
                        Direction moveDir = here.directionTo(enemy.location);
                        rc.move(moveDir);
                        return true;
                    }
                }
            }
        }

        return false;
    }

    private static void shadowHarasser(RobotInfo enemyToShadow, RobotInfo[] nearbyEnemies) throws GameActionException {
        Direction toEnemy = here.directionTo(enemyToShadow.location);
        Direction[] dirs = new Direction[] { toEnemy, toEnemy.rotateRight(), toEnemy.rotateLeft(), toEnemy.rotateRight().rotateRight(),
                toEnemy.rotateLeft().rotateLeft() };
        for (Direction dir : dirs) {
            if (!rc.canMove(dir)) continue;

            MapLocation loc = here.add(dir);
            if (inEnemyTowerOrHQRange(loc, enemyTowers)) continue;

            boolean locIsSafe = true;

            for (RobotInfo enemy : nearbyEnemies) {
                if (enemy.type.attackRadiusSquared >= loc.distanceSquaredTo(enemy.location)
                        || (enemy.type == RobotType.MISSILE && 15 >= loc.distanceSquaredTo(enemy.location))) {
                    locIsSafe = false;
                    break;
                }
            }

            if (locIsSafe) {
                rc.move(dir);
                break;
            }
        }
    }

    // private static int numOtherAlliesInAttackRange(MapLocation loc) {
    // int ret = 0;
    // RobotInfo[] allies = rc.senseNearbyRobots(loc, 15, us);
    // for (RobotInfo ally : allies) {
    // if (ally.type.attackRadiusSquared >= loc.distanceSquaredTo(ally.location)) ret++;
    // }
    // return ret;
    // }

    private static boolean isHarasser(RobotType rt) {
        switch (rt) {
            case SOLDIER:
            case BASHER:
            case DRONE:
            case TANK:
                return true;

            default:
                return false;
        }
    }

    private static boolean isWorkerOrBuilding(RobotType rt) {
        switch (rt) {
            case MINER:
            case BEAVER:
            case AEROSPACELAB:
            case BARRACKS:
            case COMPUTER:
            case HANDWASHSTATION:
            case HELIPAD:
            case MINERFACTORY:
            case SUPPLYDEPOT:
            case TANKFACTORY:
            case TECHNOLOGYINSTITUTE:
            case TRAININGFIELD:
                return true;

            default:
                return false;
        }
    }

    static boolean inHealingState = false;

    public static void doHarass() throws GameActionException {
        Supply.shareSupply();
        Supply.requestResupplyIfNecessary();

        enemyTowers = rc.senseEnemyTowerLocations();

        Debug.indicate("micro", 2, "flash cooldown = " + rc.getFlashCooldown());

        RobotInfo[] nearbyEnemies = rc.senseNearbyRobots(35, them);

        if (rc.getHealth() < 0.25 * RobotType.COMMANDER.maxHealth) {
            inHealingState = true;
        }
        if (inHealingState) {
            if (rc.isCoreReady()) {
                Nav.goTo(ourHQ, new SafetyPolicyAvoidTowersAndHQ(enemyTowers));
            }
            if (rc.getHealth() > 0.9 * RobotType.COMMANDER.maxHealth) {
                inHealingState = false;
            }
            return;
        }

        boolean shadowEnemyHarassers = here.distanceSquaredTo(ourHQ) < here.distanceSquaredTo(theirHQ);
        if (doMicro(nearbyEnemies, shadowEnemyHarassers)) {
            return;
        }

        if (rc.isCoreReady()) {
            NavSafetyPolicy safetyPolicy = new SafetyPolicyCommanderHarass(nearbyEnemies);
            HarassNav.goTo(theirHQ, safetyPolicy);
        }
    }
}
