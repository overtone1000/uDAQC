#include "ESP_Internals.h"

namespace ESP_Managers
{
namespace IO
{
  timeval ESP_Internals::last_update={0,0};

  void ESP_Internals::TimeUpdated(timeval t)
  {
    ESP_Internals::last_update=t;
  }

  String ESP_Internals::TimeAsString(timeval tv)
  {
    String retval;
    time_t now = tv.tv_sec;
    struct tm* tp = localtime(&now);
    char buf[80];
    strftime(buf, sizeof(buf), "%a %Y-%m-%d %H:%M:%S",tp); //time zone does not print correctly
    //DEBUG_print(buf);
    //DEBUG_println(" and " + (String)tv.tv_usec + " microseconds");
    retval+=buf;
    retval+=" and " + (String)tv.tv_usec + " microseconds";
    return retval;
  }

  String ESP_Internals::CurrentTimeAsString()
  {
    //Testing time
		timeval tv;
		gettimeofday(&tv,nullptr);

    return ESP_Internals::TimeAsString(tv);
  }

  ESP_Internals::ESP_Internals(String name, IO_System* collection):
  IO_Group(name, collection),
  cycle_count("Cycle Count", "cycles", this)
  {
  }

  String ESP_Internals::Report()
  {
    String retval;
    retval += "Cycle count = " + (String)ESP.getCycleCount();
    retval +=  HTML_Builder::breakline;
    retval += "Free memory = " + (String)ESP.getFreeHeap();
    retval +=  HTML_Builder::breakline;

    retval += "Current time: " + ESP_Internals::CurrentTimeAsString() + HTML_Builder::breakline;
    retval += "Last update: " + ESP_Internals::TimeAsString(last_update);

    return retval;
  }
};
};
