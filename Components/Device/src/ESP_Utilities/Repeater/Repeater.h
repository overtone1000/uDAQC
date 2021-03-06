#ifndef Repeater_h
#define Repeater_h

#include <Arduino.h>
#ifdef ROUND_WORKAROUND
#undef round
#endif

class Repeater{
  public:
	Repeater();
  Repeater(unsigned long providedtimedelay);
  bool repeatnow();
  void reset();
  unsigned long remaining();

  private:
  unsigned long timeofnextexec;
  unsigned long timedelay;
};

#endif
