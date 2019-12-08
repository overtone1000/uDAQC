# **Development Tasks**

## In Progress Notes

## Device
* Convert to saving only hashed password (v2.6.0 is out!)
* Fix Device web interface (broken by changes allowing multiple systems on one device, see Network.cpp:299).
* Check PlatformIO library availability and add to installation instructions.
* Device is running out of memory when trying to accept a second client.
    * Switch debug to F()?
    * Switch to char* everywhere possible (so many Arduino String objects...)
* Might be nice to shorten file system names.

## Center
* Consider compression of TimescaleDB older chunks. This may be a workaround for the fact that continuous aggregates can't have different data retention than their source tables.
* Should drop tables of systems that lose all their data to expiration.
* Why is there a duplicate system with a description that passes equivalency test? This looks like a problem in the way database tables are named (devices have different descriptions but the same system table name). See output statements in `loadDevices()` in Database_udAQC.java. Should table names be hashes of the descriptions?

## Web Interface
* Need to be able to export all data or a subset of data to a CSV file.
* Need to keep WebSocket connection alive? Timeout is printing error to stack trace. Changed error handling to allow these to close without error output.
* Window resize works okay with `setTree()` but isn't perfect. Probably conflicting with resize of jstree container.
* Mutable chart height. Right now it's set statically in the `IO_Value.createDashboard()` function.
* Chart grouping
* Epochs are handled, but y-axis range is off for aggregated data.

## Documentation
* Improve README.md.
* Add instructions for creating Device key using shell script.

## Deployment
* Move deployment directory to main repository directory (and update docs)
* Separate Center deployment into a separate deploy subdirectory
* Include Device library in deployment directory

## Stretch and Brainstorming
* Ability to make Dashboards? Too ambitious? Would require...
    * Change device descriptions. First send name and unique ID. Then send the remainder of the desription, which would be identical for devices using the same configuration. A dashboard could then be available to any device with that configuration.
    * Could even do this at a smaller scope for all IO_Groups. Then, a group (like a PID controller) could have a default, reusable dashboard.
* Plugins to allow flows, alarms like many other dashboards?
* Plugins to interop with other protocols like MQTT?

## Deferred
* Can't put library on Arduino Library Manager (https://github.com/arduino/Arduino/wiki/Library-Manager-FAQ) because the whole git repository must be dedicated to that library only.
* No need for guest domain behavior for device credentials, so web interface dropdown is limited one available selection.
* Guest credentials for server could be interesting, but low priority now.