class Matrix4 {
    float[][] values;
    Matrix4(float[][] values) {
        this.values = values;
    }
    Matrix4(){
    	this.values = new float[4][4];
    }
	public float get(int x, int y){
		return values[x][y];
	}
	public void initPerspective(float fov, float aspectRatio, float zNear, float zFar){
		float tanHalfFOV = (float)Math.tan(fov/2);
		float zRange = zNear-zFar;

		values[0][0]= aspectRatio*(1.0f/tanHalfFOV); values[0][1]= 0; values[0][2]= 0;	values[0][3]= 0;
		values[1][0]= 0; values[1][1]= 1.0f/tanHalfFOV; values[1][2]= 0;	values[1][3]= 0;
		values[2][0]= 0; values[2][1]= 0;	values[2][2]= (-zNear -zFar)/zRange; values[2][3]= 2*zFar*zNear/zRange;
		values[3][0]= 0; values[3][1]= 0;	values[3][2]= 1; values[3][3]= 0;		
	}
	
	public Matrix4 initRotation(Vector v, float angle){
		float x=v.x;
		float y=v.y;
		float z=v.z;
		float sin = (float)Math.sin(angle);
		float cos = (float)Math.cos(angle);

		values[0][0] = cos+x*x*(1-cos); values[0][1] = x*y*(1-cos)-z*sin; values[0][2] = x*z*(1-cos)+y*sin; values[0][3] = 0;
		values[1][0] = y*x*(1-cos)+z*sin; values[1][1] = cos+y*y*(1-cos);	values[1][2] = y*z*(1-cos)-x*sin; values[1][3] = 0;
		values[2][0] = z*x*(1-cos)-y*sin; values[2][1] = z*y*(1-cos)+x*sin; values[2][2] = cos+z*z*(1-cos); values[2][3] = 0;
		values[3][0] = 0;	values[3][1] = 0;	values[3][2] = 0;	values[3][3] = 1;

		return this;
	}
	
	public Matrix4 initRotation(Vector forward, Vector up, Vector right){
		Vector f = forward;
		Vector r = right;
		Vector u = up;

		values[0][0] = r.getX();  values[0][1] = r.getY();	values[0][2] = r.getZ();  values[0][3] = 0;
		values[1][0] = u.getX();  values[1][1] = u.getY();	values[1][2] = u.getZ();  values[1][3] = 0;
		values[2][0] = f.getX();  values[2][1] = f.getY();	values[2][2] = f.getZ();  values[2][3] = 0;
		values[3][0] = 0;		  values[3][1] = 0;			values[3][2] = 0;		  values[3][3] = 1;

		return this;
	}
	
	
	public Matrix4 initPointAt(Vector pos, Vector target, Vector up){
		// Calculate new forward direction
		Vector newForward = target.sub(pos);
		newForward.normalize();

		// Calculate new Up direction
		Vector a = newForward.mul(up.dot(newForward));
		Vector newUp = up.sub(a);
		newUp.normalize();

		// New Right direction is easy, its just cross product
		Vector newRight = newUp.cross(newForward);

		// Construct Dimensioning and Translation Matrix	
		Matrix4 matrix= new Matrix4();
		matrix.values[0][0] = newRight.x;	matrix.values[0][1] = newRight.y;	matrix.values[0][2] = newRight.z;	matrix.values[0][3] = 0;
		matrix.values[1][0] = newUp.x;		matrix.values[1][1] = newUp.y;		matrix.values[1][2] = newUp.z;		matrix.values[1][3] = 0;
		matrix.values[2][0] = newForward.x;	matrix.values[2][1] = newForward.y;	matrix.values[2][2] = newForward.z;	matrix.values[2][3] = 0;
		matrix.values[3][0] = pos.x;		matrix.values[3][1] = pos.y;		matrix.values[3][2] = pos.z;		matrix.values[3][3] = 1;
		return matrix;
	}
	
	public Matrix4 invertMatrix() { // Only for Rotation/Translation Matrices
		Matrix4 matrix = new Matrix4();
		matrix.values[0][0] = this.values[0][0]; matrix.values[0][1] = this.values[1][0]; matrix.values[0][2] = this.values[2][0]; matrix.values[0][3] = 0;
		matrix.values[1][0] = this.values[0][1]; matrix.values[1][1] = this.values[1][1]; matrix.values[1][2] = this.values[2][1]; matrix.values[1][3] = 0;
		matrix.values[2][0] = this.values[0][2]; matrix.values[2][1] = this.values[1][2]; matrix.values[2][2] = this.values[2][2]; matrix.values[2][3] = 0;
		matrix.values[3][0] = -(this.values[3][0] * matrix.values[0][0] + this.values[3][1] * matrix.values[1][0] + this.values[3][2] * matrix.values[2][0]);
		matrix.values[3][1] = -(this.values[3][0] * matrix.values[0][1] + this.values[3][1] * matrix.values[1][1] + this.values[3][2] * matrix.values[2][1]);
		matrix.values[3][2] = -(this.values[3][0] * matrix.values[0][2] + this.values[3][1] * matrix.values[1][2] + this.values[3][2] * matrix.values[2][2]);
		matrix.values[3][3] = 1;
		return matrix;
	}
	
	
	public Matrix4 initTranslation(Vector v) {
		values[0][0] = 1;	values[0][1] = 0;	values[0][2] = 0;	values[0][3] = v.x;
		values[1][0] = 0;	values[1][1] = 1;	values[1][2] = 0;	values[1][3] = v.y;
		values[2][0] = 0;	values[2][1] = 0;	values[2][2] = 1;	values[2][3] = v.z;
		values[3][0] = 0;	values[3][1] = 0;	values[3][2] = 0;	values[3][3] = 1;
		return this;
	}
	
	public Matrix4 initTranslation(float x, float y, float z) {
		values[0][0] = 1;	values[0][1] = 0;	values[0][2] = 0;	values[0][3] = x;
		values[1][0] = 0;	values[1][1] = 1;	values[1][2] = 0;	values[1][3] = y;
		values[2][0] = 0;	values[2][1] = 0;	values[2][2] = 1;	values[2][3] = z;
		values[3][0] = 0;	values[3][1] = 0;	values[3][2] = 0;	values[3][3] = 1;
		return this;
	}
	
    Matrix4 multiply(Matrix4 other) {
        float[][] result = new float[4][4];
        for (byte row = 0; row < 4; ++row) {
            for (byte col = 0; col < 4; ++col) {
                for (byte i = 0; i < 4; ++i) {
                	result[row][col] +=
                        this.values[row][i] * other.values[i][col];
                }
            }
        }
        return new Matrix4(result);
    }
    

    Vector transform(Vector in) {
        return new Vector(
               in.x * values[0][0] + in.y * values[1][0] + in.z * values[2][0] + in.w * values[3][0],
               in.x * values[0][1] + in.y * values[1][1] + in.z * values[2][1] + in.w * values[3][1],
               in.x * values[0][2] + in.y * values[1][2] + in.z * values[2][2] + in.w * values[3][2],
               in.x * values[0][3] + in.y * values[1][3] + in.z * values[2][3] + in.w * values[3][3]);
    }
}