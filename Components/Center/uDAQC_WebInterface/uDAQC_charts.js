"use strict";

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

window.onload=function(){
  console.log("Window loaded.");
  $("#x_reset_button").on("click",resetX);
};
