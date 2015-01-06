package framework;

import battlecode.common.GameConstants;

public class Util extends Bot {
    public static int indexFromCoords(int x, int y) {
        if(x < 0 || y < 0) {
            System.out.println("x = " + x + ", y = " + y);
            System.exit(-1);
        }
        x %= GameConstants.MAP_MAX_WIDTH;
        y %= GameConstants.MAP_MAX_HEIGHT;
        return y * GameConstants.MAP_MAX_WIDTH + x;
    }
}
