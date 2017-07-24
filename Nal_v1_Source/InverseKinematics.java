import au.edu.federation.caliko.FabrikBone3D;
import au.edu.federation.caliko.FabrikChain3D;
import au.edu.federation.caliko.FabrikChain3D.BaseboneConstraintType3D;
import au.edu.federation.utils.Vec3f;

public class InverseKinematics {
	
	FabrikChain3D chain;
	
	float handToRobotRatio = 1.5f; // robotmovement / handmovement
	
	float handLength = 60.0f; // final target = position + handLength*direction
	float lastBoneLength = 60.0f; // real length of last bone
	
	float maxTargetMovement = 20.0f;
	
	float angleConstraint = 135.0f;
	
	Vec3f prevTarget;
	
	public InverseKinematics() {
		createChain();
		prevTarget = new Vec3f(0.0f, chain.getChainLength(), 0.0f);
	}
	
	public float[] getAngles(Vec3f palmPosition, Vec3f handDirection, 
			Vec3f palmNormal, boolean controlMode) {
		solve(palmPosition, handDirection, prevTarget);
		if(controlMode) return adjustAndCalculate(palmNormal);
		else return null;
	}
	
	public FabrikChain3D getChain() {
		return chain;
	}
	
	private void createChain(){
		
		chain = new FabrikChain3D();		
		
		FabrikBone3D base = new FabrikBone3D(
				new Vec3f(0.0f, 0.0f, 0.0f), // start point
				new Vec3f(0.0f, 1.0f, 0.0f), // end point
				80.0f // length
				);
		chain.addBone(base);
		
		chain.addConsecutiveRotorConstrainedBone( // ball joint
				new Vec3f(0.0f, 1.0f, 0.0f), // default direction
				400.0f, // bone length
				90.0f // constraint angle degs
				);
		
		chain.addConsecutiveRotorConstrainedBone(
				new Vec3f(0.0f, 1.0f, 0.0f),
				250.0f,
				angleConstraint);
		
		chain.addConsecutiveRotorConstrainedBone(
				new Vec3f(0.0f, 1.0f, 0.0f),
				lastBoneLength, 
				angleConstraint);
		
		chain.setRotorBaseboneConstraint(
				BaseboneConstraintType3D.GLOBAL_ROTOR, 
				new Vec3f(0.0f, 1.0f, 0.0f),  // axis
				0.0f // angle
				);
		
	}
	
	public void solve(Vec3f palmPosition, Vec3f handDirection, Vec3f prevTarget){
		if(palmPosition.x == 0 &&
			palmPosition.y == 0 &&
			palmPosition.z == 0){
			return;
		}
		
		handDirection.normalise();
		
		Vec3f target = new Vec3f();
		
		Vec3f lastHandDirection = chain.getBone(chain.getNumBones()-1).getDirectionUV();
		
		chain.removeBone(chain.getNumBones()-1);
		
		Vec3f.add(target, palmPosition);
		Vec3f.add(target, handDirection.times(lastBoneLength-handLength));
		
		if(Vec3f.distanceBetween(target, prevTarget) > maxTargetMovement){
			Vec3f velocity = new Vec3f(target);
			Vec3f.subtract(velocity, prevTarget);
			velocity.normalise();
			Vec3f.times(velocity, maxTargetMovement);
			target = new Vec3f(prevTarget);
			Vec3f.add(target, velocity);
		}
		
		chain.solveForTarget(target);
		prevTarget.set(target);
		
		if(handDirection.length()!=0.0f) {
			chain.addConsecutiveRotorConstrainedBone(
					handDirection, lastBoneLength, angleConstraint);
		}
		else {
			chain.addConsecutiveRotorConstrainedBone(
					lastHandDirection, lastBoneLength, angleConstraint);
		}
				
		
	}
	
	private float[] adjustAndCalculate(Vec3f palmNormal){
		
		palmNormal.normalise();
		
		// adjust if final joint is over limit
		int numBones = 4;
		Vec3f[] UVs = new Vec3f[numBones];
		for(int i=0; i<numBones; i++){
			FabrikBone3D bone = chain.getBone(i);
			UVs[i] = bone.getDirectionUV();
			UVs[i].normalise();
			
		}
		
		if(Vec3f.dotProduct(UVs[2], UVs[3]) < Math.cos(Math.toRadians(angleConstraint))){
			// same length & angle = angleConstraint; system of equations
			Vec3f newUV = new Vec3f();
			// newUV = t1*UVs[2] + t2*UVs[3]
			// t2 = sin(angCon)/sqrt(1-(uv2*uv3)^2)
			// t1 = cos(angCon)-(uv2*uv3)*t2
			
			float dot = Vec3f.dotProduct(UVs[2], UVs[3]);
			float t2 = (float)(Math.sin(Math.toRadians(angleConstraint)) / Math.sqrt(1-dot*dot));
			float t1 = (float)(Math.cos(Math.toRadians(angleConstraint))-dot*t2);
			Vec3f.add(newUV, UVs[2].times(t1));
			Vec3f.add(newUV, UVs[3].times(t2));
			newUV.normalise();
			
			chain.removeBone(3);
			chain.addConsecutiveRotorConstrainedBone(
					newUV, lastBoneLength, angleConstraint);
			
		}
		
		// print motor outputs
		
		Vec3f[][] coords = new Vec3f[4][3];
		// coords[i][j] : ith bone's (j=0 : x, j=1 : y, j=2 : z) unit vector
		// in global coordinate
		
		coords[0][0] = new Vec3f(1.0f, 0.0f, 0.0f);
		coords[0][1] = new Vec3f(0.0f, 1.0f, 0.0f);
		coords[0][2] = new Vec3f(0.0f, 0.0f, 1.0f);
		fillInheritCoordInBones(coords, 1, UVs[1]);
		fillInheritCoordInBones(coords, 2, UVs[1]);
		fillInheritCoordInBones(coords, 3, UVs[1]);
		
		
		float[] theta = new float[7]; // degree
		
		theta[0] = servoRange(twistAngle(coords, UVs, 1));
		theta[1] = azimuthAngle(coords, UVs, 1);
		theta[2] = servoRange(twistAngle(coords, UVs, 2));
		theta[3] = servoRange(azimuthAngle(coords, UVs, 2));
		theta[4] = servoRange(twistAngle(coords, UVs, 3));
		theta[5] = servoRange(azimuthAngle(coords, UVs, 3));
		theta[6] = servoRange(lastTwistAngle(coords, palmNormal));
		
		return theta;
		
	}
	
	private void fillInheritCoordInBones(Vec3f[][] coords, int i, Vec3f di){
		// fill coords[][]'s ith bone = coords[i][]
		
		coords[i][1] = di;
		Vec3f tempvec = new Vec3f(coords[i-1][1]);
		Vec3f.subtract(tempvec, di.times(Vec3f.dotProduct(di, coords[i-1][1])));
		coords[i][2] = tempvec.normalise();
		if(coords[i][2].length() <= 0.0f) coords[i][2] = new Vec3f(0.0f, 0.0f, -1.0f);
		coords[i][0] = Vec3f.crossProduct(coords[i][1], coords[i][2]).normalise();
		
	}
	
	private float azimuthAngle(Vec3f[][] coords, Vec3f[] UVs, int i){
		return (float)Math.toDegrees(Math.acos(Vec3f.dotProduct(UVs[i], UVs[i-1])));
		
	}
	
	private float twistAngle(Vec3f[][] coords, Vec3f[] UVs, int i){
		float x = Vec3f.dotProduct(UVs[i], coords[i-1][0]);
		float z = Vec3f.dotProduct(UVs[i], coords[i-1][2]);
		float mag = (float)Math.sqrt(x*x+z*z);
		if(mag==0.0f) return 0.0f;
		x /= mag;
		z /= mag;
		
		return (float)Math.toDegrees(Math.atan2(-x, -z));
		
	}
	
	private float lastTwistAngle(Vec3f[][] coords, Vec3f normal){
		float x = Vec3f.dotProduct(normal, coords[3][0]);
		float z = Vec3f.dotProduct(normal, coords[3][2]);
		float mag = (float)Math.sqrt(x*x+z*z);
		if(mag==0.0f) return 0.0f;
		x /= mag;
		z /= mag;
		
		return (float)Math.toDegrees(Math.atan2(-x, -z));
	}
	
	private float servoRange(float angle){
		// input : 0~360
		if(angle>180.0f) angle -= 360.0f;
		
		if(angle > angleConstraint) angle = angleConstraint;
		if(angle < -angleConstraint) angle = -angleConstraint;
		
		return angle;
		
	}
	

}
