#ifndef CommandCodec_h
#define CommandCodec_h

#include <Arduino.h>
#include <sys/time.h> // struct timeval
#include <WiFiClientSecure.h>

namespace CommandCodec
{
  struct TCP_Command_Header
  {
    int32_t message_length=-1;
    int16_t command_id=-1;
    static int bytelength(){return 6;}
  };

  struct TCP_Command:public TCP_Command_Header
  {
    uint8_t* message=nullptr;
  };

  class TCP_Command_Client:public WiFiClientSecure
  {
  public:
    TCP_Command_Client();
    TCP_Command_Client(WiFiClientSecure client);
    ~TCP_Command_Client(); //This object DOES manage "message", need to delete[]
    bool command_available(); //message must be handled immediately, next call will start next command
    int connect(IPAddress host, int center_port, int timesync_port);
    bool Initialized();
    TCP_Command get_command();
    unsigned int send_command_header(TCP_Command_Header com);
  private:
    TCP_Command command;
    bool new_command_read=true;
    bool time_synchronized=false;
  };
};

#endif
