//#define TEMP_TESTING

#include <ESP_Managers.h>
#include <cmath>

#ifndef TEMP_TESTING
ESP_Managers::IO::IO_SaveableValue<float> test_float("Test float1", "barfoos", ESP_Managers::IO::System());
ESP_Managers::IO::IO_Value<float> test_input_temp("Test input0","degC",ESP_Managers::IO::System());
ESP_Managers::IO::PID test_PID("Test PID1", "degC", "%", ESP_Managers::IO::System());
#endif

const int LED_PIN = 5; // Thing's onboard, green LED

#include "Certificate/certificate.h"

void setup()
{
  Serial.begin(74880); //This is the default C++ what happens if you delete a member of an arrayhttps://docs.google.com/dCocument/d/1DzxBngHUOFZhpleMMP3qco2PPNh6JigrA3S00HgfX0Q/editefault boot output, so errors can be seen

  #ifdef DEBUG_TRM
    delay(5000); //Allow serial monitor time to connect...
  #endif

  DEBUG_println("Serial configured.");
  ESP_Managers::FileSystem::Initialize(); //do this so any state info can be loaded first before connecting Wifi. This'll get things working faster after a reset.

  DEBUG_println("Setting pin modes.");
  pinMode(LED_PIN, OUTPUT);

  DEBUG_println("Initializing wifi manager.");
  ESP_Managers::Initialize("TRM ESP8266 Template", bundle);

  DEBUG_println("Main loop complete.");
}

Repeater update_data(5000); //Update data and send every 5 seconds
void loop()
{

  #ifndef TEMP_TESTING
  if(update_data.repeatnow())
  {
    float temp = 20.0 + 10.0*sin(millis()/1000.0/20.0*PI);
    test_input_temp.Set(temp);
    ESP_Managers::IO::System()->SetTimeToNow(); //Set the time stamp for when data was acquired; this can be important because the loops for WiFi can be time consuming
    ESP_Managers::IO::System()->SendDataReportTCP();
  }
  #endif

  ESP_Managers::Network::Loop();
}
