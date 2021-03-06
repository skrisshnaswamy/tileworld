/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package tileworld.environment;

import sim.util.Int2D;

/**
 * TWDirection
 *
 * @author michaellees
 * Created: Apr 21, 2010
 * <p>
 * Copyright michaellees 2010
 * <p>
 * <p>
 * Description:
 * <p>
 * Enum specially designed to allow spiral neighbour iteration. Indicates the
 * direction of movement of the agent: 4 possible Up, down, left, right or
 * North, South, West, East
 */
public enum TWDirection {

    E(1, 0) {
        public TWDirection next() {
            return N;
        }
    },
    N(0, -1) {
        public TWDirection next() {
            return W;
        }
    },
    W(-1, 0) {
        public TWDirection next() {
            return S;
        }
    },
    S(0, 1) {
        public TWDirection next() {
            return E;
        }
    },

    Z(0, 0) {//no direction - not really used

        public TWDirection next() {
            return E;
        }
    },
    ;
    public final static Int2D ORIGIN = new Int2D(0, 0);
    public final int dx;
    public final int dy;

    TWDirection(int dx, int dy) {
        this.dx = dx;
        this.dy = dy;
    }

    public Int2D advance(Int2D point) {
        return new Int2D(point.x + dx, point.y + dy);
    }

    public abstract TWDirection next();
}

