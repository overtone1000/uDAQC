package udaqc.io.fx;

import java.nio.ByteOrder;
import java.util.Vector;

import org.apache.mina.core.buffer.IoBuffer;

import gndm.io.IO_Constants;
import gndm.io.IO_Group;
import gndm.io.IO_ModifiableValue;
import gndm.io.IO_Node;
import gndm.io.IO_Value;
import gndm.io.log.IO_System_Logged;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.util.converter.FloatStringConverter;

public class IO_Live extends VBox implements IO_Display
{
	protected IO_System_Logged system;
	
	public static class Value extends HBox
	{
		protected Label name=new Label();
		protected Label value=new Label();
		protected IO_Value basis=null;
		public Value(IO_Value basis)
		{
			this.basis=basis;
			this.Update();
			
			this.setBackground(new Background(new BackgroundFill(Color.TRANSPARENT,null,null)));
			this.setPadding(new Insets(10));
			this.setSpacing(8);
			this.getChildren().addAll(name,value);
		}
		public void Update()
		{
			if(basis==null || name ==null)
			{
				System.out.println("Occasional error 1 encountered. This should be handled better.");
				return;
			}
			name.setText(basis.FullName());
			if(value == null || basis.Value()==null)
			{
				System.out.println("Occasional error 2 encountered. This should be handled better.");
				return;
			}
			value.setText(basis.Value().toString());
		}
	}
	public static class ModifiableValue extends Value
	{
		protected IO_System_Logged system;
		protected ModificationField modification_field;
		protected Button button=new Button("Set");
		public ModifiableValue(IO_Value basis, ModificationField mod_field, IO_System_Logged system)
		{
			super(basis);
			this.system=system;
			this.modification_field = mod_field;
			this.getChildren().addAll(modification_field,button);
			
			button.setOnAction(new EventHandler<ActionEvent>(){
				@Override
				public void handle(ActionEvent event) {
					try
					{
						byte[] value = modification_field.value();
						Short index = ((IO_ModifiableValue)basis).Index();
						system.ModifyValue(index, value);
					}
					catch(Exception e)
					{
						System.out.println(e.getMessage());
					}
				}});
		}
	}
	
	public static class ModificationField extends Pane
	{
		public ModificationField()
		{
			super();
		}
		
		public byte[] value()
		{
			return null;
		}
	}
	
	public static class BooleanMod extends ModificationField
	{
		protected CheckBox cb=new CheckBox();
		public BooleanMod()
		{
			super();
			this.getChildren().add(cb);
		}
		
		@Override
		public byte[] value()
		{
			byte[] retval=new byte[1];
			if(cb.isSelected())
			{
				retval[0]=1;
			}
			else
			{
				retval[0]=0;
			}
			return retval;
		}
	}
	
	public static class TextBoxMod extends ModificationField
	{
		protected TextField tf=new TextField();
		public TextBoxMod()
		{
			super();
			this.getChildren().add(tf);
		}
	}
	
	public static class FloatTextBoxMod extends TextBoxMod
	{
		public static FloatStringConverter conv = new FloatStringConverter();
		
		@Override
		public byte[] value()
		{
			IoBuffer bb = IoBuffer.allocate(Float.BYTES);
			bb.order(ByteOrder.LITTLE_ENDIAN);
			float fval = conv.fromString(tf.getText());
			bb.putFloat(fval);
			return bb.array();
		}
	}
	
	public static class DoubleTextBoxMod extends FloatTextBoxMod
	{
		public static FloatStringConverter conv = new FloatStringConverter();
		
		@Override
		public byte[] value()
		{
			IoBuffer bb = IoBuffer.allocate(Double.BYTES);
			bb.order(ByteOrder.LITTLE_ENDIAN);
			double fval = conv.fromString(tf.getText());
			bb.putDouble(fval);
			return bb.array();
		}
	}
	
	public static class IntTextBoxMod extends TextBoxMod
	{
		@Override
		public byte[] value()
		{
			IoBuffer bb = IoBuffer.allocate(Integer.BYTES);
			bb.order(ByteOrder.LITTLE_ENDIAN);
			int fval = Integer.parseInt(tf.getText());
			bb.putInt(fval);
			return bb.array();
		}
	}
	
	public static class ShortTextBoxMod extends IntTextBoxMod
	{
		@Override
		public byte[] value()
		{
			IoBuffer bb = IoBuffer.allocate(Short.BYTES);
			bb.order(ByteOrder.LITTLE_ENDIAN);
			short fval = Short.parseShort(tf.getText());
			bb.putShort(fval);
			return bb.array();
		}
	}
	
	public static class LongTextBoxMod extends IntTextBoxMod
	{
		@Override
		public byte[] value()
		{
			IoBuffer bb = IoBuffer.allocate(Long.BYTES);
			bb.order(ByteOrder.LITTLE_ENDIAN);
			long fval = Long.parseLong(tf.getText());
			bb.putLong(fval);
			return bb.array();
		}
	}
	
	public IO_Live()
	{
		super();
	}
	
	public void Set_IO(IO_Node selected_item, IO_System_Logged system)
	{
		this.getChildren().clear();
		values.clear();
		this.system=system;
		Add_IO(selected_item);
	}
	
	private Vector<Value> values=new Vector<Value>();
	private void Add_IO(IO_Node val)
	{
		switch (val.IO_Type())
		{
		case IO_Constants.Command_IDs.group_description:
			for(IO_Node r:((IO_Group)val).GetMembers())
			{
				Add_IO(r);
			}
			break;
		case IO_Constants.Command_IDs.value_description:
			Value v = new Value((IO_Value)val);
			this.getChildren().add(v);
			values.add(v);
			break;
		case IO_Constants.Command_IDs.modifiablevalue_description:
			ModificationField f=GetModField((IO_Value)val);
			ModifiableValue mv = new ModifiableValue((IO_Value)val,f,system);
			this.getChildren().add(mv);
			values.add(mv);
			break;
		default:
			break;
		}
	}
	
	public void Update_IO()
	{
		if(system==null)
		{
			return;
		}
		Platform.runLater(new Runnable()
		{
			@Override
			public void run()
			{
				for(Value v:values) 
				{
					v.Update();
				}
			}
		});
	}
	
	private static ModificationField GetModField(IO_Value r)
	{
		switch(r.Format())
		{
		case IO_Constants.DataTypes.floating_point:
			switch(r.Size())
			{
			case 4:
				return new FloatTextBoxMod();
			case 8:
				return new DoubleTextBoxMod();
			default:
				return null;
				
			}
			
		case IO_Constants.DataTypes.signed_integer:
			switch(r.Size())
			{
			case 4:
				return new IntTextBoxMod();
				
			case 8:
				return new LongTextBoxMod();
				
			default:
				return null;
				
			}
		case IO_Constants.DataTypes.unsigned_integer:
			System.out.println("Warning: Java doesn't play well with unsigned integers.");
			return null;
		case IO_Constants.DataTypes.bool:
			return new BooleanMod();
		case IO_Constants.DataTypes.undefined:	
		default:
			return null;
		}
	}
}
