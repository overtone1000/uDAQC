package udaqc.io;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

public class IO_Value extends IO_Reporter
{
	protected String units;
	protected short format_type;

	protected Object value;

	public String Units()
	{
		return units;
	}

	public IO_Value(IO_Reporter basis, ByteBuffer data)
	{
		super(basis);
		Construct(data);
	}

	private void Construct(ByteBuffer data)
	{
		// Values then have a string containing their unit
		// another command telling their type
		// and a short saying how many bytes their type is
		short units_length = data.getShort();
		byte[] units_bytes = new byte[units_length];
		data.get(units_bytes);
		ByteBuffer units_bb = ByteBuffer.wrap(units_bytes);

		units = StandardCharsets.US_ASCII.decode(units_bb).toString();

		format_type = data.getShort();
	}

	@Override
	public void InterpretData(ByteBuffer data)
	{
		switch (format_type)
		{
		case IO_Constants.DataTypes.floating_point:
			switch (this.data_bytecount)
			{
			case 4:
				value = data.getFloat();
				break;
			case 8:
				value = data.getDouble();
				break;
			default:
				DefaultInterpret(data);
				break;
			}
			break;
		case IO_Constants.DataTypes.signed_integer:
			switch (this.data_bytecount)
			{
			case 4:
				value = data.getInt();
				break;
			case 8:
				value = data.getLong();
				break;
			default:
				DefaultInterpret(data);
				break;
			}
			break;
		case IO_Constants.DataTypes.unsigned_integer:
			System.out.println("Warning: Java doesn't play well with unsigned integers.");
			DefaultInterpret(data);
			break;
		case IO_Constants.DataTypes.bool:
			byte b = data.get();
			if (b == 0)
			{
				value = false;
			} else
			{
				value = true;
			}
			break;
		case IO_Constants.DataTypes.undefined:
		default:
			DefaultInterpret(data);
		}
		// System.out.println("Data received = " + value);
	}

	private void DefaultInterpret(ByteBuffer data)
	{
		byte[] data_bytes = new byte[this.data_bytecount];
		data.get(data_bytes);
		System.out.println("Indeterminant data received = " + data_bytes);
	}

	public short Format()
	{
		return format_type;
	}

	public int Size()
	{
		return data_bytecount;
	}

	public Object Value()
	{
		return value;
	}

	public Float ValueAsFloat()
	{
		switch (format_type)
		{
		case IO_Constants.DataTypes.floating_point:
			switch (this.data_bytecount)
			{
			case 4:
				return (float) value;
			case 8:
				return ((Double) value).floatValue();
			default:
				return -1.0f;
			}

		case IO_Constants.DataTypes.signed_integer:
			switch (this.data_bytecount)
			{
			case 2:
				return ((Short) value).floatValue();
			case 4:
				return ((Integer) value).floatValue();
			case 8:
				return ((Long) value).floatValue();

			default:
				return -1.0f;
			}
			
		case IO_Constants.DataTypes.unsigned_integer:
			System.out.println("Warning: Java doesn't play well with unsigned integers.");
			return -1.0f;
		case IO_Constants.DataTypes.bool:
			if ((Boolean) value)
			{
				return 1.0f;
			} else
			{
				return 0.0f;
			}
		case IO_Constants.DataTypes.undefined:
		default:
			return -1.0f;
		}
	}

}
