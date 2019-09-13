#ifndef BinarySwitch_h
#define BinarySwitch_h

#include <Arduino.h>
#include <vector>

namespace UDAQC{ namespace IO
{
  class BinarySwitch:public IO_SaveableValue<bool>
  {
  public:
    BinarySwitch(String name, IO_System* collection):IO_SaveableValue<bool>(name, collection){}
    String Report();
  };
}};

#endif
