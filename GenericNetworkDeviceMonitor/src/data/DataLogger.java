package data;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Iterator;
import java.util.logging.Logger;

import org.apache.commons.lang.exception.ExceptionUtils;
import org.joda.time.DateTime;
import org.joda.time.Duration;
import trm.fx_chart.children.Point;
import trm.logging.Data_State;
import trm.logging.Data_State_ComFlags;
import trm.logging.Data_State_Enum_Interface;
import trm.logging.LoggerListener;
import trm.tcp.Command;
import trm.priv.tcp.TCP_Constants;
import trm.priv.tcp.TCP_Constants.Commands.Cabinet;

public class DataLogger<DS extends Data_State>{
	DS instanceforfunctions;
	private final static String minute_dir="minute_data";
	private final static String hour_dir="hour_data";
	private final static String filesep=System.getProperty("file.separator");
	private boolean keepfilelog;
	private Logger log;
	private LoggerListener listener;	
	
	private DS currentstate;
	
	public DS CurrentState(){
		return currentstate;
	}
	/*
	final static int MINUTE_AVERAGE_SECONDS=2;
	final static int HOUR_AVERAGE_MINUTES=1;
	final static int RAW_MINUTES=2;
	final static int TOTAL_AVERAGED_MINUTES=2;
	final static int TOTAL_AVERAGED_HOURS=1;
	*/
	
	final static int MINUTE_AVERAGE_SECONDS=60; //Average 60 seconds of data to get a minute averaged point
	final static int HOUR_AVERAGE_MINUTES=60; //Average 60 minutes of data to get an hour averaged point

	//final static int MINUTE_AVERAGE_SECONDS=3; //Average 60 seconds of data to get a minute averaged point
	//final static int HOUR_AVERAGE_MINUTES=1; //Average 60 minutes of data to get an hour averaged point
	
	final static int RAW_MINUTES=15; //15 minutes of raw data
	final static int TOTAL_AVERAGED_MINUTES=7*24*60; //One week worth of minute data
	final static int TOTAL_AVERAGED_HOURS=30*24; //Thirty days worth of hour data
	
	
	private java.util.TreeSet<DS> raw_60min=new java.util.TreeSet<DS>(); //60 minutes of raw data (3600 points)
	private java.util.TreeSet<DS> minute_720hr=new java.util.TreeSet<DS>(); //7*24 hours of 1 minute averaged data (43200 points)
	private java.util.TreeSet<DS> hour_365days=new java.util.TreeSet<DS>(); //365 days of 1 hour averaged data (8760 points)
	
	private Controller_Enum controller;
	
	public DataLogger(Logger log, boolean keepfilelog, LoggerListener listener, Controller_Enum controller, DS instanceforfunctions){
		this.log=log;
		this.keepfilelog=keepfilelog;
		this.listener=listener;
		this.controller=controller;
		this.instanceforfunctions=instanceforfunctions;
		loadhistory(minute_720hr, Paths.get(minute_dir));
		loadhistory(hour_365days, Paths.get(hour_dir));
	}

	private class HistoryIterator implements Iterator<Command>{
		private Iterator<?> i;
		private Time_Enum time;
		private Controller_Enum controller;
		Object clone;
		public HistoryIterator(Time_Enum time, Controller_Enum controller){
			this.controller=controller;
			this.time=time;
			switch(time){
			case RAW:
				clone=raw_60min.clone();
				if(clone instanceof java.util.TreeSet<?>){
					i=((java.util.TreeSet<?>) clone).iterator();
				}
				break;
			case MINUTE:
				clone=minute_720hr.clone();
				if(clone instanceof java.util.TreeSet<?>){
					i=((java.util.TreeSet<?>) clone).iterator();
				}
				break;
			case HOUR:
				clone=hour_365days.clone();
				if(clone instanceof java.util.TreeSet<?>){
					i=((java.util.TreeSet<?>) clone).iterator();
				}
				break;
			default:
				break;
			}
		}
		@Override
		public boolean hasNext() {
			return i.hasNext();
		}

		@Override
		public Command next() {
			Data_State_ComFlags comflags=new Data_State_ComFlags();
			comflags.time_included=true;
			comflags.nature_included=true;
			int commandheading=-1;
			switch(time){
			case RAW:
				switch(controller)
				{
				case Cabinet: commandheading=TCP_Constants.Commands.Cabinet.control_report_raw;break;
				case Gravity: commandheading=TCP_Constants.Commands.Cabinet.gravity_report_raw;break;
				}
				return new Command(commandheading,((DS)i.next()).toBytes(comflags));
			case MINUTE:
				switch(controller)
				{
				case Cabinet: commandheading=TCP_Constants.Commands.Cabinet.control_report_avg_minute;break;
				case Gravity: commandheading=TCP_Constants.Commands.Cabinet.gravity_report_avg_minute;break;		
				}
				return new Command(commandheading,((DS)i.next()).toBytes(comflags));
			case HOUR:
				switch(controller)
				{
				case Cabinet: commandheading=TCP_Constants.Commands.Cabinet.control_report_avg_hour;break;
				case Gravity: commandheading=TCP_Constants.Commands.Cabinet.gravity_report_avg_hour;break;
				}
				return new Command(commandheading,((DS)i.next()).toBytes(comflags));
			default:
				return null;
			}
		}
	}
	
	public Iterator<Command> historytosend(Time_Enum time){
		return new HistoryIterator(time,this.controller);
	}
	
	private void loadhistory(java.util.TreeSet<DS> tree, Path p){
		if(!keepfilelog){return;}
		try {
			Files.createDirectories(p);
		} catch (IOException e1) {
			log.severe(ExceptionUtils.getStackTrace(e1));
		}
		
		DirectoryStream<Path> stream;
				
		try {
			stream = Files.newDirectoryStream(p, "*" + filesuffix());
			for(Path f:stream){
				try {
					Data_State_ComFlags comflags=new Data_State_ComFlags();
					comflags.time_included=true;
					comflags.nature_included=true;
					tree.add((DS)(instanceforfunctions.fromFile(f,comflags).Clone()));
				} catch (Exception e) {
					log.severe(ExceptionUtils.getStackTrace(e));
				}
			}
		} catch (IOException e) {
			log.severe(ExceptionUtils.getStackTrace(e));
		}
		NotifyAll();
	}
	
		public void AddHistoryState(Time_Enum time, Object state){
			if(state==null){System.out.println("Poorly formed history state.");return;}
			switch(time){
			case RAW:
				raw_60min.add((DS)state);
				break;
			case MINUTE:
				minute_720hr.add((DS)state);
				break;
			case HOUR:
				hour_365days.add((DS)state);
				break;
			default:
				break;
			}
		}
		
		public void NotifyAll(){
			listener.RawUpdated();
			listener.MinuteUpdated();
			listener.HourUpdated();
		}
		
		private String filesuffix()
		{
			//filesuffix is used to differentiate gravity and main cabinet
			String filesuffix="";
			switch(controller)
			{
			case Cabinet:
				return ".cab";
			case Gravity:
				return ".gra";
			}
			return "unknown";
		}
	
		public void AddState(DS state){
			if(state==null){System.out.println("Poorly formed state.");return;}
	 		currentstate=state;
			raw_60min.add(state);
			listener.RawUpdated();
			//if the time of the new state is one minute later than the timestamp on the last item in the minute data, add another minute data point
			//since cabinet_state_average uses the first member for its timestamp, must add two minutes
			DateTime lasttime;
			
			if(!minute_720hr.isEmpty()){
				lasttime=minute_720hr.last().time;
			}else{
				lasttime=raw_60min.first().time;
			}
		
			if(state.time.isAfter(lasttime.plusSeconds(MINUTE_AVERAGE_SECONDS))){
				System.out.println("Averaging minute data.");
				Iterator<DS> i;
				i=raw_60min.descendingIterator();
				DS first=null;
				do{
					if(i.hasNext()){first=i.next();}else{break;}
				}while(first.time.isAfter(lasttime));
				Data_State[] passarr=new Data_State[1];
				passarr=raw_60min.tailSet(first, false).toArray(passarr);
				if(passarr.length==1 && passarr[0]==null){passarr=null;}
				if(passarr==null){System.out.println("Need to initialize passarr to something other than null.");}
				if(passarr!=null && passarr.length>0){
					DS unclonednewavgstate=(DS)instanceforfunctions.CreateAverageState(passarr);
					DS newavgstate=(DS)unclonednewavgstate.Clone();
					newavgstate.filename=minute_dir + filesep + newavgstate.time.getMillis() + filesuffix();
					minute_720hr.add((DS)newavgstate.Clone());
					listener.MinuteUpdated();
					save_state_average((DS)newavgstate.Clone());
					System.out.println("Minute averaging successful. This time: " + newavgstate.time.toString() + ". " + minute_720hr.size() + " minute points.");
					System.out.println(newavgstate.toString());
				}
			}
			
			//if the time of the new state is one hour later than the timestamp on the last item in the hour data, add another hour data point
			//since cabinet_state_average uses the first member for its timestamp, must add two hours
				
			if(!hour_365days.isEmpty()){
				lasttime=hour_365days.last().time;
			}else if(!minute_720hr.isEmpty()){
				lasttime=minute_720hr.first().time;
			}else{
				lasttime=new DateTime(0);
			}
			
			if(!minute_720hr.isEmpty() && state.time.isAfter(lasttime.plusMinutes(HOUR_AVERAGE_MINUTES))){
				System.out.println("Averaging hour data.");
				Iterator<DS> i;
				i=minute_720hr.descendingIterator();
				DS first=null;
				do{
					if(i.hasNext()){first=i.next();}else{break;}
				}while(first.time.isAfter(lasttime));
				
				Data_State[] passarr=new Data_State[1];
				passarr=minute_720hr.tailSet(first, false).toArray(passarr);
				if(passarr.length==1 && passarr[0]==null){passarr=null;}
				if(passarr==null){System.out.println("Need to initialize passarr to something other than null.");}
				if(passarr!=null && passarr.length>0){
					DS newavgstate=(DS)instanceforfunctions.CreateAverageState(passarr).Clone();
					newavgstate.CreateAverageState(passarr);
					newavgstate.filename=hour_dir + filesep + newavgstate.time.getMillis() + filesuffix();
					hour_365days.add((DS)newavgstate.Clone());
					listener.HourUpdated();
					save_state_average((DS)newavgstate.Clone());
					System.out.println("Hour averaging successful. This time: " + newavgstate.time.toString() + ". " + hour_365days.size() + " hour points.");
				}
			}
			
			trimlists();
		}

	private void save_state_average(DS state){
		if(!keepfilelog){return;}
		try {
			Data_State_ComFlags comflags = new Data_State_ComFlags();
			comflags.time_included=true;
			comflags.nature_included=true;
			state.toFile(comflags);
		} catch (FileNotFoundException e) {
			log.severe(ExceptionUtils.getStackTrace(e));
		} catch (IOException e) {
			log.severe(ExceptionUtils.getStackTrace(e));
		}
	}
	
	private void trimlists(){
		//trim up lists based on time instead of number of elements
		while(raw_60min.first()!=null && raw_60min.first().time==null){raw_60min.remove(raw_60min.first());}
		while(raw_60min.first().time.isBefore(raw_60min.last().time.minusMinutes(RAW_MINUTES))){
			raw_60min.pollFirst();
		}
		if(!minute_720hr.isEmpty()){
			while(minute_720hr.first()!=null && minute_720hr.first().time==null){
				log.info("minute_720hr entry or its time is null. Removing.");
				minute_720hr.remove(minute_720hr.first());
				}
			while(minute_720hr.first().time.isBefore(minute_720hr.last().time.minusMinutes(TOTAL_AVERAGED_MINUTES))){
				//pollFirst removes the first item from the array while providing the object to get filename and delete serialized file.
				try {
					DS first=minute_720hr.pollFirst();
					if(keepfilelog && first.filename!=null){
						Path p=Paths.get(first.filename);
						if(Files.exists(p)){Files.delete(p);}
						}
					if(first.filename==null){log.info("minute_720hr entry does not have a filename.");}
				} catch (IOException e) {
					log.info(ExceptionUtils.getStackTrace(e));
				}
			}
		}
		if(!hour_365days.isEmpty()){
			while(hour_365days.first()!=null && hour_365days.first().time==null){
				log.info("hour_365days entry or its time is null. Removing.");
				hour_365days.remove(hour_365days.first());
				}
			while(hour_365days.first().time.isBefore(hour_365days.last().time.minusHours(TOTAL_AVERAGED_HOURS))){
				//pollFirst removes the first item from the array while providing the object to get filename and delete serialized file.
				try {
					DS first=hour_365days.pollFirst();
					if(keepfilelog && first.filename!=null){
						Path p=Paths.get(first.filename);
						if(Files.exists(p)){Files.delete(p);}
						}
					if(first.filename==null){
						log.info("hour_365days entry does not have a valid filename.");
					}
				} catch (IOException e) {
					log.info(ExceptionUtils.getStackTrace(e));
				}
			}
		}
	}
	
	public java.util.ArrayList<java.util.ArrayList<Point>> getpoints(Time_Enum time, Data_State_Enum_Interface var){
		java.util.ArrayList<java.util.ArrayList<Point>> retval = new java.util.ArrayList<java.util.ArrayList<Point>>();
		Iterator<?> i;
		DS thisone=null;
		DS lastone;
		Duration breaktime;
		Duration interval;
		switch(time){
			case RAW:
				i=raw_60min.iterator();
				breaktime=Duration.standardSeconds(5);
				break;
			case MINUTE:
				i=minute_720hr.iterator();
				breaktime=Duration.standardSeconds((long)(MINUTE_AVERAGE_SECONDS*1.2));
				break;
			case HOUR:
				i=hour_365days.iterator();
				breaktime=Duration.standardMinutes((long)(HOUR_AVERAGE_MINUTES*1.2));
				break;
			default:
				i=raw_60min.iterator();
				breaktime=Duration.standardSeconds(3);
		}
		java.util.ArrayList<Point> newarray= new java.util.ArrayList<Point>();
		if(i.hasNext()){
			thisone=(DS)i.next();
			if(thisone.haspoint(var)){
				newarray.add(thisone.getpoint(var));
				}
		}
		
		while(i.hasNext()){
			lastone=thisone;
			thisone=(DS)i.next();
			interval=new Duration(lastone.time(),thisone.time());
			if(interval.isLongerThan(breaktime)){
				retval.add(newarray);
				newarray= new java.util.ArrayList<Point>();
				if(thisone.haspoint(var)){
					newarray.add(thisone.getpoint(var));
					}
			}else{
				if(thisone.haspoint(var)){
					newarray.add(thisone.getpoint(var));
					}
				if(!i.hasNext()){
					retval.add(newarray);
					}
			}
		}
		
		return retval;
	}
	/*
	private void setup_textlog(){
		int lognum=1000;
		boolean logalreadyexists=true;
		while(logalreadyexists){
			//filename="Datalog " + lognum + ".txt";
			//logalreadyexists=Files.exists(Paths.get(filename));
			lognum+=1;
		}
		
	}
	
	public void old_Addstate(DS state){
		
		try{
			writer.write(Long.toString(state.time.getMillis()));
			writer.write(',');
			writer.write(state.time.getDayOfWeek() + " at " + state.time.getHourOfDay() + ":" + state.time.getMinuteOfHour() + ":" + state.time.getSecondOfMinute());
			writer.write(',');
			writer.write(Integer.toBinaryString(state.mode_run));
			writer.write(',');
			writer.write(Integer.toBinaryString(state.mode_tempvar));
			writer.write(',');
			writer.write(Integer.toBinaryString(state.mode_control));
			writer.write(',');
			writer.write(Float.toString(state.temp_setpoint));
			writer.write(',');
			writer.write(Float.toString(state.temp_fermentor));
			writer.write(',');
			writer.write(Float.toString(state.temp_cabinet));
			writer.write(',');
			writer.write(Float.toString(state.temp_external));
			writer.write(',');
			writer.write(Float.toString(state.temp_cool_in));
			writer.write(',');
			writer.write(Float.toString(state.temp_cool_out));
			writer.write(',');
			writer.write(Float.toString(state.flow_cool));
			writer.write(',');
			writer.write(Boolean.toString(state.power_pump));
			writer.write(',');
			writer.write(Boolean.toString(state.power_refrigerator));
			writer.write(',');
			writer.write(Boolean.toString(state.power_12V));
			writer.write(',');
			writer.write(Boolean.toString(state.power_fan_refrigerator));
			writer.write(',');
			writer.write(Boolean.toString(state.power_fan_radiator));
			writer.write(',');
			writer.write(Float.toString(state.peltier));
			writer.write(newline);
			writer.flush();
		}catch(IOException e){
			log.severe(ExceptionUtils.getStackTrace(e));
			log.severe("Resetting data logger with new filename.");
			setup_textlog();
		}
		
	}
	*/
}
