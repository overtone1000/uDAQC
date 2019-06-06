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
    let retval = this.view.getInt64(this.pointer, this.view.isLittleEndian);
    this.pointer+=8;
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
  constructor(bytebuffer, device_index, index_tracker){
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

    this.device_index=device_index;
    this.reporter_index=index_tracker.current;
    index_tracker.current++;
  }

  id()
  {
    return this.device_index + "_" + this.reporter_index;
  }

  getNode()
  {
    return document.getElementById(this.getNodeID());
  }

  getChartspace()
  {
    return document.getElementById(this.getChartspaceID());
  }

  getNodeID()
  {
    return this.id() + "_node";
  }

  getChartspaceID()
  {
    return this.id() + "_div";
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
      id : this.getNodeID(),
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

  createChartspace()
  {
    let retval = document.createElement("div");
    retval.id = this.getChartspaceID();

    let title = document.createElement("div");
    title.innerHTML = this.name;

    retval.appendChild(title);

    return retval;
  }
}

class IO_Group extends IO_Reporter
{
  constructor(bytebuffer, device_index, index_tracker){
    console.log("Checking index_tracker.");
    if(index_tracker===undefined || index_tracker.current===undefined)
    {
      console.log("Making index_tracker.");
      index_tracker =
      {
        current:0
      };
      console.log(index_tracker);
    }
    console.log(index_tracker);
    super(bytebuffer, device_index, index_tracker);
    this.member_count = bytebuffer.getInt16();
    //console.log("Member count is " + this.member_count);
    //console.log("Byte buffer pointer at " + bytebuffer.pointer);
    this.members = new Array(this.member_count);
    let i;
    for(i=0;i<this.member_count;i++)
    {
        //need to peek the command_description for the next object to decide what kind to add
        let command_description = bytebuffer.Peek(DataTypes.signed_integer,2);
        switch(command_description)
        {
          case IO_Constants.group_description:
            this.members[i]=new IO_Group(bytebuffer, device_index, index_tracker);
            //console.log("Processeed member " + this.members[i].name);
          break;
          case IO_Constants.emptyreporter_description:
            this.members[i]=new IO_Reporter(bytebuffer, device_index, index_tracker);
            //console.log("Processeed member " + this.members[i].name);
          break;
          case IO_Constants.value_description:
            this.members[i]=new IO_Value(bytebuffer, device_index, index_tracker);
            //console.log("Processeed member " + this.members[i].name);
          break;
          case IO_Constants.modifiablevalue_description:
            this.members[i]=new IO_ModifiableValue(bytebuffer, device_index, index_tracker);
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

  createChartspace()
  {
    let retval = super.createChartspace(); //get the default IO_Reporter chartspace, which is just a div

    for(let child of this.members)
    {
      retval.appendChild(child.createChartspace());
    }

    return retval;
  }

}

class IO_Value extends IO_Reporter
{
  constructor(bytebuffer, device_index, index_tracker){
    super(bytebuffer, device_index, index_tracker);
    this.units_length = bytebuffer.getInt16();
    this.units = bytebuffer.ReadString(this.units_length);
    this.data_type = bytebuffer.getInt16();
  }

  createChartspace()
  {
    let retval = super.createChartspace(); //get the default IO_Reporter chartspace, which is just a div

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
  constructor(bytebuffer, device_index, index_tracker){
    super(bytebuffer, device_index, index_tracker);
    this.modval_index = bytebuffer.getInt16();
  }
}
