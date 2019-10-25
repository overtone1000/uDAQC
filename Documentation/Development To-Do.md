# **Development Tasks**

## In Progress Notes
Added a "history_request" command. Instead of automatically sending historical update when a client connects to the Center web interface, the client shoudl request history FOR EACH SYSTEM and provide the time of the most recent datum the client has FOR THAT SYSTEM. This will allow saving historical data in a temp directory. Using this saved data instead of downloading again will limit bandwidth consumption on subsequent sessions. It'll also eventually allow for a smaller memory footprint.

Center code is drafted. Should correctly interpret history_request commands and respond only with a select history for one regime of one device of one system. Still need to modify the PassthroughInitialization function in IO_System_Logged.java to remove all data equal to or older than the supplied last_time before sending.

## Device
* Convert to saving only hashed password once ESP8266 Arduino library v2.6.0 is released
* Fix Device web interface (broken by changes allowing multiple systems on one device, see Network.cpp:299).
* Check PlatformIO library availability and add to installation instructions.

## Web Interface
* Profile performance. There is probably need for decimation to allow charting large quantities of data.
* Should probably store historical data on client hard drive. When reconnecting, it should only update history instead of transmitting the entire history.
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