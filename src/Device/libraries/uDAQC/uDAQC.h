#ifndef UDAQC_h
#define UDAQC_h

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

namespace UDAQC
{
	void Initialize(String device_name, Network::SecurityBundle bundle);
};

#endif
