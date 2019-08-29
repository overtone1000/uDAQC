#include "IO_System.h"

namespace ESP_Managers{ namespace IO
{
  //WiFiServer IO_System::tcp_server(ESP_Managers::IO::Constants::tcp_main_port);
  std::vector<IO_Saveable*> IO_System::saveable_members;

  IO_Group IO_System::device("Unnamed Device 1",nullptr);

  WiFiUDP IO_System::udp;
  Repeater IO_System::udp_timer(2*60*1000);
  //std::vector<IO_System*> IO_System::systems;
  IO_System* IO_System::current_system;
  std::list<CommandCodec::TCP_Command_Client> IO_System::tcp_clients;

  IO_System::IO_System(String name):
  IO_Group(name,nullptr),
  ts("Timestamp",this)
  {
    this->system_index = device.MemberCount();
    device.add_node(this);
  }

  IO_System::~IO_System()
  {
    //if(ts)
    //{
    //  delete ts;
    //}
  }


  int IO_System::add_saveable(IO_Saveable* new_member)
  {
    int retval = saveable_members.size();
    saveable_members.push_back(new_member);
    return retval;
  }

  void IO_System::InitializeSaveables()
  {
    //Resize();

    //Load saveable members from SPIFFS.
    DEBUG_println("Initializing IO objects.");
    Dir dir = SPIFFS.openDir("/devices");
    while (dir.next()) {
      DEBUG_println("Found file " + dir.fileName());
      bool deleteme=true;
      for(auto it=saveable_members.begin();it!=saveable_members.end();it++)
      {
        DEBUG_println((*it)->file_name());
        DEBUG_println(dir.fileName());
        if((*it)->file_name()==dir.fileName())
        {
          deleteme=false;
          break;
        }
      }
      if(deleteme)
      {
        SPIFFS.remove(dir.fileName());
        DEBUG_println("Deleting file " + dir.fileName());
      }
    }

    for(auto it=saveable_members.begin();it!=saveable_members.end();it++)
    {
      DEBUG_println("Initializing " + (*it)->file_name());
      (*it)->ReadFromFile();
    }
  }

  void IO_System::InitializeTCPClient()
  {
    //tcp_server.begin();
  }

  void IO_System::add_center(IPAddress host, int center_port)
  {
    DEBUG_println("Adding uDAQC Center.");

    DEBUG_println("Checking whether client " + host.toString() + ":" + (String)center_port + "exists.");
    for(auto it = tcp_clients.begin();it!=tcp_clients.end();it++)
    {
      DEBUG_println("Comparing to client " + it->remoteIP().toString() + ":" + (String)(it->remotePort()));
      if(it->remoteIP()==host && it->remotePort() == center_port)
      {
        DEBUG_println("Already exists.");
        return;
      }
    }

    CommandCodec::TCP_Command_Client new_client; //apparently this method of creation is okay although this is within the scope of this function...
    new_client.connect(host,center_port);

    //new_client.println("Description sent.");
    tcp_clients.push_back(new_client);

    //Request authentication
    ESP_Managers::FileSystem::Credentials creds = ESP_Managers::FileSystem::read_credentials();
    CommandCodec::TCP_Command_Header auth_request_header;
    auth_request_header.message_length = creds.login.length() + Network::realm.length() + sizeof(int16_t)*2;
    auth_request_header.command_id = ESP_Managers::IO::NetworkCommands::auth_request;
    new_client.send_command_header(auth_request_header);
    SendString(&new_client, &(creds.login));
    SendString(&new_client, &(Network::realm));
    new_client.flush();
  }

  void IO_System::LoopTCPClient()
  {
    //DEBUG_println("Starting tcp loop.");

    /*
    if(tcp_server.hasClient())
    {
      DEBUG_println("Adding tcp client.");
      CommandCodec::TCP_Command_Client new_client(tcp_server.available());

      CommandCodec::TCP_Command_Header header;

      header.message_length = DescriptionSize();
      header.command_id = NetworkCommands::group_description;

      unsigned int header_size = new_client.send_command_header(header);
      DEBUG_println("Actual header size sent " + (String)header_size);
      unsigned int description_length_sent=SendDescription(&(new_client));
      DEBUG_println("Sending description of size " + (String)header.message_length);
      DEBUG_println("Actual description  size " + (String)description_length_sent);

      //new_client.println("Description sent.");
      tcp_clients.push_back(new_client);
    }
    */

    for(auto it=tcp_clients.begin();it!=tcp_clients.end();it++)
    {
      CommandCodec::TCP_Command_Client* client = &*it;
      if(client->connected())
      {
        while(client->command_available())
        {
          CommandCodec::TCP_Command new_command = client->get_command();
          DEBUG_print("Received command " + (String)new_command.command_id);
          DEBUG_println(" (" + (String)new_command.message_length + " b)");

          if(client->Authenticated())
          {
            //Now need to do something with the command
            //Pretty much just need to handle changes to IO_SaveableValue
            switch(new_command.command_id)
            {
              case ESP_Managers::IO::NetworkCommands::handshake:
              DEBUG_println("Handshake.");
              break;
              case ESP_Managers::IO::NetworkCommands::modifiablevalue_modification:
              DEBUG_println("Received modification command.");
              IO_Saveable::HandleModificationCommand(new_command);
              break;
              default:
              DEBUG_println("Unhandled command " + (String)new_command.command_id);
              break;
            }
          }
          else
          {
            if(new_command.command_id==ESP_Managers::IO::NetworkCommands::auth_provision)
            {
              DEBUG_println("Authentication provision command received.");

              char* auth = new char[new_command.message_length];
              memcpy(auth,new_command.message,new_command.message_length);
              String auth_str = String(auth);
              DEBUG_println("Authentication received is: " + auth_str);

              String correct_hash = Network::getMD5Hash();
              if(correct_hash.equals(auth_str))
              {
                client->Authenticate();
                DEBUG_println("Sending device description.");

                CommandCodec::TCP_Command_Header header;
                header.command_id = NetworkCommands::group_description;
                header.message_length = device.DescriptionSize();
                client->send_command_header(header);
                device.SendDescription(client);
              }
              else
              {
                DEBUG_println("Incorrect auth.");
                client->stop();
              }

              delete[] auth;
            }
          }
        }
      }
      else
      {
        DEBUG_println("Erasing client not connected.");
        tcp_clients.erase(it--); //Erase the item and move iterator back to the previous element or iteration loop throaws an exception.
        DEBUG_println("Client erased.");
      }
    }
    yield();
  }

  void IO_System::SetTimeToNow()
  {
      ts.SetTimeToNow();
  }

  IO_System* IO_System::Current()
  {
    return IO_System::current_system;
  }

  void IO_System::SetCurrent(IO_System* new_current)
  {
    current_system=new_current;
  }

  std::vector<IO_System*> IO_System::Systems()
  {
    const std::vector<IO_Node*> raw_systems = device.Members();
    std::vector<IO_System*> cast_systems;
    for(auto it=raw_systems.begin();it!=raw_systems.end();it++)
    {
      cast_systems.push_back((IO_System*)(*it));
    }
    return cast_systems;
  }

  //unsigned int IO_System::SendDescription(WiFiClient* client)
  //{
  //
  //  unsigned int retval=0;
  //  retval+=IO_Group::SendDescription(client);
  //  //retval+=client->write((uint8_t*)&system_index,sizeof(system_index));
  //  return retval;
  //}

  void IO_System::SendDataReportTCP()
  {
    for(auto it=tcp_clients.begin();it!=tcp_clients.end();it++)
    {
      CommandCodec::TCP_Command_Client* client = &*it;
      if(client->connected() && client->Authenticated())
      {
        client->flush(); //Don't start sending new data until old data send is finished.

        //Send the header
        CommandCodec::TCP_Command_Header header;
        header.message_length = DataSize()+sizeof(system_index);
        header.command_id = NetworkCommands::data;
        client->send_command_header(header);

        //Send the data
        client->write((uint8_t*)&system_index,sizeof(system_index));
        SendData(client);

        //For debugging for now.
        //DEBUG_println("Stopping client.");
        //client->stop();
        //DEBUG_println("Client stopped.");
      }
    }
    yield();
  }
/*
  void IO_System::Resize()
  {
    current_size=0;
    for(auto it=members.begin();it!=members.end();it++)
    {
      current_size+=(**it).DataSize();
    }

    if(data)
    {
      delete[] data;
    }
    DEBUG_println("Total size is " + (String)current_size);
    data = new uint8_t[current_size];

    memset(data,0,current_size); //Zero the whole buffer

    IO_Group::Set_p(data); //assign pointers to children
  }
*/

  void IO_System::ShowReportPage()
  {
    //DEBUG_println("Building page.");
    String page;
	  page = HTML_Builder::html_header;

    page += R"(<h2>IO</h2>)";

    page += HTML_Builder::create_hyperlink("/","Back") + HTML_Builder::breakline  + HTML_Builder::breakline;

    page +="     " + (String)(((float)(ESP.getFreeHeap()))) + " bytes used by the heap" + HTML_Builder::breakline;

    page += String(members.size()) + R"( reporters.)" + HTML_Builder::breakline;
    if(ESP_Managers::IO::IO_Node::errcnt>0)
    {
      page += String(ESP_Managers::IO::IO_Node::errcnt) + R"( unsuccessful device addition attempts.)" + HTML_Builder::breakline;
    }

    for(auto it=members.begin();it!=members.end();it++)
    {
      page+=R"(<h3>)";
      page+=(**it).Name();
      page+=R"(</h3>)";
      page+=(**it).Report(); //The memory leak goes away when this line is commented out. Fixed in IO_SaveableValue by not adding the handler repeatedly.
      page+=HTML_Builder::breakline;
    }

    page += HTML_Builder::html_footer;
    //DEBUG_println("Sending page.");

    ESP_Managers::Network::webserver.send(200, "text/html", page);
    //DEBUG_println("Page sent.");
  }

  void IO_System::DirectToReportPage()
  {
    ESP_Managers::Network::webserver.sendHeader("Location",ESP_Managers::IO::IOpanel_path,true);
    ESP_Managers::Network::webserver.send ( 302, "text/plain", "");
  }

	void IO_System::InitializeUDP()
	{
    DEBUG_println("Starting UDP multicast listener.");
		udp.stop();
		//int retval = udp.beginMulticast(WiFi.localIP(), ESP_Managers::IO::Constants::udp_multicast_IP, ESP_Managers::IO::Constants::udp_multicast_port);

    //multicast doesn't work well (subscription expires), just use broadcasting
    //this is fine, as this isn't really the intended situation for multicasting use
    udp.begin(ESP_Managers::IO::Constants::udp_multicast_port);

    announceUDP();
	}

  void IO_System::announceUDP()
  {
    CommandCodec::TCP_Command_Header header;
    header.message_length = 0;
    header.command_id = IO::NetworkCommands::new_device_avaialable;

    udp.beginPacket(IO::Constants::udp_multicast_IP, IO::Constants::udp_multicast_port);
    udp.write((uint8_t*)&(header.message_length),sizeof(header.message_length));
    udp.write((uint8_t*)&(header.command_id),sizeof(header.command_id));
    udp.endPacket();

    DEBUG_println("UDP announcement sent.");
  }

  void IO_System::handleTimeSync(IPAddress center_address, uint32_t center_port, int64_t current_center_time)
  {
    CommandCodec::TCP_Command_Header header;
    header.message_length = sizeof(int64_t)*2;
    header.command_id = ESP_Managers::IO::NetworkCommands::timesync_response;

    udp.beginPacket(center_address, center_port);
    udp.write((uint8_t*)&(header.message_length),sizeof(header.message_length));
    udp.write((uint8_t*)&(header.command_id),sizeof(header.command_id));
    udp.write((uint8_t*)&(current_center_time),sizeof(current_center_time));
    int64_t mics = (int64_t)(micros64());
    udp.write((uint8_t*)&(mics),sizeof(mics));
    udp.endPacket();

    DEBUG_println("Timesync reponse sent.");
  }

	void IO_System::LoopUDP()
	{
		//max packet size is 65,507 bytes
    //but this define is in wifiudp.h: #define UDP_TX_PACKET_MAX_SIZE 8192

    if(tcp_clients.empty() && udp_timer.repeatnow())
    {
      announceUDP();
      yield();
    }

    //Eventually, the device stops responding to multicast. This looks like a bug in the ESP8266 library. Here is a workaround.
		int packetSize = udp.parsePacket();
    yield();

    //Process a packet if received in full.
		if (packetSize>=CommandCodec::TCP_Command_Header::bytelength())
		{
			DEBUG_printf("UDP Packet received: %d bytes from %s, port %d,", packetSize, udp.remoteIP().toString().c_str(), udp.remotePort());

			CommandCodec::TCP_Command_Header header;
			udp.read((uint8_t*)&(header.message_length),sizeof(header.message_length));
			udp.read((uint8_t*)&(header.command_id),sizeof(header.command_id));

      DEBUG_println("Command id " + (String)header.command_id);

      if(header.message_length>0)
      {
        switch(header.command_id)
        {
          case ESP_Managers::IO::NetworkCommands::request_subscription:
    			{
            int32_t center_port;
            udp.read((uint8_t*)(&center_port),sizeof(center_port));
            add_center(udp.remoteIP(), center_port);
            yield();
    			}
          break;
          case ESP_Managers::IO::NetworkCommands::timesync_request:
          {
            //DEBUG_println("Received time sync request.");
            int64_t current_center_time;
            udp.read((uint8_t*)(&current_center_time),sizeof(current_center_time));
            handleTimeSync(udp.remoteIP(), udp.remotePort(),current_center_time);
            yield();
          }
          break;
          default:
          DEBUG_println("Unrecognized UDP command " + header.command_id);
          break;
        }
      }
		}
	}
}
};
