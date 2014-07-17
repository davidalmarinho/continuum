package com.continuum;

import org.lwjgl.Sys;
import org.lwjgl.util.vector.Vector2f;
import org.lwjgl.util.vector.Vector3f;

/**
 * Created by gil0mendes on 12/07/14.
 */
public class Helper {
	// Helper instance
	private static Helper instance = null;

	private static long timerTicksPerSecond = Sys.getTimerResolution();

	public static enum SIDE {
		LEFT, RIGHT, TOP, BOTTOM, FRONT, BACK
	}

	public static final float div = 1.0f / 16.0f;

	/**
	 * Get a helper instance.
	 *
	 * @return Helper instance
	 */
	public static Helper getInstance() {
		if (instance == null) {
			instance = new Helper();
		}

		return instance;
	}

	// ----

	/**
	 * Constructor
	 */
	public Helper() {}

	public Vector2f calcOffsetForTextureAt(int x, int y) {
		return new Vector2f(x * div, y * div);
	}

	public Vector2f getTextureOffsetFor(int type, SIDE side) {
		switch (type) {
			// Grass block
			case 0x1:
				if (side == SIDE.LEFT || side == SIDE.RIGHT || side == SIDE.FRONT || side == SIDE.BACK) {
					return calcOffsetForTextureAt(3, 0);
				} else if (side == SIDE.TOP) {
					return calcOffsetForTextureAt(0, 0);
				}
				break;
			// Dirt block
			case 0x2:
				return calcOffsetForTextureAt(2, 0);
			// Stone block
			case 0x3:
				return calcOffsetForTextureAt(1, 0);
			// Water block
			case 0x4:
				return calcOffsetForTextureAt(15, 13);
			// Tree block
			case 0x5:
				if (side == SIDE.LEFT || side == SIDE.RIGHT || side == SIDE.FRONT || side == SIDE.BACK) {
					return calcOffsetForTextureAt(4, 1);
				} else if (side == SIDE.TOP || side == SIDE.BOTTOM) {
					return calcOffsetForTextureAt(5, 1);
				}
				break;
			// Leaf block
			case 0x6:
				return calcOffsetForTextureAt(4, 3);
			// Sand block
			case 0x7:
				return calcOffsetForTextureAt(2, 1);
			// Dirt block is the default
			default:
				return calcOffsetForTextureAt(2, 0);
		}

		return calcOffsetForTextureAt(2, 0);
	}

	public Vector3f getColorOffsetFor(int type, SIDE side) {
		switch (type) {
			// Grass block
			case 0x1:
				if (side == SIDE.TOP) {
					return new Vector3f(204f / 255f, 255f / 255f, 25f / 255f);
				}
				break;
			// Lead block
			case 0x6:
				return new Vector3f(159f / 255f, 235f / 255f, 89f / 255f);
		}
		return new Vector3f(1.0f, 1.0f, 1.0f);
	}

	public boolean isBlockTypeTranslucent(int type) {
		switch (type) {
			// Grass block
			case 0x6:
				return true;
			default:
				return false;
		}
	}

	public Vector3f calcPlayerOrigin() {
		return new Vector3f(Chunk.CHUNK_DIMENSIONS.x * Configuration._viewingDistanceInChunks.x / 2, 127, (Chunk.CHUNK_DIMENSIONS.z * Configuration._viewingDistanceInChunks.z) / 2);
	}

	public long getTime() {
		return (Sys.getTime() * 1000) / timerTicksPerSecond;
	}

	public int cantorize(int k1, int k2) {
		return ((k1 + k2) * (k1 + k2 + 1) / 2) + k2;
	}
}