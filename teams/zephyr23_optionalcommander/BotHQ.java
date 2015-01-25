package zephyr23_optionalcommander;

import battlecode.common.*;

public class BotHQ extends Bot {
    public static void loop(RobotController theRC) throws GameActionException {
        Bot.init(theRC);
        // Debug.init("analyze");
        MessageBoard.setDefaultChannelValues();
        init();
        while (true) {
            try {
                turn();
            } catch (Exception e) {
                e.printStackTrace();
            }
            rc.yield();
        }
    }

    static int gameNumberInMatch;
    static int game1Score;
    static int game2Score;

    static boolean attackMode = false;

    static int numTowers;
    static int numBarracks;
    static int numMiners;
    static int numSoldiers;
    static int numHelipads;
    static int numMinerFactories;
    static int numAerospaceLabs;
    static int numLaunchers;
    static int numBeavers;
    static int numTankFactories;
    static int numTanks;
    static int numBashers;
    static int numSupplyDepots;
    static int numDrones;
    static int numCommanders;
    static int numComputers;
    static int numHandwashStations;
    static int numTechInstitutes;
    static int numTrainingFields;

    static boolean haveAnIdleHelipad;
    static boolean haveAnIdleBarracks;
    static boolean haveAnIdleAerospaceLab;

    static boolean shouldBuildCommanderBasedOnMapAnalysis;

    static int nextCommanderOreCost = RobotType.COMMANDER.oreCost;
    static boolean commanderWasAlive = false;

    static int totalSupplyUpkeep;
    public static double totalSupplyGenerated;
    static double supplyDepotsNeeded;

    static MapLocation[] enemyTowers;

    private static void init() throws GameActionException {
        trySpawnBeaver();
        MessageBoard.DESIRED_BUILDING.writeRobotType(RobotType.MINERFACTORY);

        MapAnalysis.analyze();

        if (MapAnalysis.estimatedProtectedOre > 8000 || MapAnalysis.minProtectedMapFraction > 0.3) {
            shouldBuildCommanderBasedOnMapAnalysis = false;
        } else {
            shouldBuildCommanderBasedOnMapAnalysis = true;
        }

        rc.yield();
    }

    private static void turn() throws GameActionException {
        Supply.hqGiveSupply();

        updateStrategicInfo();

        if (rc.isWeaponReady()) attackEnemies();

        directStrategy();
    }

    private static void updateStrategicInfo() {
        enemyTowers = rc.senseEnemyTowerLocations();

        countAlliedUnits();

        double effectiveSupplyUpkeep = 1.3 * totalSupplyUpkeep;

        totalSupplyGenerated = GameConstants.SUPPLY_GEN_BASE
                * (GameConstants.SUPPLY_GEN_MULTIPLIER + Math.pow(numSupplyDepots, GameConstants.SUPPLY_GEN_EXPONENT));
        if (effectiveSupplyUpkeep < GameConstants.SUPPLY_GEN_BASE * GameConstants.SUPPLY_GEN_MULTIPLIER) {
            supplyDepotsNeeded = 0;
        } else {
            supplyDepotsNeeded = Math.pow(effectiveSupplyUpkeep / GameConstants.SUPPLY_GEN_BASE - GameConstants.SUPPLY_GEN_MULTIPLIER,
                    1.0 / GameConstants.SUPPLY_GEN_EXPONENT);
        }

        // Debug.indicate("supply", 0, "total supply upkeep = " + totalSupplyUpkeep);
        // Debug.indicate("supply", 1, "total supply generated = " + totalSupplyGenerated + ", effectiveSupplyUpkeep = " + effectiveSupplyUpkeep);
        // Debug.indicate("supply", 2, "supply depots needed = " + supplyDepotsNeeded);
    }

    private static void directStrategy() throws GameActionException {
        directStrategyOptionalCommanderIntoLaunchers();
    }

    private static void directStrategyOptionalCommanderIntoLaunchers() throws GameActionException {
        RobotType desiredBuilding;

        // Choose what building to make
        if (numMinerFactories < 1) {
            desiredBuilding = RobotType.MINERFACTORY;
        } else if (shouldBuildCommanderBasedOnMapAnalysis && numTechInstitutes < 1) {
            desiredBuilding = RobotType.TECHNOLOGYINSTITUTE;
        } else if (shouldBuildCommanderBasedOnMapAnalysis && numTrainingFields < 1) {
            desiredBuilding = RobotType.TRAININGFIELD;
        } else if (numHelipads < 1) {
            desiredBuilding = RobotType.HELIPAD;
        } else if (numAerospaceLabs < 1) {
            desiredBuilding = RobotType.AEROSPACELAB;
        } else {
            if (haveAnIdleAerospaceLab) {
                desiredBuilding = RobotType.HQ; // we already have enough production apparently
            } else {
                desiredBuilding = RobotType.AEROSPACELAB;
            }
        }
        if (supplyDepotsNeeded > numSupplyDepots) {
            if (rc.getTeamOre() < 1000) {
                desiredBuilding = RobotType.SUPPLYDEPOT;
            }
        }
        if (Clock.getRoundNum() > rc.getRoundLimit() - 150 && Clock.getRoundNum() <= rc.getRoundLimit() - 100) {
            desiredBuilding = RobotType.HANDWASHSTATION;
        }
        MessageBoard.DESIRED_BUILDING.writeRobotType(desiredBuilding);

        // Debug.indicate("orders", 0, "desiredBuilding = " + desiredBuilding.toString());

        // Choose what units to make
        boolean makeBashers = false;
        boolean makeBeavers = false;
        boolean makeCommander = false;
        boolean makeComputers = false;
        boolean makeDrones = false;
        boolean makeLaunchers = false;
        boolean makeMiners = false;
        boolean makeSoldiers = false;
        boolean makeTanks = false;

        int numBeaversNeeded = 1;
        if (numMinerFactories >= 1) numBeaversNeeded = 2;
        int missingSupplyDepots = 1 + (int) (supplyDepotsNeeded - numSupplyDepots);
        if (missingSupplyDepots > numBeaversNeeded) numBeaversNeeded = missingSupplyDepots;

        if (numBeavers < numBeaversNeeded) {
            makeBeavers = true;
        }

        if (numMiners < 30) {
            makeMiners = true;
        }

        if (numDrones < 1) {
            makeDrones = true;
        }

        if (rc.hasCommander()) {
            commanderWasAlive = true;
        } else {
            if (commanderWasAlive) {
                nextCommanderOreCost *= 2;
                commanderWasAlive = false;
            }
            if (nextCommanderOreCost <= 400) {
                makeCommander = true;
            }
        }

        makeLaunchers = true;

        MessageBoard.CONSTRUCTION_ORDERS.writeConstructionOrder(RobotType.BASHER, makeBashers);
        MessageBoard.CONSTRUCTION_ORDERS.writeConstructionOrder(RobotType.COMMANDER, makeCommander);
        MessageBoard.CONSTRUCTION_ORDERS.writeConstructionOrder(RobotType.COMPUTER, makeComputers);
        MessageBoard.CONSTRUCTION_ORDERS.writeConstructionOrder(RobotType.DRONE, makeDrones);
        MessageBoard.CONSTRUCTION_ORDERS.writeConstructionOrder(RobotType.LAUNCHER, makeLaunchers);
        MessageBoard.CONSTRUCTION_ORDERS.writeConstructionOrder(RobotType.MINER, makeMiners);
        MessageBoard.CONSTRUCTION_ORDERS.writeConstructionOrder(RobotType.SOLDIER, makeSoldiers);
        MessageBoard.CONSTRUCTION_ORDERS.writeConstructionOrder(RobotType.TANK, makeTanks);

        if (makeBeavers) {
            if (rc.isCoreReady()) trySpawnBeaver();
        }

        setRallyLoc();
    }

    private static void directStrategySoldiersIntoLaunchers() throws GameActionException {
        RobotType desiredBuilding;

        // Choose what building to make
        if (numMinerFactories < 1) {
            desiredBuilding = RobotType.MINERFACTORY;
        } else if (numBarracks < 1) {
            desiredBuilding = RobotType.BARRACKS;
        } else if (numHelipads < 1) {
            desiredBuilding = RobotType.HELIPAD;
        } else if (numAerospaceLabs < 1) {
            desiredBuilding = RobotType.AEROSPACELAB;
        } else {
            if (haveAnIdleAerospaceLab && haveAnIdleBarracks) {
                desiredBuilding = RobotType.HQ; // we already have enough production apparently
            } else if (haveAnIdleAerospaceLab && !haveAnIdleBarracks) {
                desiredBuilding = RobotType.BARRACKS; // barracks are all busy, build more
            } else if (haveAnIdleBarracks && !haveAnIdleAerospaceLab) {
                desiredBuilding = RobotType.AEROSPACELAB; // aerospace labs are all busy, build more
            } else {
                // all barracks and aerospace labs are busy
                if (numAerospaceLabs < numBarracks) {
                    desiredBuilding = RobotType.AEROSPACELAB;
                } else {
                    desiredBuilding = RobotType.BARRACKS;
                }
            }
        }
        if (supplyDepotsNeeded > numSupplyDepots) {
            if (rc.getTeamOre() < 1000) {
                desiredBuilding = RobotType.SUPPLYDEPOT;
            }
        }
        if (Clock.getRoundNum() > rc.getRoundLimit() - 150 && Clock.getRoundNum() <= rc.getRoundLimit() - 100) {
            desiredBuilding = RobotType.HANDWASHSTATION;
        }
        MessageBoard.DESIRED_BUILDING.writeRobotType(desiredBuilding);

        // Debug.indicate("orders", 0, "desiredBuilding = " + desiredBuilding.toString());

        // Choose what units to make
        boolean makeBashers = false;
        boolean makeBeavers = false;
        boolean makeCommanders = false;
        boolean makeComputers = false;
        boolean makeDrones = false;
        boolean makeLaunchers = false;
        boolean makeMiners = false;
        boolean makeSoldiers = false;
        boolean makeTanks = false;

        int numBeaversNeeded = 1;
        if (numMinerFactories >= 1) numBeaversNeeded = 2;
        int missingSupplyDepots = 1 + (int) (supplyDepotsNeeded - numSupplyDepots);
        if (missingSupplyDepots > numBeaversNeeded) numBeaversNeeded = missingSupplyDepots;

        if (numBeavers < numBeaversNeeded) {
            makeBeavers = true;
        }

        if (numMiners < 30) {
            makeMiners = true;
        }

        if (numDrones < 1) {
            makeDrones = true;
        }

        if (rc.getTeamOre() > 600) {
            makeLaunchers = true;
            makeSoldiers = true;
        } else {
            if (Clock.getRoundNum() < 460) {
                makeSoldiers = true;
            } else {
                if (6 * numLaunchers < numSoldiers) {
                    makeLaunchers = true;
                } else {
                    makeSoldiers = true;
                }
            }
        }
        if (desiredBuilding == RobotType.SUPPLYDEPOT) {
            // streaming soldiers can delay supply depots by using up all the ore
            if (rc.getTeamOre() < RobotType.SUPPLYDEPOT.oreCost + 0.5 * RobotType.SOLDIER.oreCost * numBarracks) {
                makeSoldiers = false;
            }
        }

        MessageBoard.CONSTRUCTION_ORDERS.writeConstructionOrder(RobotType.BASHER, makeBashers);
        MessageBoard.CONSTRUCTION_ORDERS.writeConstructionOrder(RobotType.COMMANDER, makeCommanders);
        MessageBoard.CONSTRUCTION_ORDERS.writeConstructionOrder(RobotType.COMPUTER, makeComputers);
        MessageBoard.CONSTRUCTION_ORDERS.writeConstructionOrder(RobotType.DRONE, makeDrones);
        MessageBoard.CONSTRUCTION_ORDERS.writeConstructionOrder(RobotType.LAUNCHER, makeLaunchers);
        MessageBoard.CONSTRUCTION_ORDERS.writeConstructionOrder(RobotType.MINER, makeMiners);
        MessageBoard.CONSTRUCTION_ORDERS.writeConstructionOrder(RobotType.SOLDIER, makeSoldiers);
        MessageBoard.CONSTRUCTION_ORDERS.writeConstructionOrder(RobotType.TANK, makeTanks);

        if (makeBeavers) {
            if (rc.isCoreReady()) trySpawnBeaver();
        }

        setRallyLoc();
    }

    private static void directStrategyPureLaunchers() throws GameActionException {
        RobotType desiredBuilding;

        // Choose what building to make
        if (numMinerFactories < 1) {
            desiredBuilding = RobotType.MINERFACTORY;
        } else if (numHelipads < 1) {
            desiredBuilding = RobotType.HELIPAD;
        } else if (numAerospaceLabs < 2) {
            desiredBuilding = RobotType.AEROSPACELAB;
        } else {
            if (!haveAnIdleAerospaceLab) {
                desiredBuilding = RobotType.AEROSPACELAB;
            } else {
                desiredBuilding = RobotType.HQ;
            }
        }
        if (supplyDepotsNeeded > numSupplyDepots) {
            if (rc.getTeamOre() < 1000) {
                desiredBuilding = RobotType.SUPPLYDEPOT;
            }
        }
        if (Clock.getRoundNum() > rc.getRoundLimit() - 150 && Clock.getRoundNum() <= rc.getRoundLimit() - 100) {
            desiredBuilding = RobotType.HANDWASHSTATION;
        }
        MessageBoard.DESIRED_BUILDING.writeRobotType(desiredBuilding);

        // Debug.indicate("orders", 0, "desiredBuilding = " + desiredBuilding.toString());

        // Choose what units to make
        boolean makeBashers = false;
        boolean makeBeavers = false;
        boolean makeCommanders = false;
        boolean makeComputers = false;
        boolean makeDrones = false;
        boolean makeLaunchers = false;
        boolean makeMiners = false;
        boolean makeSoldiers = false;
        boolean makeTanks = false;

        int numBeaversNeeded = 1;
        if (numMinerFactories >= 1) numBeaversNeeded = 2;
        int missingSupplyDepots = 1 + (int) (supplyDepotsNeeded - numSupplyDepots);
        if (missingSupplyDepots > numBeaversNeeded) numBeaversNeeded = missingSupplyDepots;

        if (numBeavers < numBeaversNeeded) {
            makeBeavers = true;
        }

        if (numMiners < 30) {
            makeMiners = true;
        }

        if (numDrones < 1) {
            makeDrones = true;
        }

        makeLaunchers = true;

        MessageBoard.CONSTRUCTION_ORDERS.writeConstructionOrder(RobotType.BASHER, makeBashers);
        MessageBoard.CONSTRUCTION_ORDERS.writeConstructionOrder(RobotType.COMMANDER, makeCommanders);
        MessageBoard.CONSTRUCTION_ORDERS.writeConstructionOrder(RobotType.COMPUTER, makeComputers);
        MessageBoard.CONSTRUCTION_ORDERS.writeConstructionOrder(RobotType.DRONE, makeDrones);
        MessageBoard.CONSTRUCTION_ORDERS.writeConstructionOrder(RobotType.LAUNCHER, makeLaunchers);
        MessageBoard.CONSTRUCTION_ORDERS.writeConstructionOrder(RobotType.MINER, makeMiners);
        MessageBoard.CONSTRUCTION_ORDERS.writeConstructionOrder(RobotType.SOLDIER, makeSoldiers);
        MessageBoard.CONSTRUCTION_ORDERS.writeConstructionOrder(RobotType.TANK, makeTanks);

        if (makeBeavers) {
            if (rc.isCoreReady()) trySpawnBeaver();
        }

        setRallyLoc();
    }

    private static void setRallyLoc() throws GameActionException {
        // Choose the rally point
        attackMode = true;

        MapLocation rallyLoc = null;
        if (!attackMode) {
            rallyLoc = new MapLocation((ourHQ.x + theirHQ.x) / 2, (ourHQ.y + theirHQ.y) / 2);
        } else {
            boolean goForHQ;
            if (enemyTowers.length == 0) {
                goForHQ = true;
            } else {
                // if we are ahead by at least two towers and their HQ is sufficiently debuffed,
                // try to end as quickly as possible. However if it gets too late only try this
                // if we are ahead in towers; it's more important not to lose on tower count
                // Also don't try this if we don't have enough launchers to end relatively quickly.
                if (numLaunchers >= 5 && enemyTowers.length <= 3 && ((Clock.getRoundNum() < rc.getRoundLimit() - 600) || (numTowers >= enemyTowers.length + 2))) {
                    goForHQ = true;
                } else {
                    goForHQ = false;
                }
            }

            if (goForHQ) {
                rallyLoc = theirHQ;
            } else {
                for (MapLocation tower : enemyTowers) {
                    if (rallyLoc == null || ourHQ.distanceSquaredTo(tower) < ourHQ.distanceSquaredTo(rallyLoc)) {
                        rallyLoc = tower;
                    }
                }
            }
        }
        MessageBoard.RALLY_LOC.writeMapLocation(rallyLoc);
    }

    // Unbuffed attack range^2 is 24 (same as towers)
    // At two towers range is buffed to 35
    // At five towers HQ does splash damage
    // ........ ........ ........
    // ........ ........ sssss...
    // ........ XXXX.... XXXXss..
    // XXX..... XXXXX... XXXXXss.
    // XXXX.... XXXXXX.. XXXXXXs.
    // XXXXX... XXXXXX.. XXXXXXs.
    // XXXXX... XXXXXX.. XXXXXXs.
    // HXXXX... HXXXXX.. HXXXXXs.
    // With splash damage, the HQ can damage a unit at a range^2 of 52
    // (but can't damage all units within that radius).
    //
    // Other ranges:
    // Basher (2):
    // ...
    // XX.
    // OX.
    //
    // Soldier, Beaver, Miner (5):
    // ....
    // XX..
    // XXX.
    // OXX.
    //
    // Drone, Commander (10):
    // .....
    // XX...
    // XXX..
    // XXXX.
    // OXXX.
    //
    // Tank (15):
    // .....
    // XXX..
    // XXXX.
    // XXXX.
    // OXXX.
    private static void attackEnemies() throws GameActionException {
        int attackRangeSq = numTowers < 2 ? RobotType.HQ.attackRadiusSquared : GameConstants.HQ_BUFFED_ATTACK_RADIUS_SQUARED;

        boolean splash = false;
        int searchRadiusSq = attackRangeSq;
        if (numTowers >= 5) {
            splash = true;
            searchRadiusSq = 52;
        }

        RobotInfo[] nearbyEnemies = rc.senseNearbyRobots(searchRadiusSq, them);

        if (nearbyEnemies.length == 0) return;

        // TODO: prioritize targets by unit type
        MapLocation bestTarget = null;
        double minHealth = 999999;
        for (int i = nearbyEnemies.length; i-- > 0;) {
            RobotInfo enemy = nearbyEnemies[i];
            MapLocation target = enemy.location;
            if (splash) {
                if (ourHQ.distanceSquaredTo(target) > attackRangeSq) {
                    target = target.add(target.directionTo(ourHQ));
                    if (ourHQ.distanceSquaredTo(target) > attackRangeSq) continue;
                }
            }

            if (enemy.health < minHealth) {
                minHealth = enemy.health;
                bestTarget = target;
            }
        }

        if (bestTarget != null) {
            rc.attackLocation(bestTarget);
        }
    }

    private static void trySpawnBeaver() throws GameActionException {
        if (rc.getTeamOre() < RobotType.BEAVER.oreCost) return;

        Direction[] dirs = Direction.values();
        Direction bestDir = null;
        double maxOre = -1;
        for (Direction dir : dirs) {
            if (rc.canSpawn(dir, RobotType.BEAVER)) {
                double ore = rc.senseOre(ourHQ.add(dir));
                if (ore > maxOre) {
                    maxOre = ore;
                    bestDir = dir;
                }
            }
        }

        if (bestDir != null) {
            rc.spawn(bestDir, RobotType.BEAVER);
        }
    }

    private static void countAlliedUnits() {
        RobotInfo[] allAllies = rc.senseNearbyRobots(999999, us);

        totalSupplyUpkeep = 0;

        numAerospaceLabs = numBarracks = numBashers = numBeavers = numCommanders = numComputers = numDrones = numHandwashStations = numHelipads = numLaunchers = numMiners = numMinerFactories = numSoldiers = numSupplyDepots = numTanks = numTankFactories = numTechInstitutes = numTowers = numTrainingFields = 0;

        haveAnIdleBarracks = haveAnIdleHelipad = haveAnIdleAerospaceLab = false;

        for (int i = allAllies.length; i-- > 0;) {
            RobotType allyType = allAllies[i].type;
            switch (allyType) {
                case AEROSPACELAB:
                    numAerospaceLabs++;
                    if (allAllies[i].builder == null && allAllies[i].coreDelay <= 5) haveAnIdleAerospaceLab = true;
                    break;

                case BARRACKS:
                    numBarracks++;
                    if (allAllies[i].builder == null && allAllies[i].coreDelay <= 2) haveAnIdleBarracks = true;
                    break;

                case BASHER:
                    numBashers++;
                    break;

                case BEAVER:
                    numBeavers++;
                    break;

                case COMMANDER:
                    numCommanders++;
                    break;

                case COMPUTER:
                    numComputers++;
                    break;

                case DRONE:
                    numDrones++;
                    break;

                case HANDWASHSTATION:
                    numHandwashStations++;
                    break;

                case HELIPAD:
                    if (allAllies[i].builder == null && allAllies[i].coreDelay <= 3) haveAnIdleHelipad = true;
                    numHelipads++;
                    break;

                case HQ:
                    break;

                case LAUNCHER:
                    numLaunchers++;
                    break;

                case MINER:
                    numMiners++;
                    break;

                case MINERFACTORY:
                    numMinerFactories++;
                    break;

                case MISSILE:
                    break;

                case SOLDIER:
                    numSoldiers++;
                    break;

                case SUPPLYDEPOT:
                    numSupplyDepots++;
                    break;

                case TANK:
                    numTanks++;
                    break;

                case TANKFACTORY:
                    numTankFactories++;
                    break;

                case TECHNOLOGYINSTITUTE:
                    numTechInstitutes++;
                    break;

                case TOWER:
                    numTowers++;
                    break;

                case TRAININGFIELD:
                    numTrainingFields++;
                    break;
            }

            totalSupplyUpkeep += allyType.supplyUpkeep;
        }
    }
}
