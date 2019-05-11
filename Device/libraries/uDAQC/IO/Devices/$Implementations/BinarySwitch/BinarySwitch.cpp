#include "BinarySwitch.h"

namespace ESP_Managers{ namespace IO
{
  String BinarySwitch::Report()
  {
    //Serial.println("Binary Switch report function called.");
    String retval;
    if(Get()){retval = "1";}
    else{retval = "0";}
    retval += HTML_Builder::breakline;
    return retval;
  }
}
}
;
