#include "CommandCodec.h"

namespace CommandCodec
{
  TCP_Command_Client::TCP_Command_Client()
  {
    setInsecure();
  }

  TCP_Command_Client::TCP_Command_Client(WiFiClientSecure client):
  WiFiClientSecure::WiFiClientSecure(client)
  {
    TCP_Command_Client();
  }

  TCP_Command_Client::~TCP_Command_Client()
  {
    DEBUG_println("Client deletion called.");
    if(command.message)
    {
      delete[] command.message;
    }
  }

  int TCP_Command_Client::connect(IPAddress host, int center_port)
  {
    int connection_result = WiFiClientSecure::connect(host,center_port);
    if(!connection_result)
    {
      DEBUG_println("WiFiClientSecure connection failed = " + connection_result);
      return connection_result;
    }

    return 0;
  }

  unsigned int TCP_Command_Client::send_command_header(TCP_Command_Header com)
  {
    //return write((uint8_t*)&(com),sizeof(com)); //can't do this, sizeof a struct accounts for padding
    unsigned int retval=0;
    retval+=write((uint8_t*)&(com.message_length),sizeof(com.message_length));
    retval+=write((uint8_t*)&(com.command_id),sizeof(com.command_id));

    return retval;
    /*
    DEBUG_print("Bytes:");
    uint8_t* byte_pointer = (uint8_t*)&com;
    for(uint8_t* it = byte_pointer;it<(byte_pointer+sizeof(com));it++)
    {
      DEBUG_print(*it, HEX);
      DEBUG_print(",");
    }
    DEBUG_println();
    */
  }

  bool TCP_Command_Client::command_available()
  {
    if(command.message) //If
    {
      delete[] command.message;
      command.message=nullptr;
    }
    while(available())
    {
      if(new_command_read)
      {
        //If this is a new command, will need to get message_length and command first.
        if(available()>=(int)sizeof(command.message_length)+(int)sizeof(command.command_id))
        {
          //If there is enough in the buffer to determine message length, populate it.
          read((uint8_t*)&(command.message_length),sizeof(command.message_length));
          read((uint8_t*)&(command.command_id),sizeof(command.command_id));
          new_command_read=false;
        }
        else
        {
          //Otherwise, return false and check again later.
          return false;
        }
      }
      else
      {
        if(command.message_length>=0)
        {
          //If there is a message, process it.
          if(available()>=(command.message_length))
          {
            command.message = new uint8_t[command.message_length];
            read(command.message,command.message_length);
            new_command_read=true;
            return true;
          }
        }
        else
        {
          //If there is no message, all done.
          return true;
        }
      }
    }
    return false;
  }

  TCP_Command TCP_Command_Client::get_command()
  {
    return command;
  }

  bool TCP_Command_Client::Authenticated()
  {
    return authenticated;
  }

  void TCP_Command_Client::Authenticate()
  {
    authenticated=true;
  }
};
