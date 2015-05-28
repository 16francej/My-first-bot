package finalbot;
import java.util.ArrayList;
import java.awt.geom.*;
import robocode.AdvancedRobot;
import robocode.HitByBulletEvent;
import robocode.ScannedRobotEvent;
import robocode.util.*;

public class finalBot extends AdvancedRobot {
	/*
	//for dynamic clustering (which was not used due to time constraints)
	private int scanTime = 1; 
	private double distance, velocity, bearing;
	private ArrayList<Double> list; 
	 */
	private ArrayList<WaveBullet> waves = new ArrayList<WaveBullet>();
	private static int[] stats = new int[31]; // 31 is the number of unique GuessFactors we're using
	private double guessFactorAngle;
	private int direction = 1;
	//for movement wave surfing
	public static int BINS = 47;
	public static double _surfStats[] = new double[BINS];
	public Point2D.Double _myLocation;     // our bot's location
	public Point2D.Double _enemyLocation;  // enemy bot's location
	public ArrayList _enemyWaves;
	public ArrayList _surfDirections;
	public ArrayList _surfAbsBearings;
	public static double _oppEnergy = 100.0;
	 public static Rectangle2D.Double _fieldRect = new java.awt.geom.Rectangle2D.Double(18, 18, 764, 564); // hypothetical field which is smaller to prevent collision w/ walls
	 public static double WALL_STICK = 160;
	
	 public void run() {
		setAdjustGunForRobotTurn(true);
		setAdjustRadarForGunTurn(true);
		_enemyWaves = new ArrayList();//instantiating the waves
		_surfDirections = new ArrayList();
		_surfAbsBearings = new ArrayList();
		do{//this turns the radar right until the robot is not scanned to move us into the initial robot lock
			turnRadarRightRadians(Double.POSITIVE_INFINITY);}while(true);//a do while prevents the robot from not initially moving into this loop
	}

	public void onScannedRobot(ScannedRobotEvent e) {
		//movement code comes first to dodge
		double absBearing = getHeadingRadians() + e.getBearingRadians();//this is the angle between the enemy robot and mine
		_myLocation = new Point2D.Double(getX(), getY());
		double lateralVelocity = getVelocity()*Math.sin(e.getBearingRadians());//using some trig to calculate my x direction velocity
		_surfDirections.add(0, new Integer((lateralVelocity >= 0) ? 1 : -1));//adds the new direction to stats we have
		_surfAbsBearings.add(0, new Double(absBearing + Math.PI));//adds the new bearing to our stats
		double bulletPower = _oppEnergy - e.getEnergy();//the difference in the opponent's energy must be the energy put into th e bullet
		if (bulletPower < 3.01 && bulletPower > 0.09 && _surfDirections.size() > 2) 
		{
			EnemyWave ew = new EnemyWave();
			ew.fireTime = getTime() - 1;
			ew.distanceTraveled = bulletVelocity(bulletPower);
			ew.bulletVelocity = bulletVelocity(bulletPower);
			ew.direction = ((Integer)_surfDirections.get(2)).intValue();
			ew.directAngle = ((Double)_surfAbsBearings.get(2)).doubleValue();
			ew.fireLocation = (Point2D.Double)_enemyLocation.clone(); // last tick
			_enemyWaves.add(ew);
		}
		_oppEnergy = e.getEnergy();
		// update after EnemyWave detection, because that needs the previous
		// enemy location as the source of the wave
		_enemyLocation = project(_myLocation, absBearing, e.getDistance());
		updateWaves();
		doSurfing();
		
		//gun code
		
		double power = 2;
		// find our enemy's location:
		double ex = getX() + Math.sin(absBearing) * e.getDistance();
		double ey = getY() + Math.cos(absBearing) * e.getDistance();
		// wave processing
		for (int i=0; i < waves.size(); i++)
		{
			WaveBullet currentWave = (WaveBullet)waves.get(i);//stores the current wave 
			if (currentWave.checkHit(ex, ey, getTime()))
			{
				waves.remove(currentWave);//if the wave hit us it doesn't need to be traveling - time to translate into a guess factor
				i--;
			}
		}
		if (e.getVelocity() != 0)//if the other robot is moving
		{
			if (Math.sin(e.getHeadingRadians()-absBearing)*e.getVelocity() < 0)//if it's moving backward
				direction = -1;
			else
				direction = 1;//if it's moving forward
		}
		int[] currentStats = stats; 
		WaveBullet newWave = new WaveBullet(getX(), getY(), absBearing, power, direction, getTime(), currentStats);
		//wave updating ends
		int bestindex = 15;	// initialize it to be in the middle of all possible targets, or guessfactor 0.
		for (int i=0; i<31; i++)
		{
			if (currentStats[bestindex] < currentStats[i])//essentially just a find the maximum value
			{
				bestindex = i;
			}
		}
		// this should do the opposite of the math in the WaveBullet:
		double guessfactor = (double)(bestindex - (stats.length - 1) / 2)/ ((stats.length - 1) / 2);
		double angleOffset = direction * guessfactor * newWave.maxEscapeAngle();//generates the difference in angle based upon the guess factor, direction, and max possible angle
		double gunAdjust = Utils.normalRelativeAngle(absBearing - getGunHeadingRadians() + angleOffset);
		setTurnGunRightRadians(gunAdjust);
		setFireBullet(power);//we will always use a power of two 
		//most robots employing this algorithm use a power of 1.9 or 2.0
		waves.add(newWave);	//adds the new wave to our data
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
		double radarTurn = (getHeadingRadians() + e.getBearingRadians()) - getRadarHeadingRadians();// Absolute bearing to target // Subtract current radar heading to get turn required
		// Subtract current radar heading to get turn required
		setTurnRadarRightRadians(Utils.normalRelativeAngle(radarTurn));	   
		//scanTime++; also part of dynamic clustering
	}//end of onScannedRobot method

	public void onHitByBullet(HitByBulletEvent e) {//whenever we get hit by a bullet this method will be called
		// If the _enemyWaves collection is empty, we must have missed the
		// detection of this wave somehow.
		if (!_enemyWaves.isEmpty()) {
			Point2D.Double hitBulletLocation = new Point2D.Double(
					e.getBullet().getX(), e.getBullet().getY());
			EnemyWave hitWave = null;

			// look through the EnemyWaves, and find one that could've hit us.
			for (int x = 0; x < _enemyWaves.size(); x++) {
				EnemyWave ew = (EnemyWave)_enemyWaves.get(x);

				if (Math.abs(ew.distanceTraveled -
						_myLocation.distance(ew.fireLocation)) < 50
						&& Math.abs(bulletVelocity(e.getBullet().getPower()) - ew.bulletVelocity) < 0.001) {
					hitWave = ew;
					break;
				}
			}

			if (hitWave != null) {
				logHit(hitWave, hitBulletLocation);

				// We can remove this wave now, of course.
				_enemyWaves.remove(_enemyWaves.lastIndexOf(hitWave));
			}
		}
	}//end of on hit by bullet event
	public Point2D.Double predictPosition(EnemyWave surfWave, int direction) {//this is the most crucial part of wave surfing
		//based on a precise predition method by the robot "Rozu"
		Point2D.Double predictedPosition = (Point2D.Double)_myLocation.clone();
		double predictedVelocity = getVelocity();
		double predictedHeading = getHeadingRadians();
		double maxTurning, moveAngle, moveDir;

		int counter = 0; // number of ticks in the future
		boolean intercepted = false;

		do {  
			moveAngle =
					wallSmoothing(predictedPosition, absoluteBearing(surfWave.fireLocation, predictedPosition) + //finds the angle to move so that we can continue orbiting the wave source
							(direction * (Math.PI/2)), direction) - predictedHeading;
			moveDir = 1;
			if(Math.cos(moveAngle) < 0) {//accounting for variations in position due to acceleration 
				moveAngle += Math.PI;
				moveDir = -1;
			}
			moveAngle = Utils.normalRelativeAngle(moveAngle);
			// maxTurning is built in like this, you can't turn more then this in one tick
			maxTurning = Math.PI/720d*(40d - 3d*Math.abs(predictedVelocity));
			predictedHeading = Utils.normalRelativeAngle(predictedHeading + limit(-maxTurning, moveAngle, maxTurning));
			// if predictedVelocity and moveDir have
			// different signs you want to break down
			// otherwise you want to accelerate (look at the factor "2")
			predictedVelocity +=
					(predictedVelocity * moveDir < 0 ? 2*moveDir : moveDir);//accounting for acceleration and maximum velocity
			predictedVelocity = limit(-8, predictedVelocity, 8);
			// calculate the new predicted position
			predictedPosition = project(predictedPosition, predictedHeading, predictedVelocity);
			counter++;
			if (predictedPosition.distance(surfWave.fireLocation) < surfWave.distanceTraveled + (counter * surfWave.bulletVelocity)
					+ surfWave.bulletVelocity) {
				intercepted = true;
			}
		} while(!intercepted && counter < 500);
		return predictedPosition;
	}//end of predicting position
	public void updateWaves() {//will update the waves to add 
		for (int x = 0; x < _enemyWaves.size(); x++) {//iterates through our past waves
			EnemyWave ew = (EnemyWave)_enemyWaves.get(x);
			ew.distanceTraveled = (getTime() - ew.fireTime) * ew.bulletVelocity;//calculates the position of the new wave
			if (ew.distanceTraveled > _myLocation.distance(ew.fireLocation) + 50) {//updates the distance of our wave
				_enemyWaves.remove(x);
				x--;
			}
		}
	}
	public EnemyWave getClosestSurfableWave() {
		double closestDistance = 50000; // I just used a very big number here so I always get values
		EnemyWave surfWave = null;

		for (int x = 0; x < _enemyWaves.size(); x++) {
			EnemyWave ew = (EnemyWave)_enemyWaves.get(x);
			double distance = _myLocation.distance(ew.fireLocation)
					- ew.distanceTraveled;

			if (distance > ew.bulletVelocity && distance < closestDistance) {//a find the minimum algorithm to find the closest wave
				surfWave = ew;
				closestDistance = distance;//we will be surfing a single wave
			}
		}
		return surfWave;
	}
	public static int getFactorIndex(EnemyWave ew, Point2D.Double targetLocation) {//generating and translating a guess factor into an array index
		double offsetAngle = (absoluteBearing(ew.fireLocation, targetLocation) - ew.directAngle);//relative angle that he turned to hit us
		double factor = Utils.normalRelativeAngle(offsetAngle) / maxEscapeAngle(ew.bulletVelocity) * ew.direction;//this is the guess factor
		return (int)limit(0, (factor * ((BINS - 1) / 2)) + ((BINS - 1) / 2), BINS - 1);
		/*
		 * We find the middle bin, which is also guess factor 0
		 * We then multiply the guess factor by the amount of bins from halfway to the max or min to get a value for the guess factor
		 * Because of integer truncation, this value gets converted into an integer
		 * this value can then be returned as the factor index
		 */
	}
	public void logHit(EnemyWave ew, Point2D.Double targetLocation) {//entering points in the wave at which our robot was hit
		int index = getFactorIndex(ew, targetLocation);
		for (int x = 0; x < BINS; x++) {
			/* for the spot bin that we were hit on, add 1,
			* for the bins next to it, add 1 / 2.
			* the next one, add 1 / 5, and so on
			* this creates a diminishing wave of hit densities around the hit point
			*/
			_surfStats[x] += 1.0 / (Math.pow(index - x, 2) + 1);//adds one at the point of the hit
		}
	}
	public double checkDanger(EnemyWave surfWave, int direction) {
		int index = getFactorIndex(surfWave, predictPosition(surfWave, direction));//generates the bin index corresponding to our current situation
		return _surfStats[index];
	}

	public void doSurfing() {
		EnemyWave surfWave = getClosestSurfableWave();
		if (surfWave == null) { return; }//don't move if we don't need to
		double dangerLeft = checkDanger(surfWave, -1);
		double dangerRight = checkDanger(surfWave, 1);
		double goAngle = absoluteBearing(surfWave.fireLocation, _myLocation);
		if (dangerLeft < dangerRight) {//adjusting the orbit to go move to the safest location
			goAngle = wallSmoothing(_myLocation, goAngle - (Math.PI/2), -1);
		} else {
			goAngle = wallSmoothing(_myLocation, goAngle + (Math.PI/2), 1);
		}
		setBackAsFront(this, goAngle);
	}
	//also for dynamic clustering
	/*
public double[] similarPoints(int numNeighbors,  ScannedRobotEvent e)//add all points
	{//determine manhattan distance based upon velocity, distance, and bearing
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
	class EnemyWave {
        Point2D.Double fireLocation;
        long fireTime;
        double bulletVelocity, directAngle, distanceTraveled;
        int direction;
 
        public EnemyWave() { }
    }
 
    public double wallSmoothing(Point2D.Double botLocation, double angle, int orientation) {
        while (!_fieldRect.contains(project(botLocation, angle, WALL_STICK))) {
            angle += orientation*0.05;
        }
        return angle;
    }
 
    public static Point2D.Double project(Point2D.Double sourceLocation,
        double angle, double length) {
        return new Point2D.Double(sourceLocation.x + Math.sin(angle) * length,
            sourceLocation.y + Math.cos(angle) * length);
    }
 
    public static double absoluteBearing(Point2D.Double source, Point2D.Double target) {
        return Math.atan2(target.x - source.x, target.y - source.y);
    }
 
    public static double limit(double min, double value, double max) {
        return Math.max(min, Math.min(value, max));
    }
 
    public static double bulletVelocity(double power) {
        return (20.0 - (3.0*power));
    }
 
    public static double maxEscapeAngle(double velocity) {
        return Math.asin(8.0/velocity);
    }
 
    public static void setBackAsFront(AdvancedRobot robot, double goAngle) {
        double angle =
            Utils.normalRelativeAngle(goAngle - robot.getHeadingRadians());
        if (Math.abs(angle) > (Math.PI/2)) {
            if (angle < 0) {
                robot.setTurnRightRadians(Math.PI + angle);
            } else {
                robot.setTurnLeftRadians(Math.PI - angle);
            }
            robot.setBack(100);
        } else {
            if (angle < 0) {
                robot.setTurnLeftRadians(-1*angle);
           } else {
                robot.setTurnRightRadians(angle);
           }
            robot.setAhead(100);
        }
    }
}

