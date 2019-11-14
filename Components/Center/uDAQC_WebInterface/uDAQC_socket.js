"use strict";

//let wsUri = "wss://echo.websocket.org/"; //wss is SSL, unsecure would be ws://
//let wsUri = "wss://localhost:49154/socket/"
let websocket;

function init()
{
  testWebSocket();
}

function testWebSocket()
{
  let ws_header = "wss://";
  let ws_loc = "/socket/";
  let websocket_url = ws_header + window.location.host + ws_loc;
  console.log("Websocket address " + String(websocket_url));

  websocket = new WebSocket(websocket_url);
  websocket.binaryType = "arraybuffer";
  websocket.onopen = function(evt) { onOpen(evt); };
  websocket.onclose = function(evt) { onClose(evt); };
  websocket.onmessage = function(evt) { onMessage(evt); };
  websocket.onerror = function(evt) { onError(evt); };
}

function onOpen(evt)
{
  let x = document.getElementById("connection_alert");
  x.className = "alert alert-success mb-0";
  x.innerHTML = "Connected";
  console.log(evt);
}

function onClose(evt)
{
  let x = document.getElementById("connection_alert");
  x.className = "alert alert-warning mb-0";
  x.innerHTML = "No connection";
  console.log(evt);
}

function handlePassthroughCommand(ptcom)
{
  switch(ptcom.internal_command_ID)
  {
    case IO_Constants.group_description:
      console.debug("Received description for device " + ptcom.source_ID);
      let new_device = new IO_Device(ptcom.message,ptcom.source_ID);
      update_devices();
      
      
      for(let n = 0; n<new_device.member_count; n++)
      {
        requestHistory(new_device.members[n]);
      }
      break;
    default:
    console.debug("Unexpected nested command in passthrough " +  ptcom.internal_command_ID + ".");
    console.debug(ptcom);
  }
}

function requestHistory(system, start_time=-1, end_time=-1)
{
  const history_request_size = 2+2+8+8;

  if(start_time==null){start_time=-1;}
  if(end_time==null){start_time=-1;}

  let message = new DataViewWriter(history_request_size);
  message.putInt16(system.parent_device_index);
  message.putInt16(system.system_index);
  message.putInt64(start_time);
  message.putInt64(end_time);
  let command = new Command(history_request_size,IO_Constants.history_request, message);
  command.sendto(websocket);
}

function handleHistory(com)
{
  let device_index = com.message.getInt16();
  let system_index = com.message.getInt16();
  let flags = com.message.getInt8();

  let device = IO.devices.get(device_index);
  let system = device.members[system_index];

  //console.log("Handling history for system:");
  //console.log(system);

  const aggregate_flag = Math.pow(2,0);
  let new_his=null;
  if(flags&aggregate_flag)
  {
    new_his=new AggregateHistory(system);
  }
  else
  {
    new_his=new RawHistory(system);
  }
  new_his.processEntries(com.message);

  system.setHistory(new_his);
}

function onMessage(evt)
{
  let c = Command.interpret(evt.data);

  switch(c.command_ID)
  {
    case IO_Constants.passthrough:
      {
        let ptcom = PTCommand.interpret(c);
        //console.log("Passthrough command received. Nested command is " + ptcom.internal_command_ID + ".");
        handlePassthroughCommand(ptcom);
      }
      break;
    case IO_Constants.history:
      {
        handleHistory(c);
      }
      break;
    default:
      console.log("Unexpected command " + c.command_ID + " of length " + c.message_length + " received.");
  }
}

function update_devices()
{
  let new_data = [];
  console.log("Updating devices.");

  //Clear the charts
  let dashboard = document.getElementById("chart_space");
  while(dashboard.firstChild)
  {
    dashboard.removeChild(dashboard.firstChild);
  }

  //First create jstree
  for(let key of IO.devices.keys())
  {
    let device = IO.devices.get(key);

    //Add this to the jsTree list
    new_data = new_data.concat(device.toTreeNode());

    //And create the dashboards
    dashboard.appendChild(device.createDashboard());
  }
  //console.log("Changing nodes with " + new_data.length + " members.");
  //console.log(new_data);
  changeNodes(new_data);
}

function onError(evt)
{
  console.log(evt.data);
  let x = document.getElementById("connection_alert");
  x.className = "alert alert-danger";
  x.innerHTML = "Error. Check console.";
}

window.addEventListener("load", init, false);
