var IO_Constants =
{
    group_description: 1,
		emptyreporter_description: 2,
		value_description: 3,
		modifiablevalue_description: 4,
		data_package: 5,
		handshake: 6,
		modifiablevalue_modification: 9,
		request_subscription: 10,
		history: 11,
    passthrough: 12
}

var DataTypes =
{
  undefined: -1,
  signed_integer: 1,
  unsigned_integer: 2,
  floating_point: 3,
  bool: 4
}

console.log("IO_Constants loaded. Group description is " + IO_Constants.group_description);

class ByteBuffer {
  constructor(byte_array){
    this.bytes = byte_array;
    this.pointer = 0;
  }

  ReadString(length)
  {
    view = new Uint8Array(this.bytes,pointer,length);
    var retval = "";
    var i;
    for (i = 0; i < length; i++) {
      retval += String.fromCharCode(view[i]);
    }
    this.pointer+=length; //no matter what, advance the pointer
  }

  Read(type,length)
  {
    var retval = Peek(type,length);
    this.pointer+=length; //no matter what, advance the pointer
    return retval;
  }

  Peek(type,length)
  {
    view = new Uint8Array(this.bytes);
    switch(type)
    {
      case DataTypes.undefined:
      console.log("Undefined type provided.");
        break;
      case DataTypes.signed_integer:
        switch(length)
        {
          case 1:
          view = new Int8Array(this.bytes,pointer,1);
          break;
          case 2:
          view = new Int16Array(this.bytes,pointer,1);
          break;
          case 4:
          view = new Int32Array(this.bytes,pointer,1);
          break;
          case 8:
          view = new Int64Array(this.bytes,pointer,1);
          break;
          default:
          console.log("Wrong size of signed integer requested " + length);
        }
        break;
      case DataTypes.unsigned_integer:
        switch(length)
        {
          case 1:
          view = new Uint8Array(this.bytes,pointer,1);
          break;
          case 2:
          view = new Uint16Array(this.bytes,pointer,1);
          break;
          case 4:
          view = new Uint32Array(this.bytes,pointer,1);
          break;
          case 8:
          view = new Uint64Array(this.bytes,pointer,1);
          break;
          default:
          console.log("Wrong size of signed integer requested " + length);
        }
        break;
      case DataTypes.floating_point:
        switch(length)
        {
          case 2:
          console.log("Javascript does not support that half (16-bit float) type.");
          break;
          case 4:
          view = new Float32Array(this.bytes,pointer,1);
          break;
          case 8:
          view = new Float64Array(this.bytes,pointer,1);
          break;
          default:
          console.log("Wrong size of signed integer requested " + length);
        }
        break;
      case DataTypes.bool:
        switch(length)
        {
          case 1:
          view = new Float16Array(this.bytes,pointer,1);
          break;
          default:
          console.log("Wrong size of signed integer requested " + length);
        }
        break;
      default:
        console.log("Read type incorrect.");
    }
    return view[0];
  }

}

class IO_Reporter
{
  constructor(command_description, bytebuffer){
    this.command_description = command_description;
    this.byte_count = bytebuffer.Read(signed_integer,2);
    this.name_length = bytebuffer.Read(signed_integer,2);
    this.name = bytebuffer.ReadString(this.name_length);
  }
}

class IO_Group extends IO_Reporter
{
  constructor(bytebuffer){
    super(bytebuffer);
    this.member_count = bytebuffer.Read(signed_integer,2);
    this.members = Object(this.member_count);
    var i;
    for(i=0;i<this.member_count;i++)
    {
        //need to peek the command_description for the next object to decide what kind to add
        var command_description = bytebuffer.Peek(signed_integer,2);
        switch(command_description)
        {
          case IO_Contants.group_description:
            this.members[i]=new IO_Group(bytebuffer);
          break;
          case IO_Contants.emptyreporter_description:
            this.members[i]=new IO_Reporter(bytebuffer);
          break;
          case IO_Contants.value_description:
            this.members[i]=new IO_Value(bytebuffer);
          break;
          case IO_Contants.modifiablevalue_description:
            this.members[i]=new IO_ModifiableValue(bytebuffer);
          break;
          default:
          console.log("Ruh roh...");
        }
    }
  }
}

class IO_Value extends IO_Reporter
{
  constructor(bytebuffer){
    super(bytebuffer);
    this.units_length = bytebuffer.Read(signed_integer,2);
    this.units = bytebuffer.ReadString(this.units_length);
    this.data_type = bytebuffer.Read(signed_integer,2);
  }
}

class IO_ModifiableValue extends IO_Value
{
  constructor(bytebuffer){
    super(bytebuffer);
    this.modval_index = bytebuffer.Read(signed_integer,2);
  }
}
