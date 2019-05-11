#ifndef TRM_Debug_h
#define TRM_Debug_h

#define DEBUG_TRM

#ifdef DEBUG_TRM
 #define DEBUG_println(x)  Serial.println(x)
 #define DEBUG_printf(...) Serial.printf(__VA_ARGS__)
 #define DEBUG_print(x)  Serial.print(x)
#else
 #define DEBUG_println(x)
 #define DEBUG_printf(...)
 #define DEBUG_print(x)
#endif

#endif
