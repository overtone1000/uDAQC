package udaqc.jdbc;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Iterator;
import java.util.Properties;

import threading.ThreadWorker;
import udaqc.io.IO_Value;
import udaqc.io.IO_System;
import udaqc.io.IO_Device;
import udaqc.network.IO_Device_Synchronized;
import udaqc.network.center.IO_Device_Connected;

public class Database_uDAQC
{
	
	private static final String url = "jdbc:postgresql://localhost/udaqc_database";
	private static Properties props = new Properties();
	{
		props.setProperty("user","udaqc");
		props.setProperty("password","udaqc");
		props.setProperty("ssl","false");
	}
	
	Connection conn=null;
		
	private BackgroundManager background = new BackgroundManager();
	public Database_uDAQC()
	{
		try
		{
			conn = DriverManager.getConnection(url, props);
			initDeviceTable();
			background.start();
		} catch (SQLException e)
		{
			e.printStackTrace();
		}
	}
	
	private String IOValueToColumn(udaqc.io.IO_Value value)
	{
		String type;
		switch(value.getDataType())
		{
		case udaqc.io.IO_Constants.DataTypes.bool:
			type="bool";
			break;
		case udaqc.io.IO_Constants.DataTypes.floating_point:
			switch(value.Size())
			{
			case 2:type="float(24)";
				break;
			case 4:type="float(24)";
				break;
			case 8:type="float(53)";
				break;
			default:type="float(53)";
			}
			break;
		case udaqc.io.IO_Constants.DataTypes.signed_integer:
		case udaqc.io.IO_Constants.DataTypes.unsigned_integer:
			switch(value.Size())
			{
			case 2:type="SMALLINT";
				break;
			case 4:type="INT";
				break;
			case 8:type="BIGINT";
				break;
			default:type="INT";
			}
			break;
		default:
			type="INT";
			break;
		}
		String retval = "\"" + value.Name() + "\"" + " " + type + " NOT NULL"; 
		System.out.println(retval);
		return retval;
	}
	
	/*
	private static class ByteFlag
	{
		public static final int length = 1;
		public static enum EntryFlags
		{
			NewEpoch,
			SplitEpoch;
		}
		private byte flags;
		private static byte FlagByte(EntryFlags flag)
		{
			return (byte)(((byte)1)<<flag.ordinal());
		}
		public boolean Get(EntryFlags flag)
		{
			boolean retval = (flags&FlagByte(flag))!=0;
			return retval;
		}
		public void Set(EntryFlags flag, boolean value)
		{
			if(value)
			{
				flags |= FlagByte(flag);
			}
			else
			{
				flags &= (~FlagByte(flag));
			}
			
		}
	}
	*/
	
	static final String systems_schema = "io_systems";
	
	static final String flagcolumn_name = "end_of_epoch";
	static final String flagcolumn = flagcolumn_name + " boolean NOT NULL";
	static final String timecolumn_name = "time";
	static final String timecolumn = timecolumn_name + " TIMESTAMPTZ PRIMARY KEY";
		
	static final String devicetablename = "devices";
	private void initDeviceTable()
	{
		Statement s;
		String command;
		
		try
		{
			//meta = conn.getMetaData();
			//ResultSet meta_res = meta.getTables(null, null, devicetablename,new String[] {"TABLE"});
			//System.out.println("Checking for tables with name " + devicetablename);
			//if(meta_res.next()) 
			//{
			//	System.out.println("Found table with name " + devicetablename);
			//}
			s = conn.createStatement();
			command = "create table if not exists " + devicetablename + " ( ";
			command += "description bytea PRIMARY KEY";
			command += ");";
			s.executeUpdate(command);
			s.close();
			
			s = conn.createStatement();
			command = "create schema if not exists " + systems_schema + ";";
			s.executeUpdate(command);
			s.close();
			
		} catch (SQLException e)
		{
			e.printStackTrace();
		}
	}
	public void insertDevice(IO_Device device)
	{
		System.out.println("Inserting device " + device.FullName() + " into database.");
		String command;
		PreparedStatement ps;
		try
		{
			command = "insert into " + devicetablename + " values (?) on conflict do nothing;";
			ps = conn.prepareStatement(command);
			ps.setBytes(1, device.Description());
			ps.executeUpdate();
			ps.close();
				
			/*
			Statement s = conn.createStatement();
			String command2 = "select * from " + devicetablename + ";";
			s.execute(command2);
			ResultSet res = s.getResultSet();
			res.next();
			byte[] bs = res.getBytes(1);
			
			System.out.println("Description of length " + device.Description().length);
			System.out.println("Returned description is of length " + bs.length);
			if(device.Description().length==bs.length) 
			{
				for(int n=0;n<bs.length;n++)
				{
					System.out.println(Integer.toHexString((int)device.Description()[n]) + ":" + Integer.toHexString((int)bs[n]));
				}
				
			}
			*/
		} catch (SQLException e)
		{
			e.printStackTrace();
		}
		
		for(IO_System sys:device.Systems())
		{
			initSystemTable(sys);
		}
	}
	private String getEscapedSystemTableName(IO_System system)
	{
		return "\"" + getSystemTableName(system) + "\"";
	}
	private String getSystemTableName(IO_System system)
	{
		//ESP.getChipId() is always appended to device name, so this is guaranteed to be unique
		return system.Device().Name() + "_" + system.Index().toString();
	}
	
	public void loadDevices()
	{
		System.out.println("Loading stored devices.");
		Statement s;
		try
		{
			System.out.println("Querying for devices.");
			s = conn.createStatement();
			String command = "select * from " + devicetablename + ";";
			s.execute(command);
			ResultSet res = s.getResultSet();
			while(res.next())
			{
				System.out.println("Wrapping bytes.");
				byte[] description = res.getBytes(1);
				ByteBuffer bb = ByteBuffer.wrap(description);
				bb.order(ByteOrder.LITTLE_ENDIAN);
				System.out.println("Creating device.");
				IO_Device_Connected dev = IO_Device_Connected.getDirectDevice(bb);
				System.out.println("Device " + dev.FullName() + " loaded from database.");
				for(IO_System sys:dev.Systems())
				{
					System.out.println("Initializing system table for " + sys.FullName());
					initSystemTable(sys);
					closeEpoch(sys);
				}
			}
		} catch (SQLException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}	
	}
		
	public void closeEpoch(IO_System system)
	{
		String command;
		String system_table_name = getEscapedSystemTableName(system);
		try
		{
			command = "select " + timecolumn_name + " from " + systems_schema + "." + system_table_name + " order by " + timecolumn_name + " desc limit 1;";  
			Statement s = conn.createStatement();
			s.execute(command);
			ResultSet res = s.getResultSet();
			Timestamp last = null;
			while(res.next())
			{
				last = res.getTimestamp(1, java.util.Calendar.getInstance(java.util.TimeZone.getDefault()));
			}
			if(last==null) {
				System.out.println("Table is empty. No epoch to close.");
				return;
			}
			
			command = "update " + systems_schema + "." + system_table_name + " set " + flagcolumn_name + " = true";
			command += " where " + timecolumn_name + " = ?;";
			PreparedStatement ps = conn.prepareStatement(command);
			ps.setTimestamp(1, last);
			ps.execute();
			ps.close();
		} catch (SQLException e)
		{
			e.printStackTrace();
		}
	}
	
	private void initSystemTable(IO_System system)
	{
		System.out.println("Initializing system table.");
		Statement s;
		String command;
		DatabaseMetaData meta;
		String new_table_name = getEscapedSystemTableName(system);
		String raw_table_name = getSystemTableName(system);
		try
		{
			meta = conn.getMetaData();
			ResultSet meta_res = meta.getTables(null, systems_schema, null, new String[] {"TABLE"});
			System.out.println("Checking for tables with name " + new_table_name);
			
			while(meta_res.next()) 
			{
				String this_name = meta_res.getString("TABLE_NAME");
				System.out.print("Found table with name " + this_name);
				System.out.println(", comparing to " + raw_table_name);
				if(this_name.equals(raw_table_name))
				{
					return;
				}
			}
			
			System.out.println("Table " + new_table_name + " doesn't exist, creating.");
			
			
			s = conn.createStatement();
			command = "create table if not exists " + systems_schema + "." + new_table_name + " ( ";
			command += flagcolumn + ",";
			command += timecolumn + ",";
			
			Iterator<IO_Value> i = system.GetNestedValues().iterator();
			i.next(); //ditch the IO_Value timestamp
			
			while(i.hasNext())
			{
				command += IOValueToColumn(i.next());
				if(i.hasNext()) {command += ",";}
			}
			
			command += ");";
			s.executeUpdate(command);
			s.close();
			
			s = conn.createStatement();
			command = "select create_hypertable('" + systems_schema + "." + new_table_name + "', '" + timecolumn_name + "');";
			s.execute(command);
			s.close();
		} catch (SQLException e)
		{
			System.err.println("Error during system table initialization.");
			e.printStackTrace();
		}
	}
	
	public void insertSystemTable(IO_Device_Synchronized d, short system_index)
	{
		String command;
		PreparedStatement ps=null;
		IO_System system = d.System(system_index);
		String system_table_name = getEscapedSystemTableName(system);
		try
		{
			Timestamp ts = Timestamp.from(Instant.ofEpochMilli(d.GetTimestamp(system.Index()).getMillis()));
			
			Iterator<IO_Value> i = system.GetNestedValues().iterator();
			i.next(); //ditch the IO_Value timestamp
			
			String vals = "(false,?,";
			while(i.hasNext())
			{
				IO_Value v = i.next();
				vals += v.Value().toString();
				
				if(i.hasNext())
				{
					vals += ",";
				}
				else
				{
					vals += ")";
				}
			}
			
			command = "insert into " + systems_schema + "." + system_table_name + " values " + vals + ";";
			ps = conn.prepareStatement(command);
			ps.setTimestamp(1, ts);
			ps.execute();
			ps.close();
		} catch (SQLException e)
		{
			System.err.println("Error during system table insertion.");
			if(ps!=null)
			{
				System.err.println("Command was " + ps.toString());
			}
			e.printStackTrace();
		}
	}
	public void printSystemTable(IO_System system)
	{
		String system_table_name = getSystemTableName(system);
		Statement s;
		try
		{
			s = conn.createStatement();
			String command = "select * from " + system_table_name + " order by " + timecolumn_name;
			s.execute(command);
			ResultSet res = s.getResultSet();
			while(res.next())
			{
				Timestamp time = res.getTimestamp(1, java.util.Calendar.getInstance(java.util.TimeZone.getDefault()));
				Double temperature = res.getDouble(2);
				System.out.println(time.toString() + ", " + temperature);
			}
		} catch (SQLException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	
	private void dropTable(String tablename)
	{
		Statement s;
		try
		{
			s = conn.createStatement();
			s.executeUpdate("drop table " + tablename + ";");
			s.close();
		} catch (SQLException e)
		{
			e.printStackTrace();
		}

	}

	
	private class BackgroundManager extends ThreadWorker
	{
		public BackgroundManager()
		{
			super("Database Manager");
		}
		private static final int hour = 1000*60*60;
		@Override
		public void run()
		{
			while(this.continueRunning())
			{
				System.out.println("Dropping old chunks.");
				Statement s;
				String command;
				DatabaseMetaData meta;
				try
				{
					meta = conn.getMetaData();
					ResultSet meta_res = meta.getTables(null, systems_schema, null, new String[] {"TABLE"});
					while(meta_res.next()) 
					{
						String this_name = meta_res.getString("TABLE_NAME");
						s = conn.createStatement();
						command = "SELECT drop_chunks(older_than => interval '3 seconds', schema_name => '" + systems_schema + "', table_name => '" + this_name + "');";
						System.out.println(command);
						s.execute(command);
						s.close();
					}			
					
				} catch (SQLException e)
				{
					System.err.println("Error during background operations.");
					e.printStackTrace();
				}
				

				try
				{
					Thread.sleep(hour);
				} catch (InterruptedException e)
				{
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
	}
}
