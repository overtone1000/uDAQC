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
1. int_16 containing the command description (this specifies which type of IO_Object this is)
2. int_16 containing the byte count for the data represented by this IO_Object (in practice, this only applies to IO_Value and classes that inherit from IO_Value; for all other classes, this will be 0)
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
When a data message for an IO_System is sent from a Device to a Center, the message contains an entry for each IO_Reporter in that system in the order that each IO_Reporter is found in the description for that IO_System. The size of that entry is equal to the data byte count found in the description for that IO_Repoerter. In practice, the byte count will only be non-zero for IO_Value objects and any classes that inherit from IO_Value, although custom IO_Reporter objects not derived from IO_Value objects could conceivably be created that might be included in the data message for an IO_System.

# Value Modification Structure
IO_ModifiableValue modification message is a command whose message has the following structure:
1. int_16 containing the index for the ModifiableValue (see above)
2. byte[] containing the value to which the ModifiableValue should be changed. The array is equal to the length of the data for this object (inherited from IO_Reporter).
