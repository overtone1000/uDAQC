#ifndef PID_h
#define PID_h

#include "Arduino.h"

namespace UDAQC{ namespace IO
{
  class PID:public IO_Group{
    public:
      PID(String name, String input_units, String output_units, IO_Group* collection);

      String Report();

      IO_SaveableValue<float> P_coef;//=IO_SaveableValue<float>("1",nullptr);
      IO_SaveableValue<float> I_coef;//=IO_SaveableValue<float>("2",nullptr);
      IO_SaveableValue<float> setpoint;//=IO_SaveableValue<float>("3",nullptr);

      //Don't allow these to be modified. These should be hard coded.
      float intercept; //this should be set to the default initial output for the PID controller
      float maxout; //maximum controller output value
      float minout; //minimum controller output value

      float out();
      void in(float value, unsigned long time);
      void reset();

    private:
      static float timeconstant; //Just make this a constant private value that is never modified (see .cpp)

      float outval;
      unsigned long lasttime;
      float integrated_err;
  };
};};

#endif
