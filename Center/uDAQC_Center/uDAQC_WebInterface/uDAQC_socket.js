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
  console.log("Received message.");
  
  var data = evt.data;
  var dv = new DataView(data);

  console.log("Message of length " + dv.byteLength + " received.");
}

function onError(evt)
{
  console.log(evt.data);
  var x = document.getElementById("connection_alert");
  x.className = "alert alert-danger";
  x.innerHTML = "Error. Check console."
}

window.addEventListener("load", init, false);
