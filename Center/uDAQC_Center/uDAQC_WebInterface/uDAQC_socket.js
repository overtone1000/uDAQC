//var wsUri = "wss://echo.websocket.org/"; //wss is SSL, unsecure would be ws://
//var wsUri = "wss://localhost:49154/socket/"
var output;
var websocket;

function init()
{
  testWebSocket();
}

function testWebSocket()
{
  var ws_header = "wss://";
  var ws_loc = ":49154/socket/";
  var websocket_url = ws_header + window.location.hostname + ws_loc;
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
  var x = document.getElementById("connection_alert");
  x.className = "alert alert-success";
  x.innerHTML = "Connected"

  websocket.send("Client hello.");
}

function onClose(evt)
{
  var x = document.getElementById("connection_alert");
  x.className = "alert alert-warning";
  x.innerHTML = "No connection"
}

function onMessage(evt)
{
  var c = new Command(evt.data);

  //All commands should be passthrough as of now
  switch(c.command_ID)
  {
    case IO_Constants.passthrough:
    {
      var ptcom = new PTCommand(c);
      console.log("Passthrough command received. Nested command is " + ptcom.PTcommand_ID + ".");
      HandlePassthroughCommand(ptcom);
    }
    break;
    default:
    console.log("Unexpected command " + c.command_ID + " of length " + c.message_length + " received.");
  }
}

function HandlePassthroughCommand(ptcom)
{
  switch(ptcom.PTcommand_ID)
  {
    case IO_Constants.group_description:
      var new_group = new IO_Group(ptcom.message);
    break;
    default:
    console.log("Unexpected nested command in passthrough " +  ptcom.PTcommand_ID + ".");
  }
}

function onError(evt)
{
  console.log(evt.data);
  var x = document.getElementById("connection_alert");
  x.className = "alert alert-danger";
  x.innerHTML = "Error. Check console."
}

window.addEventListener("load", init, false);
