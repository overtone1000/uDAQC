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

# Time
When a Center connects to a Device, it will perform a time synchronization over UDP before accepting data messages. Any data messages received before this synchronization will be discarded. The result is that the time of the Device boot (the time at which the micros64() function would have been zero) is stored by the Center.

A raw timestamp is included in every data message by use a IO_Timestamp instance. This instance is added as the first member of every IO_System when it is created. The IO_Timestamp class inherits from the class IO_Value<int64_t>. The contents of int64_t is set to the result of the ESP8266 Arduino function micros64() when IO_Timestamp::SetTimeToNow() is called. The sketch should call this function at the time that should be used as the effective timestamp for this data update before sending the update.

The raw data message contains this raw timestamp. When the server receives the message and stores it as a history structure (see below) it calculates a complete Unix timestamp from its own clock and the prior synchronization.

There is currently a timing discrepancy. The raw tiemstamp is in microseconds since but, but Java converts to milliseconds since Unix epoch. This is simply due Java's limited compatibility with microsecond times. Use of microseconds would be alright from a type perspective even in Javascript, as 2,147,483,647,000,000 microseconds per epoch is still less than the safe integer value for Javascript.

Furthermore, the PostgreSQL has an alternative long definition of the time. This is abstracted away by the Java Timestamp class. For reference, from the PostgreSQL docs (https://www.postgresql.org/docs/9.1/datatype-datetime.html):
>Note: When timestamp values are stored as eight-byte integers (currently the default), microsecond precision is available over the full range of values. When timestamp values are stored as double precision floating-point numbers instead (a deprecated compile-time option), the effective limit of precision might be less than 6. timestamp values are stored as seconds before or after midnight 2000-01-01. When timestamp values are implemented using floating-point numbers, microsecond precision is achieved for dates within a few years of 2000-01-01, but the precision degrades for dates further away. Note that using floating-point datetimes allows a larger range of timestamp values to be represented than shown above: from 4713 BC up to 5874897 AD. The same compile-time option also determines whether time and interval values are stored as floating-point numbers or eight-byte integers. In the floating-point case, large interval values degrade in precision as the size of the interval increases.

# Live Data 
## Data Message Structure
When a data message for an IO_System is sent from a Device to a Center, the message first contains:
1. The int_16 index of that IO_System
2. An entry for each IO_Node in that system in the order that each IO_Node is found in the description for that IO_System.  

The size of each entry is equal to the data byte count found in the description for that IO_Node. In practice, the byte count will only be non-zero for IO_Value objects and any classes that inherit from IO_Value, although custom IO_Node objects not derived from IO_Value objects could conceivably be created that might be included in the data message for an IO_System.

## Value Modification Structure
IO_ModifiableValue modification message is a command whose message has the following structure:
1. int_16 containing the index for the ModifiableValue (see above)
2. byte[] containing the value to which the ModifiableValue should be changed. The array is equal to the length of the data for this object (inherited from IO_Node).

# Historical Data
The Center can serve historical data stored in the TimescaleDB/PostgreSQL database to web clients for display.

## History Request Structure
This message is a command that contains a request from a web client to the Center to send historical data. Its structure is as follows:
1. int_16 indicating the IO_Device index for the device about which data is being requested
2. int_16 indicating the IO_System index for the system about which data is being requested
3. boolean (8 bytes): 
  * If true, the request is for the latest available data and the remainder is as follows:
    1. int_64 containing the number of milliseconds of data requested.
  * If false, the request is for a specific interval and the remainder is as follows:
    1. int_64 containing the timestamp of the start time of the displayed interval. If the earliest available data is requested, this value will be negative.
    2. int_64 containing the timestamp of the end time of the displayed interval. If the last available data is requested, this value will be negative.

## History Structure
This message contains a response to the History Request comand. Its structure is as follows:
1. int_16 indicating the IO_Device index for the device about which data is being requested
2. int_16 indicating the IO_System index for the system about which data is being requested
3. int_8 containing only a single flag in the smallest bit indicating whether this is raw data or an aggregate
4. An array containing the data

Each datum for raw data will have the following structure:
1. int_8 flag. The least significant bit will be true if the current epoch ends on this datum.
2. The remainder of the datum is identifcal to the Data Message structure above.

Each datum for aggregated data will have the following structure:
1. int_8 flag. The second least significant bit will be true if a new epoch starts on this datum. Note that this is different from the flag for raw data.
2. The remainder of the datum contains 3 values for each IO_Value such as in a Data Message for this system: 
  1. Average
  2. Minimum
  3. Maximum
  Each of these values will have the same type and size as that of the IO_Value with the exception of the boolean type, which will be converted to a 32-bit float and have values ranging between 0.0f (false) and 1.0f (true).

## History Update Structure
This message contains an update to the existing History possessed by the client. Its structure is identical to a History Structure (as above), but instead of completely replacing the existing historical data, the client performs an update. If the update contains a timestamp already possessed, the data for that timestamp is updated. If the update contains timestamp(s) not possessed by the client, oldest timestamps are discarded to keep the number of points the same.