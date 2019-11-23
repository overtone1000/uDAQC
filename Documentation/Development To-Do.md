# **Development Tasks**

## In Progress Notes

## Device
* Convert to saving only hashed password (v2.6.0 is out!)
* Fix Device web interface (broken by changes allowing multiple systems on one device, see Network.cpp:299).
* Check PlatformIO library availability and add to installation instructions.
* Device is running out of memory when trying to accept a second client.
* Might be nice to shorten file system names.

## Center
* Consider compression of TimescaleDB older chunks. This may be a workaround for the fact that continuous aggregates can't have different data retention than their source tables.
* Should drop tables of systems that lose all their data to expiration.

## Web Interface
* Need to be able to export all data or a subset of data to a CSV file.
* Need to implement data streaming and history updates.
* Need to keep WebSocket connection alive.
* Mutable chart height. Right now it's set statically in the `IO_Value.createDashboard()` function.
* Chart grouping
* Handle epochs

## Documentation
* Improve README.md.
* Add instructions for creating Device key using shell script.

## Deployment
* Move deployment directory to main repository directory (and update docs)
* Separate Center deployment into a separate deploy subdirectory
* Include Device library in deployment directory

## Deferred
* Can't put library on Arduino Library Manager (https://github.com/arduino/Arduino/wiki/Library-Manager-FAQ) because the whole git repository must be dedicated to that library only.
* No need for guest domain behavior for device credentials, so web interface dropdown is limited one available selection.
* Guest credentials for server could be interesting, but low priority now.