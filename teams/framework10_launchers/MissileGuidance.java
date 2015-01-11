package framework10_launchers;

import battlecode.common.*;

public class MissileGuidance extends Bot {
    static final int baseChannel = 1000;

    public static MapLocation receivedTargetLocation;
    public static int receivedTargetID;
    
    public static void setMissileTarget(MapLocation start, MapLocation targetLocation) throws GameActionException {
        int dx = targetLocation.x - start.x;
        int dy = targetLocation.y - start.y;
        
        int targetID = 0;
        if(rc.canSenseLocation(targetLocation)) {
            RobotInfo targetRobot = rc.senseRobotAtLocation(targetLocation);
            if(targetRobot != null) targetID = targetRobot.ID;
        }
        
        //int data = 100*(100 + dx) + 50 + dy;
        int data = (targetID << 8) + ((8 + dx) << 4) + (8 + dy);
        rc.broadcast(Util.indexFromCoords(start.x, start.y), data);
    }

    public static void readMissileTarget(MapLocation start) throws GameActionException {
        int data = rc.readBroadcast(Util.indexFromCoords(start.x, start.y));
        //int dx = (data / 100) - 100;
        //int dy = data - 100*(100 + dx) - 50;
        int dy = (data & 0x0f) - 8;
        int dx = ((data & 0xf0) >> 4) - 8;
        receivedTargetLocation = new MapLocation(start.x + dx, start.y + dy);
        receivedTargetID = (data & 0xfffff00) >> 8;
    }
}
