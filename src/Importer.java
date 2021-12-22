import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

import java.awt.Color;

public class Importer {
	public Importer() {}
	
	public ArrayList<Triangle> importModel(String filelocation) {
		ArrayList<Triangle>tris = new ArrayList<Triangle>();
		ArrayList<Vector> vertices= new ArrayList<Vector>();
		Triangle temp;
		File file = new File(filelocation);
		if (!file.isFile()) return null;
		try {
			BufferedReader br = new BufferedReader(new FileReader(file));
			String st; 
			String[] elems;
			while ((st = br.readLine()) != null) {
			    elems=st.split(" ");
			    if(elems[0].equals("v")) {
			    	vertices.add(new Vector(		
			    					Float.parseFloat(elems[1])*0.25f,
			    					Float.parseFloat(elems[2])*0.25f,
			    					Float.parseFloat(elems[3])*0.25f,1.0f));
			    }else if(elems[0].equals("f")){
			    	temp= new Triangle(
			    			vertices.get(Integer.parseInt(elems[1])-1),
			    			vertices.get(Integer.parseInt(elems[2])-1),
			    			vertices.get(Integer.parseInt(elems[3])-1),
			    			Color.WHITE);
			    	tris.add(temp);
			    }	    
			}
			br.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}		
		return tris;
	}
	
	public ArrayList<Triangle> importDefault(){
		ArrayList<Triangle>tris = new ArrayList<Triangle>();
		tris.add(new Triangle(new Vector(1, 1, 1, 1),
				new Vector(-1, 1, -1, 1),
				new Vector(-1, -1, 1, 1),
				Color.WHITE));
		tris.add(new Triangle(new Vector(1, 1, 1, 1),
				new Vector(-1, -1, 1, 1),
				new Vector(1, -1, -1, 1),
				Color.RED));
		tris.add(new Triangle(new Vector(-1, 1, -1, 1),		           
				new Vector(1, 1, 1, 1),
				new	Vector(1, -1, -1, 1),
				Color.GREEN));
		tris.add(new Triangle(new Vector(-1, 1, -1, 1),
				new Vector(1, -1, -1, 1),
				new Vector(-1, -1, 1, 1),
				Color.BLUE));

		for (byte i=0; i<5; ++i) {
			tris=inflate(tris);
		}
		return tris;
	}
	
    public ArrayList<Triangle> inflate(ArrayList<Triangle> tris) {
        ArrayList<Triangle> result = new ArrayList<Triangle>();
        //loop through existing triangles
        for (Triangle t : tris) {
        	//get three new vertices inside the existing triangle
            Vector m1 =
                new Vector((t.v1.x + t.v2.x)/2, (t.v1.y + t.v2.y)/2, (t.v1.z + t.v2.z)/2, 1);
            Vector m2 =
                new Vector((t.v2.x + t.v3.x)/2, (t.v2.y + t.v3.y)/2, (t.v2.z + t.v3.z)/2, 1);
            Vector m3 =
                new Vector((t.v1.x + t.v3.x)/2, (t.v1.y + t.v3.y)/2, (t.v1.z + t.v3.z)/2, 1);
            //use the new vertices to create 4 new triangles (that form a tetrahedron)
            result.add(new Triangle(t.v1, m1, m3, t.color));
            result.add(new Triangle(m2, m1, t.v2, t.color));
            result.add(new Triangle(m3, m2, t.v3, t.color));
            result.add(new Triangle(m1, m2, m3, t.color));
        }
        //loop through the vertices of the triangle and use a sphere radius to 'inflate' the new tetrahedron
        double sqrt1 = Math.sqrt(1);
        for (Triangle t : result) {
            for (Vector v : new Vector[] { t.v1, t.v2, t.v3 }) {
                double l = Math.sqrt(v.x * v.x + v.y * v.y + v.z * v.z) / sqrt1;
                v.x /= l;
                v.y /= l;
                v.z /= l;
            }
        }
        return result;
    }
}
