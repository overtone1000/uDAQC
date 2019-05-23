# uDAQC
An architecture for data acquisition, visualization, and control using the ESP8266 and a Java application with a Javascript UI.

# IO Objects
uDAQC is based on the conceptual organization of IO Objects into heirarchical systems.
-IO_Reporter: This is the fundamental IO Object class. All other IO Objects inherit from this class or one of the other IO Object classes. The IO_Reporter has:
  A String name
  An int16 command description
  An IO_Group parent
  An int32 byte count
-IO_Value: This is an IO Object that extends/inherits IO_Reporter. It is to be used as an IO Object that represents an underlying value and so exends the class with:
  A String for the units of the value
  An int16 format type
  An oject value (in Devices, this is handled with templates, in Java with a generic Object)
-IO_ModifiableValue: This is an IO Object that extends/inherits IO_Value. It adds functionality allowing the IO_Value to be modified remotely and saved on the device.
-IO_Group: This is an IO Object that extends/inherits IO_Reporter and extends the class with a list of child IO_Reporters.
-IO_System: This is an IO Object that extends/inherits IO_Group and extends the class with functions that encapsulate network communication.

# Communication
TCP communication occurs between uDAQC Devices and uDAQC Centers. When a device or center begins, it broadcasts a UDP message requesting TCP communication on a given port. The recipient responds by initiating the TCP communication.
Afterwards, there are three primary communications that occur in uDAQC:
1. On initialization, the device sends a Description for each IO_System, which contains information about the structure and contents of the IO_System.
2. Afterwards, the device sends Data for each IO_System, which contains a timestamp and the values of the IO_Values contained in its system.
3. Centers can send messages to Devices directing them to change an IO_ModifiableValue and the value to which it should be changed.

# Description Structure

# Data Structure

# Value Modification Structure
