package finalbot;
import robocode.AdvancedRobot;
import robocode.ScannedRobotEvent;
import robocode.util.*;
public class finalBot extends AdvancedRobot {
	
	private int scanTime = 1; 
	private double distance;
	private double velocity;
	
	public void run() {
	    // ...
		setAdjustRadarForGunTurn(false);
	   do{
			turnRadarRightRadians(Double.POSITIVE_INFINITY);}while(true);
	}
	 
	public void onScannedRobot(ScannedRobotEvent e) {
	    double absBearing = e.getBearingRadians()+getHeadingRadians();;
	    double radarTurn =
		        // Absolute bearing to target
		        getHeadingRadians() + e.getBearingRadians()
		        // Subtract current radar heading to get turn required
		        - getRadarHeadingRadians();
		    setTurnRadarRightRadians(1.9 * Utils.normalRelativeAngle(radarTurn));
		    if(scanTime % 3 == 0){
		    	distance = e.getDistance();
		    	velocity = e.getVelocity();//add to arrayList
		    }
		    scanTime++;
	}
	public int[] similarPoints(int numNeighbors, int currentPoint)//add all points
	{//determine euclidian distance based upon velocity, distance, and bearing
		int[] simPoints = new int[3];//return 3 arrays 
		return simPoints;
	}
	
	public double firingAngle(int[] similarPoints, int guessFactors, int bulletPower)//should call similarPoints method for similar points int
	{
		int firingAngle = 0;
		return firingAngle;
	}
}
