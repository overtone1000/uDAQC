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

# Commands
TCP messages are structured as commands.
Commands have little endian format.
Commands between a Center and a Device have the following format:
1. int_32 containing the length of the message
2. int_16 containing the command ID
3. byte[] containing the message, which has a length equal to that provided in 1.

There are special commands between a Center and a Secondary device that called PassthroughCommands. They have the following format:
1. int_32 containing the length of the message (which, in this case, includes two additional int_16, numbers 3 and 4 below)
2. int_16 containing the command ID for a PassthroughCommand
3. int_16 containing the source_id
4. int_16 containing the command ID for the nested command
5. byte[] containing the message, which does NOT have length equal to the that provided in 1.

# Constants
A set of int_16 constants is maintained for communications. They must be identical for each language. There are two categories:
Command IDs - these are int_16 values sent as part of every TCP message to declare the nature of the message.
Data Types - these are int_16 values that identify the type of data contained by an IO_Value.

# Description Structure
Each IO Object has a function that will send its description. The contents of that description is as follows for each IO Object:
## IO_Reporter
1. int_16 containing the command description (this specifies which type of IO_Object this is); at this time, this is incorrectly duplicated once in the description when an IO_System is sent, but this error is currently present in the C++, Java, and JS classes.
2. int_32 containing the byte count for the data represented by this IO_Object (in practice, this only applies to IO_Value and classes that inherit from IO_Value; for all other classes, this will be 0)
3. int_16 containing the length of the name of this IO_Reporter
4. byte[] containing the name String (ASCII)
## IO_Group
1. The description for an IO_Reporter comes first (see above)
2. int_16 containing the number of members
3. byte[] containing the descriptions of each of its members in succession
## IO_Value
1. The description for an IO_Reporter comes first (see above)
2. int_16 containing the length of the units string
3. byte[] containing the units String (ASCII)
4. int_16 containing the data type (see Constants section above)
## IO_ModifiableValue
1. The description for an IO_Value comes first (see above)
2. int_16 containing the index for this ModifiableValue (this must be sent back to the device when changing the modifiable value)
## IO_System
This class description is equal to its parent class IO_Group

# Data Message Structure
When a data message for an IO_System is sent from a Device to a Center, the message contains an entry for each IO_Reporter in that system in the order that each IO_Reporter is found in the description for that IO_System. The size of that entry is equal to the data byte count found in the description for that IO_Reporter. In practice, the byte count will only be non-zero for IO_Value objects and any classes that inherit from IO_Value, although custom IO_Reporter objects not derived from IO_Value objects could conceivably be created that might be included in the data message for an IO_System.

# History Structure
This message contains the logged data for a single regime. Its structure is as follows:
1. int_32 indicating the temporal regime (0 for live, 1 for minute, 2 for hour, 3 for day). This might be improved by instead sending the number of nanoseconds or milliseconds over which this time series has been averaged or otherwise consolidated.
2. int_64 indicating the maximum size of the following byte array (NOT necessarily the number provided in this message). Although javascript does not handle 64-bit integers well, this is likely large enough for practical purposes.
3. byte[] containing the log file

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

There is currently a timing discrepancy. The Device sends microseconds since the last epoch, but Java converts to milliseconds. This is simply due to a desire to use JodaTime in the Java IO_System class. This would be okay with javascript, as 2,147,483,647,000,000 microseconds per epoch is still less than the safe integer value for Javascript.

# Value Modification Structure
IO_ModifiableValue modification message is a command whose message has the following structure:
1. int_16 containing the index for the ModifiableValue (see above)
2. byte[] containing the value to which the ModifiableValue should be changed. The array is equal to the length of the data for this object (inherited from IO_Reporter).
