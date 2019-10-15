#include "IO.h"

namespace UDAQC
{
namespace IO
{
  const char* IOpanel_path = "/IOPanel";

  namespace Constants
  {
    //const uint32_t tcp_main_port = 49152;
    const uint32_t udp_multicast_port = 49155;
    const IPAddress udp_multicast_IP = IPAddress(255,255,255,255);
  }

  namespace NetworkCommands //Use sign 16 bit integer. Java doesn't support unsigned shorts.
  {
    const int16_t system_description=17;
    const int16_t group_description=1;
    const int16_t emptynode_description=2;
    const int16_t value_description=3;
    const int16_t modifiablevalue_description=4;
    const int16_t data=5;
    const int16_t handshake=6;
    const int16_t auth_request=7;
    const int16_t auth_provision=8;
    const int16_t modifiablevalue_modification=9;
    const int16_t request_subscription=10;
    const int16_t history=11;
    const int16_t passthrough=12;
    const int16_t new_device_avaialable=13;
    const int16_t history_addendum=14;
    const int16_t timesync_request=15;
    const int16_t timesync_response=16;
    const int16_t history_request=17;
  };
  namespace DataTypes
  {
    const int16_t undefined=-1;
    const int16_t signed_integer=1;
    const int16_t unsigned_integer=2;
    const int16_t floating_point=3;
    const int16_t boolean=4;
  };

  unsigned int SendString(WiFiClient* client, const String* str)
  {
    unsigned int retval=0;
    int16_t str_length = str->length();
    retval+=client->write((uint8_t*)&(str_length),sizeof(str_length));
    retval+=client->write((uint8_t*)(str->c_str()),str_length);
    //Serial.println("Sent string of length " + (String)(str_length) + ":"+*str);
    return retval;
  }
};};

#include "Devices/TCP/CommandCodec.cpp"

#include "Devices/Bases/IO_Node.cpp"
#include "Devices/Bases/IO_Group.cpp"
#include "Devices/Bases/IO_Timestamp.cpp"
#include "Devices/Bases/IO_System.cpp"

//#include "Devices/@Bases/IO_SaveableValue.cpp"

#include "Devices/Implementations/ESP_Internals/ESP_Internals.cpp"
#include "Devices/Implementations/BinarySwitch/BinarySwitch.cpp"
#include "Devices/Implementations/PID/PID.cpp"
