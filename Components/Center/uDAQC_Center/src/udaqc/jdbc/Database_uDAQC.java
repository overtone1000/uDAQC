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
import java.util.Vector;

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
			conn.setAutoCommit(true);
			initDeviceTable();
			DatabaseMetaData meta = conn.getMetaData();
			boolean supports_isolation = meta.supportsTransactionIsolationLevel(java.sql.Connection.TRANSACTION_SERIALIZABLE);
			if(supports_isolation)
			{
				conn.setTransactionIsolation(java.sql.Connection.TRANSACTION_SERIALIZABLE);
			}
			else
			{
				System.err.println("Transaction isolation is not supported by this database. This will result in allocation errors.");
			}
			background.start();
		} catch (SQLException e)
		{
			e.printStackTrace();
		}
	}
	
	private static String IOValueTypeToSQLType(udaqc.io.IO_Value value)
	{
		switch(value.getDataType())
		{
		case udaqc.io.IO_Constants.DataTypes.bool:
			return "bool";
		case udaqc.io.IO_Constants.DataTypes.floating_point:
			switch(value.Size())
			{
			case 2:return "float(24)";
			case 4:return "float(24)";
			case 8:return "float(53)";
			default:return "float(53)";
			}
			
		case udaqc.io.IO_Constants.DataTypes.signed_integer:
		case udaqc.io.IO_Constants.DataTypes.unsigned_integer:
			switch(value.Size())
			{
			case 2:return "SMALLINT";
			case 4:return "INT";
			case 8:return "BIGINT";
			default:return "INT";
			}
		default:
			return "INT";
		}
	}
	
	private static String IOValueToEscapedColumnName(udaqc.io.IO_Value value)
	{
		return "\"" + value.Name() + "\"";
	}
	private String IOValueToColumnDeclareLine(udaqc.io.IO_Value value)
	{
		String type = IOValueTypeToSQLType(value);
		String retval = IOValueToEscapedColumnName(value) + " " + type + " NOT NULL"; 
		return retval;
	}
	
	public static enum AggregateEntities
	{
		Average,
		Maximum,
		Minimum;
		private String columnName(udaqc.io.IO_Value value)
		{
			return "\"" + value.Name() + "_" + this.toString() + "\"";
		}
	}
	
	private String IOValueToAggregateLine(udaqc.io.IO_Value value)
	{
		String valcol = IOValueToEscapedColumnName(value);
		String retval = "";
		switch(value.getDataType())
		{
		case udaqc.io.IO_Constants.DataTypes.bool:
			System.err.println("Booleans not working for aggregate yet. Will probably require some advanced logic and a type change.");
			//Example possible function might start with "count(*) filter (where " + valcol + ")"
			//This would best be served by creating a function for it.
			break;
		case udaqc.io.IO_Constants.DataTypes.floating_point:
		case udaqc.io.IO_Constants.DataTypes.signed_integer:
		case udaqc.io.IO_Constants.DataTypes.unsigned_integer:
		default:
			retval = "avg(" + valcol + ") as " + AggregateEntities.Average.columnName(value) + ", ";
			retval += "max(" + valcol + ") as " + AggregateEntities.Maximum.columnName(value) + ", ";
			retval += "min(" + valcol + ") as " + AggregateEntities.Minimum.columnName(value);
			break;
		}
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
	
	public static enum Regime
	{
		raw,
		minute,
		hour,
		day;
		private String schema()
		{
			return "iodata" + this.toString();
		}
		private String retention()
		{
			switch(this)
			{
			case raw:
				return "2 months";
			case minute:
				return "4 months";
			case hour:
				return "6 months";
			case day:
				return "12 months";
			default:
				return "1 seconds";
			}
		}
		private String length()
		{
			switch(this)
			{
			case minute:
				return "1 minute";
			case hour:
				return "1 hour";
			case day:
				return "1 day";
			default:
				return "0 second";
			}
		}
	}
		
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
			
			for(Regime r:Regime.values())
			{
				s = conn.createStatement();
				command = "create schema if not exists " + r.schema() + ";";
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
	
	private String getFullSystemTableName(IO_System system, Regime r)
	{
		return r.schema() + "." + getEscapedSystemTableName(system);
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
			command = "select " + timecolumn_name + " from " + Regime.raw.schema() + "." + system_table_name + " order by " + timecolumn_name + " desc limit 1;";  
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
			
			command = "update " + Regime.raw.schema() + "." + system_table_name + " set " + flagcolumn_name + " = true";
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
	
	public void insert_test_data()
	{
		String system_table_name = "\"uDAQC Template Device(2418134)_0\"";
		String command = "insert into " + Regime.raw.schema() + "." + system_table_name + " values ";
		command += "(false,?,?,0,1,2,3);";
		
		System.out.println("Starting test data insertion.");
		int points = 100000;
		int milli_step = 10000;
		Instant start_time = Instant.now().minusMillis(milli_step*points);
		Timestamp ts;
		PreparedStatement ps;
		try
		{
		ps = conn.prepareStatement(command);
		for(int n=0;n<points;n++)
		{
			ts = Timestamp.from(start_time.plusMillis(milli_step*n));
			ps.setTimestamp(1, ts);
			ps.setFloat(2, (float)Math.random());
			ps.addBatch();
		}
		ps.executeBatch();
		} catch (SQLException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		System.out.println("Ending test data insertion");
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
			
			ResultSet meta_res = meta.getTables(null, Regime.raw.schema(), null, new String[] {"TABLE"});
			System.out.println("Checking for tables with name " + new_table_name);
			
			while(meta_res.next()) 
			{
				String this_name = meta_res.getString("TABLE_NAME");
				System.out.print("Found table with name " + this_name);
				System.out.println(", comparing to " + raw_table_name);
				if(this_name.equals(raw_table_name))
				{
					return;
					//dropTable(Regime.raw.schema() + "." + new_table_name);
				}
			}
			
			System.out.println("Table " + new_table_name + " doesn't exist, creating.");
			
			
			s = conn.createStatement();
			command = "create table if not exists " + Regime.raw.schema() + "." + new_table_name + " ( ";
			command += flagcolumn + ",";
			command += timecolumn + ",";
			
			Iterator<IO_Value> i = system.GetNestedValues().iterator();
			i.next(); //ditch the IO_Value timestamp
			
			while(i.hasNext())
			{
				command += IOValueToColumnDeclareLine(i.next());
				if(i.hasNext()) {command += ",";}
			}
			
			command += ");";
			s.executeUpdate(command);
			s.close();
			
			s = conn.createStatement();
			command = "select create_hypertable('" + Regime.raw.schema() + "." + new_table_name + "', '" + timecolumn_name + "');";
			s.execute(command);
			s.close();
			
			//Now create regime continuous aggregates
			String timebucketcolumn_name = timecolumn_name+"bucket";
			for(Regime r:Regime.values())
			{
				if(r!=Regime.raw)
				{
					command = "create view " + r.schema() + "." + new_table_name;
					command += " with (timescaledb.continuous) as select";
					command += " time_bucket('" + r.length() + "', " + timecolumn_name + ") as " + timebucketcolumn_name + ", ";
					
					i = system.GetNestedValues().iterator();
					i.next(); //ditch the IO_Value timestamp
					while(i.hasNext())
					{
						command += IOValueToAggregateLine(i.next());
						if(i.hasNext()) {command += ", ";}
					}
					command += " from " + Regime.raw.schema() + "." + new_table_name;
					command += " group by " + timebucketcolumn_name;
					
					System.out.println(command);
					
					/*
					CREATE VIEW device_summary
					WITH (timescaledb.continuous) --This flag is what makes the view continuous
					AS
					SELECT
					  time_bucket('1 hour', observation_time) as bucket, --time_bucket is required
					  device_id,
					  avg(metric) as metric_avg, --We can use any parallelizable aggregate
					  max(metric)-min(metric) as metric_spread --We can also use expressions on aggregates and constants
					FROM
					  device_readings
					GROUP BY bucket, device_id; --We have to group by the bucket column, but can also add other group-by columns
					*/
					
					s = conn.createStatement();
					s.execute(command);
					s.close();
				}
			}
		} catch (SQLException e)
		{
			System.err.println("Error during system table initialization.");
			e.printStackTrace();
		}
	}
		
	public int count(IO_System system, Regime r, Timestamp start, Timestamp end)
	{
		String full_table_name = getFullSystemTableName(system,r);
		PreparedStatement ps=null;
		
		try
		{
			String command = "select count(" + timecolumn_name + ") from " + full_table_name;
			command += " where " + timecolumn_name + " >= ?";
			command += " and " + timecolumn_name + " <= ?";
			ps = conn.prepareStatement(command);
			ps.setTimestamp(1, start);
			ps.setTimestamp(2, end);
			ps.execute();
			ResultSet res = ps.getResultSet();
			while(res.next())
			{
				int count = res.getInt(1);
				System.out.println(full_table_name + " has " + count + " rows between " + start.toString() + " and " + end.toString());
				return count;
			}
		} catch (SQLException e)
		{
			System.out.println("Error during counting.");
			if(ps!=null)
			{
				System.err.println("Command was " + ps.toString());
			}
			e.printStackTrace();
		}
		return -1;
	}
	public ByteBuffer getHistory(IO_System system, Regime r, Timestamp start, Timestamp end) 
	{
		String full_table_name = getFullSystemTableName(system,r);
		ByteBuffer retval=null;
		PreparedStatement ps=null;
		try
		{
			conn.setAutoCommit(false);
			
			int count = count(system,r,start,end);
			retval = ByteBuffer.allocate(count*system.HistoryEntrySize(r) + Short.BYTES*2 + Byte.BYTES);
			retval.putShort(system.Device().DeviceIndex());
			retval.putShort(system.Index());
			if(r==Regime.raw)
			{
				retval.put((byte)0);
			}
			else
			{
				retval.put((byte)1);
			}
			
			String command = "select * from " + full_table_name;
			command += " where " + timecolumn_name + " >= ?";
			command += " and " + timecolumn_name + " <= ?";
			command += " order by " + timecolumn_name;
			ps = conn.prepareStatement(command);
			ps.setTimestamp(1, start);
			ps.setTimestamp(2, end);
			ps.execute();
			ResultSet res = ps.getResultSet();
			Vector<IO_Value> values = system.GetNestedValues();
			while(res.next())
			{
				if(r==Regime.raw)
				{
					retval.put(res.getByte(1)); //end of epoch
					retval.putLong(res.getTimestamp(2).getTime()); //timestamp
					int i = 3;
					for(int n=1;n<values.size();n++) //Start at one to skip the timestamp
					{
						retval.put(res.getBytes(i));
						i++;
					}
				}
				else
				{
					retval.putLong(res.getTimestamp(1).getTime()); //timestamp
					int i = 2;
					for(int n=1;n<values.size();n++) //Start at one to skip the timestamp
					{
						for(int q=0;q<3;q++)
						{
							retval.put(res.getBytes(i));
							i++;
						}
					}
				}
			}
			ps.close();
			conn.commit();
		} catch (SQLException e)
		{
			System.out.println("Error during history retrieval.");
			if(ps!=null)
			{
				System.err.println("Command was " + ps.toString());
			}
			e.printStackTrace();
		}
		
		try
		{
			conn.setAutoCommit(true);
		} catch (SQLException e)
		{
			e.printStackTrace();
		}
		
		return retval;
	}
	
	public void insertSystemTable(IO_Device_Synchronized d, short system_index)
	{
		String command;
		PreparedStatement ps=null;
		IO_System system = d.System(system_index);
		String full_table_name = getFullSystemTableName(system, Regime.raw);
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
			
			command = "insert into " + full_table_name + " values " + vals + ";";
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
		String full_table_name = getFullSystemTableName(system, Regime.raw);
		Statement s;
		try
		{
			s = conn.createStatement();
			String command = "select * from " + full_table_name + " order by " + timecolumn_name;
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
			s.executeUpdate("drop table " + tablename + " cascade;");
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
					//for(Regime r:Regime.values())
					//{
					Regime r = Regime.raw; //for now, can't manage retention in continous aggregates separately.
					ResultSet meta_res = meta.getTables(null, r.schema(), null, new String[] {"TABLE"});
					while(meta_res.next()) 
					{
						String this_name = meta_res.getString("TABLE_NAME");
						s = conn.createStatement();
						command = "SELECT drop_chunks(older_than => interval '" + r.retention() + "' , schema_name => '" + r.schema() + "', table_name => '" + this_name;
						command +=  "', cascade_to_materializations => true);"; //currently have to do this if there are continuous aggregates. TimescaleDB may improve this in the future.
						System.out.println(command);
						s.execute(command);
						s.close();
					}	
					//}	
					
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
