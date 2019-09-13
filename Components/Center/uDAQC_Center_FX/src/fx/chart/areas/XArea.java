package fx.chart.areas;

import javafx.application.Platform;
import javafx.geometry.VPos;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;

import org.joda.time.DateTime;

import fx.chart.Chart;

public class XArea {
	private Chart parent;
	public XArea(Chart parent){
		this.parent=parent;
		initialize_elements();
	}
	
	public DateTime x_min(){return parent.x_min;}
	public DateTime x_max(){return parent.x_max;}
	public void hideXmarker(){
		s.marker_visible=false;
		Platform.runLater(s);
	}
	public void setXmarker(double x, DateTime t, boolean inchartarea){
		s.x=x;
		s.t=t;
		s.marker_visible=inchartarea;
		Platform.runLater(s);
	}
	
	public void setzoom(double x, DateTime t, boolean visible){
		s.zoom_x=x;
		s.zoommarker_visible=visible;
		Platform.runLater(s);
	}
	
	private Text tex_x_min=new Text();
	private Text tex_x_max=new Text();
	private Line marker=new Line();
	private Line zoommarker=new Line();
	private Text markerlabel=new Text();
		
	public double left(){return parent.chart_left();}
	public double right(){return parent.chart_right();}
	public double top(){return parent.chart_top();}
	public double bottom(){return parent.chart_bottom();}
	
	private void initialize_elements(){
		//Axis Labels
		tex_x_min.setTextOrigin(VPos.CENTER);
		tex_x_max.setTextOrigin(VPos.CENTER);
		markerlabel.setTextOrigin(VPos.CENTER);
		tex_x_min.setTextAlignment(TextAlignment.LEFT);
		tex_x_max.setTextAlignment(TextAlignment.RIGHT);
		markerlabel.setTextAlignment(TextAlignment.CENTER);
		parent.getChildren().add(tex_x_min);
		parent.getChildren().add(tex_x_max);
		parent.getChildren().add(marker);
		parent.getChildren().add(markerlabel);
		parent.getChildren().add(zoommarker);
		draw_axis_labels();
	}
	
	private double label_y(){return (bottom()+parent.scene_bottom)/2;}
	public void draw_axis_labels(){
								
		tex_x_min.setText(Chart.dtf.print(x_min()));
		tex_x_max.setText(Chart.dtf.print(x_max()));
		
		tex_x_min.setX(left());
		tex_x_min.setY(label_y());
		
		tex_x_max.setX(right()-tex_x_max.getLayoutBounds().getWidth());
		tex_x_max.setY(label_y());
		
		
	}
	
	private setupXmarker s=new setupXmarker();
	public class setupXmarker implements Runnable{
		public double x;
		public DateTime t;
		public boolean marker_visible;
		public double zoom_x;
		public boolean zoommarker_visible;
		@Override
		public void run() {	
			marker.setVisible(marker_visible);
			marker.setStartX(x);
			marker.setEndX(x);
			marker.setStartY(parent.chart_top());
			marker.setEndY(parent.chart_bottom());
			marker.setStroke(Color.BLACK);
			marker.setStrokeWidth(2);
			
			markerlabel.setVisible(marker_visible);
			markerlabel.setText(Chart.dtf.print(t));
			markerlabel.setX(x-tex_x_max.getLayoutBounds().getWidth()/2);
			markerlabel.setY(label_y());
			
			zoommarker.setVisible(zoommarker_visible);
			zoommarker.setStartX(zoom_x);
			zoommarker.setEndX(zoom_x);
			zoommarker.setStartY(parent.chart_top());
			zoommarker.setEndY(parent.chart_bottom());
			zoommarker.setStroke(Color.BLACK);
			zoommarker.setStrokeWidth(2);
		}	
	}
}
