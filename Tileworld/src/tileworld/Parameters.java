package tileworld;

/**
 * Parameters
 *
 * @author michaellees
 * Created: Apr 21, 2010
 * <p>
 * Copyright michaellees
 * <p>
 * Description:
 * <p>
 * Class used to store global simulation parameters.
 * Environment related parameters are still in the TWEnvironment class.
 */
public class Parameters {

    /*********************************************************************************************/
    /*  TRIAL#1: Env Size = 50x50, Avg Obj Creation Rate: N(mu=0.2, sigma=0.05), Lifetime = 100  */
    /*********************************************************************************************/
    //Simulation Parameters
    public final static int seed = 4162012; //4162012; //no effect with gui
    public static final long endTime = 5000; //no effect with gui

    //Agent Parameters
    public static final int defaultFuelLevel = 500;
    public static final int defaultSensorRange = 3;

    //Environment Parameters
    public static final int xDimension = 50; //size in cells
    public static final int yDimension = 50;

    //Object Parameters
    // mean, dev: control the number of objects to be created in every time step (i.e. average object creation rate)
    public static final double tileMean = 0.2;
    public static final double holeMean = 0.2;
    public static final double obstacleMean = 0.2;
    public static final double tileDev = 0.05f;
    public static final double holeDev = 0.05f;
    public static final double obstacleDev = 0.05f;
    // the life time of each object
    public static final int lifeTime = 100;

//    /*********************************************************************************************/
//    /*    TRIAL#2: Env Size = 80x20, Avg Obj Creation Rate: N(mu=2, sigma=0.5), Lifetime = 30    */
//    /*********************************************************************************************/
//    //Simulation Parameters
//    public final static int seed = 4162012; //no effect with gui
//    public static final long endTime = 5000; //no effect with gui
//
//    //Agent Parameters
//    public static final int defaultFuelLevel = 500;
//    public static final int defaultSensorRange = 3;
//
//    //Environment Parameters
//    public static final int xDimension = 80; //size in cells
//    public static final int yDimension = 80;
//
//    //Object Parameters
//    // mean, dev: control the number of objects to be created in every time step (i.e. average object creation rate)
//    public static final double tileMean = 2;
//    public static final double holeMean = 2;
//    public static final double obstacleMean = 2;
//    public static final double tileDev = 0.5f;
//    public static final double holeDev = 0.5f;
//    public static final double obstacleDev = 0.5f;
//    // the life time of each object
//    public static final int lifeTime = 30;


    /*********************************************************************************************/
    /*  TRIAL#3: Env Size = 50x50, Avg Obj Creation Rate: N(mu=0.1, sigma=0.5), Lifetime = 100   */
    /*********************************************************************************************/
//    //Simulation Parameters
//    public final static int seed = 4162012; //no effect with gui
//    public static final long endTime = 5000; //no effect with gui
//
//    //Agent Parameters
//    public static final int defaultFuelLevel = 500;
//    public static final int defaultSensorRange = 3;
//
//    //Environment Parameters
//    public static final int xDimension = 50; //size in cells
//    public static final int yDimension = 50;
//
//    //Object Parameters
//    // mean, dev: control the number of objects to be created in every time step (i.e. average object creation rate)
//    public static final double tileMean = 0.1;
//    public static final double holeMean = 0.1;
//    public static final double obstacleMean = 0.1;
//    public static final double tileDev = 0.5f;
//    public static final double holeDev = 0.5f;
//    public static final double obstacleDev = 0.5f;
//    // the life time of each object
//    public static final int lifeTime = 100;

    /*********************************************************************************************/
    /*  TRIAL#4: Env Size = 100x100, Avg Obj Creation Rate: N(mu=0.1, sigma=0.025), Lifetime = 150  */
    /*********************************************************************************************/
//    //Simulation Parameters
//    public final static int seed = 4162012; //4162012; //no effect with gui
//    public static final long endTime = 5000; //no effect with gui
//
//    //Agent Parameters
//    public static final int defaultFuelLevel = 500;
//    public static final int defaultSensorRange = 3;
//
//    //Environment Parameters
//    public static final int xDimension = 100; //size in cells
//    public static final int yDimension = 100;
//
//    //Object Parameters
//    // mean, dev: control the number of objects to be created in every time step (i.e. average object creation rate)
//    public static final double tileMean = 0.1;
//    public static final double holeMean = 0.1;
//    public static final double obstacleMean = 0.1;
//    public static final double tileDev = 0.025f;
//    public static final double holeDev = 0.025f;
//    public static final double obstacleDev = 0.025f;
//    // the life time of each object
//    public static final int lifeTime = 150;

}
