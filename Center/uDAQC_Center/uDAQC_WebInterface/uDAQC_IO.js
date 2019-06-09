'use strict';

let IO_Constants =
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
};

let DataTypes =
{
  undefined: -1,
  signed_integer: 1,
  unsigned_integer: 2,
  floating_point: 3,
  bool: 4
};

class ByteBuffer {
  constructor(bytes){
    //console.log("Constructing ByteBuffer");
    this.view = new DataView(bytes);
    this.view.isLittleEndian = true;
    this.pointer = 0;
  }

  ReadString(length)
  {
    let retval = "";
    for (let i = 0; i < length; i++) {
      let char = this.view.getUint8(this.pointer+i);
      retval = retval.concat(String.fromCharCode(char));
      //console.log("New char " + char + ", string now " + retval);
    }
    this.pointer+=length; //no matter what, advance the pointer
    return retval;
  }

  getInt8(){
    let retval = this.view.getInt8(this.pointer, this.view.isLittleEndian);
    this.pointer+=1;
    return retval;
  }

  getInt16(){
    let retval = this.view.getInt16(this.pointer, this.view.isLittleEndian);
    this.pointer+=2;
    return retval;
  }

  getInt32(){
    let retval = this.view.getInt32(this.pointer, this.view.isLittleEndian);
    this.pointer+=4;
    return retval;
  }

  getInt64(){
    let little;
    let big;
    if(this.view.isLittleEndian)
    {
      little = this.getUint32(this.pointer, this.view.isLittleEndian);
      big = this.getInt32(this.pointer, this.view.isLittleEndian);
    }
    else {
      big = this.getInt32(this.pointer, this.view.isLittleEndian);
      little = this.getUint32(this.pointer, this.view.isLittleEndian);
    }

    let max_uint32 = big*Math.pow(2,32);
    if(big<0)
    {
      little=max_uint32-little;
    }

    //works for small positive numbers
    //works for 4305453037 (which is greater than the max int32)

    let retval = little + max_uint32;
    console.log("Get int 64 results:");
    console.log("Little: ");
    console.log(little);
    console.log("Big: ");
    console.log(big);
    console.log("Sum: ");
    console.log(retval);

    return retval;
  }

  getUint8(){
    let retval = this.view.getUint8(this.pointer, this.view.isLittleEndian);
    this.pointer+=1;
    return retval;
  }

  getUint16(){
    let retval = this.view.getUint16(this.pointer, this.view.isLittleEndian);
    this.pointer+=2;
    return retval;
  }

  getUint32(){
    let retval = this.view.getUint32(this.pointer, this.view.isLittleEndian);
    this.pointer+=4;
    return retval;
  }

  getUint64(){
    let retval = this.view.getUint64(this.pointer, this.view.isLittleEndian);
    this.pointer+=8;
    return retval;
  }

  getFloat32(){
    let retval = this.view.getFloat32(this.pointer, this.view.isLittleEndian);
    this.pointer+=4;
    return retval;
  }

  getFloat64(){
    let retval = this.view.getFloat64(this.pointer, this.view.isLittleEndian);
    this.pointer+=4;
    return retval;
  }

  Read(type,length)
  {
    let retval = this.Peek(type,length);
    this.pointer+=length; //no matter what, advance the pointer
    return retval;
  }

  Peek(type,length)
  {
    let retval;
    switch(type)
    {
      case DataTypes.undefined:
      console.log("Undefined type provided.");
        break;
      case DataTypes.signed_integer:
        switch(length)
        {
          case 1:
          retval = this.view.getInt8(this.pointer, this.view.isLittleEndian);
          //view = new Int8Array(this.bytes,pointer,1);
          break;
          case 2:
          retval = this.view.getInt16(this.pointer, this.view.isLittleEndian);
          //view = new Int16Array(this.bytes,pointer,1);
          break;
          case 4:
          retval = this.view.getInt32(this.pointer, this.view.isLittleEndian);
          //view = new Int32Array(this.bytes,pointer,1);
          break;
          case 8:
          retval = this.view.getInt64(this.pointer, this.view.isLittleEndian);
          //view = new Int64Array(this.bytes,pointer,1);
          break;
          default:
          console.log("Wrong size of signed integer requested " + length);
        }
        break;
      case DataTypes.unsigned_integer:
        switch(length)
        {
          case 1:
          retval = this.view.getUint8(this.pointer, this.view.isLittleEndian);
          //view = new Uint8Array(this.bytes,pointer,1);
          break;
          case 2:
          retval = this.view.getUint16(this.pointer, this.view.isLittleEndian);
          //view = new Uint16Array(this.bytes,pointer,1);
          break;
          case 4:
          retval = this.view.getUint32(this.pointer, this.view.isLittleEndian);
          //view = new Uint32Array(this.bytes,pointer,1);
          break;
          case 8:
          retval = this.view.getUint64(this.pointer, this.view.isLittleEndian);
          //view = new Uint64Array(this.bytes,pointer,1);
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
          retval = this.view.getFloat32(this.pointer, this.view.isLittleEndian);
          //view = new Float32Array(this.bytes,pointer,1);
          break;
          case 8:
          retval = this.view.getFloat64(this.pointer, this.view.isLittleEndian);
          //view = new Float64Array(this.bytes,pointer,1);
          break;
          default:
          console.log("Wrong size of signed integer requested " + length);
        }
        break;
      case DataTypes.bool:
        switch(length)
        {
          case 1:
          retval = this.view.getUint8(this.pointer, this.view.isLittleEndian);
          //view = new Float16Array(this.bytes,pointer,1);
          break;
          default:
          console.log("Wrong size of signed integer requested " + length);
        }
        break;
      default:
        console.log("Read type incorrect.");
    }
    //return view[0];
    return retval;
  }

  Remaining()
  {
    return (this.view.byteLength - this.pointer);
  }
}

class Command
{
  constructor(bytes)
  {
    this.message = new ByteBuffer(bytes); //message is initially set to the whole bytebuffer
    this.message_length = this.message.getInt32();
    this.command_ID = this.message.getInt16();
    //after the above are consumed, the remaining contents of the bytebuffer is the message_length

  }
}

class PTCommand
{
  constructor(command)
  {
    this.message = command.message;
    this.source_ID = this.message.getInt16();
    this.PTcommand_ID = this.message.getInt16();
  }
}

class IO_Reporter
{
  constructor(bytebuffer, indices){
    this.command_description = bytebuffer.getInt16();
    //console.log("Byte buffer pointer at " + bytebuffer.pointer);
    this.byte_count = bytebuffer.getInt32();
    //console.log("Byte count is " + this.byte_count);
    //console.log("Byte buffer pointer at " + bytebuffer.pointer);
    this.name_length = bytebuffer.getInt16();
    //console.log("Name length is " + this.name_length);
    //console.log("Byte buffer pointer at " + bytebuffer.pointer);
    this.name = bytebuffer.ReadString(this.name_length);
    console.log("New reporter = " + this.name);
    //console.log("Byte buffer pointer at " + bytebuffer.pointer);

    this.device_index=indices.device;
    this.reporter_index=indices.next_reporter();

    IO.nodeid_to_reporter.set(this.getNodeID,this);
  }

  id()
  {
    return this.device_index + "_" + this.reporter_index;
  }

  toNode(parent)
  {
    let new_data = [];

    let parval = "#";
    if(parent !== undefined)
    {
        parval = parent.id;
    }
    let new_node =
    {
      id : IO.getNodeID(this.id()),
      parent : parval,
      text : this.name,
      state : {
        opened : true,
        disabled: false,
        selected: true
      },
      is_checked:true
    };

    new_data.push(new_node);
    return new_data;
  }

  createDashboard()
  {
    let retval = document.createElement("div");
    retval.id = IO.getDashboardID(this.id());

    let title = document.createElement("div");
    title.innerHTML = this.name;

    retval.appendChild(title);

    return retval;
  }
}

class IO_Group extends IO_Reporter
{
  constructor(bytebuffer, indices){
    super(bytebuffer, indices);
    this.member_count = bytebuffer.getInt16();
    //console.log("Member count is " + this.member_count);
    //console.log("Byte buffer pointer at " + bytebuffer.pointer);
    this.members = new Array(this.member_count);
    for(let i=0;i<this.member_count;i++)
    {
        //need to peek the command_description for the next object to decide what kind to add
        let command_description = bytebuffer.Peek(DataTypes.signed_integer,2);
        switch(command_description)
        {
          case IO_Constants.group_description:
            this.members[i]=new IO_Group(bytebuffer, indices);
            //console.log("Processeed member " + this.members[i].name);
          break;
          case IO_Constants.emptyreporter_description:
            this.members[i]=new IO_Reporter(bytebuffer, indices);
            //console.log("Processeed member " + this.members[i].name);
          break;
          case IO_Constants.value_description:
            this.members[i]=new IO_Value(bytebuffer, indices);
            //console.log("Processeed member " + this.members[i].name);
          break;
          case IO_Constants.modifiablevalue_description:
            this.members[i]=new IO_ModifiableValue(bytebuffer, indices);
            //console.log("Processeed member " + this.members[i].name);
          break;
          default:
          console.log("Wrong command description = " + command_description);
        }
    }
  }

  toNode(parent)
  {
    let new_data = [];
    console.log("Turning " + this.name + " into nodes.");

    let group_node_arr = super.toNode(parent);
    new_data = new_data.concat(group_node_arr[0]);

    console.log(new_data);
    for(let child of this.members)
    {
      console.log("Adding child " + child.name + " of group " + this.name);
      new_data = new_data.concat(child.toNode(group_node_arr[0]));
    }

    return new_data;
  }

  createDashboard()
  {
    let retval = super.createDashboard(); //get the default IO_Reporter Dashboard, which is just a div

    for(let child of this.members)
    {
      retval.appendChild(child.createDashboard());
    }

    return retval;
  }

  countIOValues()
  {
    //count total IO_Values in the system
    let retval = 0;
    for(let i=0;i<this.member_count;i++)
    {
        switch(this.members[i].command_description)
        {
          case IO_Constants.group_description:
            retval+=this.members[i].countIOValues();
          break;
          case IO_Constants.emptyreporter_description:
          break;
          case IO_Constants.value_description:
          case IO_Constants.modifiablevalue_description:
            retval+=1;
          break;
        }
    }
    return retval;
  }
}

class IO_System extends IO_Group
{
  constructor(bytebuffer, indices){
    super(bytebuffer, indices);
    this.ioValueCount = this.countIOValues()-1; //subtract one for the Timestamp
    console.log("System has " + this.ioValueCount + " values.");
  }

  toNode()
  {
    let new_data = super.toNode();

    //Remove timestamp
    new_data.splice(1,1);

    console.log("Updated nodes:");
    console.log(new_data);

    return new_data;
  }

  createDashboard()
  {
    let retval = super.createDashboard(); //get the default IO_Reporter Dashboard, which is just a div

    //Remove timestamp
    retval.removeChild(retval.children[1]);

    return retval;
  }
}

class IO_Device
{
  constructor(bytebuffer, device_index){
    this.index = device_index;
    let indices =
    {
      device:device_index,
      reporter:0,
      next_reporter:function(){
        let retval=this.reporter;
        this.reporter++;
        return retval;
      }
    };
    this.system = new IO_System(bytebuffer,indices);
    IO.devices.set(this.index,this);
  }
}

class IO{
  static getNode(raw_id)
  {
    return document.getElementById(IO.getNodeID(raw_id));
  }

  static getDashboard(raw_id)
  {
    return document.getElementById(IO.getDashboardID(raw_id));
  }

  static getRawID(derived_id)
  {
    let lastindexof = derived_id.lastIndexOf("_");
    return derived_id.substring(0,lastindexof);
  }

  static getNodeID(reporter_id)
  {
    return reporter_id + "_node";
  }

  static getDashboardID(reporter_id)
  {
    return reporter_id + "_div";
  }
}
IO.devices = new Map();
IO.nodeid_to_reporter = new Map();

class IO_Value extends IO_Reporter
{
  constructor(bytebuffer, indices){
    super(bytebuffer, indices);
    this.units_length = bytebuffer.getInt16();
    this.units = bytebuffer.ReadString(this.units_length);
    this.data_type = bytebuffer.getInt16();
  }

  createDashboard()
  {
    let retval = super.createDashboard(); //get the default IO_Reporter Dashboard, which is just a div

    let chart = document.createElement("canvas");
    chart.id = this.id() + "_chart";
    chart.innerHTML = "Chart for " + this.name;
    createChart(chart);
    retval.appendChild(chart);

    return retval;
  }
}

class IO_ModifiableValue extends IO_Value
{
  constructor(bytebuffer, indices){
    super(bytebuffer, indices);
    this.modval_index = bytebuffer.getInt16();
  }
}
