package framework;

import battlecode.common.GameConstants;

public class Util extends Bot {
    public static int indexFromCoords(int x, int y) {
        x %= GameConstants.MAP_MAX_WIDTH;
        if (x < 0) x += GameConstants.MAP_MAX_WIDTH;
        y %= GameConstants.MAP_MAX_HEIGHT;
        if (y < 0) y += GameConstants.MAP_MAX_HEIGHT;
        return y * GameConstants.MAP_MAX_WIDTH + x;
    }
}
