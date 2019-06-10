"use strict";
let ctx = document.getElementById("test_chart").getContext("2d");
let chart = new Chart(ctx, {
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
        position: "right"
      },
      scales:
      {
        xAxes: [{
          type: "time"
        }]
      }
    }
});

function createChart(canvas)
{
  return new Chart(canvas.getContext("2d"),
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
          position: "right"
        },
        scales:
        {
          xAxes: [{
            type: "time"
          }]
        }
      }
  });
}
