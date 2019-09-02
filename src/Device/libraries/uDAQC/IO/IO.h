#ifndef ESP_IO_h
#define ESP_IO_h

#include <Arduino.h>
#ifdef ROUND_WORKAROUND
#undef round
#endif

#include <vector>
#include <ESP_Utilities.h>
#include <ESP8266WebServer.h>

namespace UDAQC
{
namespace IO
{
  namespace Constants
  {
    //extern const uint32_t tcp_main_port;
    extern const uint32_t udp_multicast_port;
    extern const IPAddress udp_multicast_IP;
  }

  namespace NetworkCommands //Use sign 16 bit integer. Java doesn't support unsigned shorts.
  {
    extern const int16_t system_description;
    extern const int16_t group_description;
    extern const int16_t emptynode_description;
    extern const int16_t value_description;
    extern const int16_t modifiablevalue_description;
    extern const int16_t data;
    extern const int16_t handshake;
    extern const int16_t auth_request;
    extern const int16_t auth_provision;;
    extern const int16_t modifiablevalue_modification;
    extern const int16_t request_subscription;
    extern const int16_t history;
    extern const int16_t passthrough;
    extern const int16_t new_device_avaialable;
    extern const int16_t history_addendum;
    extern const int16_t timesync_request;
    extern const int16_t timesync_response;
  };

  namespace DataTypes
  {
    extern const int16_t undefined;
    extern const int16_t signed_integer;
    extern const int16_t unsigned_integer;
    extern const int16_t floating_point;
    extern const int16_t boolean;
  };

  unsigned int SendString(WiFiClient* client, const String* str);
}};

#include "Devices/TCP/CommandCodec.h"

#include "Devices/@Bases/IO_System.h"

namespace UDAQC
{
  namespace IO
{
  extern const char* IOpanel_path;
}};

#include "Devices/@Bases/IO_SaveableValue.h"

//Then derived classes
#include "Devices/$Implementations/ESP_Internals/ESP_Internals.h"
#include "Devices/$Implementations/BinarySwitch/BinarySwitch.h"
#include "Devices/$Implementations/PID/PID.h"

#endif
