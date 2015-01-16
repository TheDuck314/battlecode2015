package anatid14_purelauncher;

import battlecode.common.*;

public enum MessageBoard {
    DESIRED_BUILDING(0),
    RALLY_LOC(1),
    NEEDIEST_SUPPLY_LOC(3),
    MAX_SUPPLY_NEEDED(5),  
    MAP_MIN_X(6),
    MAP_MAX_X(7),
    MAP_MIN_Y(8),
    MAP_MAX_Y(9),
    FINAL_ATTACK_SIGNAL(10),
    RESUPPLY_DRONE_ID(11),
    CONSTRUCTION_ORDERS(100);

    public static void setDefaultChannelValues() throws GameActionException {
        DESIRED_BUILDING.writeRobotType(RobotType.MINERFACTORY);
        RALLY_LOC.writeMapLocation(new MapLocation(Bot.rc.senseHQLocation().x, Bot.rc.senseHQLocation().y));
        
        MAP_MIN_X.writeInt(-MeasureMapSize.COORD_UNKNOWN);
        MAP_MAX_X.writeInt(MeasureMapSize.COORD_UNKNOWN);
        MAP_MIN_Y.writeInt(-MeasureMapSize.COORD_UNKNOWN);
        MAP_MAX_Y.writeInt(MeasureMapSize.COORD_UNKNOWN);
        
        // construction orders are set to false by default
    }

    private final int channel;

    private MessageBoard(int theChannel) {
        channel = theChannel;
    }

    public void writeInt(int data) throws GameActionException {
        Bot.rc.broadcast(channel, data);
    }

    public int readInt() throws GameActionException {
        return Bot.rc.readBroadcast(channel);
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
        Bot.rc.broadcast(channel, loc.x);
        Bot.rc.broadcast(channel+1, loc.y);
    }

    public MapLocation readMapLocation() throws GameActionException {
        return new MapLocation(Bot.rc.readBroadcast(channel), Bot.rc.readBroadcast(channel+1));
    }
    
    public void writeRobotType(RobotType rt) throws GameActionException {
        writeInt(rt.ordinal());
    }
    
    public RobotType readRobotType() throws GameActionException {
        return RobotType.values()[readInt()];
    }
    
    public void writeConstructionOrder(RobotType rt, boolean order) throws GameActionException {
        Bot.rc.broadcast(channel + rt.ordinal(), order ? 1 : 0);
    }

    public boolean readConstructionOrder(RobotType rt) throws GameActionException {
        return Bot.rc.readBroadcast(channel + rt.ordinal()) == 1;
    }
}