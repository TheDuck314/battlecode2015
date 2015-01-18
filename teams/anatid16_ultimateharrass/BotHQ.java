package anatid16_ultimateharrass;

import battlecode.common.*;

public class BotHQ extends Bot {
    public static void loop(RobotController theRC) throws GameActionException {
        Bot.init(theRC);
        MessageBoard.setDefaultChannelValues();
        Debug.init("supply");
        while (true) {
            try {
                turn();
            } catch (Exception e) {
                e.printStackTrace();
            }
            rc.yield();
        }
    }

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

    static int totalSupplyUpkeep;
    public static double totalSupplyGenerated;
    static double supplyDepotsNeeded;

    private static void turn() throws GameActionException {
        Supply.hqGiveSupply();

        updateStrategicInfo();

        if (rc.isWeaponReady()) attackEnemies();

        directStrategyDronesIntoLaunchers();

    }

    private static void updateStrategicInfo() {
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

         Debug.indicate("supply", 0, "total supply upkeep = " + totalSupplyUpkeep);
         Debug.indicate("supply", 1, "total supply generated = " + totalSupplyGenerated + ", effectiveSupplyUpkeep = " + effectiveSupplyUpkeep);
         Debug.indicate("supply", 2, "supply depots needed = " + supplyDepotsNeeded);
    }
    
    private static void directStrategyDronesIntoLaunchers() throws GameActionException {
        RobotType desiredBuilding;

        // Choose what building to make
        if (numMinerFactories < 1) {
            desiredBuilding = RobotType.MINERFACTORY;
        } else if (numHelipads < 2) {
            desiredBuilding = RobotType.HELIPAD;
        } else if (numAerospaceLabs <= numHelipads) {
            desiredBuilding = RobotType.AEROSPACELAB;
        } else {
            desiredBuilding = RobotType.HELIPAD;
        }
        if (supplyDepotsNeeded > numSupplyDepots) {
            if (rc.getTeamOre() < 1000) {
                desiredBuilding = RobotType.SUPPLYDEPOT;
            }
        }
        if (Clock.getRoundNum() > 1850 && Clock.getRoundNum() <= 1900) {
            desiredBuilding = RobotType.HANDWASHSTATION;
        }
        MessageBoard.DESIRED_BUILDING.writeRobotType(desiredBuilding);

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
        int missingSupplyDepots = 1 + (int) (supplyDepotsNeeded - numSupplyDepots);
        if (missingSupplyDepots > numBeaversNeeded) numBeaversNeeded = missingSupplyDepots;

        if (numBeavers < numBeaversNeeded) {
            makeBeavers = true;
        }

        if (numMiners < 30) {
            makeMiners = true;
        }

        if (rc.getTeamOre() > 600) {
            makeLaunchers = true;
            makeDrones = true;
        } else {
            /*if (numAerospaceLabs == 0) {
                // have to stop making drones for a bit at some point so we get money for the aerospace lab
                if (numDrones < 10 && Clock.getRoundNum() < 380) {
                    makeDrones = true;
                }
            }*/
            if(Clock.getRoundNum() < 500) {
                makeDrones = true;
            } else {
                if (3 * numLaunchers < numDrones) {
                    makeLaunchers = true;
                } else {
                    makeDrones = true;
                }
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

        // Choose the rally point
        attackMode = true;

        MapLocation[] enemyTowers = rc.senseEnemyTowerLocations();
        MapLocation rallyLoc = null;
        if (!attackMode) {
            rallyLoc = new MapLocation((ourHQ.x + theirHQ.x) / 2, (ourHQ.y + theirHQ.y) / 2);
        } else {
            if (enemyTowers.length == 0) {
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
        // Debug.indicate("rally", 0, "rally loc = " + rallyLoc.toString());

        if (Clock.getRoundNum() > 1940) {
            int numAlliedTowers = rc.senseTowerLocations().length;
            if (numAlliedTowers <= enemyTowers.length) {
                MessageBoard.FINAL_ATTACK_SIGNAL.writeBoolean(true);
            }
        }
    }

    private static void directStrategySoldiers() throws GameActionException {
        RobotType desiredBuilding;

        // Choose what building to make
        if (numMinerFactories < 1) {
            desiredBuilding = RobotType.MINERFACTORY;
        } else if (numBarracks < 1) {
            desiredBuilding = RobotType.BARRACKS;
        } else if (numHelipads < 1) {
            desiredBuilding = RobotType.HELIPAD;
        } else {
            desiredBuilding = RobotType.BARRACKS;
        }
        if (2 * supplyDepotsNeeded > numSupplyDepots) {
            if (rc.getTeamOre() < 1000) {
                desiredBuilding = RobotType.SUPPLYDEPOT;
            }
        }
        if (Clock.getRoundNum() > 1850 && Clock.getRoundNum() <= 1900) {
            desiredBuilding = RobotType.HANDWASHSTATION;
        }
        MessageBoard.DESIRED_BUILDING.writeRobotType(desiredBuilding);

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
        int missingSupplyDepots = 1 + (int) (2 * supplyDepotsNeeded - numSupplyDepots);
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

        makeSoldiers = true;
        if (1.2 * supplyDepotsNeeded > numSupplyDepots) {
            if (rc.getTeamOre() < 100 + RobotType.SOLDIER.oreCost * numBarracks) {
                // don't make soldiers if it would prevent us from accumulating the ore for a supply depot
                makeSoldiers = false;
            }
        }
        if (numHelipads > 0 && numDrones == 0) {
            if (rc.getTeamOre() < RobotType.DRONE.oreCost + RobotType.SOLDIER.oreCost * numBarracks) {
                // don't make soldiers if it would prevent us from accumulating the ore for a drone
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

        // Choose the rally point
        attackMode = true;

        MapLocation[] enemyTowers = rc.senseEnemyTowerLocations();
        MapLocation rallyLoc = null;
        if (!attackMode) {
            rallyLoc = new MapLocation((ourHQ.x + theirHQ.x) / 2, (ourHQ.y + theirHQ.y) / 2);
        } else {
            if (enemyTowers.length == 0) {
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
        // Debug.indicate("rally", 0, "rally loc = " + rallyLoc.toString());

        if (Clock.getRoundNum() > 1940) {
            int numAlliedTowers = rc.senseTowerLocations().length;
            if (numAlliedTowers <= enemyTowers.length) {
                MessageBoard.FINAL_ATTACK_SIGNAL.writeBoolean(true);
            }
        }
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

        // Attack damage is buffed by 50% at 3 towers and 1000% at 6 towers
        double attackPower = RobotType.HQ.attackPower;
        if (numTowers >= 6) attackPower *= GameConstants.HQ_BUFFED_DAMAGE_MULTIPLIER_LEVEL_2;
        else if (numTowers >= 3) attackPower *= GameConstants.HQ_BUFFED_DAMAGE_MULTIPLIER_LEVEL_1;

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

        for (int i = allAllies.length; i-- > 0;) {
            RobotType allyType = allAllies[i].type;
            switch (allyType) {
                case AEROSPACELAB:
                    numAerospaceLabs++;
                    break;

                case BARRACKS:
                    numBarracks++;
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
