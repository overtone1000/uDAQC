#include "IO_Group.h"

namespace ESP_Managers{ namespace IO
{
  IO_Group::IO_Group(String name, IO_Group* collection):IO_Reporter(name,collection)
  {
  }

  void IO_Group::add_reporter(IO_Reporter* new_member)
  {
    members.push_back(new_member);
  }

  String IO_Group::Report()
  {
    String page;

    for(auto it=members.begin();it!=members.end();it++)
    {
      //DEBUG_println("Adding name for " + (**it).Name());
      page+=R"(<h3>)";
      page+=(**it).Name();
      page+=R"(</h3>)";
      //DEBUG_println("Adding report for " + (**it).Name());
      page+=(**it).Report();
      //page+=HTML_Builder::breakline;
      //DEBUG_println("Finished with " + (**it).Name());
    }

    return page;
  }


  int32_t IO_Group::DataSize()
  {
    /*
    unsigned int currenDataSizet_size=0;

    current_size=0;
    for(auto it=members.begin();it!=members.end();it++)
    {
      current_size+=(**it).DataSize();
    }
    */
    return current_size;
  }

  /*
  void IO_Group::Set_p(uint8_t* pointer)
  {
    uint8_t* p = pointer;
    for(auto it=members.begin();it!=members.end();it++)
    {
      DEBUG_println("Member " + (**it).Name() + " is at " + (String)(unsigned long)p + " and is " + (**it).DataSize() + "bytes");
      (**it).Set_p(p);
      p+=(**it).DataSize();
    }
  }
  */

  unsigned int IO_Group::SendData(WiFiClient* client)
  {
    //client->write(Get_p(), current_size);
    unsigned int retval=0;
    for(auto it=members.begin();it!=members.end();it++)
    {
      retval+=(**it).SendData(client);
    }
    return retval;
  }

  unsigned int IO_Group::SendDescription(WiFiClient* client)
  {
    //This is the best time to calculate current_size
    current_size=0;
    for(auto it=members.begin();it!=members.end();it++)
    {
      current_size+=(**it).DataSize();
    }

    /*
    Structure:
    Standard Reporter description header
    short containing number of members to look for
    each member's description
    */
    unsigned int retval=0;

    uint16_t membercount = members.size();

    retval+=IO_Reporter::SendDescription(client);
    retval+=client->write((uint8_t*)&membercount,sizeof(membercount));

    /*
<<<<<<< HEAD
    To handle description, recipient will need to:
      1. Process members until the number of members has been reached, then return to the parent loop
      2. Handle IO_Reporters, IO_SaveableValues, IO_Values, and IO_Groups as different possible members
    To handle data, recipient will need to:
      1. Keep track of IO_SaveableValues and IO_Values, which will report the length of data they will transmit.
=======
    Recipient will need to:
      1. Process members until the number of members has been reached, then return to the parent loop
      2. Handle IO_Reporters, IO_SaveableValues, IO_Values, and IO_Groups as different possible members
>>>>>>> ac8e61fb9d488ba97c5f66ed94f58d391b9fdcb5
    */

    for(auto it=members.begin();it!=members.end();it++)
    {
      retval+=(**it).SendDescription(client);
    }
    return retval;
  }
}
};
