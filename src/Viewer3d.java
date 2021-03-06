import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.KeyEvent;
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
	private BufferedImage img;
	private int width, height, sleep;
	private Graphics2D g2d;
	private float[][] zBuffer;
	private float fov, ratio, camYaw;
	private Matrix4 projMatrix;
	private Input input;
	private final Vector up = new Vector(0, 1, 0, 0);
	private final Vector right = new Vector(1, 0, 0, 0);
	// X GOES RIGHT
	// Y GOES UP
	// Z GOES TOWARDS CAMERA
	private final Vector forward = new Vector(0, 0, -1, 0);
	private Vector lightDir = new Vector(0, -1, 1, 0);
	private Vector cameraLookVec = new Vector(0, 0, -1, 0);
	private Vector cameraPos = new Vector(0, 0, 5, 0);

	public Viewer3d(JSlider slider1, JSlider slider2, JSlider slider3, JSlider slider4, Input in) {
		this.headingSlider = slider1;
		this.pitchSlider = slider2;
		this.rollSlider = slider3;
		this.fovSlider = slider4;
		this.input = in;
		this.go=true;
		this.fov = (float) Math.toRadians(90);
		this.ratio = 1.0f;
		this.camYaw=0;
		this.tris = new ArrayList<Triangle>();
		this.myThread = new Thread(this);
		this.sleep = 33;
		this.projMatrix = new Matrix4();
		this.lightDir.normalize3d();

		Importer i = new Importer();
		this.tris=i.importDefault();
		//this.tris = i.importModel("C:\\Users\\Usuario\\Desktop\\ship.obj");
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
				TimeUnit.MILLISECONDS.sleep(60);
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
					if (input.GetKey(KeyEvent.VK_A)) {
						camYaw+=0.1f;
					}
					if (input.GetKey(KeyEvent.VK_D)) {
						camYaw-=0.1f;
					}
					if (input.GetKey(KeyEvent.VK_W)) {
						cameraPos=cameraPos.add(cameraLookVec);
						System.out.println(cameraPos.z);
					}
					if (input.GetKey(KeyEvent.VK_S)) {
						//cameraPos.z+=0.1f;
						cameraPos=cameraPos.sub(cameraLookVec);
						System.out.println(cameraPos.z);
					}
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}else {
				System.out.println("pause");
				try {
					Thread.sleep(80);
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
		
		// create new zBuffer of the right size
		zBuffer = new float[width*4][height*4];
		// initialize zBuffer
		for (int q = 0; q < height; ++q) {
			if(!go)break;
			for (int r = 0; r < width; ++r) {
				zBuffer[q][r] = Float.NEGATIVE_INFINITY;
			}
		}

		// loop through all the the triangles in the scene
		for (Triangle t : tris) {
			if(!go)break;
			
			// Apply rotation to the three vertices of the triangle
			Vector v1 = t.v1.rotate(rotQuaternion);
			Vector v2 = t.v2.rotate(rotQuaternion);
			Vector v3 = t.v3.rotate(rotQuaternion);
			
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
			Vector norm = new Vector(ab.y * ac.z - ab.z * ac.y, ab.z * ac.x - ab.x * ac.z, ab.x * ac.y - ab.y * ac.x,1);
			
			// Normalize normal vector
			norm.normalize3d();
			
			if (norm.dot(cameraLookVec)<0.0f&&inFrontOfCamera(cameraLookVec, cameraPos, t, norm)) {
				// Get light incidence angle
				float angleCos = Math.max(0.1f, norm.dot(lightDir));

				// Translate the unitary coordinates to the size of the screen
				++v1.x;
				++v1.y;
				++v2.x;
				++v2.y;
				++v3.x;
				++v3.y;
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

	public void paintComponent(Graphics g) {
	}

	public Color getShade(Color color, float shade) {
		// apply shading to the pixel
		float redLinear = (color.getRed() * color.getRed()) * shade;
		float greenLinear = (color.getGreen() * color.getGreen()) * shade;
		float blueLinear = (color.getBlue() * color.getBlue()) * shade;
		// convert the resulting colour from linear to sRGB
		short red = (short) Math.sqrt(redLinear);
		short green = (short) Math.sqrt(greenLinear);
		short blue = (short) Math.sqrt(blueLinear);

		return new Color(red, green, blue);
	}
	
	public boolean inFrontOfCamera(Vector cameraVec, Vector cameraPos, Triangle t, Vector normal) {
		if(cameraVec.dot(t.v1.sub(cameraPos)) > 0.0f || cameraVec.dot(t.v2.sub(cameraPos)) > 0.0f || cameraVec.dot(t.v3.sub(cameraPos)) > 0.0f) {
			return true;
		}else {
			return false;
		}
	}
	
	public Vector intersectPlane(Vector plane_p, Vector plane_n, Vector lineStart, Vector lineEnd){
		plane_n.normalize3d();
		float plane_d = -plane_n.dot(plane_p);
		float ad = lineStart.dot(plane_n);
		float bd = lineEnd.dot(plane_n);
		float t = (-plane_d - ad) / (bd - ad);
		Vector lineStartToEnd = lineEnd.sub(lineStart);
		Vector lineToIntersect = lineStartToEnd.mul(t);
		return lineStart.add(lineToIntersect);
	}
	
	
	// Return signed shortest distance from point to plane, plane normal must be normalised
	public float dist(Vector p, Vector plane_n, Vector plane_p) {
		p.normalize3d();
		return (plane_n.x * p.x + plane_n.y * p.y + plane_n.z * p.z - plane_n.dot(plane_p));
	}
	
	public int clipAgainstPlane(Vector plane_p, Vector plane_n, Triangle in_tri, Triangle out_tri1, Triangle out_tri2){
		// Make sure plane normal is indeed normal
		plane_n.normalize3d();

		// Create two temporary storage arrays to classify points either side of plane
		// If distance sign is positive, point lies on "inside" of plane
		Vector inside_points[]= new Vector[3];  int nInsidePointCount = 0;
		Vector outside_points[]= new Vector[3]; int nOutsidePointCount = 0;

		// Get signed distance of each point in triangle to plane
		float d0 = dist(in_tri.v1,plane_n,plane_p);
		float d1 = dist(in_tri.v2,plane_n,plane_p);
		float d2 = dist(in_tri.v3,plane_n,plane_p);

		if (d0 >= 0) { inside_points[nInsidePointCount++] = in_tri.v1; }
		else { outside_points[nOutsidePointCount++] = in_tri.v1; }
		if (d1 >= 0) { inside_points[nInsidePointCount++] = in_tri.v2; }
		else { outside_points[nOutsidePointCount++] = in_tri.v2; }
		if (d2 >= 0) { inside_points[nInsidePointCount++] = in_tri.v3; }
		else { outside_points[nOutsidePointCount++] = in_tri.v3; }

		// Now classify triangle points, and break the input triangle into 
		// smaller output triangles if required. There are four possible
		// outcomes...

		if (nInsidePointCount == 0){
			// All points lie on the outside of plane, so clip whole triangle
			// It ceases to exist

			return 0; // No returned triangles are valid
		}else if (nInsidePointCount == 3){
			// All points lie on the inside of plane, so do nothing
			// and allow the triangle to simply pass through
			out_tri1 = in_tri;

			return 1; // Just the one returned original triangle is valid
		}else if (nInsidePointCount == 1 && nOutsidePointCount == 2){
			// Triangle should be clipped. As two points lie outside
			// the plane, the triangle simply becomes a smaller triangle

			// Copy appearance info to new triangle
			out_tri1.color =  in_tri.color;

			// The inside point is valid, so keep that...
			out_tri1.v1 = inside_points[0];

			// but the two new points are at the locations where the 
			// original sides of the triangle (lines) intersect with the plane
			out_tri1.v2 = intersectPlane(plane_p, plane_n, inside_points[0], outside_points[0]);
			out_tri1.v3 = intersectPlane(plane_p, plane_n, inside_points[0], outside_points[1]);

			return 1; // Return the newly formed single triangle
		}else if (nInsidePointCount == 2 && nOutsidePointCount == 1){
			// Triangle should be clipped. As two points lie inside the plane,
			// the clipped triangle becomes a "quad". Fortunately, we can
			// represent a quad with two new triangles

			// Copy appearance info to new triangles
			out_tri1.color =  in_tri.color;

			out_tri2.color =  in_tri.color;

			// The first triangle consists of the two inside points and a new
			// point determined by the location where one side of the triangle
			// intersects with the plane
			out_tri1.v1 = inside_points[0];
			out_tri1.v2 = inside_points[1];
			out_tri1.v3 = intersectPlane(plane_p, plane_n, inside_points[0], outside_points[0]);

			// The second triangle is composed of one of he inside points, a
			// new point determined by the intersection of the other side of the 
			// triangle and the plane, and the newly created point above
			out_tri2.v1 = inside_points[1];
			out_tri2.v2 = out_tri1.v3;
			out_tri2.v3 = intersectPlane(plane_p, plane_n, inside_points[1], outside_points[0]);

			return 2; // Return two newly formed triangles which form a quad
		}else {return 0;}
	}
	

}
