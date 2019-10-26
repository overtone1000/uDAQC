# **Development Tasks**

## In Progress Notes


## Device
* Convert to saving only hashed password once ESP8266 Arduino library v2.6.0 is released
* Fix Device web interface (broken by changes allowing multiple systems on one device, see Network.cpp:299).
* Check PlatformIO library availability and add to installation instructions.

## Web Interface
* Profile performance. There is probably need for decimation to allow charting large quantities of data.
* ~~Should probably store historical data on client hard drive. When reconnecting, it should only update history instead of transmitting the entire history.~~ HTML5 API for local storage appears to be only implemented in Chrome. Can't store local data.
* Make x-axis manipulation faster and more aesthetic.
* When there are more points than pixels along the x-axis, convert to a different drawing regime (bin the data and then draw min-max curves)

## Documentation
* Improve README.md.
* Add instructions for creating Device key using shell script.

## Deployment
* Move deployment directory to main repository directory (and update docs)
* Separate Center deployment into a separate deploy subdirectory
* Include Arduino library in deployment directory

## Deferred
* Can't put library on Arduino Library Manager (https://github.com/arduino/Arduino/wiki/Library-Manager-FAQ) because the whole git repository must be dedicated to that library only.
* No need for guest domain behavior for device credentials. Just limited web interface dropdown to one available selection.
* Guest credentials for server could be interesting, but low priority now.