package finalbot;
import java.util.ArrayList;

import robocode.AdvancedRobot;
import robocode.ScannedRobotEvent;
import robocode.util.*;
public class finalBot extends AdvancedRobot {
	
	private int scanTime = 1; 
	private double distance;
	private double velocity;
	private double bearing;
	private ArrayList<Double> list; 
	
	public void run() {
	    // ...
		setAdjustRadarForGunTurn(false);
	   do{
			turnRadarRightRadians(Double.POSITIVE_INFINITY);}while(true);
	}
	 
	public void onScannedRobot(ScannedRobotEvent e) {
	    double absBearing = e.getBearingRadians()+getHeadingRadians();
	    double radarTurn =
		        // Absolute bearing to target
		        getHeadingRadians() + e.getBearingRadians()
		        // Subtract current radar heading to get turn required
		        - getRadarHeadingRadians();
		    setTurnRadarRightRadians(1.9 * Utils.normalRelativeAngle(radarTurn));
		  turnGunRight(firingAngle(null, scanTime, scanTime)); 
		    if(scanTime % 3 == 0){
		    	distance = e.getDistance()/(getBattleFieldWidth()*getBattleFieldHeight());
		    	velocity = e.getVelocity()/8;//add to arrayList
		    	bearing = absBearing/360;
		    	list.add(distance);
		    	list.add(velocity);
		    	list.add(bearing);
		    }
		    scanTime++;
	}
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
	}
	
	public double firingAngle(int[] similarPoints, int guessFactors, int bulletPower)//should call similarPoints method for similar points int
	{
		int firingAngle = 0;
		return firingAngle;
	}
}
