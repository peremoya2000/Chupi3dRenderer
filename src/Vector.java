class Vector {
	float x;
	float y;
	float z;
	float w;

	Vector(float x, float y, float z, float w) {
		this.x = x;
		this.y = y;
		this.z = z;
		this.w = w;
	}

	public float length() {
		return (float) Math.sqrt(x * x + y * y + z * z);
	}

	public float dot(Vector r) {
		return x * r.getX() + y * r.getY() + z * r.getZ();
	}

	public Vector cross(Vector r) {
		float x_ = y * r.getZ() - z * r.getY();
		float y_ = z * r.getX() - x * r.getZ();
		float z_ = x * r.getY() - y * r.getX();

		return new Vector(x_, y_, z_, 1);
	}

	public Vector normalized() {
		float length = length();

		return new Vector(x / length, y / length, z / length, w);
	}
	
	public void normalize() {
		float length = length();
		this.x/=length;
		this.y/=length;
		this.z/=length;
	}
	
	public Vector rotate(Vector axis, float angle) {
		float sinAngle = (float) Math.sin(-angle);
		float cosAngle = (float) Math.cos(-angle);

		return this.cross(axis.mul(sinAngle)).add( 	// Rotation on local X
				(this.mul(cosAngle)).add( 			// Rotation on local Z
				axis.mul(this.dot(axis.mul(1 - cosAngle))))); // Rotation on local Y
	}

	public Vector rotate(Quaternion rotation) {
		Quaternion conjugate = rotation.conjugate();
		Quaternion w = rotation.mul(this).mul(conjugate);

		return new Vector(w.getX(), w.getY(), w.getZ(), 1);
	}



	public Vector add(Vector r) {
		return new Vector(x+r.getX(), y+r.getY(), z+r.getZ(), w);
	}

	public Vector add(float r) {
		return new Vector(x + r, y + r, z + r, w);
	}

	public Vector sub(Vector r) {
		return new Vector(x - r.getX(), y - r.getY(), z - r.getZ(), w);
	}

	public Vector sub(float r) {
		return new Vector(x - r, y - r, z - r, w);
	}

	public Vector mul(Vector r) {
		return new Vector(x * r.getX(), y * r.getY(), z * r.getZ(), w);
	}

	public Vector mul(float r) {
		return new Vector(x * r, y * r, z * r, w);
	}
	
	public void xyscale(float r) {
		this.x *= r;
		this.y *= r;
	}

	public Vector div(Vector r) {
		return new Vector(x / r.getX(), y / r.getY(), z / r.getZ(), w);
	}

	public Vector div(float r) {
		return new Vector(x / r, y / r, z / r, w);
	}

	public Vector abs() {
		return new Vector(Math.abs(x), Math.abs(y), Math.abs(z), Math.abs(w));
	}

	public float getX() {
		return x;
	}

	public float getY() {
		return y;
	}

	public float getZ() {
		return z;
	}

	public float getW() {
		return w;
	}

	public boolean equals(Vector r) {
		return x == r.getX() && y == r.getY() && z == r.getZ() && w == r.getW();
	}
}
