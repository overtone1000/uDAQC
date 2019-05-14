function createNodes()
{
  //This function successfully creates new nodes on the fly
  retval = $('#jstree').jstree('create_node', '#', {data:'Foo'});
  console.log("Returned " + String(retval));

  retval = $('#jstree').jstree('create_node', 'm1', {data:'Bar'});
  console.log("Returned " + String(retval));

  $('#jstree').jstree(true).redraw(); //this is the desired function
  //$('#jstree').jstree(true).refresh(); //Refreshing returns the tree to its original state...can never call if tree is updated using create_node!
}

function changeNodes(parent, new_node)
{
  //This function successfully resets the jstree data. This is preferred.

  var new_data = [
   { "id" : "dev1", "parent" : "#", "text" : "Device 1" },
   { "id" : "dev1sys1", "parent" : "dev1", "text" : "System 1" },
   { "id" : "dev1sys2", "parent" : "dev1", "text" : "System 2" },
   { "id" : "dev2", "parent" : "#", "text" : "Device 2" },
   { "id" : "dev1sys1", "parent" : "dev2", "text" : "System 1" },
   { "id" : "dev1sys2", "parent" : "dev2", "text" : "System 2" },
   { "id" : "dev1sys3", "parent" : "dev2", "text" : "System 3" },
  ];

  $('#jstree').jstree(true).settings.core.data=new_data;

  $('#jstree').jstree(true).refresh(); //Refreshing returns the tree to its original state...can never call if tree is updated using create_node!

}

var data = [
  { "id":"m1", "text": "M1", "children":
    [
      {"parent": "m1", "text": "T1"},
      {"parent": "m1", "text": "T2"},
    ],
    "state":{"opened":true}}
  ];

$(function () {
  // 6 create an instance when the DOM is ready
  $('#jstree').jstree({
    'core' : {
      'check_callback': true,
      'multiple' : true,
      "animation" : 0,
      'data' : data,
      "themes" : {
        "variant" : "large",
        "icons" : false,
        "stripes" : false}
    },
    "checkbox" : {
      "keep_selected_style" : false
    },
    "plugins" : [ "wholerow", "checkbox" ]
  });
  // 7 bind to events triggered on the tree
  $('#jstree').on("changed.jstree", function (e, data) {
    console.log(data.selected);
  });
  // 8 interact with the tree - either way is OK
  $('#create1').click(function () {
    changeNodes(null,null);
  });

});
