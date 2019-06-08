'use strict';

function changeNodes(new_data)
{
  //This function successfully resets the jstree data. This is preferred.
  $('#jstree').jstree(true).settings.core.data=new_data;

  $('#jstree').jstree(true).refresh(); //Refreshing returns the tree to its original state...can never call if tree is updated using create_node!

}

$(function () {
  // 6 create an instance when the DOM is ready
  $('#jstree').jstree({
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
      cascade : 'down'
    },
    plugins : [ "wholerow", "checkbox" ]
  });
  // 7 bind to events triggered on the tree
  $('#jstree').on("changed.jstree", changedJSTree);
  // 8 interact with the tree - either way is OK
  $('#create1').click(function () {
    changeNodes(null,null);
  });

});

function changedJSTree(e, data)
{
  console.log(data);
  if(data.action==="deselect_all")
  {
    console.log("Need to hide everything here.");
  }
  if(!data.node)
  {
    return;
  }

  setDashboardVisibilityFromNodeID("#");
};

function setDashboardVisibilityFromNodeID(node_id)
{
  let retval=false;

  let node = $('#jstree').jstree(true).get_node(node_id); //this is ugly, but this seems to be how nodes are accessed with children_d
  for (let node_index in node.children)
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
  //if($('#jstree').jstree(true).is_checked(node)){ //ugly....
    dashboard.style.display = "block";
    retval = true;
  }
  else {
    dashboard.style.display = "none";
  }

  return retval;
};
