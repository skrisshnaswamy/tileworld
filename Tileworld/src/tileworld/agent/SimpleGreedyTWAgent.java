package tileworld.agent;

import sim.field.grid.ObjectGrid2D;
import sim.util.Int2D;
import tileworld.Parameters;
import tileworld.environment.*;
import tileworld.exceptions.CellBlockedException;
import tileworld.exceptions.InsufficientFuelException;
import tileworld.planners.AstarPathGenerator;
import tileworld.planners.TWPath;
import tileworld.planners.TWPathStep;

import tileworld.environment.TWDirection;
import tileworld.environment.TWEntity;
import tileworld.environment.TWEnvironment;
import tileworld.environment.TWFuelStation;
import tileworld.environment.TWHole;
import tileworld.environment.TWTile;
import tileworld.exceptions.CellBlockedException;
import tileworld.exceptions.InsufficientFuelException;
import tileworld.planners.*;
import tileworld.planners.PatrolPath.Shape;

import java.lang.Math;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;

public class SimpleGreedyTWAgent extends TWAgent {

    private static final long serialVersionUID = 1L;
    private String name;
    private TWPath path;
    private Random randomGenerator = new Random();
    private AstarPathGenerator pathGenerator = new AstarPathGenerator(
            this.getEnvironment(), this, 5000);
    private static final Logger LOGGER = Logger
            .getLogger(Logger.GLOBAL_LOGGER_NAME);
    private long startTime;
    private int FUEL_LOW = 50;
    private int FUEL_CRITICAL = 25;
    private int TILE_CAP = 3;
    private TWAction prevAction = null;
    private int canTeleport = 10;

    // Communication variables
    private ClosestCorner closestCorner = ClosestCorner.BOTTOM_RIGHT;
    int xleft = 0;
    int ytop = 0;
    int xright = 1;
    int ybottom = 1;
    int idx = 1;
    int agentsCount = 1;

  ArrayList<String> memcoords = new ArrayList<>();
    boolean isFuelingStnLocKnown = false;

    public enum ClosestCorner {
        BOTTOM_LEFT, BOTTOM_RIGHT, TOP_LEFT, TOP_RIGHT
    }

    public boolean canCarry() {
        return this.carriedTiles.size() < TILE_CAP;
    }

    private boolean fuelLow() {
        return this.getFuelLevel() <= this.getX() + this.getY() + FUEL_LOW;
    }

    private boolean fuelCritical() {
        return this.getFuelLevel() <= this.getX() + this.getY() + FUEL_CRITICAL;
    }

    @Override
    public String getName() {
        return "(Greedy) Agent " + this.name;
    }

    /**
     * @param xpos      initial position of the agent
     * @param ypos
     * @param //id      identifier of the agent
     * @param env       environment
     * @param fuelLevel fuel level
     */
    public SimpleGreedyTWAgent(String name, int xpos, int ypos, TWEnvironment env,
                         double fuelLevel, int idx, int agentsCount) {
        super(xpos, ypos, env, fuelLevel);
        this.name = name;
        this.idx = idx;
        this.agentsCount = agentsCount;
        System.out.println("Agent" + this.idx  + ": " + this.getName() + " @" + "(" + xpos + "," + ypos + ")");
        this.computeFuelLimits();
        startTime = System.nanoTime();
        LOGGER.setLevel(Level.INFO);
        this.xleft = this.sensor.sensorRange - 1;
        this.ytop = this.sensor.sensorRange - 1;
        this.xright = this.getEnvironment().getxDimension() - this.sensor.sensorRange;
        this.ybottom = this.getEnvironment().getyDimension() - this.sensor.sensorRange;
    }

    private void computeFuelLimits() {
        path = pathGenerator.findPath(0, 0, this.getEnvironment().getxDimension() - 1, this.getEnvironment().getyDimension() - 1);
        if (path != null && path.getpath() != null) {
            this.FUEL_LOW = path.getpath().size() * 2;
            this.FUEL_CRITICAL = path.getpath().size();
        } else {
            this.FUEL_LOW = Parameters.defaultFuelLevel / 5;
            this.FUEL_CRITICAL = Parameters.defaultFuelLevel / 7;
        }
        System.out.println(this.getName() + "[" + this.idx + "] Fuel limit set - Low: " + this.FUEL_LOW + ", Critical: " + this.FUEL_CRITICAL);
        System.out.println("*********************************************");
    }

    private boolean isMovePossible(TWDirection dir, boolean extraCond) {
        boolean withinBoundary = true;
        boolean cellNotBlocked = false;
        try {
            if (this.getX() + dir.dx > this.getEnvironment().getxDimension()
                    || this.getY() + dir.dy > this.getEnvironment().getyDimension()
                    || this.getX() + dir.dx < 0
                    || this.getY() + dir.dy < 0
            )
                withinBoundary = false;
        } catch (Exception ex) {
            withinBoundary = false;
        }

        try {
            if (!this.getMemory().isCellBlocked(this.getX() + dir.dx, this.getY() + dir.dy))
                cellNotBlocked = true;
        } catch (Exception ex){
            cellNotBlocked = false;
        }
        return withinBoundary && extraCond && cellNotBlocked;
    }

    private boolean locateFuelingStation() {
        boolean isAtFuelStation = false;
        ObjectGrid2D objectGrid = this.getEnvironment().getObjectGrid();
        int objCount = 0;

        int fuelX = this.getX();
        int fuelY = this.getY();
        int xmin = Math.max(this.getX() - this.sensor.sensorRange, 0);
        int xmax = Math.min(this.getX() + this.sensor.sensorRange, this.getEnvironment().getxDimension() - 1);
        int ymin = Math.max(this.getY() - this.sensor.sensorRange, 0);
        int ymax = Math.min(this.getY() + this.sensor.sensorRange, this.getEnvironment().getyDimension() - 1);
        for(int x0 = xmin; x0 <= xmax; ++x0) {
            for (int y0 = ymin; y0 <= ymax; ++y0) {
                TWEntity e = (TWEntity) objectGrid.get(x0, y0);
                boolean obj = this.getEnvironment().doesCellContainObject(x0, y0);
                if (obj && (e instanceof  TWTile || e instanceof TWHole)) {
                    objCount++;
                    this.getMemory().updateMemory(e, x0, y0);
                }
                if (obj && (e instanceof TWFuelStation)) {
                    if (!memcoords.contains(x0 + ":" + y0)) {
                        System.out.println(this.getName() + ": Fueling station found @ (" + x0 + "," + y0 + ")");
                            memcoords.add(x0 + ":" + y0);
                        this.sendMsg(TWAction.REFUEL.name() + " " + x0 + " " + y0);
                        this.getMemory().updateMemory(e, x0, y0);
                        fuelX = x0;
                        fuelY = y0;
                        isAtFuelStation = true;
                        break;
                    }
                }
            }
        }
        if (objCount > 0)
            System.out.println(objCount + " Objects found in perimeter sweep!!!");
        if (fuelLow())
            path = pathGenerator.findPath(this.getX(), this.getY(), fuelX, fuelY);
        return isAtFuelStation;
    }

    private boolean isInvalidCoords() {
        if (this.getX() > this.getEnvironment().getxDimension()
                || this.getY() > this.getEnvironment().getyDimension()
                || this.getX() < 0
                || this.getY() < 0
        ) {
            LOGGER.warning("out of border");
            return true;
        }
        else {
            return false;
        }
    }

    @Override
    protected TWThought think() {
        TWAction act;
        TWDirection dir = null;
        ObjectGrid2D objectGrid = this.getMemory().getMemoryGrid();
        TWEntity e = (TWEntity) objectGrid.get(this.getX(), this.getY());

        isInvalidCoords();
        if (!isFuelingStnLocKnown) {
            System.out.println(this.getName() + ": Searching fueling station... Current fuel level: " + this.getFuelLevel());
           isFuelingStnLocKnown = findFuelingStation();
        }

        System.out.println("Greedy Agent: " + name + " Score: " + this.score + " at ("
                + this.getX() + "," + this.getY() + "), Fuel Level: " + this.getFuelLevel()
                + ", Runtime: " + (int) ((System.nanoTime() - startTime) / 1000000));
        LOGGER.info("Greedy Agent: " + name + " Score: " + this.score + " at "
                + this.getX() + " " + this.getY() + ", Fuel Level: " + this.getFuelLevel()
                + ", Runtime: " + (int) ((System.nanoTime() - startTime) / 1000000));

        boolean obj = this.getEnvironment().doesCellContainObject(this.getX(), this.getY());
        System.out.println(this.getName() + ": Starting patrol....");
        if (this.canCarry() && obj && (e instanceof TWTile)) {
            act = TWAction.PICKUP;
            LOGGER.info("Action: PICKUP");
        } else if (obj && (e instanceof TWHole) && this.hasTile()) {
            act = TWAction.PUTDOWN;
            LOGGER.info("Action: PUTDOWN");
        } else if ( (prevAction == null || !prevAction.equals(TWAction.REFUEL)) &&
                ((obj && (e instanceof TWFuelStation)) || this.getEnvironment().inFuelStation(this))) {
            LOGGER.info("Action: REFUEL");
            act = TWAction.REFUEL;
            LOGGER.info("Broadcasting FuelStation location");
            this.sendMsg(TWAction.REFUEL.name() + " " + this.getX() + " " + this.getY());
            this.getMemory().updateMemory(e, this.getX(), this.getY());
        } else {
            LOGGER.info("Action: MOVE");
            act = TWAction.MOVE;
            dir = this.locateNearestObject();
            if (dir == TWDirection.Z) {
                System.out.println("No moves");
                canTeleport--;
            }
        }
        return new TWThought(act, dir);
    }

    private boolean findFuelingStation() {
        int leastDist = this.getEnvironment().getxDimension() + this.getEnvironment().getyDimension();
        TWPath fpath = null;
        int yIncrement = (2 * this.sensor.sensorRange) - 1;
        int yIter = (int) Math.ceil(this.getEnvironment().getyDimension() / yIncrement);

        if (this.agentsCount > 1) {
            updateQuadrants();
            System.out.println(this.getName() + "[" + this.idx + "] perimeter: (" + this.xleft + "," + this.ytop + ")" +
                    ", (" + this.xright + "," + this.ytop + ")" +
                    ", (" + this.xleft + "," + this.ybottom + ")" +
                    ", (" + this.xright + "," + this.ybottom + ")"
            );
            switch (this.closestCorner) {
                case TOP_LEFT:
                    fpath = pathGenerator.findPath(this.getX(), this.getY(), xleft, ytop);
                    break;
                default: //BOTTOM_LEFT
                    fpath = pathGenerator.findPath(this.getX(), this.getY(), xleft, ybottom);
                    break;
            }
        }
        else {
            int topLeftDist = this.fuelRequired(this.xleft, this.ytop);
            if (topLeftDist < leastDist) {
                leastDist = topLeftDist;
                closestCorner = ClosestCorner.TOP_LEFT;
                fpath = pathGenerator.findPath(this.getX(), this.getY(), xleft, ytop);
            }
            int bottomLeftDist = this.fuelRequired(xleft, ybottom);
            if (bottomLeftDist < leastDist) {
                leastDist = bottomLeftDist;
                closestCorner = ClosestCorner.BOTTOM_LEFT;
                fpath = pathGenerator.findPath(this.getX(), this.getY(), xleft, ybottom);
            }
            int topRightDist = this.fuelRequired(xright, ytop);
            if (topRightDist < leastDist) {
                leastDist = topRightDist;
                closestCorner = ClosestCorner.TOP_RIGHT;
                fpath = pathGenerator.findPath(this.getX(), this.getY(), xright, ytop);
            }
            int bottomRightDist = this.fuelRequired(xright, ybottom);
            if (bottomRightDist < leastDist) {
                leastDist = bottomRightDist;
                closestCorner = ClosestCorner.BOTTOM_RIGHT;
                fpath = pathGenerator.findPath(this.getX(), this.getY(), xright, ybottom);
            }
        }

        System.out.print(this.getName() + ": Starting @ (" + this.getX() + "," + this.getY() + "), Fuel level: " + this.getFuelLevel());
        System.out.println(", Closest Corner:" + closestCorner.toString());

        // if fuel is critical then wait
        if (this.fuelCritical() && !checkMsgForFuelStation()) {
            System.out.println(this.getName() + ": Fuel level: CRITICAL\nAbandon the hunt!!!");
            return false;
        }

        boolean fuelFound = checkMsgForFuelStation() || locateFuelingStation();
        if (fuelFound)
            return true;
        if (fpath != null && fpath.getpath() != null && fpath.getpath().size() > 0) {
            for (TWPathStep eachStep : fpath.getpath()) {
                try {
                    if (this.fuelCritical() && !checkMsgForFuelStation()) {
                        System.out.println(this.getName() + ": Fuel level: CRITICAL\nAbandon the hunt!!!");
                        return false;
                    }
                    this.move(eachStep.getDirection());
                    fuelFound = checkMsgForFuelStation() || locateFuelingStation();
                    if (fuelFound)

                        return true;
                } catch (CellBlockedException e) {
                    e.printStackTrace();
                }
            }
        }
        System.out.println(this.getName() + ": Currently @ (" + this.getX() + "," + this.getY() + "), Fuel level: " + this.getFuelLevel());

        for (int i = 0; i < yIter; i++) {
            int iter = closestCorner.equals(ClosestCorner.BOTTOM_LEFT) || closestCorner.equals(ClosestCorner.TOP_LEFT) ? i : i + 1;
            //TODO: move right for odd iters if starting @ left; move right for even iters if starting @ right;
            if (iter % 2 == 0) { fpath = pathGenerator.findPath(this.getX(), this.getY(), xright, this.getY()); }
            //TODO: move left for even iters if starting @ left; move left for odd iters if starting @ right;
            else { fpath = pathGenerator.findPath(this.getX(), this.getY(), xleft, this.getY()); }
            if (fpath != null && fpath.getpath() != null && fpath.getpath().size() > 0) {
                for (TWPathStep eachStep : fpath.getpath()) {
                    try {
                        if (this.fuelCritical() && !checkMsgForFuelStation()) {
                            System.out.println(this.getName() + ": Fuel level: CRITICAL\nAbandon the hunt!!!");
                            return false;
                        }
                        this.move(eachStep.getDirection());
                        fuelFound = checkMsgForFuelStation() || locateFuelingStation();
                        if (fuelFound)
                            return true;
                    } catch (CellBlockedException e) {
                        LOGGER.warning("Cell blocked! Find another way...");
                    }
                }
            }
            System.out.print(this.getName() + ": Sideways sweep complete...");
            System.out.println("Currently @ (" + this.getX() + "," + this.getY() + "), Fuel level: " + this.getFuelLevel());

            int nextHop;
            //TODO: move up if starting @ bottom
            if (closestCorner.equals(ClosestCorner.TOP_LEFT) || closestCorner.equals(ClosestCorner.TOP_RIGHT)) { nextHop = Math.min(this.getY() + yIncrement, this.getEnvironment().getyDimension() - this.sensor.sensorRange); }
            //TODO: move down if starting @ top
            else { nextHop = Math.max(this.getY() - yIncrement, 0 + this.sensor.sensorRange); }
            try {
                fpath = pathGenerator.findPath(this.getX(), this.getY(), this.getX(), nextHop);
                if (fpath != null && fpath.getpath() != null && fpath.getpath().size() > 0) {
                    for (TWPathStep eachStep : fpath.getpath()) {
                        try {
                            if (this.fuelCritical() && !checkMsgForFuelStation()) {
                                System.out.println(this.getName() + ": Fuel level: CRITICAL\nAbandon the hunt!!!");
                                return false;
                            }
                            this.move(eachStep.getDirection());
                            fuelFound = checkMsgForFuelStation() || locateFuelingStation();
                            if (fuelFound)
                                return true;
                        } catch (CellBlockedException e) {
                            e.printStackTrace();
                        }
                    }
                }
            } catch (ArrayIndexOutOfBoundsException ex) {
                System.out.println("Tried moving from (" + this.getX() + "," + this.getY() + ") to (" + this.getX() + "," + nextHop + ")");
            }
        }
        return false;
    }

    private void updateQuadrants() {
        if (this.agentsCount > 1) {
            switch (this.agentsCount) {
                case 2:
                    int newBoundaryPoint = (int) Math.ceil(this.getEnvironment().getyDimension() / 2) + 1;
                    switch (this.idx) {
                        case 1:
                            this.ybottom = newBoundaryPoint;
                            this.closestCorner = ClosestCorner.BOTTOM_LEFT;
                            break;
                        case 2:
                            this.ytop = newBoundaryPoint;
                            this.closestCorner = ClosestCorner.TOP_LEFT;
                            break;
                    }
                    break;
                case 3:
                    int newBoundaryPoint1 = (int) Math.ceil(this.getEnvironment().getyDimension() / 3) + 1;
                    int newBoundaryPoint2 = (int) Math.ceil(this.getEnvironment().getyDimension() * 2 / 3) + 1;
                    switch (this.idx) {
                        case 1:
                            this.ybottom = newBoundaryPoint1;
                            this.closestCorner = ClosestCorner.BOTTOM_LEFT;
                            break;
                        case 2:
                            this.ytop = newBoundaryPoint1;
                            this.ybottom = newBoundaryPoint2;
                            this.closestCorner = ClosestCorner.TOP_LEFT;
                            break;
                        case 3:
                            this.ytop = newBoundaryPoint2;
                            this.closestCorner = ClosestCorner.TOP_LEFT;
                            break;
                    }
                    break;
                default:
                    break;
            }
        }
    }

    @Override
    protected void act(TWThought thought) {
        TWEntity e = (TWEntity) this.getMemory().getMemoryGrid()
                .get(this.getX(), this.getY());
        switch (thought.getAction()) {
            case PICKUP:
                try {
                    this.pickUpTile((TWTile) e);
                    prevAction = TWAction.PICKUP;
                } catch (Exception ex) {
                    LOGGER.info("no tile on the ground");
                }
                this.getMemory().removeAgentPercept(this.getX(), this.getY());
                this.getMemory().removeObject(e);
                break;
            case REFUEL:
                try {
                    this.refuel();
                    prevAction = TWAction.REFUEL;
                } catch (Exception ex) {
                    LOGGER.info("not at fuel station");
                }
                break;
            case PUTDOWN:
                try {
                    this.putTileInHole((TWHole) e);
                    prevAction = TWAction.PUTDOWN;
                } catch (Exception ex) {
                    LOGGER.info("no hole on the ground");
                }
                this.getMemory().removeAgentPercept(this.getX(), this.getY());
                this.getMemory().removeObject(e);
                break;
            case MOVE:
                try {
                    this.move(thought.getDirection());
                    prevAction = TWAction.MOVE;
                } catch (CellBlockedException e1) {
                    LOGGER.info("cell blocked");
                    this.path = null;
                } catch (InsufficientFuelException e2) {
                    LOGGER.info("agent: " + name);
                }
                this.getMemory().removeAgentPercept(this.getX(), this.getY());
                break;
            default:
                break;
        }
    }

    private int fuelRequired(int x, int y) {
        int fReq = (int) this.getFuelLevel();
        if (this.getX() == x && this.getY() == y)
            fReq = 0;
        else if (this.getEnvironment().isCellBlocked(x, y))
            fReq = (int) this.getFuelLevel();
        else {
            TWPath p = pathGenerator.findPath(this.getX(), this.getY(), x, y);
            if (p != null && p.getpath() != null) {
                fReq = p.getpath().size();
            }
        }
        return fReq;
    }

    private TWAction findBestAction() {
        TWAction action = null;
        if (this.fuelCritical())
            action = TWAction.REFUEL;
        else if (this.carriedTiles.size() == TILE_CAP)
            action = TWAction.PUTDOWN;
        else if (!this.hasTile() && this.carriedTiles.size() < TILE_CAP)
            action = TWAction.PICKUP;
        return action;
    }

    private TWDirection locateNearestObject() {
        TWDirection dir = null;
        int nearestObject = this.getEnvironment().getxDimension() + this.getEnvironment().getyDimension();
        ObjectGrid2D memoryObjectGrid2D = this.getMemory().getMemoryGrid();
        ObjectGrid2D vicinityObjectGrid2D = this.getEnvironment().getObjectGrid();
        TWPath npath = null;

        // sensorRange perimeter
        int xmin = Math.max(this.getX() - this.sensor.sensorRange, 0);
        int xmax = Math.min(this.getX() + this.sensor.sensorRange, this.getEnvironment().getxDimension() - 1);
        int ymin = Math.max(this.getY() - this.sensor.sensorRange, 0);
        int ymax = Math.min(this.getY() + this.sensor.sensorRange, this.getEnvironment().getyDimension() - 1);

        for(int x0 = 0; x0 < this.getEnvironment().getxDimension(); ++x0) {
            for (int y0 = 0; y0 < this.getEnvironment().getyDimension(); ++y0) {
                TWAction action = this.findBestAction();

                if (action == TWAction.REFUEL) {
                    emergencyRefueling();
                }

                //TODO: 1. check agent memory of objects
                TWEntity mEntity = (TWEntity) memoryObjectGrid2D.get(x0, y0);
                boolean mObj = this.getEnvironment().doesCellContainObject(x0, y0);
                if (mObj) {
                    if (mEntity instanceof TWFuelStation && action == TWAction.REFUEL) {
                        nearestObject = fuelRequired(x0, y0);
                        npath = pathGenerator.findPath(this.getX(), this.getY(), x0, y0);
                    }
                    else if (this.fuelRequired(x0, y0) < nearestObject) {
//                    System.out.println("Looking at (" + x0 + "," + y0 + ") in memory");
                        if (mEntity instanceof TWTile && (action == null || action == TWAction.PICKUP)) {
                            nearestObject = fuelRequired(x0, y0);
                            npath = pathGenerator.findPath(this.getX(), this.getY(), x0, y0);
                        } else if (mEntity instanceof TWHole && (action == null || action == TWAction.PUTDOWN)) {
                            nearestObject = fuelRequired(x0, y0);
                            npath = pathGenerator.findPath(this.getX(), this.getY(), x0, y0);
                        } else if (mEntity instanceof TWFuelStation) {
                            nearestObject = fuelRequired(x0, y0);
                            npath = pathGenerator.findPath(this.getX(), this.getY(), x0, y0);
                        }
                    }
                }

                //TODO: 2. check vicinity for objects
                if (x0 >= xmin && y0 >= ymin && x0 <= xmax && y0 < ymax) {
                    TWEntity vEntity = (TWEntity) vicinityObjectGrid2D.get(x0, y0);
                    boolean vObj = this.getEnvironment().doesCellContainObject(x0, y0);
                    if (vObj) {
                        if (vEntity instanceof TWFuelStation && action == TWAction.REFUEL) {
                            nearestObject = fuelRequired(x0, y0);
                            npath = pathGenerator.findPath(this.getX(), this.getY(), x0, y0);
                        }
                        else if (this.fuelRequired(x0, y0) < nearestObject) {
                            if (vEntity instanceof TWTile && (action == null || action == TWAction.PICKUP)) {
                                nearestObject = fuelRequired(x0, y0);
                                npath = pathGenerator.findPath(this.getX(), this.getY(), x0, y0);
                            } else if (vEntity instanceof TWHole && (action == null || action == TWAction.PUTDOWN)) {
                                nearestObject = fuelRequired(x0, y0);
                                npath = pathGenerator.findPath(this.getX(), this.getY(), x0, y0);
                            } else if (vEntity instanceof TWFuelStation) {
                                nearestObject = fuelRequired(x0, y0);
                                npath = pathGenerator.findPath(this.getX(), this.getY(), x0, y0);
                            }
                        }
                    }
                }
            }
        }
        if (npath == null || npath.getpath() == null || npath.getpath().size() == 0) {
            canTeleport--;
            Int2D pos;
            TWPath newPath;
            boolean nextDirFound = false;
            do {
                pos = this.getEnvironment().generateRandomLocation();
                newPath = pathGenerator.findPath(this.getX(), this.getY(), pos.getX(), pos.getY());

                if (newPath != null && newPath.getpath() != null &&
                        newPath.getpath().size() > 0 &&
                        newPath.getpath().size() < this.getFuelLevel() - FUEL_CRITICAL
                ) {
                    LinkedList<TWPathStep> steps = newPath.getpath();
                    dir = newPath.popNext().getDirection();
                    nextDirFound = isMovePossible(dir, true);
                    if ( canTeleport <= 0 && nextDirFound && steps.size() > 0) {
                        int destX = steps.get(steps.size() - 1).getX();
                        int destY = steps.get(steps.size() - 1).getY();
                        System.out.println("Agent seem to have stuck in a rut...\nRandomly teleporting to (" + destX + "," + destY + ")");
                        for (TWPathStep s: steps) {
                            try {
                                this.move(s.getDirection());
                            } catch (CellBlockedException e) {
                                System.out.println("Cell blocked!!!");
                            }
                        }
                        canTeleport = 10;
                        nextDirFound = false;
                    }
                }
            } while (!nextDirFound);
        }
        else
            dir = npath.popNext().getDirection();
        return dir;
    }

    private void emergencyRefueling() {
        System.out.println(this.getName() + ": Rushing for emergency refueling...");
        TWPath npath;
        ObjectGrid2D objectGrid = this.getEnvironment().getObjectGrid();
        int x = 0, y = 0;
        boolean fStnLocLocked = false;
        TWFuelStation fStn = this.getMemory().getFuelStation();
        boolean fuelFound = false;
        if (fStn == null) {
            System.out.println(this.getName() + ": Fuel not in memory! checking msgs...");
            for (Message m : this.getMsg()) {
                // if fuel station has been previously found update memory:
                if (m.getMessage().contains(TWAction.REFUEL.name())) {
                    String[] splitMsg = m.getMessage().split("\\s+");
                    x = Integer.parseInt(splitMsg[1]);
                    y = Integer.parseInt(splitMsg[2]);
                    this.getMemory().updateMemory((TWEntity) objectGrid.get(x, y), x, y);
                    fStn = this.getMemory().getFuelStation();
                    fuelFound = true;
                    if (fStn != null) {
                        System.out.println(this.getName() + ": Fuel Found!");
                        break;
                    }
                }
            }
        } else {
            fuelFound = true;
            x = fStn.getX();
            y = fStn.getY();
        }

        if (fuelFound) {
            npath = pathGenerator.findPath(this.getX(), this.getY(), x, y);
            if (npath != null && npath.getpath() != null && npath.getpath().size() > 0)
                for (TWPathStep s : npath.getpath()) {
                    try {
                        this.move(s.getDirection());
                        fStnLocLocked = this.getEnvironment().doesCellContainObject(this.getX(), this.getY());
                        if (fStnLocLocked) {
                            fStnLocLocked = false;
                            TWEntity e = (TWEntity) objectGrid.get(this.getX(), this.getY());
                            if (e instanceof TWFuelStation) {
                                fStnLocLocked = true;
                                break;
                            }
                        }
                    } catch (CellBlockedException e) {
                        System.out.println("Cell blocked!!!");
                    }
                }

            if (fStnLocLocked) {
                System.out.println(this.getName() + ": Action: REFUEL (Emergency)");
                this.refuel();
            }
        }
    }


    private boolean checkMsgForFuelStation() {
        for (Message m : this.getMsg()) {
            if (m.getMessage().contains(TWAction.REFUEL.name())) {
                return true;
            }
        }
        return false;
    }

}