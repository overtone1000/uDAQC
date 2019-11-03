package udaqc.jdbc;

import java.nio.ByteBuffer;
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
	
	public Database_uDAQC()
	{
		try
		{
			conn = DriverManager.getConnection(url, props);
			initDeviceTable();
		} catch (SQLException e)
		{
			e.printStackTrace();
		}
	}
	
	private String IOValueToColumn(udaqc.io.IO_Value value)
	{
		String type;
		switch(value.IO_Type())
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
		return "\"" + value.Name() + "\"" + " " + type + " NOT NULL";
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
	
	static final String flagcolumn_name = "end_of_epoch";
	static final String flagcolumn = flagcolumn_name + " boolean NOT NULL";
	static final String timecolumn_name = "time";
	static final String timecolumn = timecolumn_name + " TIMESTAMPTZ PRIMARY KEY";
		
	static final String devicetablename = "devices";
	private void initDeviceTable()
	{
		Statement s;
		String command;
		DatabaseMetaData meta;
		try
		{
			meta = conn.getMetaData();
			ResultSet meta_res = meta.getTables(null, null, devicetablename,new String[] {"TABLE"});
			System.out.println("Checking for tables with name " + devicetablename);
			if(meta_res.next()) 
			{
				System.out.println("Found table with name " + devicetablename);
			}
			else
			{
				System.out.println("Table " + devicetablename + " doesn't exist, creating.");
				
				s = conn.createStatement();
				command = "create table " + devicetablename + "( ";
				command += "description bytea PRIMARY KEY";
				command += ");";
				s.executeUpdate(command);
				s.close();
			}
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
			command = "insert into " + devicetablename + " values (?);";
			ps = conn.prepareStatement(command);
			ps.setBytes(1, device.Description());
			ps.executeUpdate();
			ps.close();
			
			System.out.println("Inserted device with the following command:");
			System.out.println(ps.toString());
		} catch (SQLException e)
		{
			e.printStackTrace();
		}
		
		for(IO_System sys:device.Systems())
		{
			initSystemTable(sys);
		}
	}
	private String getSystemTableName(IO_System system)
	{
		//ESP.getChipId() is always appended to device name, so this is guaranteed to be unique
		String retval = system.Device().Name() + "_" + system.Index().toString();
		retval = "\"" + retval + "\"";
		return retval;
	}
	
	public void loadDevices()
	{
		System.out.println("Loading stored devices.");
		Statement s;
		try
		{
			s = conn.createStatement();
			String command = "select * from " + devicetablename + ";";
			s.execute(command);
			ResultSet res = s.getResultSet();
			while(res.next())
			{
				ByteBuffer bb = ByteBuffer.wrap(res.getBytes(1));
				IO_Device_Connected dev = IO_Device_Connected.getDirectDevice(bb);
				
				System.out.println("Device " + dev.FullName() + " loaded from database.");
				for(IO_System sys:dev.Systems())
				{
					System.out.println("Initializing system table for " + sys.FullName());
					initSystemTable(sys);
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
		String system_table_name = getSystemTableName(system);
		try
		{
			command = "select " + timecolumn_name + " from " + system_table_name + " order by " + timecolumn_name + "desc limit 1;";  
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
			
			command = "update " + system_table_name + " set '" + flagcolumn_name + "' = true";
			command += "where " + timecolumn_name + " = ?;";
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
		String new_table_name = getSystemTableName(system);
		try
		{
			meta = conn.getMetaData();
			ResultSet meta_res = meta.getTables(null, null, new_table_name,new String[] {"TABLE"});
			System.out.println("Checking for tables with name " + new_table_name);
			if(meta_res.next()) 
			{
				System.out.println("Found table with name " + new_table_name);
			}
			else
			{
				System.out.println("Table " + new_table_name + " doesn't exist, creating.");
				
				
				s = conn.createStatement();
				command = "create table " + new_table_name + "( ";
				command += flagcolumn + ",";
				command += timecolumn + ",";
				
				Iterator<IO_Value> i = system.GetNestedValues().iterator();
				while(i.hasNext())
				{
					command += IOValueToColumn(i.next());
					if(i.hasNext()) {command += ",";}
				}
				
				command += ");";
				s.executeUpdate(command);
				s.close();
				
				s = conn.createStatement();
				command = "select create_hypertable('" + new_table_name + "', '" + timecolumn_name + "');";
				s.execute(command);
				s.close();
			}
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
		String system_table_name = getSystemTableName(system);
		try
		{
			Timestamp ts = Timestamp.from(Instant.ofEpochMilli(d.GetTimestamp(system.Index()).getMillis()));
			
			Iterator<IO_Value> i = system.GetNestedValues().iterator();
			
			
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
			
			command = "insert into " + system_table_name + " values " + vals + ";";
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
}
