package finalbot;
import java.util.ArrayList;
import java.awt.geom.*;
import robocode.AdvancedRobot;
import robocode.ScannedRobotEvent;
import robocode.util.*;
public class finalBot extends AdvancedRobot {
	
	private int scanTime = 1; 
	private double distance;
	private double velocity;
	private double bearing;
	private ArrayList<Double> list; 
	private ArrayList<WaveBullet> waves = new ArrayList<WaveBullet>();
	private static int[] stats = new int[31]; // 31 is the number of unique GuessFactors we're using
	private double guessFactorAngle;
	private int direction = 1;
	
	public void run() {
	    // ...
		setAdjustRadarForGunTurn(false);
	   do{
			turnRadarRightRadians(Double.POSITIVE_INFINITY);}while(true);
	}
	 
	public void onScannedRobot(ScannedRobotEvent e) {
		double power = 2;
		double absBearing = getHeadingRadians() + e.getBearingRadians();
		 
		// find our enemy's location:
		double ex = getX() + Math.sin(absBearing) * e.getDistance();
		double ey = getY() + Math.cos(absBearing) * e.getDistance();
 
		// Let's process the waves now:
		for (int i=0; i < waves.size(); i++)
		{
			WaveBullet currentWave = (WaveBullet)waves.get(i);
			if (currentWave.checkHit(ex, ey, getTime()))
			{
				waves.remove(currentWave);
				i--;
			}
		}
		if (e.getVelocity() != 0)
		{
			if (Math.sin(e.getHeadingRadians()-absBearing)*e.getVelocity() < 0)
				direction = -1;
			else
				direction = 1;
		}
		int[] currentStats = stats; // This seems silly, but I'm using it to
					    // show something else later
		WaveBullet newWave = new WaveBullet(getX(), getY(), absBearing, power,
                        direction, getTime(), currentStats);
		//wave updating ends
		int bestindex = 15;	// initialize it to be in the middle, guessfactor 0.
		for (int i=0; i<31; i++)
		{
			if (currentStats[bestindex] < currentStats[i])
				{
				bestindex = i;
				}
		}
 
		// this should do the opposite of the math in the WaveBullet:
		double guessfactor = (double)(bestindex - (stats.length - 1) / 2)
                        / ((stats.length - 1) / 2);
		double angleOffset = direction * guessfactor * newWave.maxEscapeAngle();
                double gunAdjust = Utils.normalRelativeAngle(
                     absBearing - getGunHeadingRadians() + angleOffset);
                setTurnGunRightRadians(gunAdjust);
                setFireBullet(power);
                waves.add(newWave);	
		   //for dynamic clustering
              /*
                if(scanTime % 3 == 0){
		    	distance = e.getDistance()/(getBattleFieldWidth()*getBattleFieldHeight());
		    	velocity = e.getVelocity()/8;//add to arrayList
		    	bearing = absBearing/360;
		    	list.add(distance);
		    	list.add(velocity);
		    	list.add(bearing);
		    	*/
               //radar lock
                double radarTurn =
                        // Absolute bearing to target
                        getHeadingRadians() + e.getBearingRadians()
                        // Subtract current radar heading to get turn required
                        - getRadarHeadingRadians();
                 
                    setTurnRadarRightRadians(Utils.normalRelativeAngle(radarTurn));	    
		}
		    //scanTime++;
	
//also for dynamic clustering
/*
public double[] similarPoints(int numNeighbors,  ScannedRobotEvent e)//add all points
	{//determine euclidian distance based upon velocity, distance, and bearing
		double absBearing = e.getBearingRadians()+getHeadingRadians();
		double currentDistance = e.getDistance()/(getBattleFieldWidth()*getBattleFieldHeight());
		double currentVelocity = e.getVelocity()/8;//add to arrayList
    	double currentBearing = absBearing/360;
    	int simPointsIndex = 0; 
		double[] simPoints = new double[numNeighbors*3];//return 3 arrays 
		for(int i = 0; i < list.size(); i++)
			if((Math.abs(currentDistance - list.get(i)) +
					Math.abs(currentVelocity - list.get(i+1)) + Math.abs(currentBearing - list.get(i+2))) <= .1)
			{
				simPoints[simPointsIndex] = list.get(i);
				simPoints[simPointsIndex + 1] = list.get(i + 1);
				simPoints[simPointsIndex + 2] = list.get(i + 2);
				simPointsIndex += 3; 
			}
		return simPoints;
	}*/
}
