#include "ESP_Managers.h"

#include "IO/IO.cpp"
#include "Network/Network.cpp"
#include "FileSystem/FileSystem.cpp"

namespace ESP_Managers {
	void Initialize(Network::SecurityBundle bundle)
	{
		FileSystem::Initialize(); //do this before configuring

		IO::IO_System::InitializeSaveables(); //Once IO_System name is correct, then initialize saveables (saveables file system is dependent on IO_System name)

		Network::Initialize(bundle);
	}
};
