#ifndef ESP_Network_h
#define ESP_Network_h

#include <ESP8266WiFi.h>
#include <ESP8266WiFiMulti.h>
#include <ESP8266WiFiScan.h>
#include <ESP8266WebServer.h>
#include <ESP8266WebServerSecure.h>
#include <DNSServer.h>
#include <ESP8266HTTPUpdateServer.h>
//#include <ESP8266mDNS.h>

#include "../FileSystem/FileSystem.h"

//Time includes
//3600000 one hour
//86400000 one day
#define SNTP_UPDATE_DELAY 86400000 //this is used by sntp.h (used by time.h below), but one hour (3600000 ms) is pretty short
#include <time.h>     // time() ctime()
#include <sys/time.h> // struct timeval
#include <coredecls.h> // settimeofday_cb(); this has to be included after ESP8266WiFi.h


namespace ESP_Managers
{
	namespace Network
	{
		extern ESP8266WiFiMulti wifiMulti;
		extern const String realm;

		#ifdef TLS
		extern ESP8266WebServerSecure webserver;
		#else
		extern ESP8266WebServer webserver; //web sever on port 80
		#endif

		struct SecurityBundle
		{
			const uint8_t* x509;
			int x509_size;
			const uint8_t* key;
			int key_size;
		};

		void Initialize(Network::SecurityBundle bundle);
		void Loop();

		void time_set_cb();
		void initWiFi_AP();
	 	void initWiFi_station();
	 	void add_saved_networks();

		bool session_authenticated();

		void Serial_AvailableNetworks();

		void init_server(SecurityBundle bundle);

		void redirect();
		IPAddress my_ip();

		void showbasicwificonfigpage();
		void showcredentialpage();
		void handlecredentialchange();

		String network_to_html(int index, String SSID);
		void wifiserver_handle_newnetwork();
		void wifiserver_handle_managenetwork();
		void wifiserver_handle_showIOpanel();
		void serialprintallargs();

 		ESP8266WebServer* get_webserver(); //Don't do this. Doesn't work. Ruins serial output.
		void webserver_send(String webpage);
		//void webserver_addresponse(String uri, ESP8266WebServer::THandlerFunction handler);
		//void webserver_addresponse(const char* uri, ESP8266WebServer::THandlerFunction handler);

		void modem_sleep(int duration_millis);

		String getMD5Hash();
  };
};

#endif
