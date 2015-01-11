package framework10_drones;

import battlecode.common.*;

public class Supply extends Bot {
    public static void shareSupply() throws GameActionException {
        RobotInfo[] nearbyAllies = rc.senseNearbyRobots(GameConstants.SUPPLY_TRANSFER_RADIUS_SQUARED, us);

        double minSupply = rc.getSupplyLevel();
        RobotInfo allyToSupply = null;
        for (RobotInfo ally : nearbyAllies) {
            if (ally.type.isBuilding) continue;
            if (ally.type == RobotType.BEAVER || ally.type == RobotType.MISSILE) continue;
            if (ally.supplyLevel < minSupply) {
                minSupply = ally.supplyLevel;
                allyToSupply = ally;
            }
        }

        if (allyToSupply == null) {
            return;
        }

        try {
            int transferAmount = (int) (rc.getSupplyLevel() - allyToSupply.supplyLevel) / 2;
            rc.transferSupplies(transferAmount, allyToSupply.location);
        } catch (GameActionException e) {
            System.out.println("exception: couldn't transfer supply to " + allyToSupply.type.toString() + " at " + allyToSupply.location.toString());
        }
    }

    public static void requestResupplyIfNecessary() throws GameActionException {
        if (Clock.getRoundNum() % 3 == 0) return; // can only request supply on even rounds

        double lookaheadTurns = Math.sqrt(here.distanceSquaredTo(ourHQ));
        int supplyNeededHere = (int) (lookaheadTurns * rc.getType().supplyUpkeep - rc.getSupplyLevel());

        if (supplyNeededHere <= 0) return;

        RobotInfo[] nearbyAllies = rc.senseNearbyRobots(GameConstants.SUPPLY_TRANSFER_RADIUS_SQUARED, us);
        for (RobotInfo ally : nearbyAllies) {
            supplyNeededHere += (int) (lookaheadTurns * ally.type.supplyUpkeep - ally.supplyLevel); // deliberately allowing negatives
        }

        // Debug.indicate("supply", 0, "supply needed here: " + supplyNeededHere);

        if (supplyNeededHere > MessageBoard.MAX_SUPPLY_NEEDED.readInt()) {
            MessageBoard.MAX_SUPPLY_NEEDED.writeInt(supplyNeededHere);
            MessageBoard.NEEDIEST_SUPPLY_LOC.writeMapLocation(here);
        }
    }

    static MapLocation supplyRunnerDest = null;
    static double supplyRunnerNeed = 0;
    static boolean supplyRunnerReturning = true;

    public static void runSupplies() throws GameActionException {
        MessageBoard.RESUPPLY_DRONE_CHECKIN.writeInt(Clock.getRoundNum());

        if (supplyRunnerDest != null && here.distanceSquaredTo(supplyRunnerDest) < GameConstants.SUPPLY_TRANSFER_RADIUS_SQUARED) {
            supplyRunnerTransferSupply();
        }

        if (Clock.getRoundNum() % 3 == 1) {
            supplyRunnerNeed = MessageBoard.MAX_SUPPLY_NEEDED.readInt();
            if (supplyRunnerNeed > 0) {
                supplyRunnerDest = MessageBoard.NEEDIEST_SUPPLY_LOC.readMapLocation();
                // Debug.indicate("supply", 0, "max supply needed = " + supplyRunnerNeed + " at " + supplyRunnerDest.toString());
            } else {
                supplyRunnerDest = null;
                // Debug.indicate("supply", 0, "no supply need");
            }
        }

        if (Clock.getRoundNum() % 3 == 2) {
            MessageBoard.MAX_SUPPLY_NEEDED.writeInt(0);
        }

        if (here.distanceSquaredTo(ourHQ) < GameConstants.SUPPLY_TRANSFER_RADIUS_SQUARED) {
            supplyRunnerReturning = false;
        }

        if (8 * Math.sqrt(here.distanceSquaredTo(ourHQ)) > rc.getSupplyLevel()) {
            supplyRunnerReturning = true;
        }

        if (!supplyRunnerReturning && supplyRunnerNeed > 0) {
            Nav.goTo(supplyRunnerDest);
            // Debug.indicate("supply", 2, "going to supply dest");
        } else {
            Nav.goTo(ourHQ);
            // Debug.indicate("supply", 2, "returning to HQ");
        }
    }

    public static void supplyRunnerTransferSupply() throws GameActionException {
        RobotInfo[] nearbyAllies = rc.senseNearbyRobots(GameConstants.SUPPLY_TRANSFER_RADIUS_SQUARED, us);

        double minSupply = rc.getSupplyLevel();
        RobotInfo allyToSupply = null;
        for (RobotInfo ally : nearbyAllies) {
            if (ally.type.isBuilding) continue;
            if (ally.type == RobotType.BEAVER || ally.type == RobotType.MISSILE || ally.type == RobotType.DRONE) continue;
            if (ally.supplyLevel < minSupply) {
                minSupply = ally.supplyLevel;
                allyToSupply = ally;
            }
        }

        if (allyToSupply == null) return;

        double supplyNeededToReturn = 8 * Math.sqrt(here.distanceSquaredTo(ourHQ));

        try {
            int transferAmount = (int) (rc.getSupplyLevel() - supplyNeededToReturn);
            if (transferAmount > 0) {
                // Debug.indicate("supply", 1, "transferring " + transferAmount + " supply and keeping " + supplyNeededToReturn);
                rc.transferSupplies(transferAmount, allyToSupply.location);
            }
        } catch (GameActionException e) {
            System.out.println("exception: couldn't transfer supply to " + allyToSupply.type.toString() + " at " + allyToSupply.location.toString());
        }
    }

}
