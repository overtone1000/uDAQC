#ifndef IO_Timestamp_h
#define IO_Timestamp_h

#include <Arduino.h>

#include <time.h>     // time() ctime()
#include <sys/time.h> // struct timeval

#include "IO_Value.h"

namespace ESP_Managers{ namespace IO
{
  class IO_System;
  class IO_Timestamp:public IO_Value<int64_t>
  {
  public:
    IO_Timestamp(String name, IO_System* collection);
    ~IO_Timestamp(){};
    void SetTime(timeval tv);
    void SetTimeToNow();
  };
}};

#endif
