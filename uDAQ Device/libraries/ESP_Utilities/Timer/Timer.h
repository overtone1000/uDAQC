#ifndef Timer_h
#define Timer_h

#include <Arduino.h>

class Timer{
  public:
  Timer();
  void start();
  void stop();
  void reset();
  bool isrunning();
  unsigned long elapsed();

  private:
  unsigned long lasttime;
  unsigned long elapsedtime;
  bool running;
  void updateelapsed();
};

#endif
