package first;

import battlecode.common.*;

public class RobotPlayer {
    static RobotController rc;

    public static void run(RobotController theRC)
    {
        rc = theRC;

        while(true) {
            try {
                switch(rc.getType()) {
                    case HQ:
                        turnHQ();
                        break;

                    case TOWER:
                        turnTower();
                        break;

                    case BEAVER:
                        turnBeaver();
                        break;

                    case MINERFACTORY:
                        turnMinerFactory();
                        break;

                    case MINER:
                        turnMiner();
                        break;

                    default:
                        System.out.println("unknown robot type: " + rc.getType().toString());
                        return;
                }
            }
            catch(Exception e)
            {
                System.out.println("Caught an exception!");
                e.printStackTrace();
            }

            rc.yield();
        }
    }

    static void turnHQ() throws GameActionException
    {
        
        if(rc.getSupplyLevel() > 50) {
            RobotInfo[] nearbyAllies = rc.senseNearbyRobots(GameConstants.SUPPLY_TRANSFER_RADIUS_SQUARED, rc.getTeam());
            for(RobotInfo ally : nearbyAllies) {
                if(ally.supplyLevel == 0) {
                    rc.transferSupplies(50, ally.location);
                }
            }
        }
        
        if(rc.isCoreReady()) {
            Direction[] dirs = Direction.values();
            for(Direction dir : dirs) {
                if(rc.canSpawn(dir, RobotType.BEAVER)) {
                    rc.spawn(dir, RobotType.BEAVER);
                    break;
                }
            }
        }
    }

    static void turnTower() throws GameActionException
    {
        if(!rc.isWeaponReady()) return;
        
        RobotInfo[] enemies = rc.senseNearbyRobots(RobotType.TOWER.attackRadiusSquared, rc.getTeam().opponent());
        
        if(enemies.length > 0) {
            rc.attackLocation(enemies[0].location); 
        }
    }

    static void turnMinerFactory() throws GameActionException
    {
        if(!rc.isCoreReady()) return;

        if(rc.getTeamOre() > RobotType.MINER.oreCost) {
            Direction[] dirs = Direction.values();
            for(Direction dir : dirs) {
                if(rc.canSpawn(dir, RobotType.MINER)) {
                    rc.spawn(dir, RobotType.MINER);
                    break;
                }
            }
        }
    }

    static void turnMiner() throws GameActionException
    {
        if(!rc.isCoreReady()) return;

        if(rc.senseOre(rc.getLocation()) < GameConstants.MINER_MINE_MAX) {       
            Direction dir = Direction.values()[(int)(8*Math.random())];
            if(rc.canMove(dir)) {
                rc.move(dir);
            }
        } else {
            rc.mine();
        }
    }

    static void turnBeaver() throws GameActionException
    {
        if(!rc.isCoreReady()) return;

        if(Math.random() < 0.005) {
            if(rc.getTeamOre() > RobotType.MINERFACTORY.oreCost) {
                Direction[] dirs = Direction.values();
                for(Direction dir : dirs) {
                    if(rc.canBuild(dir, RobotType.MINERFACTORY)) {
                        rc.build(dir, RobotType.MINERFACTORY);
                        return;
                    }
                }
            }
        }

        if(rc.senseOre(rc.getLocation()) < GameConstants.BEAVER_MINE_MAX) {       
            Direction dir = Direction.values()[(int)(8*Math.random())];
            if(rc.canMove(dir)) {
                rc.move(dir);
            }
        } else {
            rc.mine();
        }
    }
}