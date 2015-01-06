package framework;

import battlecode.common.*;

public class Bot {
    public static RobotController rc;
    protected static Team us;
    protected static Team them;
    protected static MapLocation ourHQ;
    protected static MapLocation theirHQ;

    protected static MapLocation here; // bot classes are responsible for keeping this up to date
    
    protected static void init(RobotController theRC) throws GameActionException {
        rc = theRC;
        MessageBoard.init(theRC);
        
        us = rc.getTeam();
        them = us.opponent();
        
        ourHQ = rc.senseHQLocation();
        theirHQ = rc.senseEnemyHQLocation();
        
        here = rc.getLocation();
    }
}
