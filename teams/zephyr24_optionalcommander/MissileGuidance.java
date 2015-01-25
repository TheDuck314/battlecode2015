package zephyr24_optionalcommander;

import battlecode.common.*;

public class MissileGuidance {
    static final int BASE_CHANNEL = 1000;

    public static MapLocation receivedTargetLocation;
    public static int receivedTargetID;
    
    public static void setMissileTarget(RobotController rc, MapLocation start, MapLocation targetLocation) throws GameActionException {
        int dx = targetLocation.x - start.x;
        int dy = targetLocation.y - start.y;
        
        rc.setIndicatorString(0, "firing at " + targetLocation.toString());
        
        int targetID = 0;
        if(rc.canSenseLocation(targetLocation)) {
            RobotInfo targetRobot = rc.senseRobotAtLocation(targetLocation);
            if(targetRobot != null) targetID = targetRobot.ID;
        }
        
        //int data = 100*(100 + dx) + 50 + dy;
        int data = (targetID << 8) + ((8 + dx) << 4) + (8 + dy);
        rc.broadcast(channelFromCoords(start.x, start.y), data);
    }

    public static void readMissileTarget(RobotController rc, MapLocation start) throws GameActionException {
        int data = rc.readBroadcast(channelFromCoords(start.x, start.y));
        //int dx = (data / 100) - 100;
        //int dy = data - 100*(100 + dx) - 50;
        int dy = (data & 0x0f) - 8;
        int dx = ((data & 0xf0) >> 4) - 8;
        receivedTargetLocation = new MapLocation(start.x + dx, start.y + dy);
        receivedTargetID = (data & 0xfffff00) >> 8;
    }
    
    private static int channelFromCoords(int x, int y) {
        x %= GameConstants.MAP_MAX_WIDTH;
        if (x < 0) x += GameConstants.MAP_MAX_WIDTH;
        y %= GameConstants.MAP_MAX_HEIGHT;
        if (y < 0) y += GameConstants.MAP_MAX_HEIGHT;
        return BASE_CHANNEL + y * GameConstants.MAP_MAX_WIDTH + x;
    }


}
