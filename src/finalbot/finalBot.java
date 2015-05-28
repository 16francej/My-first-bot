package finalbot;
import java.util.ArrayList;
import java.awt.geom.*;
import robocode.AdvancedRobot;
import robocode.HitByBulletEvent;
import robocode.ScannedRobotEvent;
import robocode.util.*;

public class finalBot extends AdvancedRobot {
	//for dynamic clustering (which was not used due to time constraints)
	private int scanTime = 1; 
	private double distance;
	private double velocity;
	private double bearing;
	private ArrayList<Double> list; 
	//for shooting guess factors
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

	public void run() {
		setAdjustGunForRobotTurn(true);
		setAdjustRadarForGunTurn(true);
		_enemyWaves = new ArrayList();//instantiating the waves
		_surfDirections = new ArrayList();
		_surfAbsBearings = new ArrayList();

		do{
			turnRadarRightRadians(Double.POSITIVE_INFINITY);}while(true);//a do while prevents the robot from not initially moving into this loop
	}

	public void onScannedRobot(ScannedRobotEvent e) {
		//movement code comes first to dodge
		double absBearing = getHeadingRadians() + e.getBearingRadians();//this is the angle between the enemy robot and mine
		_myLocation = new Point2D.Double(getX(), getY());
		double lateralVelocity = getVelocity()*Math.sin(e.getBearingRadians());//using some trig to calculate my x direction velocity
		_surfDirections.add(0, new Integer((lateralVelocity >= 0) ? 1 : -1));
		_surfAbsBearings.add(0, new Double(absBearing + Math.PI));
		double bulletPower = _oppEnergy - e.getEnergy();
		if (bulletPower < 3.01 && bulletPower > 0.09
				&& _surfDirections.size() > 2) {
			EnemyWave ew = new EnemyWave();
			ew.fireTime = getTime() - 1;
			ew.bulletVelocity = bulletVelocity(bulletPower);
			ew.distanceTraveled = bulletVelocity(bulletPower);
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

		//scanTime++;
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
						&& Math.abs(bulletVelocity(e.getBullet().getPower()) 
								- ew.bulletVelocity) < 0.001) {
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
		Point2D.Double predictedPosition = (Point2D.Double)_myLocation.clone();
		double predictedVelocity = getVelocity();
		double predictedHeading = getHeadingRadians();
		double maxTurning, moveAngle, moveDir;

		int counter = 0; // number of ticks in the future
		boolean intercepted = false;

		do {    // the rest of these code comments are rozu's
			moveAngle =
					wallSmoothing(predictedPosition, absoluteBearing(surfWave.fireLocation,
							predictedPosition) + (direction * (Math.PI/2)), direction)
							- predictedHeading;
			moveDir = 1;

			if(Math.cos(moveAngle) < 0) {
				moveAngle += Math.PI;
				moveDir = -1;
			}

			moveAngle = Utils.normalRelativeAngle(moveAngle);

			// maxTurning is built in like this, you can't turn more then this in one tick
			maxTurning = Math.PI/720d*(40d - 3d*Math.abs(predictedVelocity));
			predictedHeading = Utils.normalRelativeAngle(predictedHeading
					+ limit(-maxTurning, moveAngle, maxTurning));

			// this one is nice ;). if predictedVelocity and moveDir have
			// different signs you want to breack down
			// otherwise you want to accelerate (look at the factor "2")
			predictedVelocity +=
					(predictedVelocity * moveDir < 0 ? 2*moveDir : moveDir);
			predictedVelocity = limit(-8, predictedVelocity, 8);

			// calculate the new predicted position
			predictedPosition = project(predictedPosition, predictedHeading,
					predictedVelocity);

			counter++;

			if (predictedPosition.distance(surfWave.fireLocation) <
					surfWave.distanceTraveled + (counter * surfWave.bulletVelocity)
					+ surfWave.bulletVelocity) {
				intercepted = true;
			}
		} while(!intercepted && counter < 500);

		return predictedPosition;
	}//end of predicting position
	public void updateWaves() {//will update the waves to account for the new situation
		for (int x = 0; x < _enemyWaves.size(); x++) {
			EnemyWave ew = (EnemyWave)_enemyWaves.get(x);

			ew.distanceTraveled = (getTime() - ew.fireTime) * ew.bulletVelocity;
			if (ew.distanceTraveled > _myLocation.distance(ew.fireLocation) + 50) {//removes innacurate wave points
				_enemyWaves.remove(x);
				x--;
			}
		}
	}
	public EnemyWave getClosestSurfableWave() {
		double closestDistance = 50000; // I juse use some very big number here
		EnemyWave surfWave = null;

		for (int x = 0; x < _enemyWaves.size(); x++) {
			EnemyWave ew = (EnemyWave)_enemyWaves.get(x);
			double distance = _myLocation.distance(ew.fireLocation)
					- ew.distanceTraveled;

			if (distance > ew.bulletVelocity && distance < closestDistance) {
				surfWave = ew;
				closestDistance = distance;
			}
		}
		return surfWave;
	}
	public static int getFactorIndex(EnemyWave ew, Point2D.Double targetLocation) {
		double offsetAngle = (absoluteBearing(ew.fireLocation, targetLocation)
				- ew.directAngle);
		double factor = Utils.normalRelativeAngle(offsetAngle)
				/ maxEscapeAngle(ew.bulletVelocity) * ew.direction;

		return (int)limit(0,
				(factor * ((BINS - 1) / 2)) + ((BINS - 1) / 2),
				BINS - 1);
	}
	public void logHit(EnemyWave ew, Point2D.Double targetLocation) {
		int index = getFactorIndex(ew, targetLocation);

		for (int x = 0; x < BINS; x++) {
			// for the spot bin that we were hit on, add 1;
			// for the bins next to it, add 1 / 2;
			// the next one, add 1 / 5; and so on...
			_surfStats[x] += 1.0 / (Math.pow(index - x, 2) + 1);
		}
	}
	public double checkDanger(EnemyWave surfWave, int direction) {
		int index = getFactorIndex(surfWave,
				predictPosition(surfWave, direction));

		return _surfStats[index];
	}

	public void doSurfing() {
		EnemyWave surfWave = getClosestSurfableWave();

		if (surfWave == null) { return; }

		double dangerLeft = checkDanger(surfWave, -1);
		double dangerRight = checkDanger(surfWave, 1);

		double goAngle = absoluteBearing(surfWave.fireLocation, _myLocation);
		if (dangerLeft < dangerRight) {
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
}
