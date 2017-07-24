import com.leapmotion.leap.Controller;
import com.leapmotion.leap.Hand;
import com.leapmotion.leap.Vector;

import au.edu.federation.utils.Vec3f;

public class MotionSensing {
	
	Controller controller;
	Hand hand;
	int handPlace = RIGHT_HAND;
	
	long lastFrameId = -1;
	
	public MotionSensing(Controller controller) {
		this.controller = controller;
		hand = controller.frame().hands().rightmost();
		
	}
	
	public static int LEFT_HAND = 0;
	public static int RIGHT_HAND = 1;
	
	public void changeHandPlace(int which) {
		handPlace = which;
	}
	
	public void setHand(int which) {
		if(which == LEFT_HAND) hand = controller.frame().hands().leftmost();
		else hand = controller.frame().hands().rightmost();
	}
	
	private void refreshHand() {
		if(lastFrameId != controller.frame().id()) {
			setHand(handPlace);
			lastFrameId = controller.frame().id();
		}
	}
	
	public Vec3f getPalmPosition() {
		refreshHand();
		Vector position = hand.palmPosition();
		return new Vec3f(position.getX(), position.getY(), position.getZ());
	}
	
	public Vec3f getHandDirection() {
		refreshHand();
		Vector direction = hand.direction();
		return new Vec3f(direction.getX(), direction.getY(), direction.getZ());
	}
	
	public Vec3f getPalmNormal() {
		refreshHand();
		Vector normal = hand.palmNormal();
		return new Vec3f(normal.getX(), normal.getY(), normal.getZ());
		
	}
	
	public Vec3f[] getAll() {
		// position, direction, normal
		Vec3f[] veclist = new Vec3f[3];
		veclist[0] = getPalmPosition();
		veclist[1] = getHandDirection();
		veclist[2] = getPalmNormal();
		return veclist;
	}

}
