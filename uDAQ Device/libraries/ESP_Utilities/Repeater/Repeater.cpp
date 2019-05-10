#include "Repeater.h"

Repeater::Repeater(){
	timedelay=0;
	timeofnextexec=millis();
}

Repeater::Repeater(unsigned long providedtimedelay){
  timedelay=providedtimedelay;
  timeofnextexec=millis()+timedelay;
}

bool Repeater::repeatnow(){
	unsigned long current_time = millis();
  if(timeofnextexec<=current_time) //time to repeat
		//|| (timeofnextexec-current_time)>timedelay) //in case of rollover
	{
		timeofnextexec += timedelay; //no need to handle millis rollover, as this will be handled by the simple arithmetic of this operation
	  return true;
  }
  return false;
}

unsigned long Repeater::remaining()
{
	return timeofnextexec-millis();
}

void Repeater::reset(){
  timeofnextexec=millis()+timedelay;
}
