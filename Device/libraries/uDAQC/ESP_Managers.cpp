#include "ESP_Managers.h"

#include "IO/IO.cpp"
#include "Network/Network.cpp"
#include "FileSystem/FileSystem.cpp"

namespace ESP_Managers {
	void Initialize(String device_name, Network::SecurityBundle bundle)
	{
		Serial.println("Initializing Managers for system " + device_name);
		FileSystem::Initialize(); //do this before configuring

		#ifndef TEMP_TESTING
		IO::System()->Rename(device_name); //Change IO_System name first
		IO::System()->InitializeSaveables(); //Once IO_System name is correct, then initialize saveables (saveables file system is dependent on IO_System name)
		#endif

		Network::Initialize(bundle);
	}
};
