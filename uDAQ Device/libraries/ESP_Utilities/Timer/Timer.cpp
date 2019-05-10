#include "Timer.h"

Timer::Timer(){
	running=false;
	elapsedtime=0;
	lasttime=0;
}

void Timer::start(){
	updateelapsed();
	running=true;
}

void Timer::stop(){
	updateelapsed();
	running=false;
}

void Timer::reset(){
	updateelapsed();
	elapsedtime=0;
}

bool Timer::isrunning(){
	return running;
}

void Timer::updateelapsed(){
	unsigned long thismillis=millis();
	if(lasttime>thismillis){lasttime=0;}
	if(running){elapsedtime+=thismillis-lasttime;}
	lasttime=thismillis;
}

unsigned long Timer::elapsed(){
	updateelapsed();
	return elapsedtime;
}
