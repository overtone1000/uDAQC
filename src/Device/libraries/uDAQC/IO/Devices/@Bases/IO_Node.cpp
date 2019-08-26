#include "IO_Node.h"

namespace ESP_Managers{ namespace IO
{

  size_t PseudoWiFiClient::write(uint8_t)
  {
    total++;
    return 1;
  }
  size_t PseudoWiFiClient::write(const uint8_t *buf, size_t size)
  {
    total+=size;
    return size;
  }
  void PseudoWiFiClient::reset()
  {
    total=0;
  }
  unsigned int PseudoWiFiClient::total_written()
  {
    return total;
  }

  int IO_Node::errcnt=-1; //Start at -1 because there is always one unsuccessful attempt at device addition with the main group.
  IO_Node::IO_Node(String name, IO_Group* collection)
  {
    device_name=name;

    parent = collection;
    if(parent)
    {
      parent->add_node(this);
    }
    else
    {
      errcnt++;
    }
  }

  int32_t IO_Node::DescriptionSize()
  {
    PseudoWiFiClient pclient;
    SendDescription(&pclient);
    return pclient.total_written();
  }
  int32_t IO_Node::DataSize()
  {
    PseudoWiFiClient pclient;
    SendData(&pclient);
    return pclient.total_written();
  }

  String IO_Node::FullName()
  {
    String fullname;
    if(parent)
    {
      fullname = parent->FullName();
      fullname += " ";
    }
    else
    {
      fullname = "";
    }
    fullname += device_name;
    return fullname;
  }

  String IO_Node::SafeFullName()
  {
    String safefullname = FullName();
    for(unsigned int n = 0; n<safefullname.length();n++)
    {
      //Replace spaces!
      if(safefullname[n]==' ')
      {
        safefullname[n]='_';
      }
    }
    return safefullname;
  }

  unsigned int IO_Node::SendDescription(WiFiClient* client)
  {
    /*
    Structure:
    int16_t containing the command
    int32_t containing the length of the data for this node in the data dump
    int16_t containing the length of the name
    the name string
    String name
    */
    unsigned int retval=0;
    int32_t size = DataSize();
    retval+=client->write((uint8_t*)DescriptionCommand(),sizeof(int16_t));
    retval+=client->write((uint8_t*)&(size),sizeof(size));
    retval+=ESP_Managers::IO::SendString(client,&device_name);
    return retval;
  };
}
};
