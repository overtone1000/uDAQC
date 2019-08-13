#include "Network.h"

namespace ESP_Managers
{
	namespace Network
	{
		#ifdef TLS
		ESP8266WebServerSecure webserver(443);
		#else
		ESP8266WebServer webserver(80); //web sever on port 80
		#endif

		Timer since_last_webserve;
		//String SSID_str;
		//String pw_str;
		//const char* SSID_char;
		//const char* pw_char;
		wl_status_t laststatus;
		ESP8266WiFiMulti wifiMulti;
		DNSServer dnsServer;

		bool connected=false;
		bool enable_AP=false;
		bool timestamp_updated=false;

		Timer STA_disconnected_timer=Timer();

		//Not needed with the server.on() function
		/*
		static String html_header=
		"HTTP/1.1 200 OK\r\n"
		"Content-Type: text/html\r\n\r\n"
		"<!DOCTYPE HTML_Builder>\r\n<html>\r\n";
		*/

		//html_header="<html>\n";
		//html_footer="</html>\n";

		//httpUpdater
		ESP8266HTTPUpdateServer httpUpdater;

		//const char* host = "esp8266-webupdate"; //not very interested in using DNS right now.
		//const char* update_username = "admin";
		//const char* update_password = "admin";
		//const char* ssid = "........";
		//const char* password = "........";

		const String update_path = "/update";
		const String creds_path = "/creds";
		const String new_network =  "newnetwork";
		const String manage_network =  "managenetwork";
		const String change_creds =  "changecreds";

		const String realm = "admin";

		void Initialize(Network::SecurityBundle bundle)
		{
			ESP.eraseConfig(); //This may fix some problems with AP connectivity
			WiFi.persistent(false); //This fixed the flash write poison block bug on the ESP-12 module
			Network::initWiFi_station();
			Network::init_server(bundle);

			//Web server
			Network::since_last_webserve.start();

			#ifndef TEMP_TESTING
			//IO::IO_System::web_server=ESP_Managers::Network::get_webserver();
			#endif

			//Time configurations
			int time_zone = -8; //Pacific standard
			int tz_sec = time_zone*3600;
			bool daylight_savings=false;
			int daylight_savings_adjust=0;
			if(daylight_savings)
			{
				daylight_savings_adjust = 3600;
			}
			configTime(tz_sec, daylight_savings_adjust, "time.nist.gov", "pool.ntp.org");
			settimeofday_cb(Network::time_set_cb);
		}

		void Loop()
		{
		  wl_status_t wifistatus=Network::wifiMulti.run();
		  if(wifistatus == WL_CONNECTED)
		  {
				//DEBUG_println("Status is connected.");
				//DEBUG_println("connected loop");
				if(!Network::connected)
				{
				 Network::connected=true;
				 DEBUG_println("WiFi Initialized");
				 DEBUG_println("IP address: ");
				 DEBUG_println(WiFi.localIP());

				 //This completes, but still not able to browse here. Needs Bonjour to work?
				 /*
				 if (!MDNS.begin("esp8266")) {DEBUG_println("Error setting up MDNS responder!");}
				 else{
				   DEBUG_println("mDNS responder started");
				   MDNS.addService("http", "tcp", 80);
				 }
				 */
				 if(Network::STA_disconnected_timer.isrunning())
				 {
				   Network::STA_disconnected_timer.stop();
				   Network::STA_disconnected_timer.reset();
				 }
				 Network::enable_AP=false;
				 bool disconnected;
				 disconnected=WiFi.softAPdisconnect(true);
				 DEBUG_println("Disabling access point. Function returned " + (String)disconnected);

				 #ifndef TEMP_TESTING
				 IO::System()->InitializeTCPClient();
				 IO::System()->InitializeUDP();
				 #endif
				}

				#ifndef TEMP_TESTING
				//Perform IO_System loop
				IO::System()->LoopTCPClient();
				#endif
		  }
		  else
		  {
			  //DEBUG_println("Status is disconnected.");
			  //DEBUG_println("disconnected loop");
			  if(Network::laststatus!=wifistatus)
				{
					DEBUG_println("Wifi status changed to " + (String)wifistatus);
				}
			  if(Network::connected){Network::connected=false;}
			  if(!Network::STA_disconnected_timer.isrunning()){Network::STA_disconnected_timer.start();}
			  if(!Network::enable_AP && Network::STA_disconnected_timer.elapsed()>15000)
			  {
					Network::enable_AP=true;
					Network::initWiFi_AP();
			  }
		  }
		  Network::laststatus=wifistatus;
			yield();
		  Network::dnsServer.processNextRequest(); //for AP mode only
			yield();
		  Network::webserver.handleClient(); //handle client communications
			yield();
			#ifndef TEMP_TESTING
			IO::System()->LoopUDP();
			yield();
			#endif

			//Testing time
			//timeval tv;
			//gettimeofday(&tv,nullptr);
			//time_t now = tv.tv_sec;
			//struct tm* tp = localtime(&now);
			//char buf[80];
			//strftime(buf, sizeof(buf), "%a %Y-%m-%d %H:%M:%S",tp); //time zone does not print correctly
			//DEBUG_print(buf);
			//DEBUG_println(" and " + (String)tv.tv_usec + " microseconds");
		}

		void time_set_cb()
		{
			timeval tv;
			gettimeofday(&tv,nullptr);

			#ifndef TEMP_TESTING
			IO::ESP_Internals::TimeUpdated(tv);
			DEBUG_print("Time has been set: ");
			DEBUG_println(IO::ESP_Internals::CurrentTimeAsString());
			#endif

			timestamp_updated=true;
		}

		void initWiFi_AP()
		{
		  //Start AP
		  WiFi.mode(WIFI_AP_STA);
		  DEBUG_println("Initializing access point.");
		  char* SSID=(char*)&"ESP8266_10-10-10-1";
		  IPAddress apIP(10, 10, 10, 1);
		  WiFi.softAPConfig(apIP, apIP, IPAddress(255, 255, 255, 0));
		  WiFi.softAP(SSID);
		  byte DNS_PORT=53;
		  dnsServer.setErrorReplyCode(DNSReplyCode::NoError);
		  dnsServer.start(DNS_PORT, "*", apIP); //redirect all traffic to the ESP8266
		}

		void initWiFi_station()
		{
		  //Get out of AP mode and disconnect
			DEBUG_println("Changing WiFi mode.");
		  WiFi.mode(WIFI_STA);
		  DEBUG_println("Disconnecting.");
		  WiFi.disconnect(true); //this is causing a poison block problem.
		  delay(100);

		  DEBUG_println("Scanning available networks.");
		  Serial_AvailableNetworks(); //scan probably needed when in AP mode to detect whether a transition to STA mode is warranted. maybe also be cool to put on displayed page.

		  //Configure wifimulti
		  DEBUG_println("Adding saved networks.");
		  add_saved_networks();
		}

		void add_saved_networks()
		{
		  int count=FileSystem::count_networks();
		  for(int n=0;n<count;n++)
		  {
			  DEBUG_println("Reading network SSID.");
				String SSID_str=FileSystem::read_network_SSID(n);
				//DEBUG_println("Reading network password.");
				String pw_str=FileSystem::read_network_password(n);
				//wifiMulti isn't working with the Strings pulled from the above functions. When the below is included, it works.
				//checked the addAP function, it copies values instead of using the pointer. Pointer shouldn't be needed after the function is called, so it can't
				//be a scope or variable change problem.
				//The returned error from WifiMulti.run() is that SSID isn't available.
				//SSID_str="Falcor";
				//pw_str="polonoho12345";
				//SSID_char=str_to_char(SSID_str);
				//pw_char=str_to_char(pw_str);
				const char* SSID_char=SSID_str.c_str();
				const char* pw_char=pw_str.c_str();
				DEBUG_println("Adding network: |" + SSID_str + "|" + pw_str + "|");
				//DEBUG_println("wifiMulti result = " + (String)wifiMulti.addAP("Falcor","polonoho12345"));
				DEBUG_println("wifiMulti result = " + (String)wifiMulti.addAP(SSID_char,pw_char));
		  }
		}

		bool session_authenticated()
		{
			since_last_webserve.reset();
			since_last_webserve.start();

			ESP_Managers::FileSystem::Credentials creds = ESP_Managers::FileSystem::read_credentials();
			if (webserver.authenticate(creds.login.c_str(), creds.password.c_str()))
			//Basic Auth Method with Custom realm and Failure Response
			//return server.requestAuthentication(BASIC_AUTH, www_realm, authFailResponse);
			//Digest Auth Method with realm="Login Required" and empty Failure Response
			//return server.requestAuthentication(DIGEST_AUTH);
			//Digest Auth Method with Custom realm and empty Failure Response
			//return server.requestAuthentication(DIGEST_AUTH, www_realm);
			//Digest Auth Method with Custom realm and Failure Response
			{
				DEBUG_println("Already authenticated.");
				return true;
			}
			else
			{
				DEBUG_println("Requesting authentication.");
				webserver.requestAuthentication(DIGEST_AUTH, realm.c_str());
				DEBUG_println("Sending redirect.");
				redirect();
				return false;
			}
		}

		void Serial_AvailableNetworks()
		{
		  ESP8266WiFiScanClass wifiScan;
		  int n = wifiScan.scanNetworks();
		  DEBUG_println("scan done");
		  if (n == 0)
			DEBUG_println("no networks found");
		  else
		  {
			DEBUG_print(n);
			DEBUG_println(" networks found");
			for (int i = 0; i < n; ++i)
			{
			  // Print SSID and RSSI for each network found
			  DEBUG_print(i + 1);
			  DEBUG_print(": ");
			  DEBUG_print(wifiScan.SSID(i));
			  DEBUG_print(" (");
			  DEBUG_print(wifiScan.RSSI(i));
			  DEBUG_print(")");
			  DEBUG_println((WiFi.encryptionType(i) == ENC_TYPE_NONE)?" ":"*");
			  delay(10);
			}
		  }
		  wifiScan.scanDelete();
		}

		void init_server(SecurityBundle bundle)
		{
			DEBUG_println("Initializing webserver.");
			#ifdef TLS
			//BearSSL::X509List *serverCertList = new BearSSL::X509List(server_cert);
			//BearSSL::PrivateKey *serverPrivKey = new BearSSL::PrivateKey(server_private_key);
			//webserver.setRSACert(serverCertList, serverPrivKey);
			webserver.setServerKeyAndCert_P(bundle.key, bundle.key_size, bundle.x509, bundle.x509_size);
			#endif

		  webserver.onNotFound(redirect);
			webserver.on("/generate_204", redirect);  //Android captive portal. Maybe not needed. Might be handled by notFound handler.
			webserver.on("/fwlink", redirect);  //Microsoft captive portal. Maybe not needed. Might be handled by notFound handler.
		  webserver.on("/",showbasicwificonfigpage);
		  webserver.on("/" + new_network,wifiserver_handle_newnetwork);
		  webserver.on("/" + manage_network,wifiserver_handle_managenetwork);
			webserver.on(creds_path,showcredentialpage);
			webserver.on("/" + change_creds,handlecredentialchange);

			#ifndef TEMP_TESTING
			webserver.on(IO::IOpanel_path,wifiserver_handle_showIOpanel);
			#endif

		  //Add OTA portion of webserver
			ESP_Managers::FileSystem::Credentials creds = ESP_Managers::FileSystem::read_credentials();
		  httpUpdater.setup(&webserver, update_path, creds.login, creds.password);
		  webserver.begin();
		}

		void redirect()
		{
		  serialprintallargs();

			#ifdef TLS
		  String url="https://";
			#else
			String url="http://";
			#endif

			url += my_ip().toString();

			DEBUG_println("Sending redirect to " + url);

		  /*
		  server.send(301, "text/html", html_header + redirect1 + url + redirect2 + html_footer);
		  server.client().stop();
		  */
		  webserver.sendHeader("Location", url, true);
		  webserver.send ( 302, "text/plain", ""); // Empty content inhibits Content-length header so we have to close the socket ourselves.
		  webserver.client().stop(); // Stop is needed because we sent no content length
			DEBUG_println("Redirected.");
		}

		IPAddress my_ip()
		{
			if(connected)
			{
				return WiFi.localIP();
			}
			else
			{
				return WiFi.softAPIP();
			}
		}


			void showbasicwificonfigpage()
			{
			  since_last_webserve.reset();
			  since_last_webserve.start();

				if(!session_authenticated())
				{
					return;
				}

			  DEBUG_println("Sending base response.");
			  serialprintallargs();

			  String page;
			  page = HTML_Builder::html_header;

			  float f_1024 = 1024.0;
			  	FlashMode_t ideMode = ESP.getFlashChipMode();
			  	//Working good so far, but have to use escape characters. Does not look like there's a good way to get out of this.

			  page+=
				R"(
				<h2>ESP-8266 Configuration</h2><br>

				<form action=")" + new_network + R"(" method="post">
				<h4>Add a network</h4>
				SSID:<br>
				<input type="text" name="SSID"><br>
				Password:<br>
				<input type="password" name="Password"><br>
				<p><button type="submit" name="NewNetwork">Add Network</button></p>
				</form><br>

				<form action=")" + manage_network + R"(" method="post">
				<h4>Stored networks</h4><br>
				)"
				;

			  int currentnetworks=FileSystem::count_networks();
			  for(int n=0;n<currentnetworks;n++)
			  {
				  page+=network_to_html(n,FileSystem::read_network_SSID(n));
			  }
			  uint32_t freeheap=ESP.getFreeHeap();
			  uint32_t flashchiprealsize=ESP.getFlashChipRealSize();
			  uint32_t flashchipidesize=ESP.getFlashChipSize();
			  uint32_t freesketchspace=ESP.getFreeSketchSpace();
			  uint32_t sketchsize=ESP.getSketchSize();
			  uint32_t chipID = ESP.getChipId();
			  uint32_t flashchipID = ESP.getFlashChipId();

				page +=
				R"(
				</form><br>
				<h4>
				)"
				;

				page += HTML_Builder::create_hyperlink((String)update_path, "OTA Update");

				page += HTML_Builder::breakline;

				page += HTML_Builder::create_hyperlink(creds_path, "Change Credentials");

				page += HTML_Builder::breakline;

				#ifndef TEMP_TESTING
				page += HTML_Builder::create_hyperlink((String)IO::IOpanel_path, "IO Panel");
				#endif

				page +=
				R"(
				</h4><br>
				)"
				;

				page +=
				R"(
				<h4>Status</h4>
				)"
				;

				String set_font=R"(<font color ="#ff0000">)";
			  String undo_font=R"(</font>)";

				page +="Chip ID: " + (String)chipID + HTML_Builder::breakline;
			  page +="Flash chip ID: " + (String)flashchipID + HTML_Builder::breakline;
				page +="File system size: " + (String)(((float)flashchipidesize)/f_1024) + " kB<br>";

				if(flashchiprealsize!=flashchipidesize){page+=set_font;}
				page +="Real chip size: " + (String)(((float)flashchiprealsize)/f_1024) + " kB<br>";
				if(flashchiprealsize!=flashchipidesize){page+=undo_font;}

				page +="     " + (String)(((float)sketchsize)/f_1024) + " kB used by current sketch<br>";

				if(freesketchspace<sketchsize+1024*10){page+=set_font;}
				page +="     " + (String)(((float)freesketchspace)/f_1024) + " kB available for OTA<br>";
				if(freesketchspace<sketchsize+1024*10){page+=undo_font;}

				page +="     " + (String)(((float)freeheap)/f_1024) + " kB used by the heap<br>";
				page +="Flash ide mode:  "
					+(String)(ideMode == FM_QIO ? "QIO" : ideMode == FM_QOUT ? "QOUT" : ideMode == FM_DIO ? "DIO" : ideMode == FM_DOUT ? "DOUT" : "UNKNOWN")
					+HTML_Builder::breakline;
				page +="Last reset cause: " + (String)ESP.getResetReason() + "<br><br>";

			  page += HTML_Builder::html_footer;

			  webserver.send(200, "text/html", page);
			}

			void showcredentialpage()
			{
				since_last_webserve.reset();
				since_last_webserve.start();

				if(!session_authenticated())
				{
					return;
				}

				String page;
			  page = HTML_Builder::html_header;

			  page+=
				R"(
				<h2>Login Credentials</h2><br>
				<form action=")" + change_creds + R"(" method="post">
				Login:<br>
				<input type="text" name="login"><br>
				Password:<br>
				<input type="password" name="password"><br>
				Confirm Password:<br>
				<input type="password" name="password_duplicate"><br>
				<p><button type="submit" name="newcredentials">Change Credentials</button></p>
				</form><br>
				)"
				;
				webserver.send(200, "text/html", page);
			}

			void handlecredentialchange()
			{
				if(!session_authenticated())
				{
					return;
				}

				String login = webserver.arg("login");
				String pw = webserver.arg("password");
				String pw2 = webserver.arg("password_duplicate");

				DEBUG_println("New login: " + login);
				DEBUG_println("New password: " + pw);
				DEBUG_println("New password copy: " + pw2);

				//String pw1 = pw;
				//String pw2 = pw2;
				if(login == "" || pw == "")
				{
					DEBUG_println("Empty field.");
					String dialog = HTML_Builder::html_header;
					dialog+=
					R"(
						<dialog open>A field was empty.
					)";
					dialog+= HTML_Builder::breakline;
					dialog+=HTML_Builder::create_hyperlink(creds_path, "Return");
					dialog+=
					R"(
						</dialog>
					)";
					dialog+= HTML_Builder::html_footer;
					webserver.send(200, "text/html", dialog);
					return;
				}
				else if(pw != pw2)
				{
					DEBUG_println("Passwords are not the same.");
					String dialog = HTML_Builder::html_header;
					dialog+=
					R"(
						<dialog open>Password fields were not identical.
					)";
					dialog+= HTML_Builder::breakline;
					dialog+=HTML_Builder::create_hyperlink(creds_path, "Return");
					dialog+=
					R"(
						</dialog>
					)";
					dialog+= HTML_Builder::html_footer;
					webserver.send(200, "text/html", dialog);
					return;
				}
				else
				{
					ESP_Managers::FileSystem::replace_credentials(login,pw);
					String dialog = HTML_Builder::html_header;
					dialog+=
					R"(
						<dialog open>Credentials changed.
					)";
					dialog+= HTML_Builder::breakline;
					dialog+=HTML_Builder::create_hyperlink(creds_path, "Return");
					dialog+=
					R"(
						</dialog>
					)";
					dialog+= HTML_Builder::html_footer;
					webserver.send(200, "text/html", dialog);
					return;
				}
			}

			String network_to_html(int index, String SSID)
			{
			  String retval;
			  retval=
			  SSID
				+ " "
			  + R"(<button type="submit" id="Network)"
				+ index
				+ R"(_Delete" name="delete" value=")"
				+ index
				+ R"(">Delete</button><br>)"
			  ;
			  return retval;
			}

			void wifiserver_handle_newnetwork()
			{
				if(!session_authenticated())
				{
					return;
				}

			  DEBUG_println("New network SSID: " + webserver.arg("SSID"));
			  DEBUG_println("New network password: " + webserver.arg("Password"));
			  FileSystem::add_network(&wifiMulti, webserver.arg("SSID"),webserver.arg("Password"));
			  redirect();
			}

			void wifiserver_handle_managenetwork()
			{
				if(!session_authenticated())
				{
					return;
				}

			  int index=webserver.arg("delete").toInt();
			  DEBUG_println("Delete network: " + webserver.arg("delete"));
			  FileSystem::delete_network(index);
			  redirect();
			}

			void wifiserver_handle_showIOpanel()
			{
				if(!session_authenticated())
				{
					return;
				}

				#ifndef TEMP_TESTING
				IO::System()->ShowReportPage();
				#endif
			}

			void serialprintallargs()
			{
			  DEBUG_println("Available html form arguments:");
			  String message;
			  for (uint8_t i=0; i<webserver.args(); i++){
				message += " " + webserver.argName(i) + ": " + webserver.arg(i) + "\n";
			  }
			  DEBUG_println(message);
			}


			ESP8266WebServer* get_webserver()
			{
				return &webserver;
			}

			void webserver_send(String webpage)
			{
				//Are html header and footer really needed here?? They're messing up SmartThings parsing.
				//webserver.send(200, "text/html", ESP_Managers::Network::html_header + webpage + ESP_Managers::Network::html_footer);
				//Adafruit examples don't use it. Try this out.
				webserver.send(200, "text/html", webpage);
			}

			/*
			void webserver_addresponse(String uri, ESP8266WebServer::THandlerFunction handler)
			{
				webserver.on(uri.c_str(),handler);
			}

			void webserver_addresponse(const char* uri, ESP8266WebServer::THandlerFunction handler)
			{
				webserver.on(uri,handler);
			}
			*/

			void modem_sleep(int duration_millis)
			{
				if(since_last_webserve.elapsed()>(3*60*1000))
				{
					WiFi.forceSleepBegin();
					delay(duration_millis); // wait for a second
					WiFi.forceSleepWake();
				}
				else
				{
					//If there's been a webserve in the last 3 minutes, don't sleep.
					DEBUG_println("Staying awake for the connected webserver client.");
				}
			}
			String getMD5Hash()
			{
				ESP_Managers::FileSystem::Credentials creds = ESP_Managers::FileSystem::read_credentials();
				String input_string = creds.login + ":" + realm + ":" + creds.password;
				MD5Builder md5;
			  md5.begin();
			  md5.add(input_string);  // md5 of the user:realm:password
			  md5.calculate();
				return md5.toString();
			}
  };
};
