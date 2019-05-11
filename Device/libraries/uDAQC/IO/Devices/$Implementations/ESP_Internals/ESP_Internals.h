#ifndef ESP_Internals_h
#define ESP_Internals_h

#include <Arduino.h>
#include <vector>
#include <ESP_Utilities.h>

#include <time.h>     // time() ctime()
#include <sys/time.h> // struct timeval

namespace ESP_Managers
{
namespace IO
{
  class ESP_Internals:public IO_Group
  {
  public:
    ESP_Internals(String name, IO_System* collection);
    ~ESP_Internals(){}

    IO_Value<long> cycle_count;

    virtual String Report();

    static String CurrentTimeAsString();
    static String TimeAsString(timeval tv);

  private:
    static timeval last_update;
  public:
    static void TimeUpdated(timeval t);
  };
};
};
#endif
