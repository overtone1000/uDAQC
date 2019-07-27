#include "IO_Timestamp.h"

namespace ESP_Managers{ namespace IO
{
  IO_Timestamp::IO_Timestamp(String name, IO_System* collection):
  IO_Value<int64_t>(name, (IO_Group*)collection)
  {
    Set(-1); //Initialize the timestamp to -1
  }
  void IO_Timestamp::SetTime(timeval tv)
  {
    int64_t new_val = (int64_t)(tv.tv_sec)*1000000+(int64_t)(tv.tv_usec);
    Set(new_val);
  }
  void IO_Timestamp::SetTime(int64_t micros)
  {
    Set(micros);
  }
  void IO_Timestamp::SetTimeToNow()
  {
    //The previous method used time of day, but now timestamps will just report micros and the server will calculate the timestamp

    //timeval tv;
    //gettimeofday(&tv,nullptr);
    //SetTime(tv);

    SetTime((int64_t)micros());
  }
}
};
