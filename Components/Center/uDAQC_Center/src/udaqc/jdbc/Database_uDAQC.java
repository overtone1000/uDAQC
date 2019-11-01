package udaqc.jdbc;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.Properties;

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
	
	static final String timecolumn = "time TIMESTAMPTZ NOT NULL";
	public void CreateHypertable(String new_table_name)
	{
		Statement s;
		String command;
		DatabaseMetaData meta;
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
				command += "temperature DOUBLE PRECISION NOT NULL";
				command += ");";
				s.executeUpdate(command);
				s.close();
				
				s = conn.createStatement();
				command = "select create_hypertable('" + new_table_name + "', 'time');";
				s.execute(command);
				s.close();
			}
		} catch (SQLException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
		
	public void test()
	{
		
		Statement s=null;
		String command;
		String new_table_name = "test_table";
		try
		{
			CreateHypertable(new_table_name);
			
			s = conn.createStatement();
			Double temp = Math.random();
			command = "insert into " + new_table_name + "(time,temperature) values (NoW(), " + temp.toString() + ");";
			s.executeUpdate(command);
			s.close();
			
			s = conn.createStatement();
			command = "select * from " + new_table_name + " order by time";
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
			e.printStackTrace();
		}
		finally
		{
			try
			{
				if(conn!=null)
				{
					conn.close();
				}
			} catch (SQLException e)
			{
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
}
