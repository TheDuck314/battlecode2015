package anatid18_strats_purelauncher_opt;

import battlecode.common.GameActionException;
import battlecode.common.TerrainTile;

public class MeasureMapSize extends Bot {
    public static final int COORD_UNKNOWN = 999999;

    enum MapSymmetry {
        ROTATION, UNKNOWN
    }

    public static MapSymmetry mapSymmetry = null;

    private static void determineMapSymmetry() {
        // If the HQs are connected by a horizontal, vertical, or diagonal line,
        // then the symmetry is hard to determine because it could be a rotational
        // symmetry or a reflection symmetry. If the line between the HQs is not
        // horizontal, vertical, or diagonally, then the symmetry must be rotational.
        if (ourHQ.x == theirHQ.x || ourHQ.y == theirHQ.y || Math.abs(ourHQ.x - theirHQ.x) == Math.abs(ourHQ.y - theirHQ.y)) {
            mapSymmetry = MapSymmetry.UNKNOWN;
        } else {
            mapSymmetry = MapSymmetry.ROTATION;
        }
    }

    public static void checkForMapEdges() throws GameActionException {
        if (mapSymmetry == null) {
            determineMapSymmetry();
        }

        if (MessageBoard.MAP_MIN_X.readInt() == -COORD_UNKNOWN) {
            for (int r = 4; r >= 1; r -= 1) {
                if (rc.senseTerrainTile(here.add(-r, 0)) == TerrainTile.OFF_MAP) {
                    int minX = here.x - r + 1;
                    MessageBoard.MAP_MIN_X.writeInt(minX);
                    if (mapSymmetry == MapSymmetry.ROTATION) MessageBoard.MAP_MAX_X.writeInt(theirHQ.x + (ourHQ.x - minX));
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
                    if (mapSymmetry == MapSymmetry.ROTATION) MessageBoard.MAP_MIN_X.writeInt(theirHQ.x - (maxX - ourHQ.x));
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
                    if (mapSymmetry == MapSymmetry.ROTATION) MessageBoard.MAP_MAX_Y.writeInt(theirHQ.y + (ourHQ.y - minY));
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
                    if (mapSymmetry == MapSymmetry.ROTATION) MessageBoard.MAP_MIN_Y.writeInt(theirHQ.y - (maxY - ourHQ.y));
                } else {
                    break;
                }
            }
        }
    }
}
