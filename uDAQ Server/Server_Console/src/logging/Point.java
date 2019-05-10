package logging;

import org.joda.time.DateTime;

public class Point {
	public DateTime x;
	public Float y;
	public Point(DateTime x_value, float y_value){
		x=x_value;
		y=y_value;
	}
	
	public String toString()
	{
		return x.toString() + ", " + y.toString();
	}
}
