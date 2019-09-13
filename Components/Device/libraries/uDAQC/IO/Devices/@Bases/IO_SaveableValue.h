#ifndef IO_SaveableValue_h
#define IO_SaveableValue_h

#include <Arduino.h>
#include <FS.h>
//#include "../../../Network/Network.h"

#define DIR R"(/d/)"

namespace UDAQC{ namespace IO
{
  class IO_Saveable
  {
  public:
    IO_Saveable()
    {
      this->saveable_index=UDAQC::IO::IO_System::Current()->add_saveable(this); //assigned on instantiation, guaranteed to be unique
    }
    ~IO_Saveable()
    {}

    virtual void ReadFromFile()=0;
    virtual String file_name()=0;
    virtual void Set(CommandCodec::TCP_Command c)=0;

    static void HandleModificationCommand(CommandCodec::TCP_Command c)
    {
      DEBUG_println("Handling modification command.");
      int16_t index;
      if((unsigned int)c.message_length<sizeof(index))
      {
        DEBUG_println("Poorly formed modification command(0)");
        return;
      }
      memcpy(&index,c.message,sizeof(index));
      IO_Saveable* saveable = IO_System::saveable_member(index);
      if(saveable)
      {
        DEBUG_println("Setting saveable of index " + (String)index);
        saveable->Set(c);
      }
      else
      {
        DEBUG_println("Nonexistant saveable of index " + (String)index);
      }
      DEBUG_println("Modification command handling complete.");
    }
  protected:
    bool request_handler_set=false;
    int16_t saveable_index=-1; //assigned on instantiation, guaranteed to be unique
  };

  template<typename T>
  class IO_SaveableValue:public IO_Value<T>, public IO_Saveable
  /*
  This is an IO_Value that should be saved in SPIFFS when it is changed.
  Primarily, this should be for values that need to persist through a reset but don't change very often.
  This should mainly be for values the user sets.
  */
  {
  public:
    IO_SaveableValue(String name, String units, IO_Group* collection):
    IO_Value<T>(name,units,collection),
    IO_Saveable()
    {
    }

    IO_SaveableValue(String name, IO_Group* collection):
    IO_Value<T>(name,collection),
    IO_Saveable()
    {
    }

    ~IO_SaveableValue()
    {
    }

    String Report();
    virtual unsigned int SendDescription(WiFiClient* client);
    virtual int32_t DataSize(){return IO_Value<T>::DataSize();}
    void ReadFromFile();
    virtual void Set(CommandCodec::TCP_Command c);
    void Set(T new_val);
    void Set_NoSave(T new_val);

  protected:
    virtual const int16_t* DescriptionCommand(){return &(UDAQC::IO::NetworkCommands::modifiablevalue_description);}
    void callback();
    String form_name(){return IO_Value<T>::SafeFullName() + "_frm";}
    String input_name(){return IO_Value<T>::SafeFullName() + "_in";}
    String button_name(){return IO_Value<T>::SafeFullName() + "_but";}
    String file_name()
    {
      //this should be max 32 characters for SPIFFS compatibility
      String retval;
      retval = IO_Value<T>::SafeFullName();
      int to_remove = 4;
      int length_of_prefix = ((String)saveable_index).length() + 1;
      while(retval.length() + sizeof(DIR) + length_of_prefix>=32)
      {
        retval.remove(to_remove,1);
        to_remove+=4;
        while(to_remove>=retval.length()-1)
        {
          to_remove-=retval.length()-1;
        }
      }
      retval = DIR + (String)saveable_index + '/' + retval; //saveable index goes in the directory, it's guaranteed to be unique for eache saveable value.
      return retval;
    }
  };

  template<typename T>
  unsigned int IO_SaveableValue<T>::SendDescription(WiFiClient* client)
  {
    //For saveable values, send the index of this saveable value for identification on return communication at the end of the description
    unsigned int retval=0;
    retval+=IO_Value<T>::SendDescription(client);
    retval+=client->write((uint8_t*)(&saveable_index),sizeof(saveable_index));
    return retval;
  }

  template<typename T>
  void IO_SaveableValue<T>::ReadFromFile()
  {
    String fn = file_name();
    DEBUG_println("Attempting load for filename " + fn);
    if(SPIFFS.exists(fn))
    {
      DEBUG_println("File found. Loading.");
      File f=SPIFFS.open(fn,"r");
      if(f)
      {
        DEBUG_println("File opened. Reading.");
        T old_value;
        f.read(reinterpret_cast<uint8_t*>(&old_value),IO_Value<T>::DataSize());
        DEBUG_println("File read. Setting to " + String(old_value));
        IO_Value<T>::Set(old_value);
        DEBUG_println("Set.");
      }
      DEBUG_println("Loaded value.");
      f.close();
    }
    else
    {
      DEBUG_println("File not found.");
    }
  }

  template<typename T>
  void IO_SaveableValue<T>::Set(CommandCodec::TCP_Command c)
  {
    if((unsigned int)c.message_length==(sizeof(saveable_index)+this->DataSize()))
    {
      uint8_t* new_val_p = c.message + sizeof(saveable_index);
      T new_val;
      memcpy(&new_val,new_val_p,sizeof(T));
      Set(new_val);
    }
    else
    {
      DEBUG_println("Poorly formed modification command(1)");
    }
  }

  template<typename T>
  void IO_SaveableValue<T>::Set(T new_val)
  {
    String fn = file_name();
    DEBUG_println("Attempting save for filename " + fn);
    IO_Value<T>::Set(new_val);
    File f=SPIFFS.open(fn,"w");
    if(f)
    {
      DEBUG_println("Writing.");
      T new_value = IO_Value<T>::Get();
      f.write(reinterpret_cast<uint8_t*>(&new_value),IO_Value<T>::DataSize());
      DEBUG_println("Saved.");
    }
    else
    {
      DEBUG_println("Couldn't open file.");
    }
    f.close();
  }

  template<typename T>
  void IO_SaveableValue<T>::Set_NoSave(T new_val)
  {
    IO_Value<T>::Set(new_val);
  }

  template<>
  inline void IO_SaveableValue<float>::callback()
  {
    if(!UDAQC::Network::session_authenticated())
    {
      return;
    }
    float new_value = UDAQC::Network::webserver.arg(input_name()).toFloat();
    Set(new_value);
    UDAQC::IO::IO_System::Current()->DirectToReportPage();
  }

  template<>
  inline void IO_SaveableValue<int>::callback()
  {
    if(!UDAQC::Network::session_authenticated())
    {
      return;
    }
    float new_value = UDAQC::Network::webserver.arg(input_name()).toInt();
    Set(new_value);
    UDAQC::IO::IO_System::Current()->DirectToReportPage();
  }

  template<typename T>
  void IO_SaveableValue<T>::callback()
	{
    if(!UDAQC::Network::session_authenticated())
    {
      return;
    }
    T new_value = (T)UDAQC::Network::webserver.arg(input_name());
	  Set(new_value);

    UDAQC::IO::IO_System::Current()->DirectToReportPage();
	}

  template<typename T>
  String IO_SaveableValue<T>::Report()
  {
    String retval;

    if(!request_handler_set)
    {
      UDAQC::Network::webserver.on("/" + form_name(),std::bind(&IO_SaveableValue::callback,this));
      request_handler_set=true;
    }

    retval+=
    R"(<form action=")"
    +form_name()+
    R"(" method="post">)"
    +IO_Value<T>::Report()+
    R"(   <input type="text" name=")"
    +input_name()+
    R"("> <button type="submit" name=")"
    +button_name()+
    R"(">Change Value</button></form>)"
		;

    return retval;
  }
}
};
#endif
