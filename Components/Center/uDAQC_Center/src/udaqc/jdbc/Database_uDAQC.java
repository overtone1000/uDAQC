package udaqc.jdbc;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.Iterator;
import java.util.Properties;
import java.util.Vector;

import org.joda.time.DateTime;

import udaqc.io.IO_Value;
import udaqc.io.IO_System;
import udaqc.io.IO_Device;

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
		return value.Name() + " " + type + " NOT NULL";
	}
	
	static final String timecolumn_name = "time";
	static final String timecolumn = timecolumn_name + " TIMESTAMPTZ PRIMARY KEY";
		
	static final String devicetablename = "Devices";
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
				command += "description bytea PRIMARY KEY,";
				command += ");";
				s.executeUpdate(command);
				s.close();
			}
		} catch (SQLException e)
		{
			e.printStackTrace();
		}
	}
	
	private String getSystemTableName(IO_System system)
	{
		//ESP.getChipId() is always appended to device name, so this is guaranteed to be unique
		return system.Device().Name() + "_" + system.Index().toString();
	}
	
	public Vector<IO_Device> loadDevices()
	{
		Vector<IO_Device> retval = new Vector<IO_Device>();
		
		return retval;
	}
	public void processDeviceConnection(IO_Device device)
	{
		
	}
	
	private void initSystemTable(IO_System system)
	{
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
			e.printStackTrace();
		}
	}
	public void insertSystemTable(IO_System system)
	{
		String command;
		String system_table_name = getSystemTableName(system);
		try
		{
			Timestamp ts = new Timestamp(system.GetTimestamp().getMillis());
			
			Iterator<IO_Value> i = system.GetNestedValues().iterator();
			
			String vals = "(?,";
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
			
			command = "insert into " + system_table_name + " values " + vals;
			PreparedStatement ps = conn.prepareStatement(command);
			ps.setTimestamp(1, ts);
			ps.execute();
			ps.close();
		} catch (SQLException e)
		{
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
