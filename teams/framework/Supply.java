package framework;

import battlecode.common.GameActionException;
import battlecode.common.GameConstants;
import battlecode.common.RobotInfo;

public class Supply extends Bot {
    public static void shareSupply() throws GameActionException {
        RobotInfo[] nearbyAllies = rc.senseNearbyRobots(GameConstants.SUPPLY_TRANSFER_RADIUS_SQUARED, us);

        if (nearbyAllies.length == 0) return;

        RobotInfo ally = nearbyAllies[(int) (nearbyAllies.length * Math.random())];

        try {
            int transferAmount = (int) (rc.getSupplyLevel() - ally.supplyLevel) / 2;
            if (transferAmount > 0) {
                rc.transferSupplies(transferAmount, ally.location);
            }
        } catch (GameActionException e) {
            System.out.println("exception: couldn't transfer supply to " + ally.type.toString() + " at " + ally.location.toString());
        }
    }
}
