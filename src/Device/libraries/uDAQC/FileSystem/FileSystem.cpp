#include "FileSystem.h"

namespace ESP_Managers
{
	namespace FileSystem
	{
		bool SPIFFS_enabled=false;
		const String file_networks=R"(/networklist.txt)";
		const String file_credentials=R"(/credslist.txt)";
		bool Initialize()
		{
			DEBUG_println("Initializing SPIFFS");

			if(SPIFFS_enabled){
				return true;
			}else{
				DEBUG_println("SPIFFS already configured.");
			}

			//
			String realSize = String(ESP.getFlashChipRealSize());
			String ideSize = String(ESP.getFlashChipSize());

			DEBUG_println("Real size is " + realSize);
			DEBUG_println("IDE size is " + ideSize);

			if(!SPIFFS.begin())
			{
				DEBUG_println("SPIFFS_err1, programmer flash configured?");
				return false;
			}

			FSInfo fs_info;
			DEBUG_println("Getting SPIFFS info.");
			if(!SPIFFS.info(fs_info))
			{
				DEBUG_println("SPIFFS_warn2");
				if(!SPIFFS.format())
				{
					DEBUG_println("SPIFFS_err2");
					return false;
				}
			}

			DEBUG_println("Checking if SPIFFS exists.");
			if(!SPIFFS.exists(file_networks))
			{
				DEBUG_println("SPIFFS_info3");
				SPIFFS.format();
				File f;
				f = SPIFFS.open(file_networks, "w");
				if (f)
				{
					f.close();
				}
				else
				{
					DEBUG_println("SPIFFS_err3");
					return false;
				}
			}

			SPIFFS_enabled=true;
			DEBUG_println("SPIFFS configured.");

			return true;
		}


			String read_network_SSID(int index)
			{
			  //DEBUG_println("Reading network ID.");
			  File f;
			  String retval="";
			  f=SPIFFS.open(file_networks,"r");
			  if(f){
				  f.seek(0,SeekSet);
				  String line;
				  for(int thisindex=0;thisindex<index*2;thisindex++)
				  {
					f.find(HTML_Builder::newline);
				  }//at the end of this, the stream will have moved to the start of the network of interest
				  if(f.available()){retval = f.readStringUntil(HTML_Builder::newline);}
			  }
			  //DEBUG_println("Got " + retval);
			  retval=retval.substring(0,retval.length()-1);//ditch the carriage return at the end
			  return retval;
			}

			String read_network_password(int index)
			{
			  //DEBUG_println("Reading network password.");
			  File f;
			  String retval="";
			  f=SPIFFS.open(file_networks,"r");
			  if(f){
				  f.seek(0,SeekSet);
				  String line;
				  for(int thisindex=0;thisindex<index*2;thisindex++)
				  {
					f.find(HTML_Builder::newline);
				  }//at the end of this, the stream will have moved to the start of the network of interest
				  f.find(HTML_Builder::newline);//once more to move to the password
				  if(f.available()){retval = f.readStringUntil(HTML_Builder::newline);}
			  }
			  //DEBUG_println("Got " + retval);
			  retval=retval.substring(0,retval.length()-1);//ditch the carriage return at the end
			  return retval;
			}

			void add_network(ESP8266WiFiMulti* wifiMulti, String SSID, String password)
			{
			  DEBUG_println("Adding a network.");

			  File f;
			  f=SPIFFS.open(file_networks,"a");
			  if(f){
				  f.println(SSID);
				  f.println(password);
				  f.close();
			  }
			  file_to_serial();

			  const char* SSID_char=SSID.c_str();
			  const char* pw_char=password.c_str();
			  wifiMulti->addAP(SSID_char,pw_char);
			}

			void delete_network(int nw_index)
			{
			  DEBUG_println("Deleting a network.");
			  String deletingSSID=read_network_SSID(nw_index);
			  File f;
			  String mod;
			  f=SPIFFS.open(file_networks,"r");
			  if(f){
				  mod=f.readString();
				  int index_1=0; //the index start of the remove
				  int index_2=-1; //the index stop of the remove. Initialize to -1 for the iteration to start in the right place.
				  //int thisindex=0;
				  for(int n=0;n<=nw_index;n++)
				  {
				   index_1=index_2+1;//the beginnging string index for this iteration is one after the 2nd index for the last iteration. index_2 is initialized to -1.
				   index_2=mod.indexOf(HTML_Builder::newline,index_2+1);//find the next newline after that
				   index_2=mod.indexOf(HTML_Builder::newline,index_2+1);//find the next newline after that to get to the end of this itration
				   DEBUG_println("Deletion Iteration " + (String)n);
				   DEBUG_println(mod.substring(index_1,index_2));
				  }
				  DEBUG_println("Removing from " + (String)index_1 + " to " + (String)index_2);
				  mod.remove(index_1,index_2-index_1+1);
				  f.close();
			  }
			  DEBUG_println("New file should be: ");
			  DEBUG_println(mod);
			  f=SPIFFS.open(file_networks,"w"); //open as a brand new file, discard old contents
			  if(f){
				 f.print(mod);
				 f.close();
			  }
			  file_to_serial();

			  if(deletingSSID==WiFi.SSID()) //if the current network is the one being deleted, disconnect.
			  {
				//WiFi.disconnect(false); //false keeps wifi mode the same
				 DEBUG_println("Current network deleted. Resetting.");
				 ESP.restart(); //this looks safer. The below caused a Panic error and triggered a reset.

				//delete &wifiMulti; //wifiMulti has an appropriate destructor. This will clear the list of networks, so it needs to be completely repopulated with the next statement.
				//wifiMulti=ESP8266WiFiMulti();
				//add_saved_networks(); //repopulate list
				//initWiFi_station();
				//init_server();
			  }
			}

			void clear_networks()
			{
				DEBUG_println("Clearing list of networks.");
				File f=SPIFFS.open(file_networks,"w"); //open as a brand new file, discard old contents
				f.close();
			}

			Credentials read_credentials()
			{
				Credentials retval;
				File f;
			  f=SPIFFS.open(file_credentials,"r");
			  if(f){
					String mod = f.readString();
					int index_1=mod.indexOf(HTML_Builder::newline,0);
					int index_2=mod.indexOf(HTML_Builder::newline,index_1+1);
					retval.login=mod.substring(0,index_1-1);
				  retval.password=mod.substring(index_1+1,index_2-1);
				  f.close();
			  }
				else
				{
					retval.login = "admin";
					retval.password = "password";
				}

				//retval.login = "admin";
				//retval.password = "admin";

				DEBUG_println("Login read.");
				DEBUG_println("Login: " + retval.login);
				DEBUG_println("Login length is " + (String)retval.login.length() + " characters.");
				DEBUG_println("Password: " + retval.password);
				DEBUG_println("Password length is " + (String)retval.password.length() + " characters.");

				return retval;
			}

			void replace_credentials(String login, String password)
			{
				File f=SPIFFS.open(file_credentials,"w"); //open as a brand new file, discard old contents
			  if(f){
				 f.println(login);
				 f.println(password);
				 f.close();
			  }
				DEBUG_println("Login changed.");
				DEBUG_println("Login: " + login);
				DEBUG_println("Password: " + password);
			}

			int count_networks()
			{
			  DEBUG_println("Counting networks.");
			  int retval=0;
			  File f;
			  f=SPIFFS.open(file_networks,"r");
			  if(f)
			  {
				while(f.available())
				{
				  f.readStringUntil(HTML_Builder::newline);
				  retval+=1;
				}
				retval=retval/2;
				f.close();
			  }
			  DEBUG_println((String)retval + " networks");
			  return retval;
			}

			void file_to_serial()
			{
				#ifdef DEBUG_UDAQC

				  File f;
				  f=SPIFFS.open(file_networks,"r");
				  if(f)
				  {
					DEBUG_println("Current file contents:");
					DEBUG_println(f.readString());
					f.close();
				  }
				  else
				  {
					DEBUG_println("Couldn't open file.");
				  }

				#endif
			}

			void checkflashconfig()
			 {
				#ifdef DEBUG_UDAQC

					uint32_t realSize = ESP.getFlashChipRealSize();
					uint32_t ideSize = ESP.getFlashChipSize();
					FlashMode_t ideMode = ESP.getFlashChipMode();

					DEBUG_printf("Flash real id:   %08X", ESP.getFlashChipId());
					DEBUG_printf("Flash real size: %u\n", realSize);

					DEBUG_printf("Flash ide  size: %u", ideSize);
					DEBUG_printf("Flash ide speed: %u", ESP.getFlashChipSpeed());
					DEBUG_printf("Flash ide mode:  %s", (ideMode == FM_QIO ? "QIO" : ideMode == FM_QOUT ? "QOUT" : ideMode == FM_DIO ? "DIO" : ideMode == FM_DOUT ? "DOUT" : "UNKNOWN"));

					if(ideSize != realSize) {
						DEBUG_println("Flash Chip configuration wrong!");
					} else {
						DEBUG_println("Flash Chip configuration ok.");
					}

				#endif
			 }
	};
};
