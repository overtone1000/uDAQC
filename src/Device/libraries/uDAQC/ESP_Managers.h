#ifndef ESP_Managers_h
#define ESP_Managers_h

#define TLS

#include <cmath>

#include <Arduino.h>

#include <ESP_Utilities.h>

#include "IO/IO.h"

#include "Network/Network.h"
#include "FileSystem/FileSystem.h"

namespace ESP_Managers
{
	void Initialize(Network::SecurityBundle bundle);
};

#endif
