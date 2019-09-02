#include "uDAQC.h"

#include "IO/IO.cpp"
#include "Network/Network.cpp"
#include "FileSystem/FileSystem.cpp"

namespace UDAQC {
	void Initialize(String device_name, Network::SecurityBundle bundle)
	{
		FileSystem::Initialize(); //do this before configuring

		IO::IO_System::NameDevice(device_name);
		IO::IO_System::InitializeSaveables(); //Once IO_System name is correct, then initialize saveables (saveables file system is dependent on IO_System name)

		Network::Initialize(bundle);
	}
};
