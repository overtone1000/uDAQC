# **Development Tasks**

## Repository
* Rename "src" in git repository to "Components"
* Rename "docs" in git repository to "Documentation" (and update hyperlinks in README.md)
* Move all contents of library durectory (currently uDAQC/src/Device/libraries/uDAQC) except keywords.txt and library.properties to a "src" subdirectory
* Move contents of uDAQC/src/Device/examples to the library directory in an "examples" subdirectory
* Move library directory contents directly to uDAQC/src/Device


## Device
* Convert to saving only hashed password once ESP8266 Arduino library v2.6.0 is released
* Fix Device web interface (broken by changes allowing multiple systems on one device, see Network.cpp:299).
* Make library available via Library Manager? 
  * https://github.com/arduino/Arduino/wiki/Library-Manager-FAQ
  * https://docs.platformio.org/en/latest/librarymanager/

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
