import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GraphicsConfiguration;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.media.j3d.AmbientLight;
import javax.media.j3d.Background;
import javax.media.j3d.BoundingSphere;
import javax.media.j3d.BranchGroup;
import javax.media.j3d.Canvas3D;
import javax.media.j3d.DirectionalLight;
import javax.media.j3d.Geometry;
import javax.media.j3d.Transform3D;
import javax.media.j3d.TransformGroup;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.vecmath.Color3f;
import javax.vecmath.Matrix3d;
import javax.vecmath.Point3d;
import javax.vecmath.Vector3d;
import javax.vecmath.Vector3f;

import com.sun.j3d.utils.behaviors.vp.OrbitBehavior;
import com.sun.j3d.utils.geometry.ColorCube;
import com.sun.j3d.utils.geometry.Cone;
import com.sun.j3d.utils.geometry.Sphere;
import com.sun.j3d.utils.universe.SimpleUniverse;
import com.sun.j3d.utils.universe.ViewingPlatform;

import au.edu.federation.caliko.FabrikChain3D;
import au.edu.federation.utils.Vec3f;

public class Graphics {
	
	int width, height; // size of the program
	int gWidth, gHeight; // size of the graphic area
	int cWidth, cHeight; // size of the control area
	
	SerialComm sender;
	
	ActionListener listener;
	
	MainFrame mainFrame;
	
	public Graphics(int w, int h, SerialComm sender, ActionListener listener,
			FabrikChain3D initialChain) {
		width = w;
		height = h;
		gWidth = width*4/5;
		gHeight = height;
		cWidth = width*1/5;
		cHeight = height;
		
		this.sender = sender;
		this.listener = listener;
		
		mainFrame = new MainFrame(initialChain);
		
		
	}
	
	class MainFrame extends JFrame{
		// Main Graphic Container of the program
		
		DrawingPanel drawPanel;
		ControlPanel controlPanel;
		
		public MainFrame(FabrikChain3D initialChain) {
			super("NalTaraHevaYo Control Panel");
			setBounds(100, 100, 100+width, 100+height);
			setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
			setLayout(new BorderLayout());
			
			drawPanel = new DrawingPanel(initialChain);
			controlPanel = new ControlPanel();
			
			drawPanel.setSize(gWidth, gHeight);
			controlPanel.setSize(cWidth, cHeight);
			
			add(drawPanel, "West");
			add(controlPanel, "East");
			
			setVisible(true);
			
		}
		
		class DrawingPanel extends JPanel {
			
			private SimpleUniverse su;
			private BranchGroup sceneBG;
			private BoundingSphere bounds;
			
			public DrawingPanel(FabrikChain3D initialChain) {
				
				setLayout(new BorderLayout());
				setOpaque(false);
				setPreferredSize(new Dimension(gWidth, gHeight));
				
				GraphicsConfiguration config = SimpleUniverse.getPreferredConfiguration();
				Canvas3D canvas = new Canvas3D(config);
				add("Center", canvas);
				canvas.setFocusable(true);
				canvas.requestFocus();
				
				su = new SimpleUniverse(canvas);
				createSceneGraph(initialChain);
				initUserPosition();
				orbitControls(canvas);
				
				su.addBranchGraph(sceneBG);
				
			}
			
			TransformGroup[] groups;
			
			private void createSceneGraph(FabrikChain3D initialChain) {
				// initialize the scene
				sceneBG = new BranchGroup();
				bounds = new BoundingSphere(new Point3d(0,0,0), gHeight/2);
				
				lightScene();
				addBackground();
				
				groups = new TransformGroup[4];
				
				for(int i=0; i<4; i++) {
					groups[i] = new TransformGroup();
					groups[i].setCapability(TransformGroup.ALLOW_TRANSFORM_WRITE);
					groups[i].setBounds(bounds);
					sceneBG.addChild(groups[i]);
				}
				
				addInitialCones(initialChain);
				
				refreshCones(initialChain);
				
				addDirectionCubes();
				
				sceneBG.compile();
				
				
				
			}
			
			private void lightScene() {
				// 1 ambient light, 2 directional lights
				Color3f white = new Color3f(1.0f, 1.0f, 1.0f);
				
				AmbientLight ambientLightNode = new AmbientLight(white);
				ambientLightNode.setInfluencingBounds(bounds);
				sceneBG.addChild(ambientLightNode);
				
				Vector3f light1Direction = new Vector3f(-1.0f, -1.0f, -1.0f);
				Vector3f light2Direction = new Vector3f(1.0f, 1.0f, 1.0f);
				
				DirectionalLight light1 = new DirectionalLight(white, light1Direction);
				light1.setInfluencingBounds(bounds);
				sceneBG.addChild(light1);
				
				DirectionalLight light2 = new DirectionalLight(white, light2Direction);
				light2.setInfluencingBounds(bounds);
				sceneBG.addChild(light2);
				
			}
			
			private void addBackground() {
				// blue sky
				
				Background back = new Background();
				back.setApplicationBounds(bounds);
				back.setColor(0.17f, 0.62f, 0.95f);
				sceneBG.addChild(back);
				
			}
			
			private void orbitControls(Canvas3D c) {
				// rotate & zoom in & out
				
				OrbitBehavior orbit = new OrbitBehavior(c, OrbitBehavior.REVERSE_ALL);
				orbit.setSchedulingBounds(bounds);
				
				ViewingPlatform vp = su.getViewingPlatform();
				vp.setViewPlatformBehavior(orbit);
				
			}
			
			private void initUserPosition() {
				// user's initial viewpoint
				ViewingPlatform vp = su.getViewingPlatform();
				TransformGroup steerTG = vp.getViewPlatformTransform();
				
				Transform3D t3d = new Transform3D();
				steerTG.getTransform(t3d);
				
				// viewer position, where looking, up direction
				t3d.lookAt(new Point3d(0.7,1.3,2), new Point3d(0,0.4,0), new Vector3d(0,1,0));
				t3d.invert();
				
				
				steerTG.setTransform(t3d);
				
				//su.getViewingPlatform().setNominalViewingTransform();
				// alternative setting
				
			}
			
			private void makeTransform(TransformGroup group, Point3d start, Point3d end) {
				Vector3d translation = new Vector3d();
				translation.setX((start.getX()+end.getX())/2.0);
				translation.setY((start.getY()+end.getY())/2.0);
				translation.setZ((start.getZ()+end.getZ())/2.0);
				
				double length;
				length = Math.sqrt(
						Math.pow(start.getX()-end.getX(), 2.0) + 
						Math.pow(start.getY()-end.getY(), 2.0) + 
						Math.pow(start.getZ()-end.getZ(), 2.0));
				
				Vector3d X = new Vector3d(1.0, 0.0, 0.0);
				Vector3d Y = new Vector3d(0.0, 1.0, 0.0);
				Vector3d Z = new Vector3d(0.0, 0.0, 1.0);
				
				Vector3d newY = new Vector3d();
				newY.setX((end.getX()-start.getX()));
				newY.setY((end.getY()-start.getY()));
				newY.setZ((end.getZ()-start.getZ()));
				newY.normalize();
				
				Vector3d newZ = new Vector3d(Z);
				
				Vector3d newX = new Vector3d();
				newX.cross(newY, newZ);
				newX.normalize();
				if(newX.length()==0.0) {
					newZ = new Vector3d(X);
					newX = new Vector3d();
					newX.cross(newY, newZ);
					newX.normalize();
				}
				
				Matrix3d rotation = new Matrix3d(
						newX.dot(X), newY.dot(X), newZ.dot(X),
						newX.dot(Y), newY.dot(Y), newZ.dot(Y),
						newX.dot(Z), newY.dot(Z), newZ.dot(Z)
						);
				
				
				
				
				Transform3D t3d = new Transform3D();
				t3d.setRotation(rotation);
				t3d.setTranslation(translation);
				group.setTransform(t3d);
				
			}
			
			private void addCones(FabrikChain3D chain, TransformGroup[] groups) {
				Point3d base = new Point3d(0,0,0);
				
				Vec3f n1 = chain.getBone(0).getEndLocation();
				Vec3f n2 = chain.getBone(1).getEndLocation();
				Vec3f n3 = chain.getBone(2).getEndLocation();
				Vec3f n4 = chain.getBone(3).getEndLocation();
				float length = chain.getChainLength();
				
				Point3d node1 = new Point3d(
						n1.x/length, n1.y/length, n1.z/length);
				Point3d node2 = new Point3d(
						n2.x/length, n2.y/length, n2.z/length);
				Point3d node3 = new Point3d(
						n3.x/length, n3.y/length, n3.z/length);
				Point3d node4 = new Point3d(
						n4.x/length, n4.y/length, n4.z/length);
				
				makeTransform(groups[0], base, node1);
				makeTransform(groups[1], node1, node2);
				makeTransform(groups[2], node2, node3);
				makeTransform(groups[3], node3, node4);
				
				
			}
			
			public void refreshCones(FabrikChain3D chain) {
				
				addCones(chain, groups);
				
			}
			
			private void addInitialCones(FabrikChain3D chain) {
				Vec3f n0 = new Vec3f(0.0f, 0.0f, 0.0f);
				Vec3f n1 = chain.getBone(0).getEndLocation();
				Vec3f n2 = chain.getBone(1).getEndLocation();
				Vec3f n3 = chain.getBone(2).getEndLocation();
				Vec3f n4 = chain.getBone(3).getEndLocation();
				
				float l0 = Vec3f.distanceBetween(n0, n1);
				float l1 = Vec3f.distanceBetween(n1, n2);
				float l2 = Vec3f.distanceBetween(n2, n3);
				float l3 = Vec3f.distanceBetween(n3, n4);
				
				float lt = chain.getChainLength();
				l0 /= lt;
				l1 /= lt;
				l2 /= lt;
				l3 /= lt;
				
				groups[0].addChild(new Cone(l0*2.0f, l0));
				groups[1].addChild(new Cone(l1/10.0f, l1));
				groups[2].addChild(new Cone(l2/10.0f, l2));
				groups[3].addChild(new Cone(l3/10.0f, l3));
				
			}
			
			private void addDirectionCubes() {
				TransformGroup tg1 = new TransformGroup();
				Transform3D t3d1 = new Transform3D();
				t3d1.setTranslation(new Vector3d(0.3, 0.0, 0.0));
				tg1.setTransform(t3d1);
				tg1.addChild(new ColorCube(0.03));
				sceneBG.addChild(tg1);
				
				TransformGroup tg2 = new TransformGroup();
				Transform3D t3d2 = new Transform3D();
				t3d2.setTranslation(new Vector3d(-0.3, 0.0, 0.0));
				tg2.setTransform(t3d2);
				tg2.addChild(new ColorCube(0.03));
				sceneBG.addChild(tg2);
				
				TransformGroup tg3 = new TransformGroup();
				Transform3D t3d3 = new Transform3D();
				t3d3.setTranslation(new Vector3d(0.0, 0.0, 0.3));
				tg3.setTransform(t3d3);
				tg3.addChild(new ColorCube(0.03));
				sceneBG.addChild(tg3);
				
				TransformGroup tg4 = new TransformGroup();
				Transform3D t3d4 = new Transform3D();
				t3d4.setTranslation(new Vector3d(0.0, 0.0, -0.3));
				tg4.setTransform(t3d4);
				tg4.addChild(new ColorCube(0.07));
				sceneBG.addChild(tg4);
			}
			
			
			
		}
		
		class ControlPanel extends JPanel {
			
			JButton initializer, controlizer, portRefresher;
			JComboBox<String> portselector;
			JTextField portInfo;
			
			public ControlPanel() {
				setBackground(Color.CYAN);
				setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
				
				initializer = new JButton("Initialize Mode");
				controlizer = new JButton("Control Mode");
				portRefresher = new JButton("Search Available Ports");
				
				
				initializer.addActionListener(listener);
				controlizer.addActionListener(listener);
				portRefresher.addActionListener(listener);
				
				portselector = new JComboBox<String>();
				searchAvailPorts(portselector);
				portselector.addActionListener(listener);
				
				portInfo = new JTextField("Current Port : nothing");
				
				add(initializer);
				add(controlizer);
				add(portRefresher);
				add(portselector);
				add(portInfo);
				
				
				
			}
			
			
		}
		
	}
	
	public JButton getInitBtn() {
		return mainFrame.controlPanel.initializer;
	}
	public JButton getContBtn() {
		return mainFrame.controlPanel.controlizer;
	}
	public JButton getRefrBtn() {
		return mainFrame.controlPanel.portRefresher;
	}
	public JComboBox<String> getPortCombobox(){
		return mainFrame.controlPanel.portselector;
	}
	
	public void draw(FabrikChain3D chain) {
		mainFrame.drawPanel.refreshCones(chain);
	}
	
	public void searchAvailPorts(JComboBox<String> selector) {
		String[] list = sender.getAvailablePortList();
		selector.removeAllItems();
		for(int i=0; i<list.length; i++) {
			selector.addItem(list[i]);
		}
		
	}
	
	public void refreshPortInfo() {
		String portName = sender.portName;
		if(portName == null) mainFrame.controlPanel.portInfo.setText("Current Port : nothing");
		else mainFrame.controlPanel.portInfo.setText("Current Port : "+portName);
	}

}
