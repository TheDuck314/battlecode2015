package framework10_drones;

import battlecode.common.GameActionException;
import battlecode.common.TerrainTile;

public class MeasureMapSize extends Bot {
    public static final int COORD_UNKNOWN = 999999;

    public static void checkForMapEdges() throws GameActionException {
        if (MessageBoard.MAP_MIN_X.readInt() == -COORD_UNKNOWN) {
            for (int r = 4; r >= 1; r -= 1) {
                if (rc.senseTerrainTile(here.add(-r, 0)) == TerrainTile.OFF_MAP) {
                    int minX = here.x - r + 1;
                    MessageBoard.MAP_MIN_X.writeInt(minX);
                    MessageBoard.MAP_MAX_X.writeInt(theirHQ.x + (ourHQ.x - minX));
                } else {
                    break;
                }
            }
        }

        if (MessageBoard.MAP_MAX_X.readInt() == COORD_UNKNOWN) {
            for (int r = 4; r >= 1; r -= 1) {
                if (rc.senseTerrainTile(here.add(r, 0)) == TerrainTile.OFF_MAP) {
                    int maxX = here.x + r - 1;
                    MessageBoard.MAP_MAX_X.writeInt(maxX);
                    MessageBoard.MAP_MIN_X.writeInt(theirHQ.x - (maxX - ourHQ.x));
                } else {
                    break;
                }
            }
        }

        if (MessageBoard.MAP_MIN_Y.readInt() == -COORD_UNKNOWN) {
            for (int r = 4; r >= 1; r -= 1) {
                if (rc.senseTerrainTile(here.add(0, -r)) == TerrainTile.OFF_MAP) {
                    int minY = here.y - r + 1;
                    MessageBoard.MAP_MIN_Y.writeInt(minY);
                    MessageBoard.MAP_MAX_Y.writeInt(theirHQ.y + (ourHQ.y - minY));
                } else {
                    break;
                }
            }
        }

        if (MessageBoard.MAP_MAX_Y.readInt() == COORD_UNKNOWN) {
            for (int r = 4; r >= 1; r -= 1) {
                if (rc.senseTerrainTile(here.add(0, r)) == TerrainTile.OFF_MAP) {
                    int maxY = here.y + r - 1;
                    MessageBoard.MAP_MAX_Y.writeInt(maxY);
                    MessageBoard.MAP_MIN_Y.writeInt(theirHQ.y - (maxY - ourHQ.y));
                } else {
                    break;
                }
            }
        }
    }
}
