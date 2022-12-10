import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;
import javax.swing.JPanel;
import javax.swing.JSlider;

@SuppressWarnings("serial")
public class Viewer3d extends JPanel implements Runnable {
	private final Thread myThread;
	private boolean go;
	private JSlider headingSlider, pitchSlider, rollSlider, fovSlider;
	private ArrayList<Triangle> tris;
	private Vector[] vBuffer;
	private BufferedImage img;
	private int width, height, sMaxH, sMaxW, sleep;
	private Graphics2D g2d;
	private float[][] zBuffer;
	private float fov, ratio, camYaw;
	private Matrix4 projMatrix;
	private final Vector up = new Vector(0, 1, 0, 0);
	private final Vector right = new Vector(1, 0, 0, 0);
	// X GOES RIGHT
	// Y GOES UP
	// Z GOES TOWARDS CAMERA
	private final Vector forward = new Vector(0, 0, -1, 0);
	private Vector lightDir = new Vector(0, -1, 1, 0);
	private Vector cameraLookVec = new Vector(0, 0, -1, 0);
	private final Vector cameraPos = new Vector(0, 0, 5, 0);

	public Viewer3d(JSlider slider1, JSlider slider2, JSlider slider3, JSlider slider4) {
		this.headingSlider = slider1;
		this.pitchSlider = slider2;
		this.rollSlider = slider3;
		this.fovSlider = slider4;
		this.go=true;
		this.fov = (float) Math.toRadians(90);
		GraphicsDevice gd = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
		sMaxW = gd.getDisplayMode().getWidth()+1;
		sMaxH = gd.getDisplayMode().getHeight()+1;
		this.ratio = 1.0f;
		this.camYaw=0;
		this.myThread = new Thread(this);
		this.sleep = 1;
		this.projMatrix = new Matrix4();
		this.lightDir.normalize3d();

		Importer i = new Importer();
		this.tris=i.importDefault(); 
		//this.tris = i.importModel("C:\\Users\\Usuario\\Desktop\\Monkey.obj");
		this.vBuffer=i.getVertexBuffer();
		
		
	}

	// Adjust to new size
	public synchronized void resize() {
		width=getWidth();
		height=getHeight();
		System.out.println(width+" "+height);
		this.projMatrix.initPerspective(fov, ratio, 1.0f, 1000.0f);
		img = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
		g2d = img.createGraphics();
		g2d.setColor(Color.DARK_GRAY);
		go=true;
	}
	
	public synchronized void pause() {
		if(go) {
			go=false;
			try {
				TimeUnit.MILLISECONDS.sleep(40);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

	// Change the projection matrix according to fov
	public void changeFov() {
		this.fov = (float) Math.toRadians(fovSlider.getValue());
		this.projMatrix.initPerspective(fov, ratio, 1.0f, 500.0f);
	}

	public void init() {
		myThread.start();
	}

	@Override
	public void run() {
		while (true) {
			if(go) {
				try {
					Thread.sleep(sleep);					
					myPaint();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}else {
				System.out.println("pause");
				try {
					Thread.sleep(40);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				resize();
			}
		}
	}

	public void myPaint() {
		
		Graphics2D g2 = (Graphics2D) this.getGraphics();
		g2d.fillRect(0, 0, width, height);
		
		
		// get slider values, update rotation quaternions and multiply them
		float heading = (float) Math.toRadians(headingSlider.getValue());
		Quaternion rotQuaternion = new Quaternion(up, heading);
		float pitch = (float) Math.toRadians(pitchSlider.getValue());
		rotQuaternion = rotQuaternion.mul(new Quaternion(right, pitch));
		float roll = (float) Math.toRadians(rollSlider.getValue());
		rotQuaternion = rotQuaternion.mul(new Quaternion(forward, roll));
		
		//make PointAt matrix for camera
		Vector vTarget = new Vector(0,0,forward.z,0);
		Matrix4 matCameraRot = new Matrix4().initRotation(up,camYaw);
		cameraLookVec = matCameraRot.transform(vTarget);
		vTarget = cameraPos.add(cameraLookVec);
		Matrix4 viewMat= new Matrix4().initPointAt(cameraPos, vTarget, up).invertMatrix();
		
		// create new zBuffer of the right size (extra space for big sudden changes in window size)
		zBuffer = new float[(sMaxH+height)/2][(sMaxW+width)/2];
		// initialize zBuffer	
		for (short q = 0; q < height; ++q) {
			if(!go)break;
			for (short r = 0; r < width; ++r) {
				zBuffer[q][r] = Float.NEGATIVE_INFINITY;
			}
		}		
		
		// loop through all the the triangles in the scene
		for (Triangle t : tris) {
			if(!go)break;
			
			// Apply rotation to the three vertices of the triangle
			Vector v1 = vBuffer[t.v1].rotate(rotQuaternion);
			Vector v2 = vBuffer[t.v2].rotate(rotQuaternion);
			Vector v3 = vBuffer[t.v3].rotate(rotQuaternion);
			
			//View transformations
			v1 = viewMat.transform(v1);
			v2 = viewMat.transform(v2);
			v3 = viewMat.transform(v3);				
			
			// Apply projection matrix
			v1 = projMatrix.transform(v1);
			v2 = projMatrix.transform(v2);
			v3 = projMatrix.transform(v3);

			// Compute two lines and use them to get the normal of the triangle
			Vector ab = new Vector(v2.x - v1.x, v2.y - v1.y, v2.z - v1.z, 1);
			Vector ac = new Vector(v3.x - v1.x, v3.y - v1.y, v3.z - v1.z, 1);
			Vector norm = new Vector(ab.y * ac.z - ab.z * ac.y, ab.z * ac.x - ab.x * ac.z, ab.x * ac.y - ab.y * ac.x, 1);
			
			// Normalize normal vector
			norm.normalize3d();
			
			if (norm.dot(cameraLookVec)<0 && inFrontOfCamera(cameraLookVec, cameraPos, t, norm)) {
				// Get light incidence angle
				float angleCos = Math.max(0.1f, norm.dot(lightDir));

				// Translate the unitary coordinates to the size of the screen
				++v1.x; ++v1.y;
				++v2.x; ++v2.y;				
				++v3.x; ++v3.y;
				
				float mult = 0.5f * height;
				float dif = 0.5f * (width - height);
				v1.xyscale(mult);
				v1.x += dif;
				v2.xyscale(mult);
				v2.x += dif;
				v3.xyscale(mult);
				v3.x += dif;
				
				// Calculate screen space that a triangle occupies
				short minX = (short) Math.max(0, Math.ceil(Math.min(v1.x, Math.min(v2.x, v3.x))));
				short maxX = (short) Math.min(width - 1, Math.floor(Math.max(v1.x, Math.max(v2.x, v3.x))));
				short minY = (short) Math.max(0, Math.ceil(Math.min(v1.y, Math.min(v2.y, v3.y))));
				short maxY = (short) Math.min(height - 1, Math.floor(Math.max(v1.y, Math.max(v2.y, v3.y))));

				float triangleArea = (v1.y - v3.y) * (v2.x - v3.x) + (v2.y - v3.y) * (v3.x - v1.x);
				
				// loop through the pixels that correspond to the triangle and shade them
				for (short y = minY; y <= maxY; ++y) {
					for (short x = minX; x <= maxX; ++x) {
						float b1 = ((y - v3.y) * (v2.x - v3.x) + (v2.y - v3.y) * (v3.x - x)) / triangleArea;
						float b2 = ((y - v1.y) * (v3.x - v1.x) + (v3.y - v1.y) * (v1.x - x)) / triangleArea;
						float b3 = ((y - v2.y) * (v1.x - v2.x) + (v1.y - v2.y) * (v2.x - x)) / triangleArea;
						if (b1 >= 0 && b1 <= 1 && b2 >= 0 && b2 <= 1 && b3 >= 0 && b3 <= 1) {
							float depth = b1 * v1.z + b2 * v2.z + b3 * v3.z;
							if (zBuffer[y][x] < depth) {
								img.setRGB(x, y, getShade(t.color, angleCos).getRGB());
								zBuffer[y][x] = depth;
							}
						}
					}
				}
			}
		}
		// draw image
		g2.drawImage(img, 0, 0, null);
	}

	public void paintComponent(Graphics g) {}

	public Color getShade(Color color, float shade) {
		// apply shading to the pixel
		float redLinear = (color.getRed() * color.getRed()) * shade;
		float greenLinear = (color.getGreen() * color.getGreen()) * shade;
		float blueLinear = (color.getBlue() * color.getBlue()) * shade;
		// convert the resulting color from linear to sRGB
		short red = (short) Math.sqrt(redLinear);
		short green = (short) Math.sqrt(greenLinear);
		short blue = (short) Math.sqrt(blueLinear);
		return new Color(red, green, blue);
	}
	
	public boolean inFrontOfCamera(Vector cameraVec, Vector cameraPos, Triangle t, Vector normal) {
		if(vBuffer[t.v1].z < cameraPos.z || vBuffer[t.v2].z < cameraPos.z || vBuffer[t.v3].z < cameraPos.z){
			return true;
		}else {
			return false;
		}
	}
		

}
