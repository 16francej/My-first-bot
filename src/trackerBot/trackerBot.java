package trackerBot;
import robocode.AdvancedRobot;
import robocode.ScannedRobotEvent;
import robocode.util.*;
public class trackerBot extends AdvancedRobot {
	public void run() {
	    // ...
		setAdjustRadarForGunTurn(false);
	   do{
			turnRadarRightRadians(Double.POSITIVE_INFINITY);}while(true);
	}
	 
	public void onScannedRobot(ScannedRobotEvent e) {
	    double absBearing = e.getBearingRadians()+getHeadingRadians();;
	    double gunTurnAmt = robocode.util.Utils.normalRelativeAngle(absBearing- getGunHeadingRadians());//amount to turn our gun, lead just a little bi
	    double x = e.getBearing();
	    double y = 0;//change to take into account the other person's velocity
	    if (x > 15){
	    	y = 5;
	    }
	    else if (x <= -15)
	    {
	    	y = -5;
	    }
	    double radarTurn =
	        // Absolute bearing to target
	        getHeadingRadians() + e.getBearingRadians()
	        // Subtract current radar heading to get turn required
	        - getRadarHeadingRadians();
	    setTurnRadarRightRadians(Utils.normalRelativeAngle(radarTurn));
		setTurnGunRightRadians(gunTurnAmt + y/360);
	    fire(1);
	    // ...
	}
}
