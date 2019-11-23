"use strict";

let udaqc_chartjs_plugin = {
  rendering_enabled: true,
  beforeUpdate: function(){
    //console.log("beforeUpdate hook");
    if(!this.rendering_enabled)
    {
      console.log("Cancelling update." + this.rendering_enabled);
    }
    return this.rendering_enabled;
  },
  afterUpdate: function(){
    //console.log("afterUpdate hook");
  },
  beforeLayout: function(){
    if(!this.rendering_enabled)
    {
      console.log("Cancelling layout.");
    }
    return this.rendering_enabled;
  },
  afterLayout: function(){
    //console.log("afterLayout hook");
  },
  beforeDraw: function(){
    if(!this.rendering_enabled)
    {
      console.log("Cancelling draw.");
    }
    return this.rendering_enabled;
  },
  afterDraw: function(){
    //console.log("afterLayout hook");
  },
  beforeRender: function(){
    if(!this.rendering_enabled)
    {
      console.log("Cancelling render.");
    }
    return this.rendering_enabled;
  },
  afterRender: function(){
    //console.log("afterLayout hook");
  },
  resize: function(){
    //console.log("Chart resized.");
  }  
};

Chart.pluginService.register(udaqc_chartjs_plugin);

let chartSystemMap = new Map();
function createChart(canvas, parent)
{
  let retval = new Chart(canvas.getContext("2d"),
    {
      // The type of chart we want to create
      type: "line",

      // The data for our dataset
      data: {
          labels: [
            moment().add(0, "d").toDate(),
            moment().add(1, "d").toDate()
          ],
          datasets: [{
              label: "My First dataset",
              fill: false, //no filling under the curve
              //backgroundColor: "rgb(0,0,0,0)", //transparent (this fills under the curve)
              borderColor: "rgb(255, 0, 0, 255)",
              data: [
                1,
                10
              ],
              //pointRadius: 0 //don't render points, but if this is don't you can't hover to get value
              //pointBackgroundColor: "rgb(0,0,0,0)",
              pointBorderColor: "rgb(0,0,0,0)" //transparent
          }]
      },

      // Configuration options go here
      options:
      {
        responsive: true,
        maintainAspectRatio: false,
        legend:
        {
          display: true,
          position: "bottom",
          labels:
          {
            filter: function(item, chart) {
              // Logic to remove a particular legend item goes here
              return !(item.text==null);
          }
        }
        },
        scales:
        {
          xAxes: [{
            type: "time",
            time:{
              min:0,
              max:0
            }
          }]
        },
        animation:false,
        elements:{line:{tension:0}},
        //ChartJS Plugin Zoom
        plugins: {
          zoom: {
            // Container for pan options
            pan: {
              // Boolean to enable panning
              enabled: false,
            },
        
            // Container for zoom options
            zoom: {
              // Boolean to enable zooming
              enabled: true,
        
              // Enable drag-to-zoom behavior
              drag: true,
        
              // Drag-to-zoom effect can be customized
               drag: {
               	 borderColor: 'rgba(225,225,225,0.3)',
               	 borderWidth: 5,
               	 backgroundColor: 'rgb(225,225,225)',
                 animationDuration: 0
               },
        
              // Zooming directions. Remove the appropriate direction to disable
              // Eg. 'y' would only allow zooming in the y direction
              // A function that is called as the user is zooming and returns the
              // available directions can also be used:
              //   mode: function({ chart }) {
              //     return 'xy';
              //   },
              mode: 'x',
        
              rangeMin: {
                // Format of min zoom range depends on scale type
                x: null
              },
              rangeMax: {
                // Format of max zoom range depends on scale type
                x: null
              },
        
              // Speed of zoom via mouse wheel
              // (percentage of zoom on a wheel event)
              //speed: 0.1,
        
              // Function called while the user is zooming
              onZoom: function({chart}) { 
                console.log(`I'm zooming!!!`); 
                //console.debug(chart);
                //console.debug("min="+chart.options.scales.xAxes[0].time.min);
                //console.debug("max="+chart.options.scales.xAxes[0].time.max);

                let system=chartSystemMap.get(chart);
                let min=chart.options.scales.xAxes[0].time.min;
                let max=chart.options.scales.xAxes[0].time.max;
                system.updateChartXAxis(min,max);
              },

              // Function called once zooming is completed
              //onZoomComplete: function({chart}) 
              //{ 
              //  console.log(`I was zoomed!!!`);
              //  console.debug(chart); 
              //  console.debug("min="+chart.options.scales.xAxes[0].time.min);
              //  console.debug("max="+chart.options.scales.xAxes[0].time.max);
              //}
            }
          }
        }
      }
  });
  console.log(retval);
  chartSystemMap.set(retval, parent);
  return retval;
}

let resetX = function(e)
{
  console.log("Reset x function called.");
  for(let key of IO.devices.keys())
  {
    let device = IO.devices.get(key);
    for(let sys of device.members)
    {
      sys.resetChartXAxis();
    }
  }
};

let jstree_shown=true;
let toggleTree = function(e)
{
  console.log("Clicked toggle.")
  jstree_shown=!jstree_shown;
  disable_chart_rendering();
  set_chart_widths("0px");
  let body_space = document.getElementById("body_container");
  let dash_space = document.getElementById("dash_container");
  let chart_space = document.getElementById("chart_space_container");
  let js_tree_container = document.getElementById("jstree_container");
  let js_tree = document.getElementById("jstree");
  
  if(jstree_shown)
  {
    console.log("Showing tree.");
    js_tree_container.style.display="block";
    //dash_space.style.width="50%";
    dash_space.style.width=body_space.clientWidth-js_tree_container.clientWidth + "px";
    
    console.log("Body space width is now " + body_space.clientWidth);
    console.log("JSTreeContainer width is now " + js_tree_container.clientWidth);
    console.log("Dash space width is now " + dash_space.style.width);
    
    //js_tree_container.style.width="50%";
    //js_tree_container.style.width=js_tree.clientWidth;
  }
  else
  {
    console.log("Hiding tree.");
    dash_space.style.width="100%";
    js_tree_container.style.display="none";
  }
  enable_chart_rendering();
  set_chart_widths(dash_space.clientWidth - 20 + "px");
  console.log("Dash space:");
  console.log(dash_space);
};

let disable_chart_rendering = function(e)
{
  udaqc_chartjs_plugin.rendering_enabled=false;
}
let enable_chart_rendering = function(e)
{
  udaqc_chartjs_plugin.rendering_enabled=true;
}

let set_chart_widths=function(new_width)
{
  let chart_space = document.getElementById("chart_space");
  //console.log(chart_space);
  for(let key of IO.devices.keys())
  {
    let device = IO.devices.get(key);
    for(let sys of device.members)
    {
      for(let val of sys.nestedIOValues)
      {
        val.chart.canvas.parentNode.style.width=new_width;
        //console.log(val.chart.canvas.parentNode);
      }
    }
  }
}

window.onload=function(){
  console.log("Window loaded.");
  $("#x_reset_button").on("click",resetX);
  $("#TreeToggle").on("click",toggleTree);

  jstree_shown=false;
  toggleTree();
  //$("#jstree_collapse").on('show.bs.collapse', collapse_change_started);
  //$("#jstree_collapse").on('shown.bs.collapse', collapse_change_ended);
  //$("#jstree_collapse").on('hide.bs.collapse', collapse_change_started);
  //$("#jstree_collapse").on('hidden.bs.collapse', collapse_change_ended);
};
