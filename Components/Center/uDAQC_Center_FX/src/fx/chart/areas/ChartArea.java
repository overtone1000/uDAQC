package fx.chart.areas;

import org.joda.time.DateTime;

import fx.chart.Chart;
import fx.chart.ChartStyle;
import fx.chart.children.CurveSet;
import javafx.application.Platform;
import javafx.geometry.VPos;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;

public class ChartArea {
	private Chart parent;
	private String name;
	private double y_margin=0.05;
	private ChartStyle style;
	
	public ChartArea(String name, Chart parent, ChartStyle style)
	{
		this.name=name;
		this.parent=parent;
		//this.height=height;
		this.style=style;
		initialize_elements();
	}
	public ChartArea(String name, Chart parent, double height, double minYclamp, double maxYclamp, double y_margin, ChartStyle style)
	{
		this(name,parent,style);
		this.minY_clamp=minYclamp;
		this.maxY_clamp=maxYclamp;	
		this.y_margin=y_margin;
	}
	
	public String name(){
		return name;
	}
	
	private double top_val;
	private double bottom_val;
	//private double height;
	//public double getheight(){return height;}
	
	private double y_min;
	private double y_max;
	
	public double y_min(){
		if(y_override){return y_min_o;}
		else{return y_min;}
	}
	public double y_max(){
		if(y_override){return y_max_o;}
		else{return y_max;}
		}
	
	private double y_min_o;
	private double y_max_o;
	public boolean y_override=false;
	public void set_y_overridevalues(double y_min_override, double y_max_override){
		y_min_o=y_min_override;
		y_max_o=y_max_override;
	}
	
	public DateTime x_min(){return parent.x_min;}
	public DateTime x_max(){return parent.x_max;}
	private Text tex_y_min=new Text();
	private Text tex_y_max=new Text();
	
	public double left(){return parent.chart_left();}
	public double right(){return parent.chart_right();}
	public double top(){return top_val;}
	public double bottom(){return bottom_val;}
	public void setbottom(double val){bottom_val=val;}
	public void settop(double val){top_val=val;}
	
	private java.util.ArrayList<CurveSet> curvesets=new java.util.ArrayList<CurveSet>();
	private Line[] box = {new Line(),new Line(), new Line(), new Line()};
	
	public double y_to_value(double Y){
		double retval=(Y-top())/(bottom()-top())*(y_min()-y_max())+y_max();
		System.out.println("Input=" + Y);
		System.out.println("Output=" + retval);
		System.out.println("y_max=" + retval);
		System.out.println("y_min=" + retval);
		System.out.println("top=" + retval);
		System.out.println("bottom=" + retval);
		return retval;
	}
	
	public void addCurveSet(String name, Object key, CurveSet c){
		curvesets.add(c);
		c.name=name;
		c.label=new Text(name);
		c.label.setTextOrigin(VPos.CENTER);
		c.label.setTextAlignment(TextAlignment.LEFT);
		c.label.setFill(c.col);
		parent.getChildren().add(c.getgroup());
		parent.getChildren().add(c.label);
	}
	
	public void removearea(){
		for(CurveSet c:curvesets){
			parent.getChildren().removeAll(box);
			parent.getChildren().remove(tex_y_min);
			parent.getChildren().remove(tex_y_max);
			parent.getChildren().remove(c.getgroup());
			parent.getChildren().remove(c.label);
		}
		parent.getChildren().remove(marker);
		parent.getChildren().remove(zoommarker);
	}
	
	public void setupXmarkerlabel(){
		for(CurveSet c:curvesets){
			setupXmarker s=new setupXmarker();
			s.c=c;
			s.visible=true;
			s.value=c.latest_y();
			Platform.runLater(s);
		}
	}
	
	public void setupXmarkerlabel(DateTime dt){
		for(CurveSet c:curvesets){
			setupXmarker s=new setupXmarker();
			s.c=c;
			s.visible=true;
			s.value=c.y_of_x(dt);
			Platform.runLater(s);
		}
	}
	
	public void hideYmarker(){
		s.marker_visible=false;
		Platform.runLater(s);
	}
	
	public void setYzoom(double y, boolean visible){
		s.zoom_y=y;
		s.zoommarker_visible=visible;
		Platform.runLater(s);
	}
	
	public void setupYmarkerlabel(double y){
		s.y=y;
		s.marker_visible=true;
		Platform.runLater(s);
	}
	
	private class setupXmarker implements Runnable{
		public CurveSet c;
		public float value;
		public boolean visible;
		@Override
		public void run() {
			if(visible){c.label.setText(c.name + Chart.newLine + Chart.prettylabel(value));}
			else{c.label.setText(c.name);}
		}	
	}
	
	//public CurveSet getCurveSet(String name){
	//	return curvesets.get(name);
	//}
	
	//public Set<Object> getKeys(){
	//	return curvesets.keySet();
	//}
	
	private void initialize_elements(){
		//Axes
		parent.getChildren().addAll(box);
		
		for(Line b:box){
			b.setStroke(Color.LIGHTGRAY);
			b.setStrokeWidth(2);
		}	
		draw_box();
		
		//Axis Labels
		tex_y_min.setTextOrigin(VPos.BOTTOM);
		tex_y_max.setTextOrigin(VPos.TOP);
		tex_y_min.setTextAlignment(TextAlignment.RIGHT);
		tex_y_max.setTextAlignment(TextAlignment.RIGHT);
		parent.getChildren().add(tex_y_min);
		parent.getChildren().add(tex_y_max);
		parent.getChildren().add(marker);
		parent.getChildren().add(zoommarker);
		draw_axis_labels();
	}
	
	private Line marker=new Line();
	private setupYmarker s=new setupYmarker();
	private Line zoommarker=new Line();
	private class setupYmarker implements Runnable{
		public double y;
		public boolean marker_visible;
		public double zoom_y;
		public boolean zoommarker_visible;
		@Override
		public void run() {	
			marker.setVisible(marker_visible);
			marker.setStartY(y);
			marker.setEndY(y);
			marker.setStartX(parent.chart_left());
			marker.setEndX(parent.chart_right());
			marker.setStroke(Color.BLACK);
			marker.setStrokeWidth(2);
					
			zoommarker.setVisible(zoommarker_visible);
			zoommarker.setStartY(zoom_y);
			zoommarker.setEndY(zoom_y);
			zoommarker.setStartX(parent.chart_left());
			zoommarker.setEndX(parent.chart_right());
			zoommarker.setStroke(Color.BLACK);
			zoommarker.setStrokeWidth(2);
		}
	}
	
	private void draw_box(){
		//x bottom
		box[0].setStartX(parent.scene_left);
		box[0].setStartY(bottom());
		box[0].setEndX(right());
		box[0].setEndY(bottom());
		//x top
		box[1].setStartX(parent.scene_left);
		box[1].setStartY(top());
		box[1].setEndX(right());
		box[1].setEndY(top());
		//y left
		box[2].setStartX(left());
		box[2].setStartY(bottom());
		box[2].setEndX(left());
		box[2].setEndY(top());
		//y right
		box[3].setStartX(right());
		box[3].setStartY(bottom());
		box[3].setEndX(right());
		box[3].setEndY(top());
		
	}
	
	private void draw_axis_labels(){						
		tex_y_min.setText(Chart.prettylabel(y_min()));
		tex_y_max.setText(Chart.prettylabel(y_max()));
				
		tex_y_min.setX((left()-parent.scene_left-tex_y_min.getLayoutBounds().getWidth())/2);
		tex_y_min.setY(bottom());
		
		tex_y_max.setX((left()-parent.scene_left-tex_y_max.getLayoutBounds().getWidth())/2);
		tex_y_max.setY(top());
	}
	
	private void draw_curve_labels(){
		double heightfactor=1;
		double heightchange=(bottom()-top())/(curvesets.size()+1);
		for(CurveSet c:curvesets){
			c.label.setX(right());
			c.label.setY(top()+heightchange*heightfactor);
			heightfactor+=1;
		}
	}

	
	private double minY_clamp=Double.POSITIVE_INFINITY;
	private double maxY_clamp=Double.NEGATIVE_INFINITY;
	private double min_min_Y=Double.NEGATIVE_INFINITY;
	private double max_max_Y=Double.POSITIVE_INFINITY;
	public DateTime min_X(){
		DateTime retval=DateTime.now().plusYears(100);
		for(CurveSet c:curvesets){
			DateTime x;
			x=c.curve_min_X();
			if(x!=null && x.isBefore(retval)){retval=x;}
		}
		return retval;
	}
	public DateTime max_X(){
		DateTime retval=new DateTime(0);
		for(CurveSet c:curvesets){
			DateTime x;
			x=c.curve_max_X();
			if(x!=null && x.isAfter(retval)){retval=x;}
		}
		return retval;
	}
	protected void autorange_Y(){
		double new_min_Y=minY_clamp;
		double new_max_Y=maxY_clamp;
		for(CurveSet c:curvesets){
			float y;
			y=c.curve_min_Y();
			if(new_min_Y>y){new_min_Y=y;}
			y=c.curve_max_Y();
			if(new_max_Y<y){new_max_Y=y;}
		}
		
		if(min_min_Y>new_min_Y){new_min_Y=min_min_Y;}
		if(max_max_Y<new_max_Y){new_max_Y=max_max_Y;}
		
		double diff=new_max_Y-new_min_Y;
		if(diff<=0)
		{
			diff=1.0f;
		}
		
		y_min=new_min_Y-diff*y_margin;
		y_max=new_max_Y+diff*y_margin;
	}
	
	public void predraw(){
		autorange_Y();
		for(CurveSet c:curvesets){
			c.predraw(this.style);
		}		
	}
	
	public void draw_peripherals(){
		draw_box();
		draw_axis_labels();
		draw_curve_labels();
	}
	
	public void draw_curves(){
		for(CurveSet c:curvesets){
			c.draw();
		}
	}
}
