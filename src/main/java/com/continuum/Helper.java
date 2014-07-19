package com.continuum;

import org.lwjgl.Sys;
import org.lwjgl.util.vector.Vector2f;
import org.lwjgl.util.vector.Vector3f;
import org.lwjgl.util.vector.Vector4f;

/**
 * This is a simple helper class for various tasks.
 *
 * @author Gil Mendes <gil00mendes@gmail.com>
 */
public class Helper {

	static final float _div = 1.0f / 16.0f;
	private static long _timerTicksPerSecond = Sys.getTimerResolution();
	private static Helper _instance = null;

	/**
	 * Returns the static instance of this helper class.
	 *
	 * @return The instance
	 */
	public static Helper getInstance() {
		if (_instance == null) {
			_instance = new Helper();
		}

		return _instance;
	}

	/**
	 * Calculates the texture offset for a given position within
	 * the texture atlas.
	 *
	 * @param x X-coordinate
	 * @param y Y-coordinate
	 * @return The texture offset
	 */
	public Vector2f calcOffsetForTextureAt(int x, int y) {
		return new Vector2f(x * _div, y * _div);
	}

	/**
	 * Returns the spawning point of the player.
	 * TODO: Should not determine the spawning point randomly
	 *
	 * @return The coordinates of the spawning point
	 */
	public Vector3f calcPlayerOrigin() {
		return new Vector3f(Configuration.CHUNK_DIMENSIONS.x * Configuration.VIEWING_DISTANCE_IN_CHUNKS.x / 2, 127, (Configuration.CHUNK_DIMENSIONS.z * Configuration.VIEWING_DISTANCE_IN_CHUNKS.z) / 2);
	}

	/**
	 * Returns the system time.
	 *
	 * @return The system time
	 */
	public long getTime() {
		return (Sys.getTime() * 1000) / _timerTicksPerSecond;
	}

	/**
	 * Applies Cantor's pairing function on 2D coordinates.
	 *
	 * @param k1 X-Coordinate
	 * @param k2 Y-Coordinate
	 * @return Unique 1D value
	 */
	public int cantorize(int k1, int k2) {
		return ((k1 + k2) * (k1 + k2 + 1) / 2) + k2;
	}
}