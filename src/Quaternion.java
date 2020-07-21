public class Quaternion {
	private float m_x;
	private float m_y;
	private float m_z;
	private float m_w;

	public Quaternion(float x, float y, float z, float w) {
		this.m_x = x;
		this.m_y = y;
		this.m_z = z;
		this.m_w = w;
	}

	public Quaternion(Vector axis, float angle) {
		float sinHalfAngle = (float) Math.sin(angle / 2);
		float cosHalfAngle = (float) Math.cos(angle / 2);

		this.m_x = axis.getX() * sinHalfAngle;
		this.m_y = axis.getY() * sinHalfAngle;
		this.m_z = axis.getZ() * sinHalfAngle;
		this.m_w = cosHalfAngle;
	}

	// From Ken Shoemake's "Quaternion Calculus and Fast Animation" article
	// Constructor to create a quaternion from a rotation matrix
	public Quaternion(Matrix4 rot) {
		float trace = rot.get(0, 0) + rot.get(1, 1) + rot.get(2, 2);
		float s;
		if (trace > 0) {
			s = 0.5f / (float) Math.sqrt(trace + 1.0f);
			m_w = 0.25f / s;
			m_x = (rot.get(1, 2) - rot.get(2, 1)) * s;
			m_y = (rot.get(2, 0) - rot.get(0, 2)) * s;
			m_z = (rot.get(0, 1) - rot.get(1, 0)) * s;
		} else {
			if (rot.get(0, 0) > rot.get(1, 1) && rot.get(0, 0) > rot.get(2, 2)) {
				s = 2.0f * (float) Math.sqrt(1.0f + rot.get(0, 0) - rot.get(1, 1) - rot.get(2, 2));
				m_w = (rot.get(1, 2) - rot.get(2, 1)) / s;
				m_x = 0.25f * s;
				m_y = (rot.get(1, 0) + rot.get(0, 1)) / s;
				m_z = (rot.get(2, 0) + rot.get(0, 2)) / s;
			} else if (rot.get(1, 1) > rot.get(2, 2)) {
				s = 2.0f * (float) Math.sqrt(1.0f + rot.get(1, 1) - rot.get(0, 0) - rot.get(2, 2));
				m_w = (rot.get(2, 0) - rot.get(0, 2)) / s;
				m_x = (rot.get(1, 0) + rot.get(0, 1)) / s;
				m_y = 0.25f * s;
				m_z = (rot.get(2, 1) + rot.get(1, 2)) / s;
			} else {
				s = 2.0f * (float) Math.sqrt(1.0f + rot.get(2, 2) - rot.get(0, 0) - rot.get(1, 1));
				m_w = (rot.get(0, 1) - rot.get(1, 0)) / s;
				m_x = (rot.get(2, 0) + rot.get(0, 2)) / s;
				m_y = (rot.get(1, 2) + rot.get(2, 1)) / s;
				m_z = 0.25f * s;
			}
		}
		normalize();
	}

	public float length() {
		return (float) Math.sqrt(m_x * m_x + m_y * m_y + m_z * m_z + m_w * m_w);
	}

	public Quaternion normalized() {
		float length = length();

		return new Quaternion(m_x / length, m_y / length, m_z / length, m_w / length);
	}

	public void normalize() {
		float length = length();
		m_x /= length;
		m_y /= length;
		m_z /= length;
		m_w /= length;
	}

	public Quaternion conjugate() {
		return new Quaternion(-m_x, -m_y, -m_z, m_w);
	}

	public Quaternion mul(float r) {
		return new Quaternion(m_x * r, m_y * r, m_z * r, m_w * r);
	}

	public Quaternion mul(Quaternion r) {
		float w_ = m_w * r.getW() - m_x * r.getX() - m_y * r.getY() - m_z * r.getZ();
		float x_ = m_x * r.getW() + m_w * r.getX() + m_y * r.getZ() - m_z * r.getY();
		float y_ = m_y * r.getW() + m_w * r.getY() + m_z * r.getX() - m_x * r.getZ();
		float z_ = m_z * r.getW() + m_w * r.getZ() + m_x * r.getY() - m_y * r.getX();

		return new Quaternion(x_, y_, z_, w_);
	}

	public Quaternion mul(Vector r) {
		float w = -m_x * r.getX() - m_y * r.getY() - m_z * r.getZ();
		float x = m_w * r.getX() + m_y * r.getZ() - m_z * r.getY();
		float y = m_w * r.getY() + m_z * r.getX() - m_x * r.getZ();
		float z = m_w * r.getZ() + m_x * r.getY() - m_y * r.getX();

		return new Quaternion(x, y, z, w);
	}

	public Quaternion sub(Quaternion r) {
		return new Quaternion(m_x - r.getX(), m_y - r.getY(), m_z - r.getZ(), m_w - r.getW());
	}

	public Quaternion add(Quaternion r) {
		return new Quaternion(m_x + r.getX(), m_y + r.getY(), m_z + r.getZ(), m_w + r.getW());
	}

	public Matrix4 toRotationMatrix() {
		Vector forward = new Vector(2.0f * (m_x * m_z - m_w * m_y), 2.0f * (m_y * m_z + m_w * m_x),
				1.0f - 2.0f * (m_x * m_x + m_y * m_y), 1.0f);
		Vector up = new Vector(2.0f * (m_x * m_y + m_w * m_z), 1.0f - 2.0f * (m_x * m_x + m_z * m_z),
				2.0f * (m_y * m_z - m_w * m_x), 1.0f);
		Vector right = new Vector(1.0f - 2.0f * (m_y * m_y + m_z * m_z), 2.0f * (m_x * m_y - m_w * m_z),
				2.0f * (m_x * m_z + m_w * m_y), 1.0f);

		return new Matrix4().initRotation(forward, up, right);
	}

	public float dot(Quaternion r) {
		return m_x * r.getX() + m_y * r.getY() + m_z * r.getZ() + m_w * r.getW();
	}

	public Vector getForward() {
		return new Vector(0, 0, 1, 1).rotate(this);
	}

	public Vector getBack() {
		return new Vector(0, 0, -1, 1).rotate(this);
	}

	public Vector getUp() {
		return new Vector(0, 1, 0, 1).rotate(this);
	}

	public Vector getDown() {
		return new Vector(0, -1, 0, 1).rotate(this);
	}

	public Vector getRight() {
		return new Vector(1, 0, 0, 1).rotate(this);
	}

	public Vector getLeft() {
		return new Vector(-1.0f, 0.0f, 0.0f, 1.0f).rotate(this);
	}

	public float getX() {
		return m_x;
	}

	public float getY() {
		return m_y;
	}

	public float getZ() {
		return m_z;
	}

	public float getW() {
		return m_w;
	}
}
