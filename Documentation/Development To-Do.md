# **Development Tasks**

## Device
* Convert to saving only hashed password once ESP8266 Arduino library v2.6.0 is released
* Fix Device web interface (broken by changes allowing multiple systems on one device, see Network.cpp:299).
* Check PlatformIO library availability and add to installation instructions.

## Web Interface
* Change device credentials page to a dropdown list for domain (admin or guest only).
* Profile performance. There is probably need for decimation to allow charting large quantities of data.
* Should probably store historical data on client hard drive. When reconnecting, it should only update history instead of transmitting the entire history.
* Make x-axis manipulation faster and more aesthetic.

## Documentation
* Improve README.md.

## Deployment
* Move deployment directory to main repository directory
* Separate Center deployment into a separate deploy subdirectory
* Include Arduino library in deployment directory

## Deferred
* Can't put library on Arduino Library Manager (https://github.com/arduino/Arduino/wiki/Library-Manager-FAQ) because the whole git repository must be dedicated to that library only.