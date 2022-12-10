import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

import java.awt.Color;

public class Importer {
	private Vector[] vBuffer;
	public Importer() {}
	
	public Vector[] getVertexBuffer() {
		return vBuffer;
	}
	
	public ArrayList<Triangle> importModel(String filelocation) {
		ArrayList<Triangle>tris = new ArrayList<Triangle>();
		ArrayList<Vector> vertices= new ArrayList<Vector>();
		Triangle temp;
		File file = new File(filelocation);
		if (!file.isFile()) return null;
		try (BufferedReader br = new BufferedReader(new FileReader(file))) {
			String st; 
			String[] elems;
			while ((st = br.readLine()) != null) {
			    elems=st.split(" ");
			    if(elems[0].equals("v")) {
			    	vertices.add(new Vector(		
			    					Float.parseFloat(elems[1])*0.25f,
			    					Float.parseFloat(elems[2])*0.25f,
			    					Float.parseFloat(elems[3])*0.25f, 1.0f));
			    }else if(elems[0].equals("f")){
			    	if(elems.length>4)
			    	{
			    		throw new IOException("Quad based meshes are not supported");
			    	}    		
			    	
			    	temp= new Triangle(
			    			(Integer.parseInt(elems[1].split("/")[0])-1),
			    			(Integer.parseInt(elems[2].split("/")[0])-1),
			    			(Integer.parseInt(elems[3].split("/")[0])-1),
			    			Color.WHITE);
			    	
			    	tris.add(temp);
			    }	    
			}
			br.close();
			vBuffer= vertices.toArray(new Vector[vertices.size()]);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return tris;
	}
	
	public ArrayList<Triangle> importDefault(){
		ArrayList<Triangle>tris = new ArrayList<Triangle>();
		vBuffer = new Vector[4];
		
		vBuffer[0]= new Vector(1, 1, 1, 1);
		vBuffer[1]= new Vector(-1, 1, -1, 1);
		vBuffer[2]= new Vector(-1, -1, 1, 1);
		vBuffer[3]= new Vector(1, -1, -1, 1);
		
		tris.add(new Triangle(0, 1, 2, Color.WHITE));
		tris.add(new Triangle(0, 2, 3, Color.RED));
		tris.add(new Triangle(1, 0, 3, Color.GREEN));
		tris.add(new Triangle(1, 3, 2, Color.BLUE));

		for (byte i=0; i<5; ++i) {
			tris=inflate(tris);
		}
		return tris;
	}
	
    public ArrayList<Triangle> inflate(ArrayList<Triangle> tris) {
        ArrayList<Triangle> result = new ArrayList<Triangle>();
        ArrayList<Vector> tVertexBuffer= new ArrayList<Vector>();
        for (int i=0; i<vBuffer.length; ++i) {
        	tVertexBuffer.add(vBuffer[i]);
        }
        //loop through existing triangles
        for (Triangle t : tris) {
        	//get verts
        	Vector v1,v2,v3;
        	v1=vBuffer[t.v1];
        	v2=vBuffer[t.v2];
        	v3=vBuffer[t.v3];
        	
        	//get three new vertices inside the existing triangle
            Vector m1 =
                new Vector((v1.x + v2.x)/2, (v1.y + v2.y)/2, (v1.z + v2.z)/2, 1);
            Vector m2 =
                new Vector((v2.x + v3.x)/2, (v2.y + v3.y)/2, (v2.z + v3.z)/2, 1);
            Vector m3 =
                new Vector((v1.x + v3.x)/2, (v1.y + v3.y)/2, (v1.z + v3.z)/2, 1);
            
            tVertexBuffer.add(m1);
            tVertexBuffer.add(m2);
            tVertexBuffer.add(m3);
            int last=tVertexBuffer.size()-1;
            
            //use the new vertices to create 4 new triangles (that form a tetrahedron)
            result.add(new Triangle(t.v1, last-2, last, t.color));
            result.add(new Triangle(last-1, last-2, t.v2, t.color));
            result.add(new Triangle(last, last-1, t.v3, t.color));
            result.add(new Triangle(last-2, last-1, last, t.color));
        }
        
        //set vBuffer to the new buffer
        vBuffer=tVertexBuffer.toArray(new Vector[0]);
        //loop through the vertices of the triangle and use a sphere radius to 'inflate' the new tetrahedron
        double sqrt1 = Math.sqrt(1);
        for (Vector v : vBuffer) {
                double l = Math.sqrt(v.x * v.x + v.y * v.y + v.z * v.z) / sqrt1;
                v.x /= l;
                v.y /= l;
                v.z /= l;
        }
        return result;
    }
}
