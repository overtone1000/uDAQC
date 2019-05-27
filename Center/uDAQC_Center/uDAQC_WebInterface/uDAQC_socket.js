//var wsUri = "wss://echo.websocket.org/"; //wss is SSL, unsecure would be ws://
//var wsUri = "wss://localhost:49154/socket/"
var output;
var websocket;

var devices = new Map();

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
      console.log("Adding device index " + ptcom.source_ID + " for group " + new_group.name);
      devices.set(ptcom.source_ID,new_group);
      update_devices();
    break;
    default:
    console.log("Unexpected nested command in passthrough " +  ptcom.PTcommand_ID + ".");
  }
}

function update_devices()
{
  var new_data = new Array();
  console.log("Updating devices.");

  for(key of devices.keys())
  {
    var device = devices.get(key);
    var index =
    {
      current:0
    }
    new_data = new_data.concat(ioGroupToNodes(device,key,index));
  }
  console.log("Changing nodes with " + new_data.length + " members.");
  console.log(new_data);
  changeNodes(new_data);
}

function ioGroupToNodes(group, key, index)
{
  var new_data = new Array();
  console.log("Turning " + group.name + " into nodes.");
  var group_node = ioObjectToNode(group,key,index);
  new_data.push(group_node);
  console.log(new_data);
  for(child of group.members)
  {
    console.log("Adding child " + child.name + " of group " + group.name);
    switch(child.command_description)
    {
      case IO_Constants.group_description:
      new_data = new_data.concat(ioGroupToNodes(child,key,index,group_node));
      break;
      case IO_Constants.emptyreporter_description:
      case IO_Constants.value_description:
      case IO_Constants.modifiablevalue_description:
      new_data.push(ioObjectToNode(child,key,index,group_node));
      break;
      default:
      console.log("Wrong child description for building nodes = " + child.command_description + ", name is " + child.name + " of group " + group.name);
    }
  }

  return new_data;
}

function ioObjectToNode(object, key, index, parent)
{
  var parval = "#";
  if(parent !== undefined)
  {
      parval = parent.id;
  }
  var new_node =
  {
    "id" : key + "_" + index.current,
    "parent" : parval,
    "text" : object.name
  };
  index.current++;
  console.log("Index is now " + index.current);
  return new_node;
}

function onError(evt)
{
  console.log(evt.data);
  var x = document.getElementById("connection_alert");
  x.className = "alert alert-danger";
  x.innerHTML = "Error. Check console."
}

window.addEventListener("load", init, false);
