'use strict';


//let wsUri = "wss://echo.websocket.org/"; //wss is SSL, unsecure would be ws://
//let wsUri = "wss://localhost:49154/socket/"
let output;
let websocket;

let devices = new Map();

function init()
{
  testWebSocket();
}

function testWebSocket()
{
  let ws_header = "wss://";
  let ws_loc = ":49154/socket/";
  let websocket_url = ws_header + window.location.hostname + ws_loc;
  console.log("Websocket address " + String(websocket_url));

  websocket = new WebSocket(websocket_url);
  websocket.binaryType = 'arraybuffer';
  websocket.onopen = function(evt) { onOpen(evt) };
  websocket.onclose = function(evt) { onClose(evt) };
  websocket.onmessage = function(evt) { onMessage(evt) };
  websocket.onerror = function(evt) { onError(evt) };
}

function onOpen(evt)
{
  let x = document.getElementById("connection_alert");
  x.className = "alert alert-success";
  x.innerHTML = "Connected"
}

function onClose(evt)
{
  let x = document.getElementById("connection_alert");
  x.className = "alert alert-warning";
  x.innerHTML = "No connection";
}

function handlePassthroughCommand(ptcom)
{
  switch(ptcom.PTcommand_ID)
  {
    case IO_Constants.group_description:
      let new_group = new IO_Group(ptcom.message,ptcom.source_ID);
      console.log("Adding device index " + ptcom.source_ID + " for group " + new_group.name);
      devices.set(ptcom.source_ID,new_group);
      update_devices();
    break;
    default:
    console.log("Unexpected nested command in passthrough " +  ptcom.PTcommand_ID + ".");
  }
}

function onMessage(evt)
{
  let c = new Command(evt.data);
  
  //All commands should be passthrough as of now
  switch(c.command_ID)
  {
    case IO_Constants.passthrough:
      {
        let ptcom = new PTCommand(c);
        console.log("Passthrough command received. Nested command is " + ptcom.PTcommand_ID + ".");
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

  for(let key of devices.keys())
  {
    let device = devices.get(key);
    new_data = new_data.concat(device.toNode());
  }
  console.log("Changing nodes with " + new_data.length + " members.");
  console.log(new_data);
  changeNodes(new_data);
}

function onError(evt)
{
  console.log(evt.data);
  let x = document.getElementById("connection_alert");
  x.className = "alert alert-danger";
  x.innerHTML = "Error. Check console."
}

window.addEventListener("load", init, false);
