#ifndef IO_Group_h
#define IO_Group_h

#include <Arduino.h>
#include <vector>
#include <algorithm>

#include "IO_Node.h"

namespace UDAQC{ namespace IO
{
  class IO_Node;

  class IO_Group:public IO_Node
  /*
  This class is a node that groups IO_Nodes together to allow nesting of devices in an IO_System
  */
  {
  public:
    IO_Group(String name, IO_Group* collection);
    ~IO_Group(){}

  //IO_Node stuff
    //For the web interface
    virtual String Report();
    unsigned int NameSize();

    //For binary transmissions
    virtual int32_t DataSize();
    //virtual void Set_p(uint8_t* pointer); //Needs to be overloaded to pass it along to members
    virtual unsigned int SendDescription(WiFiClient* client); //Send the description of the data. This should contain everything the recipient needs to correctly interpret the data
    virtual unsigned int SendData(WiFiClient* client);//Send all raw data
    virtual const int16_t* DescriptionCommand(){return &(NetworkCommands::group_description);}

  //IO_Group stuff
    void add_node(IO_Node* new_member);
    int MemberCount();
    const std::vector<IO_Node*> Members();
  protected:
    std::vector<IO_Node*> members;
    unsigned int current_size=0;
  };
}};

#endif
