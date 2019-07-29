package logging;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

import org.joda.time.DateTime;

public class Loghandler_File {
	private String logname;
	private FileHandler fh;
	public Loghandler_File(Level loglevel){
		
		String dir = "Errlog";
		if(!Files.exists(Paths.get(dir)))
				{try {
					Files.createDirectory(Paths.get(dir));
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}}
		
		SimpleFormatter form = new SimpleFormatter();
		
		DateTime dt = DateTime.now();
		boolean logalreadyexists=true;
		while(logalreadyexists){
			logname=dir + "/" + dt.toString() + ".log";
			logalreadyexists=Files.exists(Paths.get(logname));
		}
		
		try {
			fh = new FileHandler(logname);
		} catch (SecurityException | IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		if(fh!=null){
			fh.setFormatter(form);
			fh.setLevel(loglevel);
		}
	}
	
	public void addlog(Logger log){
		if(fh!=null){
			log.addHandler(fh);
		}
	}
	
	public String GetLog(){
		String retval;
		try {
			retval=new String(Files.readAllBytes(Paths.get(logname)));
		} catch (IOException e) {
			retval=("IO Exception error accessing logger.");
		}
		return retval;
	}
}
