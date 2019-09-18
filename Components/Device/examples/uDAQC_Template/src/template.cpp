#include "uDAQC.h"
#include <cmath>

UDAQC::IO::IO_System system1("System 1");
UDAQC::IO::IO_SaveableValue<float> test_float("Test float1", "barfoos", &system1);
UDAQC::IO::IO_Value<float> test_input_temp1("Test input0","degC", &system1);
UDAQC::IO::PID test_PID("Test PID1", "degC", "%", &system1);

UDAQC::IO::IO_System system2("System 2");
UDAQC::IO::IO_Value<float> test_input_temp2("Test input1","degC", &system2);

const int LED_PIN = 5; // Thing's onboard, green LED

#include "Certificate/certificate.h"

void setup()
{
  Serial.begin(74880); //This is the default C++ what happens if you delete a member of an arrayhttps://docs.google.com/dCocument/d/1DzxBngHUOFZhpleMMP3qco2PPNh6JigrA3S00HgfX0Q/editefault boot output, so errors can be seen

  #ifdef DEBUG_UDAQC
    delay(5000); //Allow serial monitor time to connect...
  #endif

  DEBUG_println("Serial configured.");
  UDAQC::FileSystem::Initialize(); //do this so any state info can be loaded first before connecting Wifi. This'll get things working faster after a reset.

  DEBUG_println("Setting pin modes.");
  pinMode(LED_PIN, OUTPUT);

  DEBUG_println("Initializing wifi manager.");
  UDAQC::Initialize("uDAQC Template Device", bundle);

  DEBUG_println("Main loop complete.");
}

Repeater update_data1(5000); //Update data and send every 5 seconds
Repeater update_data2(10000); //Update data and send every 5 seconds
void loop()
{

  if(update_data1.repeatnow())
  {
    float temp = 20.0 + 10.0*sin(millis()/10000.0/20.0*PI);
    test_input_temp1.Set(temp);
    system1.SetTimeToNow(); //Set the time stamp for when data was acquired; this can be important because the loops for WiFi can be time consuming
    system1.SendDataReportTCP();
    DEBUG_println("Sending update1 at " + (String)millis());
  }

  if(update_data2.repeatnow())
  {
    float temp = -20.0 + 50.0*sin(millis()/100000.0/20.0*PI);
    test_input_temp2.Set(temp);
    system2.SetTimeToNow(); //Set the time stamp for when data was acquired; this can be important because the loops for WiFi can be time consuming
    system2.SendDataReportTCP();
    DEBUG_println("Sending update1 at " + (String)millis());
  }

  UDAQC::Network::Loop();
}
