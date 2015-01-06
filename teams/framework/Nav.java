package framework;

import battlecode.common.*;

public class Nav extends Bot {
    
    public static void goTo(MapLocation dest) throws GameActionException {
        if(here.equals(dest)) return;
        
        Direction dirTo = here.directionTo(dest);
        Direction[] dirs = { dirTo, dirTo.rotateLeft(), dirTo.rotateRight(), dirTo.rotateLeft().rotateLeft(), dirTo.rotateRight().rotateRight() };
        for(Direction dir : dirs) {
            if(rc.canMove(dir)) {
                rc.move(dir);
                return;
            }
        }
    }
}
