package main.console;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.logging.Level;
import java.util.logging.Logger;

import trm.logging.Data_Nature;
import trm.logging.Data_State_ComFlags;
import trm.logging.LoggerListener;
import trm.logging.Loghandler_File;
import trm.tcp.Command;
import trm.tcp.IP_port;
import trm.priv.tcp.TCP_Constants.Commands;
import trm.priv.tcp.TCP_Constants.Addresses.Cabinet;

public class GNDM_Server implements LoggerListener {
	
	private final static String ss_filename="settingsstate.ss";
	private final static Logger log=Logger.getLogger(GNDM_Server.class.getName());
	
	private final static Loghandler_File loghandler=new Loghandler_File(Level.WARNING);
	public DataLogger<Cabinet_State> dl_cabinet=new DataLogger<Cabinet_State>(log,true,this,Controller_Enum.Cabinet,new Cabinet_State(Data_Nature.instance));
	public DataLogger<Gravity_State> dl_gravity=new DataLogger<Gravity_State>(log,true,this,Controller_Enum.Gravity,new Gravity_State(Data_Nature.instance));
	private Cabinet_Core_TCP_Client client_main;
	private Cabinet_Core_TCP_Client client_gravity;
	private Cabinet_Core_TCP_Server server;
	public static void main(String[] args) {
		loghandler.addlog(log);
		
		GNDM_Server m=new GNDM_Server();
		m.start_cabinet();
	}
	
	public void start_cabinet(){
		loadSettingState();
		this.client_main=new Cabinet_Core_TCP_Client("Console_Client_Cabinet_Main", Cabinet.IP_port_cabinet_main,this,log);
		this.client_gravity=new Cabinet_Core_TCP_Client("Console_Client_Cabinet_Gravity", Cabinet.IP_port_gravity,this,log);
		this.server=new Cabinet_Core_TCP_Server(Cabinet.port_pi,this,log);
	}
	
	public void stop_cabinet(){
		this.client_main.dispose();
		this.client_gravity.dispose();
		this.server.dispose();
	}
	
	public void messagechange (String str){
		System.out.println("\033[H\033[2J");
		System.out.println(str);
	}
	
	public void statechange_cabinet(Cabinet_State state){
		System.out.println("Cabinet state received by core.");
		dl_cabinet.AddState(state);
		//State change should send the control report with the time but not the averaged state.
		Data_State_ComFlags comflags = new Data_State_ComFlags();
		comflags.time_included=true;
		comflags.nature_included=true;
		server.BroadCast_Command(new Command(Commands.Cabinet.control_report,state.toBytes(comflags)));
	}
	
	public void statechange_gravity(Gravity_State state){
		System.out.println("Gravity state received by core.");
		dl_gravity.AddState(state);
		//State change should send the control report with the time but not the averaged state.
		Data_State_ComFlags comflags = new Data_State_ComFlags();
		comflags.time_included=true;
		comflags.nature_included=true;
		server.BroadCast_Command(new Command(Commands.Cabinet.gravity_report,state.toBytes(comflags)));
	}
	
	public void command_direct_to_cabinet_main(Command c){
		client_main.Send_Command(c);
	}
	
	public void command_direct_to_cabinet_gravity(Command c){
		client_gravity.Send_Command(c);
	}
		
	public void ReceivedSettingsState(Cabinet_State state){
		System.out.println("ReceivedSettingsState");
		setting_state=state;
		saveSettingState();
		SetParameters();
		SetManualOutputs();
		SetSimulatedInputs();
		SetPIDParameters();
	}
	
	private void saveSettingState(){
		try {
			Data_State_ComFlags comflags = new Data_State_ComFlags();
			comflags.time_included=false;
			comflags.nature_included=false;
			setting_state.filename=ss_filename;
			setting_state.toFile(comflags);
		} catch (IOException e) {
			log.warning(e.getMessage());
		}
	}
	private void loadSettingState(){
		try {
			Data_State_ComFlags comflags = new Data_State_ComFlags();
			comflags.time_included=false;
			comflags.nature_included=false;
			setting_state.fromFile(Paths.get(ss_filename),comflags);
		} catch (Exception e) {
			log.warning(e.getMessage());
			Data_State_ComFlags comflags=new Data_State_ComFlags();
			comflags.nature_included=false;
			comflags.time_included=false;
			setting_state=new Cabinet_State(Data_Nature.instance);
		}
		saveSettingState();
	}
	public Cabinet_State setting_state;
	public void SetParameters(){
		client_main.Send_Command(new Command(Commands.Cabinet.parameters,setting_state.parameters_byte()));
	}
	public void SetSimulatedInputs(){
		client_main.Send_Command(new Command(Commands.Cabinet.simulated_input,setting_state.siminput_byte()));
	}
	public void SetManualOutputs(){
		client_main.Send_Command(new Command(Commands.Cabinet.manual_output,setting_state.manoutput_byte()));
	}
	public void SetPIDParameters(){
		client_main.Send_Command(new Command(Commands.Cabinet.update_PIDparams,setting_state.PIDparams_byte()));
	}
	@Override
	public void RawUpdated() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void MinuteUpdated() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void HourUpdated() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void clientcommand(Command c) {
		// TODO Auto-generated method stub
		
	}
}

