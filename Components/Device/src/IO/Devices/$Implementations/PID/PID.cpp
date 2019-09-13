#include "PID.h"

namespace UDAQC{ namespace IO
{
		float UDAQC::IO::PID::timeconstant = 60.0*1000.0; //60E3 ms (1 minute)

		PID::PID(String name, String input_units, String output_units, IO_Group* collection):
		IO_Group(name, collection),
		P_coef("P Coef", output_units + "/" + input_units, this),
		I_coef("I Coef", output_units + "*s/" + input_units, this),
		setpoint("SetPoint", input_units, this)
		{
			timeconstant=1.0;
			outval=0;
			integrated_err=0;
			lasttime=0;
			intercept=0;
			maxout=0;
			minout=0;
		}

		float PID::out(){
			return outval;
		}

		void PID::in(float value, unsigned long time){
			//DEBUG_print("PID received in of ");
			//DEBUG_println(value);
			if(time<lasttime){lasttime=0;}
			//DEBUG_print("Lasttime: ");
			//DEBUG_println(lasttime);
			float interval=(float)(time-lasttime)/timeconstant;
			//DEBUG_print("Interval: ");
			//DEBUG_println(interval);
			float err=value-setpoint.Get();
			//DEBUG_print("Value: ");
			//DEBUG_println(value);
			//DEBUG_print("Setpoint: ");
			//DEBUG_println(setpoint);
			//DEBUG_print("Err: ");
			//DEBUG_println(err);
			integrated_err+=err*interval;
			//DEBUG_print("Int_err: ");
			//DEBUG_println(integrated_err);
			outval=P_coef.Get()*err+intercept+I_coef.Get()*integrated_err;
			//DEBUG_print("Outval 1: ");
			//DEBUG_println(outval);
			if(outval>maxout){
				outval=maxout;
				integrated_err=(outval-P_coef.Get()*err-intercept)/I_coef.Get();
			}
			if(outval<minout){
				outval=minout;
				integrated_err=(outval-P_coef.Get()*err-intercept)/I_coef.Get();
			}
			//DEBUG_print("Outval 2: ");
			//DEBUG_println(outval);

			lasttime=time;
		}

		void PID::reset(){
			lasttime=millis();
			integrated_err=0;
			outval=intercept;
		}

		String PID::Report()
		{
			String retval;
			retval+=P_coef.Report();
			retval+=HTML_Builder::breakline;
			retval+=I_coef.Report();
			retval+=HTML_Builder::breakline;
			retval+=setpoint.Report();
			return retval;
		}
}};
