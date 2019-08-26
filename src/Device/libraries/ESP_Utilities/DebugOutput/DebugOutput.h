#ifndef DebugOutput_h
#define DebugOutput_h

#ifdef DEBUG_UDAQC
 #warning uDAQC Library Debugging is enabled
 #define DEBUG_println(x)  Serial.println(x)
 #define DEBUG_printf(...) Serial.printf(__VA_ARGS__)
 #define DEBUG_print(x)  Serial.print(x)
#else
 #define DEBUG_println(x)
 #define DEBUG_printf(...)
 #define DEBUG_print(x)
#endif

#endif
