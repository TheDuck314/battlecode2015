package anatid18_strats_onehelipad400;

import battlecode.common.*;

public class Supply extends Bot {
    public static void shareSupply() throws GameActionException {
        RobotInfo[] nearbyAllies = rc.senseNearbyRobots(GameConstants.SUPPLY_TRANSFER_RADIUS_SQUARED, us);

        double mySupply = rc.getSupplyLevel();
        int myUpkeep = rc.getType().supplyUpkeep;
        double myTurnsOfSupplyLeft = mySupply / myUpkeep;

        if (myTurnsOfSupplyLeft < 4) return; // no sense spending bytecodes sharing if there's not much to share

        int resupplyDroneID = MessageBoard.RESUPPLY_DRONE_ID.readInt();

        RobotInfo allyToSupply = null;
        double minTurnsOfSupplyLeft = myTurnsOfSupplyLeft;
        for (RobotInfo ally : nearbyAllies) {
            if (needsSupply(ally.type) && ally.ID != resupplyDroneID) {
                double allyTurnsOfSupplyLeft = ally.supplyLevel / ally.type.supplyUpkeep;
                if (allyTurnsOfSupplyLeft < minTurnsOfSupplyLeft) {
                    minTurnsOfSupplyLeft = allyTurnsOfSupplyLeft;
                    allyToSupply = ally;
                }
            }
        }

        if (allyToSupply != null) {
            double allySupply = allyToSupply.supplyLevel;
            int allyUpkeep = allyToSupply.type.supplyUpkeep;

            // we solve: (my supply - x) / (my upkeep) = (ally supply + x) / (ally upkeep)
            double transferAmount = (mySupply * allyUpkeep - allySupply * myUpkeep) / (myUpkeep + allyUpkeep);

            if (transferAmount > 20) {
                Debug.indicate("supply", 2, "transferring " + (int) transferAmount + " to " + allyToSupply.location.toString() + " to even things up");
                rc.transferSupplies((int) transferAmount, allyToSupply.location);
            }
        }
    }

    // turn mod 3 = 0 -> bots compete to determine max supply need
    // turn mod 3 = 1 -> resupply drone(s) read max supply need
    // turn mod 3 = 2 -> resupply drone(s) reset max supply need comms channels

    static int numTurnsSupplyRequestUnfulfilled = 0;

    public static void requestResupplyIfNecessary() throws GameActionException {
        if (Clock.getRoundNum() % 3 == 0) return; // can only request supply on certain turns

        double travelTimeFromHQ = Math.sqrt(here.distanceSquaredTo(ourHQ));

        // lookaheadTurns increases as our supply request remains unfulfilled, giving
        // us higher and higher priority over time
        double lookaheadTurns = 2.0 * travelTimeFromHQ;
        int mySupplyNeeded = (int) (lookaheadTurns * rc.getType().supplyUpkeep - rc.getSupplyLevel());

        if (mySupplyNeeded <= 0) {
            numTurnsSupplyRequestUnfulfilled = 0;
            return;
        } else {
            numTurnsSupplyRequestUnfulfilled++;
        }

        int totalSupplyUpkeepNearby = rc.getType().supplyUpkeep;
        double totalSupplyNearby = rc.getSupplyLevel();

        RobotInfo[] nearbyAllies = rc.senseNearbyRobots(GameConstants.SUPPLY_TRANSFER_RADIUS_SQUARED, us);
        for (RobotInfo ally : nearbyAllies) {
            totalSupplyUpkeepNearby += ally.type.supplyUpkeep;
            totalSupplyNearby += ally.supplyLevel;
        }

        int supplyRequestSize = (int) ((lookaheadTurns + numTurnsSupplyRequestUnfulfilled) * totalSupplyUpkeepNearby - totalSupplyNearby);

        Debug.indicate("supply", 0, " supplyUpkeepNearby = " + totalSupplyUpkeepNearby + "; supplyNearby = " + totalSupplyNearby);
        Debug.indicate("supply", 1, "supply requestSize: " + supplyRequestSize + "; turns unfulfilled = " + numTurnsSupplyRequestUnfulfilled);

        if (supplyRequestSize > MessageBoard.MAX_SUPPLY_NEEDED.readInt()) {
            MessageBoard.MAX_SUPPLY_NEEDED.writeInt(supplyRequestSize);
            MessageBoard.NEEDIEST_SUPPLY_LOC.writeMapLocation(here);
        }
    }

    static MapLocation supplyRunnerDest = null;
    static double supplyRunnerNeed = 0;
    static boolean onSupplyRun = true;

    public static void runSupplies() throws GameActionException {

        MessageBoard.RESUPPLY_DRONE_ID.writeInt(rc.getID());

        // read supply needs
        if (Clock.getRoundNum() % 3 == 1) {
            supplyRunnerNeed = MessageBoard.MAX_SUPPLY_NEEDED.readInt();
            if (supplyRunnerNeed > 0) {
                supplyRunnerDest = MessageBoard.NEEDIEST_SUPPLY_LOC.readMapLocation();
                Debug.indicate("supply", 0, "max supply needed = " + supplyRunnerNeed + " at " + supplyRunnerDest.toString());
            } else {
                supplyRunnerDest = null;
                Debug.indicate("supply", 0, "no supply need");
            }
        }

        // reset supply need comms channels
        if (Clock.getRoundNum() % 3 == 2) {
            MessageBoard.MAX_SUPPLY_NEEDED.writeInt(0);
        }

        if (supplyRunnerDest != null && here.distanceSquaredTo(supplyRunnerDest) < 35) {
            if (supplyRunnerTransferSupplyAtDest()) {
                onSupplyRun = false; // supplies have been dropped off; return to HQ
            }
        } else {
            // try helping out whoever we encounter on the way to the main destination
            supplyRunnerTryOpportunisticTransferSupply();
        }

        if (onSupplyRun) {
            // call off a supply run if the need vanishes or if we run out of spare supply
            if (supplyRunnerNeed == 0 || supplyRunnerSpareSupplyAmount() <= 0) {
                onSupplyRun = false;
            }
        } else {
            // start a supply run when there is need and we have enough supply to fulfill it
            if (supplyRunnerNeed > 0) {
                double supplyNeededForRun = supplyRunnerNeed + RobotType.DRONE.supplyUpkeep * Math.sqrt(ourHQ.distanceSquaredTo(supplyRunnerDest));
                if (rc.getSupplyLevel() > supplyNeededForRun) {
                    onSupplyRun = true;
                } else {
                    rc.setIndicatorLine(here, supplyRunnerDest, 255, 0, 0);
                }
            }
        }

        MapLocation[] enemyTowers = rc.senseEnemyTowerLocations();
        RobotInfo[] nearbyEnemies = rc.senseNearbyRobots(35, them);
        NavSafetyPolicy safetyPolicy = new SafetyPolicyAvoidAllUnits(enemyTowers, nearbyEnemies);
        if (onSupplyRun) {
            NewNav.goTo(supplyRunnerDest, safetyPolicy);
            Debug.indicate("supply", 2, "going to supply dest");
            rc.setIndicatorLine(here, supplyRunnerDest, 0, 255, 0);
        } else {
            NewNav.goTo(ourHQ, safetyPolicy);
            Debug.indicate("supply", 2, "returning to HQ");
        }
    }

    private static boolean supplyRunnerTransferSupplyAtDest() throws GameActionException {
        int transferAmount = (int) supplyRunnerSpareSupplyAmount();
        if (transferAmount <= 0) return true; // we didn't succeed but we are out of supply so it's like we succeeded

        RobotInfo[] nearbyAllies = rc.senseNearbyRobots(GameConstants.SUPPLY_TRANSFER_RADIUS_SQUARED, us);

        double minSupply = 1e99;
        RobotInfo allyToSupply = null;
        for (RobotInfo ally : nearbyAllies) {
            if (needsSupply(ally.type)) {
                if (ally.supplyLevel < minSupply) {
                    minSupply = ally.supplyLevel;
                    allyToSupply = ally;
                }
            }
        }

        if (allyToSupply != null) {
            Debug.indicate("supply", 1, "dropping off " + transferAmount + " supplies at destination");
            rc.transferSupplies(transferAmount, allyToSupply.location);
            return true;
        } else {
            return false;
        }
    }

    // We're not at our main supply destination, but we give people we encounter
    // on the way however much they need. However we don't give them all of our
    // supply because we are saving it for the main destination.
    private static void supplyRunnerTryOpportunisticTransferSupply() throws GameActionException {
        RobotInfo[] nearbyAllies = rc.senseNearbyRobots(GameConstants.SUPPLY_TRANSFER_RADIUS_SQUARED, us);

        double travelTimeFromHQ = Math.sqrt(here.distanceSquaredTo(ourHQ));

        double transferAmount = 0;
        RobotInfo transferTarget = null;
        for (RobotInfo ally : nearbyAllies) {
            if (needsSupply(ally.type)) {
                double supplyNeed = 2 * travelTimeFromHQ * ally.type.supplyUpkeep - ally.supplyLevel;
                if (supplyNeed > transferAmount) {
                    transferAmount = supplyNeed;
                    transferTarget = ally;
                }
            }
        }

        if (transferTarget != null) {
            transferAmount = Math.min(transferAmount, supplyRunnerSpareSupplyAmount());
            if (transferAmount > 1) {
                Debug.indicate("supply", 1, "opportunistically transferring " + transferAmount + " to " + transferTarget.location);
                rc.transferSupplies((int) transferAmount, transferTarget.location);
            }
        }
    }

    private static double supplyRunnerSpareSupplyAmount() {
        return rc.getSupplyLevel() - 2 * RobotType.DRONE.supplyUpkeep * Math.sqrt(here.distanceSquaredTo(ourHQ));
    }

    private static boolean needsSupply(RobotType rt) {
        return !rt.isBuilding && rt != RobotType.BEAVER && rt != RobotType.MISSILE;
    }

    static double hqLastSupply = 0;
    static double hqSupplyReservedForResupplyDrone = 0;
    static final double HQ_SUPPLY_DRONE_RESERVE_RATIO = 0.5;
    static final double HQ_SUPPLY_TURN_BUILDUP_LIMIT = 100.0;

    public static void hqGiveSupply() throws GameActionException {
        // reserve a fraction of the supply just generated for the resupply drone
        hqSupplyReservedForResupplyDrone += HQ_SUPPLY_DRONE_RESERVE_RATIO * BotHQ.totalSupplyGenerated;

        // need to make sure vast amounts of supply don't build up unused if only the supply drone is visiting the HQ.
        // so every turn a fraction of the unreserved supply is reserved for the drone
        hqSupplyReservedForResupplyDrone += (rc.getSupplyLevel() - hqSupplyReservedForResupplyDrone) / HQ_SUPPLY_TURN_BUILDUP_LIMIT;

        Debug.indicate("supply", 0, "supply reserved for drone = " + hqSupplyReservedForResupplyDrone);

        // feed the resupply drone if it's around
        int resupplyDroneID = MessageBoard.RESUPPLY_DRONE_ID.readInt();
        if (rc.canSenseRobot(resupplyDroneID)) {
            MapLocation resupplyDroneLoc = rc.senseRobot(resupplyDroneID).location;
            if (ourHQ.distanceSquaredTo(resupplyDroneLoc) <= GameConstants.SUPPLY_TRANSFER_RADIUS_SQUARED) {
                Debug.indicate("supply", 1, "transferring " + hqSupplyReservedForResupplyDrone + " to resupply drone");
                rc.transferSupplies((int) hqSupplyReservedForResupplyDrone, resupplyDroneLoc);
                hqSupplyReservedForResupplyDrone = 0;
            }
        }

        // feed nearby robots whatever supply is not reserved for the resupply drone
        RobotInfo[] nearbyAllies = rc.senseNearbyRobots(GameConstants.SUPPLY_TRANSFER_RADIUS_SQUARED, us);
        double minTurnsOfSupplyLeft = 1e99;
        RobotInfo allyToSupply = null;
        for (RobotInfo ally : nearbyAllies) {
            if (needsSupply(ally.type) && ally.ID != resupplyDroneID) {
                double allyTurnsOfSupplyLeft = ally.supplyLevel / ally.type.supplyUpkeep;
                if (allyTurnsOfSupplyLeft < minTurnsOfSupplyLeft) {
                    minTurnsOfSupplyLeft = allyTurnsOfSupplyLeft;
                    allyToSupply = ally;
                }
            }
        }

        if (allyToSupply != null) {
            int transferAmount = (int) (rc.getSupplyLevel() - hqSupplyReservedForResupplyDrone);
            if (transferAmount > 0) {
                Debug.indicate("supply", 2, "transferring " + transferAmount + " to " + allyToSupply.location.toString());
                rc.transferSupplies(transferAmount, allyToSupply.location);
            }
        } else {
            Debug.indicate("supply", 0, "no non-drone to supply :(");
        }
    }
}
