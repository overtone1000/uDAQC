#ifndef IO_Value_h
#define IO_Value_h

#include <Arduino.h>

#include "IO_Node.h"

namespace ESP_Managers{ namespace IO
{
  class IO_Group;

  template<typename T>
  class IO_Value:public IO_Node
  /*
  A class based on IO_Node containing a value of type T.
  A prior iteration only contained a pointer to a T value, with memory being instantiated somewhere elapsed (IO_System)
  But this was likely not an improvement in efficiency and was more complicated. Reverted to simply having a T in the class
  */
  {
  private:
    T value;
    String units_str="";

  public:
    IO_Value<T>(String name, IO_Group* collection) : IO_Node(name, collection)
    {}

    IO_Value<T>(String name, String units, IO_Group* collection) : IO_Node(name, collection)
    {units_str=units;}

    virtual T Get(){
      //T retval;
      //memcpy(&retval,value,DataSize());
      //return retval;
      return value;
      }

    virtual void Set(T new_val){
      //memcpy(value,&new_val,DataSize());
      value=new_val;
    }

    virtual String Report()
    {
      //DEBUG_println("IO_Value report function called.");
      return Name() + ": " + String(Get()) + " " + units_str;
    }

    virtual int32_t DataSize(){return (int32_t)(sizeof(T));} //IO_Values need to be constant in size to work with IO_System

    virtual unsigned int SendDescription(WiFiClient* client)
    {
      /*
      Structure:
      Standard Node description header

      int containing the length of the units
      String units

      int16_t containing the type integer
      */

      unsigned int retval=0;

      const int16_t* type_int = GetTypeInt();

      retval+=IO_Node::SendDescription(client);

      retval+=SendString(client,&units_str);

      retval+=client->write((uint8_t*)type_int,sizeof(int16_t));

      return retval;
    }

    virtual unsigned int SendData(WiFiClient* client)
    {
      unsigned int retval=0;
      retval+=client->write((uint8_t*)&value,sizeof(T));
      return retval;
    }

  protected:
    virtual const int16_t* DescriptionCommand(){return &(NetworkCommands::value_description);}

  private:
    const int16_t* GetTypeInt(){return &(DataTypes::undefined);}
  };

  template<>
  inline int32_t IO_Value<String>::DataSize()
  {
    return Get().length();
  }

  //template<>
  //inline String IO_Value<float>::Report()
  //{
  //  //DEBUG_println("IO_Value float report function called.");
  //  return Name() + ": " + String(Get()) + " " + units_str;
  //}

  template<>
  inline String IO_Value<long long>::Report()
  {
    //DEBUG_println("IO_Value float report function called.");
    long long val = Get();
    return Name() + ": " + String((long)(val>>32)) + String((long)(val<<32>>32)) + " " + units_str;
  }

  template<>
  inline const int16_t* IO_Value<float>::GetTypeInt()
  {
    return &(DataTypes::floating_point);
  }

  template<>
  inline const int16_t* IO_Value<double>::GetTypeInt()
  {
    return &(DataTypes::floating_point);
  }

  template<>
  inline const int16_t* IO_Value<int>::GetTypeInt()
  {
    return &(DataTypes::signed_integer);
  }

  template<>
  inline const int16_t* IO_Value<long>::GetTypeInt()
  {
    return &(DataTypes::signed_integer);
  }

  template<>
  inline const int16_t* IO_Value<long long>::GetTypeInt()
  {
    return &(DataTypes::signed_integer);
  }

  template<>
  inline const int16_t* IO_Value<bool>::GetTypeInt()
  {
    return &(DataTypes::boolean);
  }
}}
;
#endif
