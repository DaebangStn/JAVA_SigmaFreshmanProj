import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Enumeration;

import gnu.io.CommPort;
import gnu.io.CommPortIdentifier;
import gnu.io.SerialPort;

public class SerialComm {
	
	String portName = "";
	
	int baudRate;
	boolean endian; // true : BIG, false : LITTLE
	
	CommPortIdentifier portIdentifier;
	CommPort commPort;
	OutputStream serialOutputStream;
	
	public SerialComm() {
		
		baudRate = 115200;
		endian = false;
		
	}
	
	public String[] getAvailablePortList() {
		Enumeration<CommPortIdentifier> portEnum = CommPortIdentifier.getPortIdentifiers();
        ArrayList<String> lists = new ArrayList<>();
        while ( portEnum.hasMoreElements() ) 
        {
            CommPortIdentifier portIdentifier = portEnum.nextElement();
            if(portIdentifier.getPortType()==CommPortIdentifier.PORT_SERIAL){
            	lists.add(portIdentifier.getName());
            }
            
        }
        return lists.toArray(new String[] {""});
	}
	
	public boolean isPortSelected() {
		if(commPort!=null) return true;
		else return false;
	}
	
	public int setPort(String portName) {
		// set serial-communicating port by portname
		// return -1 if error occurred
		
		this.portName = portName;
		if(commPort!=null) commPort.close();
		
		try{
			portIdentifier = CommPortIdentifier.getPortIdentifier(portName);
			if (portIdentifier.isCurrentlyOwned()){
				System.out.println("Error: Port is currently in use");
			}
			else {
				commPort = portIdentifier.open(SerialComm.class.getName(),2000);
				if(commPort instanceof SerialPort){
					SerialPort serialPort = (SerialPort) commPort;
					serialPort.setSerialPortParams(baudRate, SerialPort.DATABITS_8, SerialPort.STOPBITS_1,SerialPort.PARITY_EVEN);
					serialOutputStream = serialPort.getOutputStream();
				}
			}
			return 0;
		}
		catch (Exception e){
			e.printStackTrace();
			return -1;
		}
		
	}
	
	public void sendByte(byte data) {
		try {
			serialOutputStream.write(data);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public void sendInitializor() {
		sendByte((byte)0x10);
	}
	
	public void sendControlizer() {
		sendByte((byte)0x08);
	}
	
	public void sendAngleInfo(int motorId, float value) {
		// motorId : 1~7
		sendByte((byte)(motorId&0xff));
		
		ByteBuffer buff = ByteBuffer.allocate(Float.SIZE / 8);
    	buff.putFloat(value);
    	if(endian) buff.order(ByteOrder.BIG_ENDIAN);
    	else buff.order(ByteOrder.LITTLE_ENDIAN);
    	try {
			serialOutputStream.write(buff.array());
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	
	public void sendAngles(float[] values) {
		/*for(int i=1; i<=7; i++) {
			sendAngleInfo(i, values[i-1]);
		}*/
		
		sendByte((byte)0x03);
		sendByte((byte)0x78);
		sendByte((byte)0x78);
		sendByte((byte)0x78);
		sendByte((byte)0x78);
	}

}
