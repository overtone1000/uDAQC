#include <ESP_Managers.h>

void setup()
{
  Serial.begin(74880); //This is the default C++ what happens if you delete a member of an arrayhttps://docs.google.com/dCocument/d/1DzxBngHUOFZhpleMMP3qco2PPNh6JigrA3S00HgfX0Q/editefault boot output, so errors can be seen

  delay(5000); //Allow serial monitor time to connect...

  ESP_Managers::FileSystem::Initialize(); //do this so any state info can be loaded first before connecting Wifi. This'll get things working faster after a reset.

  //ESP_Managers::FileSystem::replace_credentials("login","password");

  //ESP_Managers::FileSystem::clear_networks();

  ESP_Managers::FileSystem::add_network(&(ESP_Managers::Network::wifiMulti), "MySpectrumWiFib3-2G", "largenest554");
}

void loop()
{
  yield();
}
