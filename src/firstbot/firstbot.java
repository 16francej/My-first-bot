package firstbot; 
import robocode.Robot;
import robocode.ScannedRobotEvent;

public class firstbot extends Robot {
	
	private int i = 0; 
	private double x = 0;
	private double y = 0;
	public void run(){
		setAdjustRadarForRobotTurn(true);
		while(true){
		if (i%2==0)
			{turnRadarLeft(360);}
			else if (i%2!=0)
				{turnRadarRight(360);}
		}}
	public void onScannedRobot(ScannedRobotEvent e){
		if (i%2==0)
		{x = getRadarHeading();
			turnRadarLeft(360);}
		else if (i%2!=0)
			{y = getRadarHeading();
			turnRadarRight(360);
			turnGunRight((x+y)/2);}
		
		fire(1);
		i++;
	}
}
