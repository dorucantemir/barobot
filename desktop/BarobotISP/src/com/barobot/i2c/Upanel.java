package com.barobot.i2c;

import java.util.ArrayList;
import java.util.List;

import com.barobot.common.IspSettings;
import com.barobot.common.constant.Methods;
import com.barobot.isp.Main;
import com.barobot.parser.Queue;
import com.barobot.parser.devices.I2C_Device;
import com.barobot.parser.devices.I2C_Device_Imp;
import com.barobot.parser.message.AsyncMessage;
import com.barobot.parser.utils.Decoder;

public class Upanel extends I2C_Device_Imp {
	public Upanel can_reset_me_dev	= null;
	public I2C_Device have_reset_to	= null;
	public int have_reset_address	= -1;
	
	public static List<Upanel> list	= new ArrayList<Upanel>();
	public static int findByI2c(int device_add) {
		for (I2C_Device s : list){
			if(s.getAddress() == device_add ){
				return Upanel.list.indexOf(s);
			}
		}
		return -1;
	}
	public Upanel(){
		this.cpuname	= "atmega8";
		this.lfuse		= "0xA4";
		this.hfuse		= "0xC7";
		this.lock		= "0x3F";
		this.efuse		= "";
	}
	public Upanel(int index, int address ){
		this();	// call default constructor
		this.setAddress(address);
		this.setIndex(index);
	}
	public Upanel(int index, int address, Upanel parent ){
		this();	// call default constructor
		this.setAddress(address);
		this.setIndex(index);
		this.can_reset_me_dev	= parent;
		parent.hasResetTo(this);
	}
	private void hasResetTo(I2C_Device child) {
		this.have_reset_to	= child;
	}
	public void canResetMe( Upanel current_dev){
		this.can_reset_me_dev = current_dev;
	}

	public String reset(Queue q, boolean execute ) {
		String command = "";
		if(getIndex() > 0 ){
			command = "RESET"+ this.myindex;
		}else if( can_reset_me_dev == null ){
			command = "RESET_NEXT"+ can_reset_me_dev.getAddress();
		}
		if(execute){
			q.add( command, true );
		}
		return command;
	}
	public void reset_next(Queue q) {
		if( this.myaddress > 0 ){
			q.add("RESET_NEXT"+ this.myaddress, true );
		}
	}
	public String getReset() {
		if(getIndex() > 0 ){
			return "P"+ this.myindex;
		}else if( can_reset_me_dev == null ){
			return "p"+ can_reset_me_dev.getAddress();
		}
		return "";
	}
	public String getIsp() {
		return "RESET"+ this.myindex;
	}

	public void isp_next(Queue q) {	// pod��czony do mnie
		q.add( "p"+ getAddress(), false );
	}

	private boolean hasNext = false;
	public boolean readHasNext( Queue q ) {
		hasNext = false;
		String command = "n" + this.myaddress;
		q.add( new AsyncMessage( command, true ){
			@Override
			public boolean isRet(String result, Queue q) {
				if(result.startsWith("" + Methods.METHOD_I2C_SLAVEMSG + ",")){		//	122,1,188,1
					int[] bytes = Decoder.decodeBytes(result);
					if(bytes[2] == Methods.METHOD_CHECK_NEXT ){
						if(bytes[3] == 1 ){							// has next
							hasNext = true;
						}
						return true;
					}
				}
				return false;
			}
		});
		q.addWaitThread(Main.mt);
	//	System.out.println("has next?" + (hasNext ? "1" : "0"));
		return hasNext;
	}

	public int resetNextAndReadI2c(Queue q) {
		have_reset_address = -1;
		String command = "RESET_NEXT"+ this.myaddress;
		q.add( new AsyncMessage( command, true ){
			@Override
			public boolean isRet(String result, Queue q) {
				if(result.startsWith(""+ Methods.METHOD_DEVICE_FOUND +",")){		//	112,18,19,1
					int[] bytes = Decoder.decodeBytes(result);	// HELLO, ADDRESS, TYPE, VERSION
					have_reset_address = bytes[1];
					return true;
				}
				return false;
			}
		});
		q.addWaitThread(Main.mt);
		return have_reset_address;
	}

	public int resetAndReadI2c( Queue q ) {
		myaddress = -1;
		String command = this.reset( q, false );
		q.add( new AsyncMessage( command, true ){
			@Override
			public boolean isRet(String result, Queue q) {
				if(result.startsWith(""+ Methods.METHOD_DEVICE_FOUND +",")){		//	112,18,19,1
					int[] bytes = Decoder.decodeBytes(result);	// HELLO, ADDRESS, TYPE, VERSION
					myaddress = bytes[1];
					return true;
				}
				return false;
			}
		});
		q.addWaitThread(Main.mt);
		return myaddress;
	}

	public String getHexFile() {
		return IspSettings.upHexPath;
	}

}