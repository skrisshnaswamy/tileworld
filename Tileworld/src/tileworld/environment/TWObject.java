/**
 *
 */
package tileworld.environment;

import sim.util.Int2D;
import tileworld.Parameters;
import tileworld.planners.TWPath;


/**
 * TWObject
 *
 * @author michaellees
 * Created: Apr 16, 2010
 *
 * Copyright michaellees 2010
 *
 *
 * Description: a class which is represent a transient entity in Tileworld (i.e., one with a lifetime).
 *
 */
public class TWObject extends TWEntity implements Comparable<TWObject> {

    protected static final int lifeTime = Parameters.lifeTime;

    private Double utility;
    private TWPath pathTo;
    private double creationTime;
    private double dTime;


    /**
     * @param creationTime
     * @param deathTime
     */
    public TWObject(int x, int y, TWEnvironment env, double creationTime, double deathTime) {
        super(x, y, env);
        this.creationTime = creationTime;
        this.dTime = deathTime;
    }

    public TWObject(Int2D pos, TWEnvironment env, Double creationTime, Double deathTime) {
        this(pos.x, pos.y, env, creationTime, deathTime);
    }

    public TWObject() {
    }

    /**
     * @return the deathTime
     */
    public double getDeathTime() {
        return dTime;
    }

    /**
     * @param d the deathTime to set
     */
    public void setDeathTime(double d) {
        this.dTime = d;
    }

    public double getTimeLeft(double timeNow) {
        return dTime - timeNow;
    }

    @Override
    protected void move(TWDirection d) {
        throw new UnsupportedOperationException("TWObjects are not movable.");
    }

    @Override
    public int compareTo(TWObject o) {
        return o.utility.compareTo(this.utility);
    }

    public TWPath getPathTo() {
        return pathTo;
    }

    public void setPathTo(TWPath pathTo) {
        this.pathTo = pathTo;
    }

    public Double getUtility() {
        return utility;
    }

    public void setUtility(Double utility) {
        this.utility = utility;
    }

}
