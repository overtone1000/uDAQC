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
      //new_device.index contains the device's index
      //new_device.member_count should contain the number of systems
      //.system_index will contain the index for a given system
      //new_device.members[] returns systems
      //new_device.members[n].system_index should give the index for each system
      const history_request_size = 2+2+4+8;
      for(let sys_index = 0; sys_index<new_device.member_count; sys_index++)
      {
        for(const regime of RegimesIterable)
        {
          let message = new DataViewWriter(history_request_size);
          message.putInt16(new_device.index);
          message.putInt16(new_device.members[sys_index].system_index);
          message.putInt32(regime);
          message.putInt64(0);
          console.debug("Requesting from device " + new_device.index + " system " + new_device.members[sys_index].system_index + " regime " + regime);
          let command = new Command(history_request_size,IO_Constants.history_request, message);
          command.sendto(websocket);
        }
      }
      break;
    case IO_Constants.history:
      handleHistory(ptcom);
      break;
    case IO_Constants.history_update:
      handleHistoryAddendum(ptcom);
      break;
    default:
    console.debug("Unexpected nested command in passthrough " +  ptcom.internal_command_ID + ".");
    console.debug(ptcom);
  }
}

function handleHistory(ptcom)
{
  let system_index = ptcom.message.getInt16();
  let regime = ptcom.message.getInt32();
  let max_size = ptcom.message.getInt64();

  //console.log("History received for regime " + regime);

  let device = IO.devices.get(ptcom.source_ID);
  console.log(device);
  let entry_size = 1 + 8 + device.members[system_index].ioValueCount * 4;

  let epochs = device.members[system_index].getEpochs(regime);

  let test_count=0;
  while(ptcom.message.remaining()>=entry_size)
  {
    //console.log("Processing entry.");
    epochs.processEntry(ptcom.message, false);
  }

  //console.log(epochs);

  if(Globals.current_regime===regime)
  {
    //If this histroy contains data for the currently displayed regime, update the chart like so...
    device.members[system_index].setChartRegime(Globals.current_regime);
  }
}

function handleHistoryAddendum(ptcom)
{
  let system_index = ptcom.message.getInt16();
  let regime = ptcom.message.getInt32();
  let first_timestamp = ptcom.message.getInt64();

  console.debug("History addendum received for system " + system_index + " regime " + regime);
  console.debug("Oldest = " + first_timestamp);
  console.debug(IO.devices);
  let device = IO.devices.get(ptcom.source_ID);
  let entry_size = 1 + 8 + device.members[system_index].ioValueCount * 4;

  let epochs = device.members[system_index].getEpochs(regime);

  //console.debug("remaining: " + ptcom.message.remaining());
  //console.debug("Size: " + entry_size);
  let test_count=0;
  while(ptcom.message.remaining()>=entry_size)
  {
    //console.debug("Processing addendum entry.");
    epochs.processEntry(ptcom.message, true);
  }

  epochs.trim(first_timestamp);

  //console.log(epochs);

  if(Globals.current_regime===regime)
  {
    //console.log("...");
    //If this histroy contains data for the currently displayed regime, update the chart like so...
    console.log(device.members);
    device.members[system_index].setChartRegime(Globals.current_regime);
  }
}

function onMessage(evt)
{
  let c = Command.interpret(evt.data);

  //All commands should be passthrough as of now
  switch(c.command_ID)
  {
    case IO_Constants.passthrough:
      {
        let ptcom = PTCommand.interpret(c);
        //console.log("Passthrough command received. Nested command is " + ptcom.internal_command_ID + ".");
        handlePassthroughCommand(ptcom);
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
    new_data = new_data.concat(device.toNode());

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
