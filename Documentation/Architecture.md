# IO Objects
uDAQC is based on the conceptual organization of IO Objects into hierarchical systems.
-IO_Node: This is the fundamental IO Object class. All other IO Objects inherit from this class or one of the other IO Object classes. The IO_Node has:
  -A String name
  -An int16 command description
  -An IO_Group parent
  -An int32 byte count
-IO_Value: This is an IO Object that extends/inherits IO_Node. It is to be used as an IO Object that represents an underlying value and so extends the class with:
  -A String for the units of the value
  -An int16 format type
  -An object value (in Devices, this is handled with templates, in Java with a generic Object)
  Of note, the IO_Node byte count is equal to the size of the type of the value (e.g. a uint32 will have an IO_Node byte count of 4)

-IO_ModifiableValue: This is an IO Object that extends/inherits IO_Value. It adds functionality allowing the IO_Value to be modified remotely and saved on the device.
-IO_Group: This is an IO Object that extends/inherits IO_Node and extends the class with a list of child IO_Nodes.
-IO_System: This is an IO Object that extends/inherits IO_Group and extends the class with functions that encapsulate network communication. Each IO_Device can contain multiple IO_Systems.

# Communication
TCP communication occurs between uDAQC Devices and uDAQC Centers. When a device or center begins, it broadcasts a UDP message requesting TCP communication on a given port. The recipient responds by initiating the TCP communication.
Afterwards, there are three primary communications that occur in uDAQC. There general nature is described in the list below with further details in the subsequent sections of this document.
1. On initialization, the device sends a Description for each IO_System, which contains information about the structure and contents of the IO_System.
2. Afterwards, the device sends Data for each IO_System, which contains a timestamp and the values of the IO_Values contained in its system.
3. Centers can send messages to Devices directing them to change an IO_ModifiableValue and the value to which it should be changed.

# Commands
TCP messages are structured as commands.
Commands have little endian format.
Commands between a Center and a Device have the following format:
1. int_32 containing the length of the message
2. int_16 containing the command ID
3. byte[] containing the message, which has a length equal to that provided in 1.

There are special commands between Centers (usually between the Primary Center and the WebSocket) that are called PassthroughCommands. These allow Centers and Secondary interfaces to communicate about specific IO_Devices. They have the following format:
1. int_32 containing the length of the message (which is larger than a normal command because of two additional int_16s, which are numbers 3 and 4 below)
2. int_16 containing the command ID for a PassthroughCommand
3. int_16 containing the IO_Device index (unique for each IO_Device on the Center)
4. int_16 containing the command ID for the nested command
5. byte[] containing the message, which does NOT have length equal to the that provided in 1.

A PassthroughCommand can be interpreted by receiving a normal Command and then interpreting the rest of the message to parse the  PassthroughCommand values above before returning the message to a handling function.

# Constants
A set of int_16 constants is maintained for communications. They must be identical for each language. There are two categories:
Command IDs - these are int_16 values sent as part of every TCP message to declare the nature of the message.
Data Types - these are int_16 values that identify the type of data contained by an IO_Value.

# Description Structure
Each IO Object has a function that will send its description. The contents of that description is as follows for each IO Object:
## IO_Node
1. int_16 containing the command description (this specifies which type of IO_Object this is)
    Previous note: At this time, this is incorrectly duplicated once in the description when an IO_System is sent, but this error is currently present in the C++, Java, and JS classes.
    It's not apparent whether this is still true.
    This was probably because the description is used in the command header and in an IO_Group's enumeration of its members to denote the type of each member.
2. int_32 containing the byte count for the data represented by this IO_Object (in practice, this only applies to IO_Value and classes that inherit from IO_Value; for all other classes, this will be 0)
3. int_16 containing the length of the name of this IO_Node
4. byte[] containing the name String (ASCII)
## IO_Group
1. The description for an IO_Node comes first (see above)
2. int_16 containing the number of members
3. byte[] containing the descriptions of each of its members in succession
## IO_Value
1. The description for an IO_Node comes first (see above)
2. int_16 containing the length of the units string
3. byte[] containing the units String (ASCII)
4. int_16 containing the data type (see Constants section above)
## IO_ModifiableValue
1. The description for an IO_Value comes first (see above)
2. int_16 containing the index for this ModifiableValue (this must be sent back to the device when changing the modifiable value)
## IO_System
1. The description for an IO_Group comes first (see above)
2. int_16 containing the index for this system. This will be included in subsequent data messages to identify which system on the device the data is for.

# Data Message Structure
When a data message for an IO_System is sent from a Device to a Center, the message first contains:
1. The int_16 index of that IO_System
2. An entry for each IO_Node in that system in the order that each IO_Node is found in the description for that IO_System.  

The size of each entry is equal to the data byte count found in the description for that IO_Node. In practice, the byte count will only be non-zero for IO_Value objects and any classes that inherit from IO_Value, although custom IO_Node objects not derived from IO_Value objects could conceivably be created that might be included in the data message for an IO_System.

# Time
When a Center connects to a Device, it will perform a time synchronization over UDP before accepting data messages. Any data messages received before this synchronization will be discarded. The result is that the time of the Device boot (the time at which the micros64() function would have been zero) is stored by the Center.

A raw timestamp is included in every data message by use a IO_Timestamp instance. This instance is added as the first member of every IO_System when it is created. The IO_Timestamp class inherits from the class IO_Value<int64_t>. The contents of int64_t is set to the result of the ESP8266 Arduino function micros64() when IO_Timestamp::SetTimeToNow() is called. The sketch should call this function at the time that should be used as the effective timestamp for this data update before sending the update.

The raw data message contains this raw timestamp. When the server receives the message and stores it as a history structure (see below) it calculates a complete Unix timestamp from its own clock and the prior synchronization.

There is currently a timing discrepancy. The raw tiemstamp is in microseconds since but, but Java converts to milliseconds since Unix epoch. This is simply due Java's limited compatibility with microsecond times. Use of microseconds would be alright from a type perspective even in Javascript, as 2,147,483,647,000,000 microseconds per epoch is still less than the safe integer value for Javascript.

# History Request Structure
This message is a command that contains a request from a web client to the Center to send historical data. Its structure is as follows:
1. int_16 indicating the IO_Device index for the device about which data is being requested
2. int_16 indicating the IO_System index for the system about which data is being requested
3. int_32 indicating the temporal regime
4. int_64 containing the timestamp of the last datum the Center already has for this regime. If it has none, this value will be left at zero.

# History Structure
This message contains the logged data for a single regime. It's a passthrough command. After the passthrough command header, the message contains:
1. int_16 indicating the IO_System index for this IO_Device
2. int_32 indicating the temporal regime (0 for live, 1 for minute, 2 for hour, 3 for day). This might be improved by instead sending the number of microseconds or milliseconds over which this time series has been averaged or otherwise consolidated.
3. int_64 indicating the maximum size of the following byte array (NOT necessarily the number provided in this message). Although javascript does not handle 64-bit integers well, this is likely large enough for practical purposes.
4. byte[] containing the log file

The log file itself is composed of a series of entries with the following structure:
1. byte containing flag bits with the following flags:
  1. New epoch - signals discontinuity of this entry from the prior entry
  2. Split epoch - signals that this entry is actually continuous with the first entry in the file
2. int_64 containing timestamp (ms since last Java epoch). Although javascript does not handle 64-bit integers well, the max safe value is 9,007,199,254,740,991 while an epoch only contains 2,147,483,647,000 milliseconds (more than 3 orders of magnitude)
3. float_32 for each IO_Value in this system (in the same order as that found in a data message)

# History Addendum structure
This message contains an additional datum for a single regime. Its structure is as follows:
1. int_32 indicating the temporal regime.
2. int_64 indicating the timestamp of the first datum in the archive. If a datum is older, it is expired and should be deleted.
3. byte[] containing the log file with the same structure as in the above but containing only a single datum

# Value Modification Structure
IO_ModifiableValue modification message is a command whose message has the following structure:
1. int_16 containing the index for the ModifiableValue (see above)
2. byte[] containing the value to which the ModifiableValue should be changed. The array is equal to the length of the data for this object (inherited from IO_Node).
