package fx.chart.children;

import java.util.LinkedList;

import org.joda.time.DateTime;

import fx.chart.ChartStyle;
import javafx.scene.shape.Line;
import javafx.scene.shape.Polygon;
import javafx.scene.shape.Shape;
import trm.logging.Point;

public class Curve {
	private CurveSet parent;

	public Curve(CurveSet parent){
		this.parent=parent;
	}
			
	public java.util.ArrayList<Point> points=new java.util.ArrayList<Point>();
	/*
	public void addpoint(DateTime x_value, float y_value){
		points.add(new Point(x_value,y_value));
	}
	public void addpoint(Point p){
		points.add(p);
	}
	public void addpoint(java.util.ArrayList<Point> p){
		points.addAll(p);
	}
	public void clearpoints(){
		points.clear();
	}
	*/
	
	public LinkedList<Shape> shapes=new LinkedList<Shape>();

	public void predraw(ChartStyle style){
		shapes.clear();
		if(points==null || points.isEmpty()){return;}
		int startn=0; int stopn=points.size()-1;
		while(points.get(startn).x.isBefore(parent.chart_min_X()) && startn<stopn){startn+=1;}
		while(points.get(stopn).x.isAfter(parent.chart_max_X()) && startn<stopn){stopn-=1;}
		
		double curvewidth=parent.getX(points.get(stopn).x)-parent.getX(points.get(startn).x);
		double stepwidth=1;
		
		if((curvewidth/stepwidth)<(double)(stopn-startn)){
			//System.out.println("Vertical draw.");
			verticaldraw(style,startn,stopn,stepwidth);
			//primitivedraw(style,startn,stopn); //for testing
		}
		else{
			//System.out.println("Primitive draw.");
			//verticaldraw(style,startn,stopn); //for testing
			primitivedraw(style,startn,stopn);
		}
	}
	
	private void verticaldraw(ChartStyle style, int startn, int stopn, double stepwidth){
		LinkedList<Shape> newshapes=new LinkedList<Shape>();
		
		int n=startn;
		double halfstepwidth=stepwidth/2;
		Line l=null;
		Line lastline=null;
		for(double x=parent.parent.left();x<parent.parent.right();x+=stepwidth){
			if(n>stopn){break;}
			boolean drawthis=false;
			float lowy=Float.POSITIVE_INFINITY; float highy=Float.NEGATIVE_INFINITY;
			//System.out.println("x is " + x);
			//System.out.println("First point x is " + parent.getX(points.get(n).x));
			while(n<stopn && parent.getX(points.get(n).x) < x-(halfstepwidth)){
				n+=1;
			}
			while(n<=stopn && parent.getX(points.get(n).x) >= x-(halfstepwidth) && parent.getX(points.get(n).x) <= x+(halfstepwidth)){
				if(points.get(n).y<lowy){lowy=points.get(n).y;}
				if(points.get(n).y>highy){highy=points.get(n).y;}
				drawthis=true;
				n+=1;
				//System.out.println("Point checked.");
			}
			
			if(lowy<parent.parent.y_min()){lowy=(float)parent.parent.y_min();}
			if(lowy>parent.parent.y_max()){lowy=(float)parent.parent.y_max();}
			if(highy<parent.parent.y_min()){highy=(float)parent.parent.y_min();}
			if(highy>parent.parent.y_max()){highy=(float)parent.parent.y_max();}
			
			/*
			if(!drawthis && n<stopn){
				float y2=(float)points.get(n+1).y;
				float y1=(float)points.get(n).y;
				float x2=(float)parent.getX(points.get(n+1).x);
				float x1=(float)parent.getX(points.get(n+1).x);
				lowy=(y2-y1)/(x2-x1)*(float)(x-0.5)+y1;
				highy=(y2-y1)/(x2-x1)*(float)(x+0.5)+y1;
				drawthis=true;
			}
			*/
			
			if(drawthis){
				l=new Line();
				switch(style){
				case LINE:				
					l.setStartX(x+(halfstepwidth));
					l.setEndX(x+(halfstepwidth));
					l.setStartY(parent.getY(lowy));
					l.setEndY(parent.getY(highy));
					l.setStroke(parent.col);
					l.setStrokeWidth(parent.strokewidth);
					newshapes.addFirst(l);
					
					if(lastline!=null){
						if(l.endYProperty().doubleValue()<lastline.startYProperty().doubleValue()){
							Line lilline=new Line();
							lilline.setStartX(x-halfstepwidth);
							lilline.setEndX(x+halfstepwidth);
							lilline.setStartY(lastline.startYProperty().doubleValue());
							lilline.setEndY(l.endYProperty().doubleValue());
							lilline.setStroke(parent.col);
							lilline.setStrokeWidth(parent.strokewidth);
							newshapes.addFirst(lilline);
						}else if(l.startYProperty().doubleValue()>lastline.endYProperty().doubleValue()){
							Line lilline=new Line();
							lilline.setStartX(x-halfstepwidth);
							lilline.setEndX(x+halfstepwidth);
							lilline.setStartY(lastline.endYProperty().doubleValue());
							lilline.setEndY(l.startYProperty().doubleValue());
							lilline.setStroke(parent.col);
							lilline.setStrokeWidth(parent.strokewidth);
							newshapes.addFirst(lilline);
						}
					}
					lastline=l;
					break;
				case BAR:
					l.setStartX(x+(halfstepwidth));
					l.setEndX(x+(halfstepwidth));
					l.setStartY(parent.getY((float)parent.parent.y_min()));
					l.setEndY(parent.getY(highy));
					l.setStroke(parent.col);
					l.setStrokeWidth(parent.strokewidth);
					newshapes.addFirst(l);
					break;
				default:break;
				}
			}else{
				lastline=null;
			}
			//System.out.println("x equals" + x + ", left is " + parent.parent.left() + ", right is " +parent.parent.right());
			//System.out.println("highy equals " + highy + ", lowy equals " + lowy);
			if(n>0){n-=1;}
			shapes=newshapes;
		}
	}
	
	private void primitivedraw(ChartStyle style, int startn, int stopn){
		LinkedList<Shape> newshapes=new LinkedList<Shape>();
		
		for(int n=startn;n<=stopn-1;n++){
			//iterate through all but the last point and make lines between the pairs
			int m=n+1;
			if(points.get(n).y==points.get(m).y){
				while(m+1<=stopn-1 && points.get(n).y==points.get(m+1).y){				 
					m+=1;
				}
			}
			
			switch(style){
			case LINE:
				Line l=new Line();
				l.setStartX(parent.getX(points.get(n).x));
				l.setEndX(parent.getX(points.get(m).x));
				l.setStartY(parent.getY(points.get(n).y));
				l.setEndY(parent.getY(points.get(m).y));
				l.setStroke(parent.col);
				l.setStrokeWidth(parent.strokewidth);
				newshapes.addFirst(l);
				break;
			case BAR:
				Polygon p=new Polygon();
				double left;double right; double topleft;double topright;
				left=parent.getX(points.get(n).x);
				right=parent.getX(points.get(m).x);
				topleft=parent.getY(points.get(n).y);
				topright=parent.getY(points.get(m).y);
				p.getPoints().add(left);
				p.getPoints().add(parent.parent.bottom());
				p.getPoints().add(left);
				p.getPoints().add(topleft);
				p.getPoints().add(right);
				p.getPoints().add(topright);
				p.getPoints().add(right);
				p.getPoints().add(parent.parent.bottom());
				p.setStroke(parent.col);
				p.setFill(parent.col);
				p.setStrokeWidth(parent.strokewidth);
				newshapes.addFirst(p);
				break;
			default:break;
			}
			n=m-1; //is going to add another during next for iteration, so -1
			shapes=newshapes;
		}
	}
	
	public DateTime min_X(){
		if(points==null || points.isEmpty() || points.get(0)==null){return DateTime.now().plusYears(100);}
		DateTime retval;
		retval=points.get(0).x;
		for(int n=1;n<points.size();n++){
			if(points.get(n).x.isBefore(retval)){retval=points.get(n).x;}
		}
		return retval;
	}
	public DateTime max_X(){
		if(points==null || points.isEmpty() || points.get(0)==null){return new DateTime(0);}
		DateTime retval;
		retval=points.get(0).x;
		for(int n=1;n<points.size();n++){
			if(points.get(n).x.isAfter(retval)){retval=points.get(n).x;}
		}
		return retval;
	}
	public float min_Y(){
		if(points==null || points.isEmpty() || points.get(0)==null){return Float.POSITIVE_INFINITY;}
		float retval;
		retval=points.get(0).y;
		for(int n=1;n<points.size();n++){
			if(points.get(n).y<retval){retval=points.get(n).y;}
		}
		return retval;
	}
	public float max_Y(){
		if(points==null || points.isEmpty() || points.get(0)==null){return Float.NEGATIVE_INFINITY;}
		float retval;
		retval=points.get(0).y;
		for(int n=1;n<points.size();n++){
			if(points.get(n).y>retval){retval=points.get(n).y;}
		}
		return retval;
	}
}
