package tileworld.agent;

import sim.engine.Schedule;
import sim.field.grid.ObjectGrid2D;
import sim.util.Bag;
import sim.util.Int2D;
import sim.util.IntBag;
import tileworld.Parameters;
import tileworld.environment.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

/**
 * TWAgentMemory
 *
 * @author michaellees
 * <p>
 * Created: Apr 15, 2010 Copyright michaellees 2010
 * <p>
 * Description:
 * <p>
 * This class represents the memory of the TileWorld agents. It stores
 * all objects which is has observed for a given period of time. You may
 * want to develop an entirely new memory system or extend/modify this one.
 * <p>
 * The memory is supposed to have a probabilistic decay, whereby an element is
 * removed from memory with a probability proportional to the length of
 * time the element has been in memory. The maximum length of time which
 * the agent can remember is specified as MAX_TIME. Any memories beyond
 * this are automatically removed.
 */
public class TWAgentWorkingMemory {

    private final static int MAX_TIME = 10;
    private final static float MEM_DECAY = 0.5f;
    static private final List<Int2D> spiral = new NeighbourSpiral(Parameters.defaultSensorRange * 4).spiral();
    /**
     * Access to Scedule (TWEnvironment) so that we can retrieve the current timestep of the simulation.
     */
    private final Schedule schedule;
    private final TWAgent me;
    private final ObjectGrid2D memoryGrid;
    /*
     * This was originally a queue ordered by the time at which the fact was observed.
     * However, when updating the memory a queue is very slow.
     * Here we trade off memory (in that we maintian a complete image of the map)
     * for speed of update. Updating the memory is a lot more straightfoward.
     */
    private final TWAgentPercept[][] objects;
    private final HashSet<Int2D> tilesAndHoles;
    /**
     * Number of items recorded in memory, currently doesn't decrease as memory
     * is not degraded - nothing is ever removed!
     */
    private int memorySize;
    /**
     * Stores (for each TWObject type) the closest object within sensor range,
     * null if no objects are in sensor range
     */
    private HashMap<Class<?>, TWEntity> closestInSensorRange;
    private final List<TWAgent> neighbouringAgents = new ArrayList<TWAgent>();

    public TWAgentWorkingMemory(TWAgent moi, Schedule schedule, int x, int y) {

        this.me = moi;

        this.objects = new TWAgentPercept[x][y];
        int i = 0;
        int j = 0;
        for (i = 0; i < x; i++) {
            for (j = 0; j < y; j++) {
                Int2D pos = new Int2D(i, j);
                TWObject object = new TWObject();
                object.setLocation(pos);
                objects[i][j] = new TWAgentPercept(object, 0);
            }
        }
        this.schedule = schedule;
        this.memoryGrid = new ObjectGrid2D(me.getEnvironment().getxDimension(), me.getEnvironment().getyDimension());
        tilesAndHoles = new HashSet<Int2D>();
    }

    /**
     * Called at each time step, updates the memory map of the agent.
     * Note that some objects may disappear or be moved, in which case part of
     * sensed may contain null objects
     * <p>
     * Also note that currently the agent has no sense of moving objects, so
     * an agent may remember the same object at two locations simultaneously.
     * <p>
     * Other agents in the grid are sensed and passed to this function. But it
     * is currently not used for anything. Do remember that an agent sense itself
     * too.
     *
     * @param sensedObjects bag containing the sensed objects
     * @param objectXCoords bag containing x coordinates of objects
     * @param objectYCoords bag containing y coordinates of object
     * @param sensedAgents  bag containing the sensed agents
     * @param agentXCoords  bag containing x coordinates of agents
     * @param agentYCoords  bag containing y coordinates of agents
     */

    // Communication function
    public void addAgentPercept(TWAgentPercept percept) {
        objects[percept.getO().getX()][percept.getO().getY()] = percept;
        memoryGrid.set(percept.getO().getX(), percept.getO().getY(), percept.getO());
        Int2D loc = new Int2D(percept.getO().getX(), percept.getO().getY());
        if (percept.getO() instanceof TWTile || percept.getO() instanceof TWHole)
            tilesAndHoles.add(loc);
    }

    public void updateMemory(Bag sensedObjects, IntBag objectXCoords, IntBag objectYCoords, Bag sensedAgents, IntBag agentXCoords, IntBag agentYCoords) {
        //reset the closest objects for new iteration of the loop (this is short
        //term observation memory if you like) It only lasts one timestep

        clearMemoryInSensorRange(Parameters.defaultSensorRange);

        //must all be same size.
        assert (sensedObjects.size() == objectXCoords.size() && sensedObjects.size() == objectYCoords.size());
        for (int i = 0; i < sensedObjects.size(); i++) {
            TWEntity o = (TWEntity) sensedObjects.get(i);
            if (!(o instanceof TWObject)) {
                continue;
            }
            //Add the object to memory
            addAgentPercept(new TWAgentPercept(o, this.getSimulationTime()));
        }

        // Agents are currently not added to working memory. Depending on how
        // communication is modelled you might want to do this.
        neighbouringAgents.clear();
        for (int i = 0; i < sensedAgents.size(); i++) {
            assert sensedAgents.get(i) instanceof TWAgent;
            TWAgent a = (TWAgent) sensedAgents.get(i);
            if (a == null || a.equals(me)) {
                continue;
            }
            neighbouringAgents.add(a);
        }
    }

    public TWAgent getNeighbour() {
        if (neighbouringAgents.isEmpty()) {
            return null;
        } else {
            return neighbouringAgents.get(0);
        }
    }

    private void clearMemoryInSensorRange(int defaultsensorrange) {
        for (int i = me.getX() - defaultsensorrange; i <= me.getX() + defaultsensorrange; i++) {
            for (int j = me.getY() - defaultsensorrange; j <= me.getY() + defaultsensorrange; j++) {
                if (!me.getEnvironment().isInBounds(i, j))
                    continue;
                removeAgentPercept(i, j);
            }
        }
    }

    public void removeAgentPercept(int x, int y) {
        Int2D loc = new Int2D(x, y);
        tilesAndHoles.remove(loc);
        TWObject object = new TWObject();
        Int2D pos = new Int2D(x, y);
        object.setLocation(pos);
        objects[x][y] = new TWAgentPercept(object, this.getSimulationTime());
        memoryGrid.set(x, y, null); //memorygrid is never really accessed much by us.
    }

    public void removeObject(TWEntity o) {
        removeAgentPercept(o.getX(), o.getY());
    }

    /**
     * @return
     */
    public double getSimulationTime() {
        return schedule.getTime();
    }

    /**
     * Finds a nearby tile we have seen less than threshold timesteps ago
     *
     * @see TWAgentWorkingMemory#getNearbyObject(int, int, double, java.lang.Class)
     */
    public TWTile getNearbyTile(int x, int y, double threshold) {
        return (TWTile) this.getNearbyObject(x, y, threshold, TWTile.class);
    }

    /**
     * Finds a nearby hole we have seen less than threshold timesteps ago
     *
     * @see TWAgentWorkingMemory#getNearbyObject(int, int, double, java.lang.Class)
     */
    public TWHole getNearbyHole(int x, int y, double threshold) {
        return (TWHole) this.getNearbyObject(x, y, threshold, TWHole.class);
    }

    public HashSet<Int2D> getTilesAndHoles() {
        return tilesAndHoles;
    }


    /**
     * Returns the nearest object that has been remembered recently where recently
     * is defined by a number of timesteps (threshold)
     * <p>
     * If no Object is in memory which has been observed in the last threshold
     * timesteps it returns the most recently observed object. If there are no objects in
     * memory the method returns null. Note that specifying a threshold of one
     * will always return the most recently observed object. Specifying a threshold
     * of MAX_VALUE will always return the nearest remembered tile.
     * <p>
     * Also note that it is likely that nearby objects are also the most recently observed
     *
     * @param x         coordinate from which to check for tiles
     * @param y         coordinate from which to check for tiles
     * @param threshold how recently we want to have see the object
     * @param type      the class of object we're looking for (Must inherit from TWObject, specifically tile or hole)
     * @return
     */
    public TWObject getNearbyObject(int sx, int sy, double threshold, Class<?> type) {

        //If we cannot find an object which we have seen recently then we want
        //the one with maxTimestamp
        double maxTimestamp = 0;
        TWObject o = null;
        double time = 0;
        TWObject ret = null;
        int x, y;
        for (Int2D offset : spiral) {
            x = offset.x + sx;
            y = offset.y + sy;

            if (me.getEnvironment().isInBounds(x, y) && objects[x][y] != null) {
                TWEntity e = getObjectAt(x, y);
                if (e instanceof TWFuelStation)
                    continue;
                o = (TWObject) objects[x][y].getO();//get mem object
                if (type.isInstance(o)) {//if it's not the type we're looking for do nothing

                    time = objects[x][y].getT();//get time of memory

                    if (this.getSimulationTime() - time <= threshold) {
                        //if we found one satisfying time, then return
                        return o;
                    } else if (time > maxTimestamp) {
                        //otherwise record the timestamp and the item in case
                        //it's the most recent one we see
                        ret = o;
                        maxTimestamp = time;
                    }
                }
            }
        }

        //this will either be null or the object of Class type which we have
        //seen most recently but longer ago than now-threshold.
        return ret;
    }

    /**
     * Is the cell blocked according to our memory?
     *
     * @param tx    x position of cell
     * @param ty    y position of cell
     * @param decay if the memory of the object is older than this parameter, returns false; pass -1 to ignore this parameter
     * @return true if the cell is blocked in our memory
     */
    public boolean isCellBlocked(int tx, int ty, int decay) {
        if (!me.getEnvironment().isInBounds(tx, ty))
            return true;

        //no memory at all, so assume not blocked
        if (objects[tx][ty] == null) {
            return false;
        }
        if (decay >= 0 && me.getEnvironment().schedule.getTime() - objects[tx][ty].getT() >= decay)
            return false;
        TWEntity e = objects[tx][ty].getO();
        //is it an obstacle?
        return (e instanceof TWObstacle);
    }

    public ObjectGrid2D getMemoryGrid() {
        return memoryGrid;
    }


    public TWEntity getObjectAt(int x, int y) {
        if (objects[x][y] == null)
            return null;
        return objects[x][y].getO();
    }

    public TWAgentPercept getPerceptAt(int x, int y) {
        return objects[x][y];
    }

    // return fuel station object if in memory
    public TWFuelStation getFuelStation() {
        TWEntity o = null;
        int x, y;
        for (Int2D offset : spiral) {
            x = offset.x;
            y = offset.y;

            if (me.getEnvironment().isInBounds(x, y) && objects[x][y] != null) {
                o = objects[x][y].getO(); //get mem object
                if (o instanceof TWFuelStation) {
                    return (TWFuelStation) o;
                }
            }
        }

        return null;
    }

    public void updateMemory(TWEntity e, int x, int y) {
        objects[x][y] = new TWAgentPercept(e, this.getSimulationTime());
    }

    public boolean isCellBlocked(int tx, int ty) {

        //no memory at all, so assume not blocked
        if (objects[tx][ty] == null) {
            return false;
        }

        TWEntity e = objects[tx][ty].getO();
        //is it an obstacle?
        return (e instanceof TWObstacle);
    }
}

