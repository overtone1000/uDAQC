#ifndef ESP_FileSystem_h
#define ESP_FileSystem_h

#include <FS.h>

namespace ESP_Managers
{
	namespace FileSystem
	{
		bool Initialize();

		String read_network_SSID(int index);
		String read_network_password(int index);
		void add_network(ESP8266WiFiMulti* wifiMulti, String SSID, String password);
		void delete_network(int nw_index);
		void clear_networks();

		struct Credentials
		{
			String login;
			String password;
		};

		Credentials read_credentials();
		void replace_credentials(String login, String password);

		int count_networks();
		void file_to_serial();
		void checkflashconfig();
	};
};

#endif
