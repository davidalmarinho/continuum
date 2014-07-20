package com.continuum.utilities;

import org.lwjgl.Sys;
import org.lwjgl.util.vector.Vector2f;

import java.io.IOException;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

/**
 * This is a simple helper class for various tasks.
 */
public class Helper {

	/**
	 * The logger used for managing and creating the default log file.
	 */
	public static final Logger LOGGER = Logger.getLogger("continuum");
	/* ---------- */
	private static final float _div = 1.0f / 16.0f;
	private static final long _timerTicksPerSecond = Sys.getTimerResolution();
	/* ---------- */
	private static Helper _instance = null;

	static {
		try {
			FileHandler fh = new FileHandler("continuum.log", true);
			fh.setFormatter(new SimpleFormatter());
			LOGGER.addHandler(fh);
		} catch (IOException ex) {
			LOGGER.log(Level.WARNING, ex.toString(), ex);
		}
	}

	/**
	 * Returns (and creates – if necessary) the static instance
	 * of this helper class.
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
	 * Returns the system time in milliseconds.
	 *
	 * @return The system time in milliseconds.
	 */
	public long getTime() {
		return (Sys.getTime() * 1000) / _timerTicksPerSecond;
	}


	/**
	 * Tests if a given position is within the bounds of a given 3D array.
	 *
	 * @param x     X-position
	 * @param y     Y-position
	 * @param z     Z-position
	 * @param array The array
	 * @return True if the position is within the bounds of the array
	 */
	public boolean checkBounds3D(int x, int y, int z, byte[][][] array) {
		int length1 = array.length;

		if (x < 0 || x >= length1) {
			return false;
		}

		int length2 = array[x].length;

		if (y < 0 || y >= length2) {
			return false;
		}

		int length3 = array[x][y].length;

		return !(z < 0 || z >= length3);

	}

	/**
	 * Returns true if the flag at given byte position
	 * is set.
	 *
	 * @param value Byte value storing the flags
	 * @param index Index position of the flag
	 * @return True if the flag is set
	 */
	public boolean isFlagSet(byte value, short index) {
		return (value & (1 << index)) != 0;
	}

	/**
	 * Sets a flag at a given byte position.
	 *
	 * @param value Byte value storing the flags
	 * @param index Index position of the flag
	 * @return The byte value containing the modified flag
	 */
	public byte setFlag(byte value, short index) {
		return (byte) (value | (1 << index));
	}
}