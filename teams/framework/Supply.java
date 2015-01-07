package framework;

import battlecode.common.*;

public class Supply extends Bot {
    public static void shareSupply() throws GameActionException {
        RobotInfo[] nearbyAllies = rc.senseNearbyRobots(GameConstants.SUPPLY_TRANSFER_RADIUS_SQUARED, us);

        double minSupply = rc.getSupplyLevel();
        RobotInfo allyToSupply = null;
        for (RobotInfo ally : nearbyAllies) {
            if (ally.type.isBuilding) continue;
            if (ally.type == RobotType.BEAVER) continue;
            if (ally.supplyLevel < minSupply) {
                minSupply = ally.supplyLevel;
                allyToSupply = ally;
            }
        }

        if (allyToSupply == null) return;

        try {
            int transferAmount = (int) (rc.getSupplyLevel() - allyToSupply.supplyLevel) / 2;
            rc.transferSupplies(transferAmount, allyToSupply.location);
        } catch (GameActionException e) {
            System.out.println("exception: couldn't transfer supply to " + allyToSupply.type.toString() + " at " + allyToSupply.location.toString());
        }
    }
    
    public static void shareSupplyIfNeedy() throws GameActionException {
        RobotInfo[] nearbyAllies = rc.senseNearbyRobots(GameConstants.SUPPLY_TRANSFER_RADIUS_SQUARED, us);

        double minSupply = rc.getSupplyLevel();
        RobotInfo allyToSupply = null;
        for (RobotInfo ally : nearbyAllies) {
            if (ally.type.isBuilding) continue;
            if (ally.type == RobotType.BEAVER) continue;
            if (ally.supplyLevel < 30 * ally.type.supplyUpkeep && ally.supplyLevel < minSupply) {
                minSupply = ally.supplyLevel;
                allyToSupply = ally;
            }
        }

        if (allyToSupply == null) return;

        try {
            int transferAmount = (int) (rc.getSupplyLevel() - allyToSupply.supplyLevel) / 2;
            rc.transferSupplies(transferAmount, allyToSupply.location);
        } catch (GameActionException e) {
            System.out.println("exception: couldn't transfer supply to " + allyToSupply.type.toString() + " at " + allyToSupply.location.toString());
        }
    }

    public static void requestResupplyIfNecessary() throws GameActionException {
        if (Clock.getRoundNum() % 2 == 1) return; // can only request supply on even rounds

        int lookaheadTurns = 30;
        int supplyNeededHere = (int) (lookaheadTurns * rc.getType().supplyUpkeep - rc.getSupplyLevel());

        if (supplyNeededHere <= 0) return;

        RobotInfo[] nearbyAllies = rc.senseNearbyRobots(GameConstants.SUPPLY_TRANSFER_RADIUS_SQUARED, us);
        for (RobotInfo ally : nearbyAllies) {
            supplyNeededHere += (int) (lookaheadTurns * ally.type.supplyUpkeep - ally.supplyLevel); // deliberately allowing negatives
        }

        Debug.indicate("supply", 0, "supply needed here: " + supplyNeededHere);

        if (supplyNeededHere > MessageBoard.MAX_SUPPLY_NEEDED.readInt()) {
            MessageBoard.MAX_SUPPLY_NEEDED.writeInt(supplyNeededHere);
            MessageBoard.NEEDIEST_SUPPLY_LOC.writeMapLocation(here);
        }
    }

    static MapLocation supplyRunnerDest = null;
    static double supplyRunnerNeed = 0;

    public static void runSupplies() throws GameActionException {
        shareSupplyIfNeedy();

        if (Clock.getRoundNum() % 2 == 1) {
            supplyRunnerNeed = MessageBoard.MAX_SUPPLY_NEEDED.readInt();
            Debug.indicate("supply", 0, "max supply needed = " + supplyRunnerNeed);
            if (supplyRunnerNeed > 0) {
                supplyRunnerDest = MessageBoard.NEEDIEST_SUPPLY_LOC.readMapLocation();
                Debug.indicate("supply", 1, "at " + supplyRunnerDest.toString());
            } else {
                supplyRunnerDest = null;
            }
            MessageBoard.MAX_SUPPLY_NEEDED.writeInt(0);
        }

        if (supplyRunnerNeed > 0) {
            double minSupplyNeeded = Math.max(supplyRunnerNeed, 10 * Math.sqrt(ourHQ.distanceSquaredTo(supplyRunnerDest)));
            if (rc.getSupplyLevel() > minSupplyNeeded) {
                Nav.goTo(supplyRunnerDest);
                Debug.indicate("supply", 2, "going to supply dest");
                return;
            }
        }
        Nav.goTo(ourHQ);
        Debug.indicate("supply", 2, "returning to HQ");
    }
}
