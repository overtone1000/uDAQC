#ifndef IO_System_h
#define IO_System_h

#include <Arduino.h>
#include <IPAddress.h>
#include <vector>
#include <list>
#include <WiFiUdp.h>

#include <ESP_Utilities.h>
#include "IO_Group.h"
#include "IO_Timestamp.h"

namespace ESP_Managers{ namespace IO
{
  class Timestamp;
  class IO_Saveable;

  class IO_System:public IO_Group
  /*
  This class encompasses the IO for an entire device.
  An instance is created in the ESP_Managers::IO namespace in ESP_Managers::IO.h and ESP_Managers::IO.cpp.
  */
  {
  public:
    IO_System(String name);
    ~IO_System();

    static std::list<CommandCodec::TCP_Command_Client> tcp_clients;

    static int add_saveable(IO_Saveable* new_member);

    static void InitializeSaveables();//This is to be run after all members and SPIFFS have been added. It creates the binary array where IO data is stored. It also reads any saveable devices from memory to inintialize their values.

    static void InitializeTCPClient(); //This must be called once a network connection is established to start the tcp_server
    static void LoopTCPClient(); //This must be called in a loop to handle tcp connection

    void SendDataReportTCP();

    void ShowReportPage();//This will display an HTML report about the data.
    void DirectToReportPage();

    static void InitializeUDP();
  	static void LoopUDP();
    static IO_Saveable* saveable_member(int index){return saveable_members[index];}

    void SetTimeToNow();

    static IO_System* Current();
    static void SetCurrent(IO_System* new_current);
    static std::vector<IO_System*> Systems();

    virtual unsigned int SendDescription(WiFiClient* client); //Send the description of the data. This should contain everything the recipient needs to correctly interpret the data

  private:
    static WiFiUDP udp;
    static Repeater udp_timer;

    int system_index;

    //uint8_t* data=nullptr;
    static std::vector<IO_System*> systems;
    static IO_System* current_system;

    static std::vector<IO_Saveable*> saveable_members;

    static void add_center(IPAddress host, int center_port);

    IO_Timestamp ts; //This is used to send the timestamp with the data. It's the first member of the group.
    //void Resize();

    //Repeater debug_timer;

    static void announceUDP();
    static void handleTimeSync(IPAddress center_address, uint32_t center_port, int64_t current_center_time);
  };
}};

#endif
