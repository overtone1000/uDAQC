package utilities;

import org.joda.time.DateTime;
import org.joda.time.Duration;

public class Timer {
	private int intvl;
	private DateTime finishtime;
	public Timer(int interval_millis){
		intvl=interval_millis;
		finishtime=new DateTime().plusMillis(intvl);
	}
	public void reset(){
		finishtime=new DateTime().plusMillis(intvl)	;
	}
	public boolean reached(){
		if(finishtime.isAfterNow()){
			return false;
		}
		else{
			reset();
			return true;
		}
	}
	public long timeleft(){
		return((new Duration(new DateTime(),finishtime)).toDuration().getStandardSeconds());
	}
}
