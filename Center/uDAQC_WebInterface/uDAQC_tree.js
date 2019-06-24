"use strict";

function changeNodes(new_data)
{
  //This function successfully resets the jstree data. This is preferred.
  $("#jstree").jstree(true).settings.core.data=new_data;
  $("#jstree").jstree(true).select_all();
  $("#jstree").jstree(true).check_all();
  $("#jstree").jstree(true).refresh(); //Refreshing returns the tree to its original state...can never call if tree is updated using create_node!
}

$(function () {

  $("#jstree").jstree({
    core : {
      check_callback: true,
      multiple : true,
      animation : 0,
      data : [],
      themes : {
        letiant : "large",
        icons : false,
        stripes : false}
    },
    checkbox : {
      keep_selected_style : false,
      three_state : true,
      cascade : "down"
    },
    plugins : [ "wholerow", "checkbox" ]
  });

  //Update dashboard visibility when the tree is changed
  $("#jstree").on("changed.jstree", function(e,data) {
    setDashboardVisibilityFromNodeID("#");
  });

  //Make all nodes visible when the tree is refreshed (called in change nodes function)
  $("#jstree").on("refresh.jstree", function(e) {
    $("#jstree").jstree(true).select_all();
  });
});

function setDashboardVisibilityFromNodeID(node_id)
{
  let retval=false;

  let node = $("#jstree").jstree(true).get_node(node_id); //this is ugly, but this seems to be how nodes are accessed with children_d

  for(let node_index=0;node_index<node.children.length;node_index++)
  //for (let node_index in node.children)
  {
    let node_id = node.children[node_index];
    if(setDashboardVisibilityFromNodeID(node_id))
    {
      retval = true;
    }
  }

  let rawID = IO.getRawID(node_id);

  let dashboard = IO.getDashboard(rawID);

  if(!dashboard)
  {
    return;
  }

  if(node.state.selected || retval){
  //if($("#jstree").jstree(true).is_checked(node)){ //ugly....
    dashboard.style.display = "block";
    retval = true;
  }
  else {
    dashboard.style.display = "none";
  }

  return retval;
}
