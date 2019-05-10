#ifndef ESP_Managers_h
#define ESP_Managers_h

#include <TRM_Debug.h>

#define TLS

#include <cmath>

#include <Arduino.h>

#include <ESP_Utilities.h>

#ifndef TEMP_TESTING
#include "IO/IO.h"
#endif

#include "Network/Network.h"
#include "FileSystem/FileSystem.h"

namespace ESP_Managers
{
	void Initialize(String device_name, Network::SecurityBundle bundle);
};

#endif
