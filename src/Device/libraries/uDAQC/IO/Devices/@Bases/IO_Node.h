#ifndef IO_Node_h
#define IO_Node_h

#include <Arduino.h>
#include <vector>
#include <ESP_Utilities.h>
#include <algorithm>

namespace ESP_Managers{ namespace IO
{
  class IO_Group;

  class PseudoWiFiClient:public WiFiClient
  {
    //This class simply allows easier measurement of messages before sending them.
  public:
    virtual size_t write(uint8_t);
    virtual size_t write(const uint8_t *buf, size_t size);
    void reset();
    unsigned int total_written();
  private:
    unsigned int total=0;
  };

  class IO_Node
  /*
  This class is used to create the fundamental members of an IO_System
  Their primary function (even before saving a value) is to:
  -Generate a reportDataSize
  -Describe how much data is DataSizestored and where for binary transmission
  Some members may not store any data but only report it (e.g. a derived class that only reports ESP's internals)
  Most members will need to store data. The IO_Value class contains appropriate functions.
  */
  {
  public:
    IO_Node(String name, IO_Group* collection);
    virtual ~IO_Node(){}

    //For the web interface
    virtual String Report(){return "Undefined";}
    unsigned int NameSize(){return device_name.length();}

  protected:
    //uint8_t* value=nullptr;
    IO_Group* parent=nullptr;

  public:
    //For binary transmissions
    virtual int32_t DescriptionSize();
    virtual int32_t DataSize();
    //uint8_t* Get_p(){return value;}
    //virtual void Set_p(uint8_t* pointer){value = pointer;} //This needs to be overloaded by IO_Group for pointer distribution to the group's members
    virtual unsigned int SendDescription(WiFiClient* client);
    virtual unsigned int SendData(WiFiClient* client){return 0;} //must be overloaded. If not overloaded, do nothing.

  protected:
    String device_name="Undefined";
    virtual const int16_t* DescriptionCommand(){return &(NetworkCommands::emptynode_description);}

  public:
    String Name(){return device_name;}
    String FullName();
    String SafeFullName();

   static int errcnt;
  };
}};

#endif
