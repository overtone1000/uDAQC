package gndm.io;

import java.nio.ByteBuffer;

public class IO_ModifiableValue extends IO_Value
{
	protected short index;
	
	public IO_ModifiableValue(IO_Reporter basis, ByteBuffer data)
	{
		super(basis, data);
		Construct(data);
	}

	
	private void Construct(ByteBuffer data)
	{
		index = data.getShort();
		System.out.println("Got IO_ModifiableValue " + ((Short)index).toString() + " " + this.FullName());
	}
	
	public short Index()
	{
		return index;
	}
}
