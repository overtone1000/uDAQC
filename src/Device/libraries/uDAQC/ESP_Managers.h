#ifndef ESP_Managers_h
#define ESP_Managers_h

#define TLS

#include <Arduino.h>
#ifdef ROUND_WORKAROUND
#undef round
#endif

#include <cmath>

#include <ESP_Utilities.h>

#include "IO/IO.h"

#include "Network/Network.h"
#include "FileSystem/FileSystem.h"

namespace ESP_Managers
{
	void Initialize(Network::SecurityBundle bundle);
};

#endif
