package com.continuum;

import org.lwjgl.util.vector.Vector3f;

/**
 * Represents a intersection of a ray with the face of a block.
 *
 * @author Gil Mendes <gil00mendes@gmail.com>
 */
public class RayFaceIntersection implements Comparable<RayFaceIntersection> {

	public enum SIDE {

		FRONT, BACK, LEFT, RIGHT, TOP, BOTTOM, NONE
	}
	private Vector3f v0, v1, v2;
	private float d;
	private float t;
	private Vector3f origin;
	private Vector3f ray;
	private Vector3f intersectPoint;
	private Vector3f blockPos;

	public RayFaceIntersection(Vector3f blockPos, Vector3f v0, Vector3f v1, Vector3f v2, float d, float t, Vector3f origin, Vector3f ray, Vector3f intersectPoint) {
		this.d = d;
		this.t = t;
		this.origin = origin;
		this.ray = ray;
		this.intersectPoint = intersectPoint;
		this.v0 = v0;
		this.v1 = v1;
		this.v2 = v2;
		this.blockPos = blockPos;
	}

	@Override
	public int compareTo(RayFaceIntersection o) {
		return new Float(Math.abs(getT())).compareTo(Math.abs(o.getT()));
	}

	/**
	 * @return the d
	 */
	public float getD() {
		return d;
	}

	/**
	 * @return the t
	 */
	public float getT() {
		return t;
	}

	/**
	 * @return the origin
	 */
	public Vector3f getOrigin() {
		return origin;
	}

	/**
	 * @return the ray
	 */
	public Vector3f getRay() {
		return ray;
	}

	/**
	 * @return the intersectPoint
	 */
	public Vector3f getIntersectPoint() {
		return intersectPoint;
	}

	public SIDE calcSide() {
		Vector3f norm = calcSurfaceNormal();

		if (norm.equals(new Vector3f(0, 1, 0))) {
			return SIDE.TOP;
		} else if (norm.equals(new Vector3f(0, -1, 0))) {
			return SIDE.BOTTOM;
		} else if (norm.equals(new Vector3f(0, 0, 1))) {
			return SIDE.BACK;
		} else if (norm.equals(new Vector3f(0, 0, -1))) {
			return SIDE.FRONT;
		} else if (norm.equals(new Vector3f(1, 0, 0))) {
			return SIDE.RIGHT;
		} else if (norm.equals(new Vector3f(-1, 0, 0))) {
			return SIDE.LEFT;
		}

		return SIDE.NONE;
	}

	public Vector3f calcSurfaceNormal() {
		Vector3f a = Vector3f.sub(v1, v0, null);
		Vector3f b = Vector3f.sub(v2, v0, null);
		Vector3f norm = Vector3f.cross(a, b, null);

		return norm;
	}

	public Vector3f calcAdjacentBlockPos() {
		return Vector3f.add(getBlockPos(), calcSurfaceNormal(), null);
	}

	/**
	 * @return the blockPos
	 */
	public Vector3f getBlockPos() {
		return blockPos;
	}
}