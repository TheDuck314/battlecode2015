package framework;

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

    private static void turn() throws GameActionException {
        updateStrategicInfo();

        if (rc.isWeaponReady()) attackEnemies();

        directStrategy();

        if (droneLoc != null && ourHQ.distanceSquaredTo(droneLoc) < GameConstants.SUPPLY_TRANSFER_RADIUS_SQUARED) {
            rc.transferSupplies((int) rc.getSupplyLevel(), droneLoc);
        } else {
            Supply.shareSupply();
        }
    }

    static int[] typeCounts;
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

    static int totalSupplyUpkeep;
    static double totalSupplyGenerated;
    static double supplyDepotsNeeded;

    static MapLocation droneLoc;

    private static void updateStrategicInfo() {
        numTowers = rc.senseTowerLocations().length;

        RobotInfo[] allAllies = rc.senseNearbyRobots(999999, us);

        totalSupplyUpkeep = 0;
        typeCounts = new int[RobotType.values().length];
        for (RobotInfo ally : allAllies) {
            typeCounts[ally.type.ordinal()]++;
            totalSupplyUpkeep += ally.type.supplyUpkeep;

            if (ally.type == RobotType.DRONE) droneLoc = ally.location;
        }

        numMinerFactories = typeCounts[RobotType.MINERFACTORY.ordinal()];
        numMiners = typeCounts[RobotType.MINER.ordinal()];
        numSoldiers = typeCounts[RobotType.SOLDIER.ordinal()];
        numHelipads = typeCounts[RobotType.HELIPAD.ordinal()];
        numAerospaceLabs = typeCounts[RobotType.AEROSPACELAB.ordinal()];
        numLaunchers = typeCounts[RobotType.LAUNCHER.ordinal()];
        numBeavers = typeCounts[RobotType.BEAVER.ordinal()];
        numBarracks = typeCounts[RobotType.BARRACKS.ordinal()];
        numTankFactories = typeCounts[RobotType.TANKFACTORY.ordinal()];
        numTanks = typeCounts[RobotType.TANK.ordinal()];
        numBashers = typeCounts[RobotType.BASHER.ordinal()];
        numSupplyDepots = typeCounts[RobotType.SUPPLYDEPOT.ordinal()];
        numDrones = typeCounts[RobotType.DRONE.ordinal()];

        totalSupplyGenerated = GameConstants.SUPPLY_GEN_BASE
                * (GameConstants.SUPPLY_GEN_MULTIPLIER + Math.pow(numSupplyDepots, GameConstants.SUPPLY_GEN_EXPONENT));
        if (totalSupplyUpkeep < GameConstants.SUPPLY_GEN_BASE * GameConstants.SUPPLY_GEN_MULTIPLIER) {
            supplyDepotsNeeded = 0;
        } else {
            supplyDepotsNeeded = Math.pow(totalSupplyUpkeep / GameConstants.SUPPLY_GEN_BASE - GameConstants.SUPPLY_GEN_MULTIPLIER,
                    1.0 / GameConstants.SUPPLY_GEN_EXPONENT);
        }

        Debug.indicate("supply", 0, "total supply upkeep = " + totalSupplyUpkeep);
        Debug.indicate("supply", 1, "total supply generated = " + totalSupplyGenerated);
        Debug.indicate("supply", 2, "supply depots needed = " + supplyDepotsNeeded);
    }

    private static void directStrategy() throws GameActionException {

        RobotType desiredBuilding;

        // Choose what building to make
        if (supplyDepotsNeeded > numSupplyDepots) {
            desiredBuilding = RobotType.SUPPLYDEPOT;
        } else if (numMinerFactories < 2) {
            desiredBuilding = RobotType.MINERFACTORY;
        } else if (numBarracks < 1) {
            desiredBuilding = RobotType.BARRACKS;
        } else if (numTankFactories < 2) {
            desiredBuilding = RobotType.TANKFACTORY;
        } else if (numHelipads < 1) {
            desiredBuilding = RobotType.HELIPAD;
        } else {
            desiredBuilding = RobotType.TANKFACTORY;
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

        if (numBeavers < 1 || (numMinerFactories >= 2 && numBeavers < 2)) {
            makeBeavers = true;
        }

        if (numMiners < 30 || numMiners < 0.5 * numBashers) {
            makeMiners = true;
        }

        if (numDrones < 1) {
            makeDrones = true;
        }

        makeTanks = true;

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
        if (!attackMode) {
            if (numTanks >= 15) {
                attackMode = true;
            }
        } else {
            if (numTanks <= 5) {
                attackMode = false;
            }
        }

        MapLocation rallyLoc = null;
        if (!attackMode) {
            rallyLoc = new MapLocation((ourHQ.x + theirHQ.x) / 2, (ourHQ.y + theirHQ.y) / 2);
        } else {
            MapLocation[] enemyTowers = rc.senseEnemyTowerLocations();
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
        if (numTowers >= 4) {
            splash = true;
            searchRadiusSq = 52;
        }

        RobotInfo[] nearbyEnemies = rc.senseNearbyRobots(searchRadiusSq, them);

        if (nearbyEnemies.length == 0) return;

        // TODO: prioritize targets by unit type
        MapLocation bestTarget = null;
        double bestDamage = 0.0;
        for (int i = nearbyEnemies.length; i-- > 0;) {
            RobotInfo enemy = nearbyEnemies[i];
            MapLocation target = enemy.location;
            double damage = 0.0;
            if (!splash) {
                if (enemy.health <= attackPower) damage = attackPower + 0.1;
                else damage = attackPower;
            } else { // splash
                boolean offTarget = false;
                if (ourHQ.distanceSquaredTo(target) > attackRangeSq) {
                    target = target.add(target.directionTo(ourHQ));
                    if (ourHQ.distanceSquaredTo(target) > attackRangeSq) continue;
                    offTarget = true;
                }
                int numSplashedEnemies = rc.senseNearbyRobots(target, GameConstants.HQ_BUFFED_SPLASH_RADIUS_SQUARED, them).length;
                if (offTarget) {
                    damage = numSplashedEnemies * GameConstants.HQ_BUFFED_SPLASH_RATE * attackPower;
                } else {
                    damage = attackPower + (numSplashedEnemies - 1) * GameConstants.HQ_BUFFED_SPLASH_RATE * attackPower;
                }
            }

            if (damage > bestDamage) {
                bestDamage = damage;
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
}
