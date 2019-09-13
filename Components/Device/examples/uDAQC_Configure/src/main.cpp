#include <uDAQC.h>

void setup()
{
  Serial.begin(74880); //This is the default C++ what happens if you delete a member of an arrayhttps://docs.google.com/dCocument/d/1DzxBngHUOFZhpleMMP3qco2PPNh6JigrA3S00HgfX0Q/editefault boot output, so errors can be seen

  delay(5000); //Allow serial monitor time to connect...

  UDAQC::FileSystem::Initialize(); //do this so any state info can be loaded first before connecting Wifi. This'll get things working faster after a reset.

  //UDAQC::FileSystem::replace_credentials("login","password");

  //UDAQC::FileSystem::clear_networks();

  UDAQC::FileSystem::add_network(&(UDAQC::Network::wifiMulti), "MySpectrumWiFib3-2G", "largenest554");
}

void loop()
{
  yield();
}
