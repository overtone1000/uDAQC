#ifndef Repeater_h
#define Repeater_h

#include "Arduino.h"

class Repeater{
  public:
	Repeater();
  Repeater(unsigned long providedtimedelay);
  bool repeatnow();
  void reset();
  unsigned long remaining();
  unsigned long timedelay;

  private:
  unsigned long timeofnextexec;
};

#endif
