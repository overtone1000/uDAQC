"use strict";

const max_uint32 = Math.pow(2,32);

const IO_Constants =
{
    system_description: 1,
    group_description: 2,
		emptynode_description: 3,
		value_description: 4,
		modifiablevalue_description: 5,
		data_package: 6,
    handshake: 7,
    auth_request: 8,
    auth_provision: 9,
		modifiablevalue_modification: 10,
		request_subscription: 11,
		history: 12,
    passthrough: 13,
    new_device_available: 14,
    history_update: 15,
    timesync_request: 16,
    timesync_response: 17,
    history_request: 18
};

const DataTypes =
{
  undefined: -1,
  signed_integer: 1,
  unsigned_integer: 2,
  floating_point: 3,
  bool: 4
};

class DataViewWriter{
  constructor(size){
    //console.log("Constructing DataViewReader");
    this.view = new DataView(new ArrayBuffer(size));
    this.view.isLittleEndian = true;
    this.pointer = 0;
  }

  toBytes()
  {
    return new Uint8Array(this.view.buffer);
  }

  put(type,byte_count,value){

    switch(type)
    {
      case DataTypes.signed_integer:
        switch(byte_count)
        {
          case 2:
            this.putInt16(value);
          break;
          case 4:
            this.putInt32(value);
          break;
          case 8:
            console.error("No write for 64 bit integers yet implemented.");
          break;
          default:
            console.error("Wrong byte count.");
        }
      break;
      case DataTypes.unsigned_integer:
        console.error("No support for unsigned integers in Java server?");
      break;
      case DataTypes.floating_point:
        switch(byte_count)
        {
          case 4:
            this.putFloat32(value);
            console.debug("Writing float " + value);
          break;
          case 8:
            this.putFloat64(value);
          break;
          default:
            console.error("Wrong byte count.");
        }
      break;
      case DataTypes.bool:
        switch(byte_count)
        {
          case 1:
            this.putInt8(value);
          break;
          default:
            console.error("Wrong byte count.");
        }
      break;
    }
  }

  putArray(source)
  {
    this.toBytes().set(source, this.pointer);
    this.pointer+=source.length;
  }

  putInt8(value){
    this.view.setInt8(this.pointer, value, this.view.isLittleEndian);
    this.pointer+=1;
  }

  putInt16(value){
    this.view.setInt16(this.pointer, value, this.view.isLittleEndian);
    this.pointer+=2;
  }

  putInt32(value){
    this.view.setInt32(this.pointer, value, this.view.isLittleEndian);
    this.pointer+=4;
  }

  putInt64(value){

    if(Math.abs(value)>Number.MAX_SAFE_INTEGER)
    {
        console.warn("Unsafe integer size = " + retval);
    }

    let little;
    let big;

    big=Math.floor(value/max_uint32);
    little=value%max_uint32;
    
    if(this.view.isLittleEndian)
    {
      this.putInt32(little, this.view.isLittleEndian);
      this.putInt32(big, this.view.isLittleEndian);
    }
    else {
      this.putInt32(big, this.view.isLittleEndian);
      this.putInt32(little, this.view.isLittleEndian);
    }
  }

  putUint8(value){
    this.view.setUint8(this.pointer, value, this.view.isLittleEndian);
    this.pointer+=1;
  }

  putUint16(value){
    this.view.setUint16(this.pointer, value, this.view.isLittleEndian);
    this.pointer+=2;
  }

  putUint32(value){
    this.view.setUint32(this.pointer, value, this.view.isLittleEndian);
    this.pointer+=4;
  }

  putUint64(value){
    console.error("Uint64 put not yet implemented.");
    this.pointer+=8;
  }

  putFloat32(value){
    this.view.setFloat32(this.pointer, value, this.view.isLittleEndian);
    this.pointer+=4;
  }

  putFloat64(value){
    this.view.setFloat64(this.pointer, value, this.view.isLittleEndian);
    this.pointer+=8;
  }
}

class DataViewReader {
  constructor(bytes){
    //console.log("Constructing DataViewReader");
    this.view = new DataView(bytes);
    this.view.isLittleEndian = true;
    this.pointer = 0;
  }

  getString(length){
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

    let retval;
    if(big>=0)
    {
      retval = little + big*max_uint32;
    }
    else
    {
      little=max_uint32-little;
      big=~big;
      retval=-(little + big*max_uint32);
    }

    if(Math.abs(retval)>Number.MAX_SAFE_INTEGER)
    {
        console.warn("Unsafe integer size = " + retval);
    }

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

  get(type,length){
    let retval = this.peek(type,length);
    this.pointer+=length; //no matter what, advance the pointer
    return retval;
  }

  peek(type,length){
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

  remaining(){
    return (this.view.byteLength - this.pointer);
  }
}

class Command
{
  constructor(message_length, command_ID, message)
  {
    this.message = message;
    this.message_length = message_length;
    this.command_ID = command_ID;
  }
  static headerSize()
  {
    return 4 + 2;
  }
  static interpret(bytes)
  {
    let message = new DataViewReader(bytes);
    let message_length = message.getInt32();
    let command_ID = message.getInt16();
    //after the above are consumed, the remaining contents of the DataViewReader is the message_length
    let retval = new Command(message_length,command_ID,message);
    return retval;
  }
  
  createArrayBuffer()
  {
    let writer = new DataViewWriter(Command.headerSize() + this.message.view.byteLength);
    writer.putInt32(this.message.view.byteLength);
    writer.putInt16(this.command_ID);
    writer.putArray(new Uint8Array(this.message.view.buffer));
    return writer.view.buffer;
  }

  sendto(websocket)
  {
    let ptcom = this.createArrayBuffer();
    console.debug("Sending to Command websocket.");
    console.debug(new Uint8Array(ptcom));
    websocket.send(ptcom);
  }
}

class PTCommand
{
  constructor(source_ID, internal_command_ID, message)
  {
    this.source_ID = source_ID;
    this.internal_command_ID = internal_command_ID;
    this.message = message;
  }
  static headerSize()
  {
    return 2*2;
  }
  static interpret(command)
  {
    let source_ID = command.message.getInt16();
    let internal_command_ID = command.message.getInt16();
    //after the above are consumed, the remaining contents of the DataViewReader is the internal command
    let retval = new PTCommand(source_ID,internal_command_ID,command.message);
    return retval;
  }
  
  createArrayBuffer()
  {
    //Would be more maintainable code to call the Command version of this function,
    //But would result in an extra memory allocation.
    let writer = new DataViewWriter(Command.headerSize() + PTCommand.headerSize() + this.message.view.byteLength);
    writer.putInt32(PTCommand.headerSize() + this.message.view.byteLength);
    writer.putInt16(IO_Constants.passthrough);
    writer.putInt16(this.source_ID);
    writer.putInt16(this.internal_command_ID);
    let message_arr = new Uint8Array(this.message.view.buffer);
    writer.putArray(message_arr);
    return writer.view.buffer;
  }

  sendto(websocket)
  {
    let ptcom = this.createArrayBuffer();
    console.debug("Sending PTCommand to websocket.");
    console.debug(new Uint8Array(ptcom));
    websocket.send(ptcom);
  }
}

class IO_Node
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
    this.name = bytebuffer.getString(this.name_length);
    //console.log("New node = " + this.name);
    //console.log("Byte buffer pointer at " + bytebuffer.pointer);

    this.device_index=indices.device;
    this.node_index=indices.next_node();

    IO.nodeid_to_node.set(this.getNodeID,this);
  }

  id()
  {
    return this.device_index + "_" + this.node_index;
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
    retval.className="col border-top  border-primary no-gutters";

    let title_row = document.createElement("div");
    title_row.className="row align-items-center mt-1 mb-1 no-gutters";

    let title_col = document.createElement("div");
    title_col.className="col no-gutters ";

    let title = document.createElement("span");
    title.className="badge badge-secondary";
    title.innerHTML=this.name;
    title.style.height="100%";
    title.style.width="100%";

    title_col.appendChild(title);
    title_row.appendChild(title_col);
    retval.appendChild(title_row);
    return retval;
  }
}

class IO_Group extends IO_Node
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
        let command_description = bytebuffer.peek(DataTypes.signed_integer,2);
        switch(command_description)
        {
          case IO_Constants.group_description:
            this.members[i]=new IO_Group(bytebuffer, indices);
            //console.log("Processeed member " + this.members[i].name + " as group.");
          break;
          case IO_Constants.system_description:
            this.members[i]=new IO_System(bytebuffer, indices, i);
            //console.log("Processeed member " + this.members[i].name + " as empty node.");
          break;
          case IO_Constants.emptynode_description:
            this.members[i]=new IO_Node(bytebuffer, indices);
            //console.log("Processeed member " + this.members[i].name + " as empty node.");
          break;
          case IO_Constants.value_description:
            this.members[i]=new IO_Value(bytebuffer, indices);
            //console.log("Processeed member " + this.members[i].name + " as value.");
          break;
          case IO_Constants.modifiablevalue_description:
            this.members[i]=new IO_ModifiableValue(bytebuffer, indices);
            //console.log("Processeed member " + this.members[i].name + " as modifiable value.");
          break;
          default:
          console.log("Wrong command description = " + command_description);
        }
    }
  }

  toNode(parent)
  {
    let new_data = [];

    let group_node_arr = super.toNode(parent);
    new_data = new_data.concat(group_node_arr[0]);

    console.debug(this.members);
    for(let child of this.members)
    {
      new_data = new_data.concat(child.toNode(group_node_arr[0]));
    }

    return new_data;
  }

  createDashboard()
  {
    let retval = super.createDashboard(); //get the default IO_Node Dashboard, which is just a div

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
          case IO_Constants.emptynode_description:
          break;
          case IO_Constants.value_description:
          case IO_Constants.modifiablevalue_description:
            retval+=1;
          break;
        }
    }
    return retval;
  }

  getIOValues()
  {
    let retval = [];
    for(let i=0;i<this.member_count;i++)
    {
        switch(this.members[i].command_description)
        {
          case IO_Constants.group_description:
            retval=retval.concat(this.members[i].getIOValues());
          break;
          case IO_Constants.emptynode_description:
          break;
          case IO_Constants.value_description:
          case IO_Constants.modifiablevalue_description:
            retval.push(this.members[i]);
          break;
        }
    }
    return retval;
  }
}

class IO_System extends IO_Group
{
  constructor(bytebuffer, indices, index){
    super(bytebuffer, indices);
    this.system_index = index;
    //this.ioValueCount = this.countIOValues()-1; //subtract one for the Timestamp
    this.ioValueCount = this.countIOValues(); //timestamp is still coming across as a float32. This should be fixed.
    //console.log("System has " + this.ioValueCount + " values.");

    this.epochs = new Map();

    this.chart_stream = true;
  }

  toNode(parent)
  {
    let new_data = super.toNode(parent);

    //Remove timestamp
    new_data.splice(1,1);

    return new_data;
  }

  createDashboard()
  {
    let retval = super.createDashboard();

    //Remove timestamp
    retval.removeChild(retval.children[1]);

    return retval;
  }

  getEpochs()
  {
    return this.epochs;
  }

  updateChartXAxis(min_frac,max_frac)
  {
    let values = this.getIOValues();

    if(max_frac<1)
    {
      this.chart_stream=false;
    }

    for(let i=0;i<values.length;i++)
    {
      let min=values[i].dashstate.chart.current_min;
      let max=values[i].dashstate.chart.current_max;
      if(!min || isNaN(min)){min=values[i].dashstate.chart.absolute_min;}
      if(!max || isNaN(max)){max=values[i].dashstate.chart.absolute_max;}
      let interval=max-min;

      console.log("min = " + min);
      console.log("max = " + max);

      let new_min=min_frac*interval+min;
      let new_max=max-(1.0-max_frac)*interval;

      values[i].chart.options.scales.xAxes[0].time.min = moment(new_min);

      if(!this.chart_stream)
      {
        values[i].chart.options.scales.xAxes[0].time.max = moment(new_max);
      }

      values[i].chart.update();
    }
  }

  trimChartXAxis()
  {
    let values = this.getIOValues();

    for(let i=0;i<values.length;i++)
    {
      values[i].dashstate.chart.current_min=moment(values[i].chart.options.scales.xAxes[0].time.min);

      if(!this.chart_stream)
      {
        values[i].dashstate.chart.current_max=moment(values[i].chart.options.scales.xAxes[0].time.max);
      }

      values[i].chart.update();
    }
  }

  resetChartXAxis()
  {
    let values = this.getIOValues();

    this.chart_stream=true;

    for(let i=0;i<values.length;i++)
    {
      values[i].dashstate.chart.current_min=values[i].dashstate.chart.absolute_min;
      values[i].dashstate.chart.current_max=values[i].dashstate.chart.absolute_max;

      values[i].chart.options.scales.xAxes[0].time.min = null;
      values[i].chart.options.scales.xAxes[0].time.max = null;

      values[i].chart.update();
    }
  }

  setChart()
  {
    let values = this.getIOValues();
    let epochs = this.getEpochs();

    //console.debug("Epoch:");
    //console.debug(epochs);

    //console.debug("Values:");
    //console.debug(values);

    for(let i=0;i<values.length;i++)
    {
      //console.debug("Modifying chart " + i);
      values[i].chart.data.labels = epochs.times;

      values[i].dashstate.chart.absolute_min=epochs.earliestTime();
      values[i].dashstate.chart.absolute_max=epochs.latestTime();

      //values[i].dashstate.chart.current_min=epochs.earliestTime();
      //values[i].dashstate.chart.current_max=epochs.latestTime();

      //values[i].chart.options.scales.xAxes[0].time.min = values[i].dashstate.chart.absolute_min;
      //values[i].chart.options.scales.xAxes[0].time.max = values[i].dashstate.chart.absolute_max;


      values[i].chart.data.datasets=
      [
        {
          label: values[i].name + " (" + values[i].units + ")",
          fill: false, //no filling under the curve
          //backgroundColor: "rgb(0,0,0,0)", //transparent (this fills under the curve)
          borderColor: "rgb(255, 0, 0, 255)",
          data: epochs.values[i],
          labels: epochs.times,
          //pointRadius: 0 //don't render points, but if this is don't you can't hover to get value
          //pointBackgroundColor: "rgb(0,0,0,0)",
          pointBorderColor: "rgb(0,0,0,0)", //transparent
          spanGaps: false
        }
      ];

      values[i].chart.update(); //need to force update regardless of whether its enabled;
    }
  }
}

class IO_Device extends IO_Group
{
  constructor(bytebuffer, device_index){
    let indices =
    {
      device:device_index,
      node:0,
      next_node:function(){
        let retval=this.node;
        this.node++;
        return retval;
      }
    };
    super(bytebuffer, indices);
    this.index = device_index;

    IO.devices.set(this.index,this);

    console.log("Got new device");
    console.log(this);
    console.log(IO.devices);
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

  static getNodeID(node_id)
  {
    return node_id + "_node";
  }

  static getDashboardID(node_id)
  {
    return node_id + "_div";
  }
  static getChartID(node_id)
  {
    return node_id + "_chart";
  }
}
IO.devices = new Map();
IO.nodeid_to_node = new Map();

class IO_Value extends IO_Node
{
  constructor(bytebuffer, indices){
    super(bytebuffer, indices);
    this.units_length = bytebuffer.getInt16();
    this.units = bytebuffer.getString(this.units_length);
    this.data_type = bytebuffer.getInt16();
    this.chart = null;
    this.dashstate = {
      chart : {
        absolute_min:0,
        absolute_max:0,
        current_min:0,
        current_max:0
      }
    };
  }

  createDashboard()
  {
    let retval = super.createDashboard(); //get the default IO_Node Dashboard, which is just a div

    let dash = document.createElement("canvas");
    dash.id = IO.getChartID(this.id());
    dash.class="row";
    this.chart = createChart(dash);
    retval.appendChild(dash);

    return retval;
  }
}

class IO_ModifiableValue extends IO_Value
{
  constructor(bytebuffer, indices){
    super(bytebuffer, indices);
    this.modval_index = bytebuffer.getInt16();
    this.input_field = null;
    }

  createDashboard()
  {
    let retval = super.createDashboard(); //get the default IO_Node Dashboard, which is just a div

    let chart = retval.childNodes[1]; //chart will be second child

    let modification_row = document.createElement("div");
    modification_row.className = "row mb-1 no-gutters";

    let modpar = this;

    this.input_field = document.createElement("input");
    this.input_field.type = "number";
    this.input_field.className = "col md-6 form-control ml-1";
    this.input_field.step = "any";
    modification_row.appendChild(this.input_field);

    let button = document.createElement("button");
    button.innerHTML = "Modify";
    button.className = "col btn btn-primary ml-1 mr-1";
    button.onclick=function(){modpar.ModifyValue();};
    modification_row.appendChild(button);

    retval.insertBefore(modification_row,chart);

    return retval;
  }

  ModifyValue()
  {
    let message_size = 2+this.byte_count;
    let message = new DataViewWriter(message_size);
    message.putInt16(this.modval_index);
    message.put(this.data_type,this.byte_count,this.input_field.value);
    let message_reader = new DataViewReader(message.view.buffer);
    let ptcom = new PTCommand(this.device_index,IO_Constants.modifiablevalue_modification,message_reader);
    ptcom.sendto(websocket);
  }
}

//This class contains  aggregate historical data after a history command from the connected Center
//Epoch separation is performed based on time between data
class AggregateHistory
{

}

//This class contains the historical data after a history command from the connected Center
//Epoch separation is performed using the included flag
class RawHistory
{
  constructor(system)
  {
    this.current_epoch_index = 0;
    this.times = [];
    this.values = new Array(system.ioValueCount);
    for(let n = 0; n<this.values.length;n++)
    {
      this.values[n]=[];
    }
    //this.startNewEpoch();
  }
  
  startNewEpoch()
  {
    //console.debug("Starting new epoch.");
    if(this.times.length)
    {
      this.current_epoch_index = this.times.length;
      this.times.push(this.times[this.times.length-1]);
      for(let n = 0; n<this.values.length;n++)
      {
        this.values[n].push(null);
      }
    }
  }

  static getTime(millis)
  {
    let time=moment(millis);
    return time;
  }

  processEntry(message, addendum)
  {
    const new_epoch_flag = Math.pow(2,0);

    let flag = message.getInt8();

    //console.log("Flag = " + flag);

    if(flag&new_epoch_flag){
        //Start a new epoch
        //console.debug("New epoch flag.");
        this.startNewEpoch();
    }

    this.times.push(Epochs.getTime(message.getInt64()));

    for(let n=0;n<this.values.length;n++)
    {
      //let val = message.getFloat32(); This won't work anymore...
      console.error("Need to implement parsing function...");
      this.values[n].push(val);
    }
  }

  earliestTime()
  {
    let retval=this.times[0];
    for(let n=0;n<this.times.length;n++)
    {
      if(retval.isAfter(this.times[n]))
      {
        retval = this.times[n];
      }
    }
    return retval;
  }
  latestTime()
  {
    let retval=this.times[0];
    for(let n=0;n<this.times.length;n++)
    {
      if(retval.isBefore(this.times[n]))
      {
        retval = this.times[n];
      }
    }
    return retval;
  }
}
