"use strict";

const IO_Constants =
{
    group_description: 1,
		emptynode_description: 2,
		value_description: 3,
		modifiablevalue_description: 4,
		data_package: 5,
		handshake: 6,
		modifiablevalue_modification: 9,
		request_subscription: 10,
		history: 11,
    passthrough: 12,
    new_device_available: 13,
    history_addendum: 14
};

const DataTypes =
{
  undefined: -1,
  signed_integer: 1,
  unsigned_integer: 2,
  floating_point: 3,
  bool: 4
};

const Regimes =
{
  live: 0,
  minute: 1,
  hour: 2,
  day: 3
};

let Globals =
{
  current_regime: Regimes.live
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

    const max_uint32 = Math.pow(2,32);

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
    this.name = bytebuffer.ReadString(this.name_length);
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
        let command_description = bytebuffer.Peek(DataTypes.signed_integer,2);
        switch(command_description)
        {
          case IO_Constants.group_description:
            this.members[i]=new IO_Group(bytebuffer, indices);
            //console.log("Processeed member " + this.members[i].name + " as group.");
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
  constructor(bytebuffer, indices){
    super(bytebuffer, indices);
    //this.ioValueCount = this.countIOValues()-1; //subtract one for the Timestamp
    this.ioValueCount = this.countIOValues(); //timestamp is still coming across as a float32. This should be fixed.
    //console.log("System has " + this.ioValueCount + " values.");

    this.epochs = new Map();

    this.chart_stream = true;
  }

  toNode()
  {
    let new_data = super.toNode();

    //Remove timestamp
    new_data.splice(1,1);

    return new_data;
  }

  createDashboard()
  {
    let retval = super.createDashboard(); //get the default IO_Node Dashboard, which is just a div

    //Remove timestamp
    retval.removeChild(retval.children[1]);

    return retval;
  }

  getEpochs(regime_index)
  {
    if(!this.epochs.has(regime_index))
    {
      this.epochs.set(regime_index,new Epochs(this));
    }
    return this.epochs.get(regime_index);
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

  setChartRegime(regime_index)
  {
    //console.log("Setting system regime to " + regime_index);
    let values = this.getIOValues();
    let epochs = this.getEpochs(regime_index);

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

class IO_Device
{
  constructor(bytebuffer, device_index){
    this.index = device_index;
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
    this.units = bytebuffer.ReadString(this.units_length);
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
  }

  createDashboard()
  {
    let retval = super.createDashboard(); //get the default IO_Node Dashboard, which is just a div

    let chart = retval.childNodes[1]; //chart will be second child

    let modification_row = document.createElement("div");
    modification_row.className = "row mb-1 no-gutters";

    let textbox = document.createElement("input");
    textbox.type = "number";
    textbox.className = "col md-6 form-control ml-1";
    textbox.step = "any";
    modification_row.appendChild(textbox);

    let button = document.createElement("button");
    button.innerHTML = "Modify";
    button.className = "col btn btn-primary ml-1 mr-1";
    modification_row.appendChild(button);

    retval.insertBefore(modification_row,chart);

    return retval;
  }

  ModifyValue(new_value)
  {
    const headersize = 2; //one short before the value
    let buffer = new ArrayBuffer(this.byte_count + headersize);
    let view = new DataView(buffer);
    view.setInt16(0,this.modval_index);
    switch(this.data_type)
    {
      case DataTypes.signed_integer:
        switch(this.byte_count)
        {
          case 2:
            view.setInt16(headersize,new_value);
          break;
          case 4:
            view.setInt32(headersize,new_value);
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
        switch(this.byte_count)
        {
          case 4:
            view.setFloat32(headersize,new_value);
          break;
          case 8:
            view.setFloat64(headersize,new_value);
          break;
          default:
            console.error("Wrong byte count.");
        }
      break;
      case DataTypes.bool:
        switch(this.byte_count)
        {
          case 1:
            view.setInt8(headersize,new_value);
          break;
          default:
            console.error("Wrong byte count.");
        }
      break;
    }

    console.error("Still need to package as a command, then as a PTCommand, then send it...");
  }
}

class Epochs{
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

  mergeLastAndFirst()
  {
    //console.log("Merging first and last.");

    if(this.current_epoch_index<=0 || this.current_epoch_index>=this.times.length-1)
    {
      return;
    }

    Epochs.reorder(this.times,this.current_epoch_index);
    for(let n = 0; n<this.values.length;n++)
    {
      Epochs.reorder(this.times[n],this.current_epoch_index);
    }
    this.current_epoch_index=this.times.length;
  }

  static reorder(array, index)
  {
    let first = array.slice(0,index);
    let last = array.slice(index);
    array = last.concat(first);
  }

  static getTime(millis)
  {
    //let seconds=millis/1000;
    //let millis_remainder=millis%1000;
    //let time=moment.unix(seconds);
    //time.milliseconds(millis_remainder);

    let time=moment(millis);
    return time;
  }

  processEntry(message, addendum)
  {
    const new_epoch_flag = Math.pow(2,0);
    const split_epoch_flag = Math.pow(2,1);

    let flag = message.getInt8();

    //console.log("Flag = " + flag);

    if(flag&new_epoch_flag){
        //Start a new epoch
        //console.debug("New epoch flag.");
        this.startNewEpoch();
    }

    if(flag&split_epoch_flag && !addendum){
      //Merge with first epoch
      //console.debug("Split flag.");
      this.mergeLastAndFirst();
    }


    this.times.push(Epochs.getTime(message.getInt64()));

    for(let n=0;n<this.values.length;n++)
    {
      let val = message.getFloat32();
      this.values[n].push(val);
    }
  }

  trim(first_timestamp)
  {
    let first_time=Epochs.getTime(first_timestamp);
    let first_index=0;
    while(first_time.isAfter(this.times[first_index]) && first_index<this.times.length)
    {
        first_index++;
    }
    //console.debug("Trimming " + first_index + " of " + this.times.length);
    this.times.splice(0,first_index);
    for(let n = 0; n<this.values.length;n++)
    {
      this.values[n].splice(0,first_index);
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
