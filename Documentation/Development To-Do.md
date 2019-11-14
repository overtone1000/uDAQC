# **Development Tasks**

## In Progress Notes

## Device
* Convert to saving only hashed password (v2.6.0 is out!)
* Fix Device web interface (broken by changes allowing multiple systems on one device, see Network.cpp:299).
* Check PlatformIO library availability and add to installation instructions.
* Device is running out of memory when trying to accept a second client.
* Might be nice to shorten file system names.

## Center
* Get rid of the "Point" class and just use a Float or a Double
* Consider compression of TimescaleDB older chunks. This may be a workaround for the fact that continuous aggregates can't have different data retention than their source tables.
* Should drop tables of systems that lose all their data to expiration.

## Web Interface
* Profile performance. There is probably need for decimation to allow charting large quantities of data.
* ~~Should probably store historical data on client hard drive. When reconnecting, it should only update history instead of transmitting the entire history.~~ HTML5 API for local storage appears to be only implemented in Chrome. Can't store local data.
* Make x-axis manipulation faster and more aesthetic.
* When there are more points than pixels along the x-axis, convert to a different drawing regime (bin the data and then draw min-max curves)
* Migrate to decimation server side to allow serving up smaller data sets for the chart needed at that time. 
* Need to be able to download all data to a file.

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