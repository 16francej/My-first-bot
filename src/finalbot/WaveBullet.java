package finalbot;
import java.awt.geom.*;
import robocode.util.Utils;
 
public class WaveBullet
{
	private double startX, startY, startBearing, power;
	private long   fireTime;
	private int    direction;
	private int[]  returnSegment;
 
	public WaveBullet(double x, double y, double bearing, double power,//these are hypothetical waves of bullets that are fired at the enemy
			int direction, long time, int[] segment)
	{
		startX         = x;
		startY         = y;
		startBearing   = bearing; //bearing to other robot
		this.power     = power;
		this.direction = direction;//direction that i'm facing 
		fireTime       = time;
		returnSegment  = segment;
	}
	public double getBulletSpeed()//calculates the speed of the bullet
	//a high power bullet has a lower speed
	{
		return 20 - power * 3;
	}
 
	public boolean checkHit(double enemyX, double enemyY, long currentTime)
	{
		// if the distance from the wave origin to our enemy has passed
		// the distance the bullet would have traveled...
		if (Point2D.distance(startX, startY, enemyX, enemyY) <= (currentTime - fireTime) * getBulletSpeed())//checks if the wave hit
		{
			double desiredDirection = Math.atan2(enemyX - startX, enemyY - startY);
			double angleOffset = Utils.normalRelativeAngle(desiredDirection - startBearing);
			double guessFactor = Math.max(-1, Math.min(1, angleOffset / maxEscapeAngle())) * direction;//gives us a guess factor
			int index = (int) Math.round((returnSegment.length - 1) /2 * (guessFactor + 1));//translates the guess factor into an index
			returnSegment[index]++;//increments that situation
			return true;
		}
		return false;
	}
	public double maxEscapeAngle() //if the robot moves at max speed in the opposite direction, what angle would we need to fire
	{//the robot will not be able to move outside of this angle range in a given tick
		double maxEscapeAngle =  Math.asin(8 / getBulletSpeed());
		return maxEscapeAngle;
	}

}