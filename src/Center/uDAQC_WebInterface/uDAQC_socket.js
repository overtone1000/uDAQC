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
      new IO_Device(ptcom.message,ptcom.source_ID);
      update_devices();
      break;
    case IO_Constants.history:
      handleHistory(ptcom);
      break;
    case IO_Constants.history_addendum:
      handleHistoryAddendum(ptcom);
      break;
    default:
    console.debug("Unexpected nested command in passthrough " +  ptcom.internal_command_ID + ".");
    console.debug(ptcom);
  }
}

function handleHistory(ptcom)
{
  let regime = ptcom.message.getInt32();
  let max_size = ptcom.message.getInt64();

  //console.log("History received for regime " + regime);

  let device = IO.devices.get(ptcom.source_ID);
  let entry_size = 1 + 8 + device.system.ioValueCount * 4;

  let epochs = device.system.getEpochs(regime);

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
    device.system.setChartRegime(Globals.current_regime);
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
  let entry_size = 1 + 8 + device.systems[system_index].ioValueCount * 4;

  let epochs = device.system.getEpochs(regime);

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
    device.system.setChartRegime(Globals.current_regime);
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
