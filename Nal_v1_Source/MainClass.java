import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JOptionPane;

import com.leapmotion.leap.Controller;

import au.edu.federation.utils.Vec3f;

public class MainClass implements ActionListener {
	
	MotionSensing sensor;
	InverseKinematics calculator;
	SerialComm sender;
	Graphics drawer;
	
	boolean controlMode = false;

	public static void main(String[] args) {
		// TODO Auto-generated method stub
		Controller controller = new Controller(); // Leap motion controller
		MotionSensing sensor = new MotionSensing(controller);
		
		
		InverseKinematics calculator = new InverseKinematics();
		
		SerialComm sender = new SerialComm();
		
		Dimension res = Toolkit.getDefaultToolkit().getScreenSize();
		MainClass mainAct = new MainClass(sensor, calculator, sender);
		Graphics drawer = new Graphics(
				(int)(res.getWidth()*0.8), (int)(res.getHeight()*0.8), 
				sender, mainAct, calculator.getChain());
		mainAct.setDrawer(drawer);
		
		mainAct.loop();

	}
	
	public MainClass(MotionSensing sensor, InverseKinematics calculator,
			SerialComm sender) {
		this.sensor = sensor;
		this.calculator = calculator;
		this.sender = sender;
	}
	
	public void setDrawer(Graphics drawer) {
		this.drawer = drawer;
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		// TODO Auto-generated method stub
		
		if(e.getSource() == drawer.getInitBtn()) {
			if(sender.isPortSelected()) {
				controlMode = false;
				sender.sendInitializor();
			}
			else {
				JOptionPane.showMessageDialog(null, "포트를 선택해주세요!", "오류", JOptionPane.ERROR_MESSAGE);
			}
			
		}
		else if(e.getSource() == drawer.getContBtn()) {
			if(sender.isPortSelected()) {
				controlMode = true;
				sender.sendControlizer();
			}
			else {
				JOptionPane.showMessageDialog(null, "포트를 선택해주세요!", "오류", JOptionPane.ERROR_MESSAGE);
			}
		}
		else if(e.getSource() == drawer.getRefrBtn()) {
			drawer.searchAvailPorts(drawer.getPortCombobox());
		}
		else if(e.getSource() == drawer.getPortCombobox()) {
			String portName = (String)drawer.getPortCombobox().getSelectedItem();
			if(portName != null) {
				sender.setPort(portName);
			}
			drawer.refreshPortInfo();
		}
		
		
	}
	
	public void loop() {
		
		Vec3f[] prevVecs = new Vec3f[3];
		for(int i=0; i<3; i++) {
			prevVecs[i] = new Vec3f();
		}
		
		
		while(true) {
			Vec3f[] vecs = sensor.getAll();
			
			if(vecs[0].equals(prevVecs[0]) &&
					vecs[1].equals(prevVecs[1]) &&
					vecs[2].equals(prevVecs[2])) {
				// Do nothing
			}
			else {
				// if changed
				float[] angs = calculator.getAngles(vecs[0], vecs[1], vecs[2], controlMode);
				
				drawer.draw(calculator.getChain());
				
				if(angs != null) {
					// if it's on controlMode
					sender.sendAngles(angs);
					
				}
				
				prevVecs[0] = new Vec3f(vecs[0]);
				prevVecs[1] = new Vec3f(vecs[1]);
				prevVecs[2] = new Vec3f(vecs[2]);
				
			}
			
			// only for 0x78 test mode
			if(sender.serialOutputStream != null) sender.sendAngles(null);
			
			
			
		}
	}
	
	

}
