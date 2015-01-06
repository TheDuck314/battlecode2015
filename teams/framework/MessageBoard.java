package framework;

import battlecode.common.*;

public enum MessageBoard {
    DESIRED_BUILDING(0),
    RALLY_LOC(1),
    MAKE_MINERS(3);

    public static void setDefaultChannelValues() throws GameActionException {
        DESIRED_BUILDING.writeRobotType(RobotType.MINERFACTORY);
        RALLY_LOC.writeMapLocation(new MapLocation(rc.senseHQLocation().x, rc.senseHQLocation().y));
        MAKE_MINERS.writeBoolean(true);
    }

    private static RobotController rc;

    public static void init(RobotController theRC) {
        rc = theRC;
    }

    private final int channel;

    private MessageBoard(int theChannel) {
        channel = theChannel;
    }

    public void writeInt(int data) throws GameActionException {
        rc.broadcast(channel, data);
    }

    public int readInt() throws GameActionException {
        return rc.readBroadcast(channel);
    }

    public void incrementInt() throws GameActionException {
        writeInt(1 + readInt());
    }

    public void writeBoolean(boolean bool) throws GameActionException {
        writeInt(bool ? 1 : 0);
    }

    public boolean readBoolean() throws GameActionException {
        return readInt() == 1;
    }

    public void writeMapLocation(MapLocation loc) throws GameActionException {
        rc.broadcast(channel, loc.x);
        rc.broadcast(channel+1, loc.y);
    }

    public MapLocation readMapLocation() throws GameActionException {
        return new MapLocation(rc.readBroadcast(channel), rc.readBroadcast(channel+1));
    }
    
    public void writeRobotType(RobotType rt) throws GameActionException {
        writeInt(rt.ordinal());
    }
    
    public RobotType readRobotType() throws GameActionException {
        return RobotType.values()[readInt()];
    }
}