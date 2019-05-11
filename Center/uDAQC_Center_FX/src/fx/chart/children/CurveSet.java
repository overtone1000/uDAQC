package fx.chart.children;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.concurrent.locks.ReentrantLock;

import org.joda.time.DateTime;

import fx.chart.ChartStyle;
import fx.chart.areas.ChartArea;
import javafx.scene.Group;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import trm.logging.Point;

public class CurveSet {
	public String name = "";
	public Text label=new Text();
	public Color col = Color.RED;
	public ChartArea parent;
	public int strokewidth=1;
	private Group g = new Group();
	
	java.util.ArrayList<Curve> curves = new java.util.ArrayList<Curve>();
	public CurveSet(ChartArea parent){
		this.parent=parent;
	}
	public void setcolor(Color col){
		this.col=col;
	}
	public Group getgroup(){
		return g;
	}
	
	public double getX(DateTime x){
		double retval;
		retval=((double)(x.getMillis()-parent.x_min().getMillis()))/((double)(parent.x_max().getMillis()-parent.x_min().getMillis()));
		retval=retval*parent.right()+(1-retval)*parent.left();
		return retval;
	}
	public double getY(float y){
		double retval;
		retval=(y-parent.y_min())/(parent.y_max()-parent.y_min());
		retval=retval*parent.top()+(1-retval)*parent.bottom();
		return retval;
	}
	
	public DateTime chart_min_X(){return parent.x_min();}
	public DateTime chart_max_X(){return parent.x_max();}
	
	public DateTime curve_min_X(){
		DateTime retval=null;
		DateTime newval;
		for(Curve c:curves){
			newval=c.min_X();
			if(newval!=null){
				if(retval==null){retval=newval;}
				else if(retval.isAfter(newval)){retval=newval;}
			}
		}
		return retval;
	}
	public DateTime curve_max_X(){
		DateTime retval=null;
		DateTime newval;
		for(Curve c:curves){
			newval=c.max_X();
			if(newval!=null){
				if(retval==null){retval=newval;}
				else if(retval.isBefore(newval)){retval=newval;}
			}
		}
		return retval;
	}
	public float curve_min_Y(){
		float retval=Float.POSITIVE_INFINITY;
		float newval;
		for(Curve c:curves){
			newval=c.min_Y();
			if(retval>newval){retval=newval;}
		}
		return retval;
	}
	public float curve_max_Y(){
		float retval=Float.NEGATIVE_INFINITY;
		float newval;
		for(Curve c:curves){
			newval=c.max_Y();
			if(retval<newval){retval=newval;}
		}
		return retval;
	}
	
	public float y_of_x(DateTime x){
		for(Curve c:curves){
			for(int n=1;n<c.points.size();n++){
				if(x.isBefore(c.points.get(n).x) && x.isAfter(c.points.get(n-1).x)){
					Point p2=c.points.get(n);
					Point p1=c.points.get(n-1);
					double d=(p2.x.getMillis()-p1.x.getMillis());
					double t=(x.getMillis()-p1.x.getMillis());
					return (float)(p2.y*(t/d)+p1.y*(1-t/d));
				}
			}
		}
		return Float.NaN;
	}
	
	public float latest_y(){
		if(curves.isEmpty()){return 0;}
		ArrayList<Point> points=curves.get(curves.size()-1).points;
		if(points.isEmpty()){return 0;}
		Point point=points.get(points.size()-1);
		return point.y;
	}
	
	private ReentrantLock curvesetlock=new ReentrantLock();
	
	
	public void setpoints(LinkedList<ArrayList<Point>> pointsets){
		curvesetlock.lock();
		try{
			while(curves.size()>pointsets.size()){curves.remove(0);}
			while(curves.size()<pointsets.size()){curves.add(new Curve(this));}
			for(int n=0;n<pointsets.size();n++){
				curves.get(n).points=pointsets.get(n);
			}
		}
		finally{
			curvesetlock.unlock();
		}
	}
	
	public void setpoints_one(ArrayList<Point> pointset){
		curvesetlock.lock();
		try{
			while(curves.size()>1){curves.remove(0);}
			while(curves.size()<1){curves.add(new Curve(this));}
			curves.get(0).points=pointset;
		}
		finally{
			curvesetlock.unlock();
		}
	}
	
	public void predraw(ChartStyle cs){
		curvesetlock.lock();
		try{
			for(Curve c:curves){
				c.predraw(cs);
			}
		}
		finally{
			curvesetlock.unlock();
		}
	}
	
	public void draw(){
		g.getChildren().clear();
		for(Curve c:curves){
			if(!(c.shapes==null) && !c.shapes.isEmpty()){
				g.getChildren().addAll(c.shapes);
			}
		}
	}
}
