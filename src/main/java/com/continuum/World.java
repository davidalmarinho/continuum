package com.continuum;

import org.lwjgl.util.vector.Vector3f;

import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * World class
 * <p/>
 * Created by gil0mendes on 12/07/14.
 */
public class World extends RenderObject {
	// Daylight intensity
	private float daylight = 0.75f;

	private Random rand;

	// Used for updating/generating the world
	private Thread updateThread = null;

	// The chunks to display
	private Chunk[][][] chunks;
	private Set<Chunk> chunkUpdateQueue = new HashSet<Chunk>();

	// Logger
	private static final Logger LOGGER = Logger.getLogger(Main.class.getName());

	// Day & Night
	private Timer dayNight = new Timer();

	// The perlin noise generator used
	// for creating the procedural terrain
	private PerlinNoise pGen;

	// Player
	private Player player = null;

	/**
	 * Init. world
	 *
	 * @param title
	 * @param seed
	 */
	public World(String title, String seed, Player p) {
		this.player = p;
		rand = new Random(seed.hashCode());
		pGen = new PerlinNoise(seed);

		chunks = new Chunk[(int) Configuration.viewingDistanceInChunks.x][(int) Configuration.viewingDistanceInChunks.y][(int) Configuration.viewingDistanceInChunks.z];

		updateThread = new Thread(new Runnable() {

			@Override
			public void run() {

				long timeStart = System.currentTimeMillis();

				LOGGER.log(Level.INFO, "Generating chunks.");

				for (int x = 0; x < Configuration.viewingDistanceInChunks.x; x++) {
					for (int y = 0; y < Configuration.viewingDistanceInChunks.y; y++) {
						for (int z = 0; z < Configuration.viewingDistanceInChunks.z; z++) {
							generateChunk(new Vector3f(x, y, z));
						}
					}
				}

				LOGGER.log(Level.INFO, "World updated ({0}s).", (System.currentTimeMillis() - timeStart) / 1000d);
			}
		});
	}

	public void init() {
		updateThread.start();
	}

	/*
	 * Update the world.
	 */
	@Override
	public void update(long delta) {
		int chunkUpdates = 0;

		if (!updateThread.isAlive()) {
			//LOGGER.log(Level.INFO, "Updating {0} chunks.", chunkUpdateQueue.size());

			List<Chunk> deletedElements = new ArrayList<Chunk>();

			for (Chunk c : chunkUpdateQueue) {
				if (chunkUpdates < 2) {
					c.calcSunlight();
					c.updateDisplayList();
					deletedElements.add(c);
					chunkUpdates++;
				} else {
					break;
				}
			}

			for (Chunk c : deletedElements) {
				chunkUpdateQueue.remove(c);
			}
		}
	}

	/**
	 * Generates the world on a per chunk basis.
	 */
	public final void generateChunk(Vector3f chunkPosition) {
		Chunk c = chunks[(int) chunkPosition.x][(int) chunkPosition.y][(int) chunkPosition.z];

		if (c != null) {
			c.clear();
		}

		Vector3f chunkOrigin = new Vector3f(chunkPosition.x * Chunk.chunkDimensions.x, chunkPosition.y * Chunk.chunkDimensions.y, chunkPosition.z * Chunk.chunkDimensions.z);

		for (int x = (int) chunkOrigin.x; x < (int) chunkOrigin.x + Chunk.chunkDimensions.x; x++) {
			for (int z = (int) chunkOrigin.z; z < (int) chunkOrigin.z + Chunk.chunkDimensions.z; z++) {
				setBlock(new Vector3f(x, 0, z), 0x3);
			}

		}

		for (int x = (int) chunkOrigin.x; x < (int) chunkOrigin.x + Chunk.chunkDimensions.x; x++) {
			for (int z = (int) chunkOrigin.z; z < (int) chunkOrigin.z + Chunk.chunkDimensions.z; z++) {

				float height = calcTerrainElevation(x, z) + (calcTerrainRoughness(x, z) * calcTerrainDetail(x, z)) * 64 + 64;

				float y = height;

				while (y > 0) {
					if (getCaveDensityAt(x, y, z) < 0.25) {

						if (height == y) {
							if (rand.nextFloat() < 150f / 100000f && height > 32) {
								generateTree(new Vector3f(x, height, z));
							}

							setBlock(new Vector3f(x, y, z), 0x1);
						} else {
							setBlock(new Vector3f(x, y, z), 0x2);
						}
					}

					y--;
				}

				// Generate water
				for (int i = 32; i > 0; i--) {
					if (getBlock(new Vector3f(x, i, z)) == 0) {
						setBlock(new Vector3f(x, i, z), 0x4);
					}
				}
			}
		}

		chunks[(int) chunkPosition.x][(int) chunkPosition.y][(int) chunkPosition.z].calcSunlight();
	}

	private void generateTree(Vector3f pos) {

		int height = rand.nextInt() % 5 + 3;

		// Generate tree trunk
		for (int i = 0; i < height; i++) {
			setBlock(new Vector3f(pos.x, pos.y + i, pos.z), 0x5);
		}

		// Generate the treetop
		for (int y = height / 2; y < height + 6; y++) {
			for (int x = -4; x < 4; x++) {
				for (int z = -4; z < 4; z++) {
					if (rand.nextFloat() < 0.75 && !(x == 0 && z == 0)) {
						setBlock(new Vector3f(pos.x + x, pos.y + y, pos.z + z), 0x6);
					}
				}
			}
		}

	}

	/**
	 * Sets the type of a block at a given position.
	 * @param pos
	 * @param type
	 */
	public final void setBlock(Vector3f pos, int type) {
		Vector3f chunkPos = calcChunkPos(pos);
		Vector3f blockCoord = calcBlockPos(pos, chunkPos);

		try {
			Chunk c = chunks[(int) chunkPos.x][(int) chunkPos.y][(int) chunkPos.z];

			// Create a new chunk if needed
			if (c == null) {
				c = new Chunk(this, new Vector3f(chunkPos.x, chunkPos.y, chunkPos.z));
				//LOGGER.log(Level.INFO, "Generating chunk at X: {0}, Y: {1}, Z: {2}", new Object[]{chunkPos.x, chunkPos.y, chunkPos.z});
				chunks[(int) chunkPos.x][(int) chunkPos.y][(int) chunkPos.z] = c;
			}

			// Generate or update the corresponding chunk
			c.setBlock(blockCoord, type);
			chunkUpdateQueue.add(c);
		} catch (Exception e) {
			return;
		}
	}

	/**
	 * Returns a block at a position by looking up the containing chunk.
	 */
	public final int getBlock(Vector3f pos) {
		Vector3f chunkPos = calcChunkPos(pos);
		Vector3f blockCoord = calcBlockPos(pos, chunkPos);

		try {
			Chunk c = chunks[(int) chunkPos.x][(int) chunkPos.y][(int) chunkPos.z];
			return c.getBlock((int) blockCoord.x, (int) blockCoord.y, (int) blockCoord.z);
		} catch (Exception e) {
			return -1;
		}
	}

	public final float getLight(Vector3f pos) {
		Vector3f chunkPos = calcChunkPos(pos);
		Vector3f blockCoord = calcBlockPos(pos, chunkPos);

		try {
			Chunk c = chunks[(int) chunkPos.x][(int) chunkPos.y][(int) chunkPos.z];
			return c.getLight((int) blockCoord.x, (int) blockCoord.y, (int) blockCoord.z);
		} catch (Exception ex) {
			return 0.0f;
		}
	}

	/**
	 * Returns true if the given position is hitting a block below.
	 */
	public boolean isHitting(Vector3f pos) {
		Vector3f chunkPos = calcChunkPos(pos);
		Vector3f blockCoord = calcBlockPos(pos, chunkPos);

		try {
			Chunk c = chunks[(int) chunkPos.x][(int) chunkPos.y][(int) chunkPos.z];
			return (c.getBlock((int) blockCoord.x, (int) blockCoord.y, (int) blockCoord.z) > 0);
		} catch (Exception e) {
			return false;
		}

	}

	/**
	 * Calculate the corresponding chunk for a given position within the world.
	 */
	private Vector3f calcChunkPos(Vector3f pos) {
		return new Vector3f((int) (pos.x / Chunk.chunkDimensions.x), (int) (pos.y / Chunk.chunkDimensions.y), (int) (pos.z / Chunk.chunkDimensions.z));
	}

	/**
	 * Calculate the position of a world-block within a specific chunk.
	 */
	private Vector3f calcBlockPos(Vector3f pos, Vector3f chunkPos) {
		return new Vector3f(pos.x - (chunkPos.x * Chunk.chunkDimensions.x), pos.y - (chunkPos.y * Chunk.chunkDimensions.y), pos.z - (chunkPos.z * Chunk.chunkDimensions.z));
	}

	/**
	 * Returns the base elevation for the terrain.
	 */
	private float calcTerrainElevation(float x, float z) {
		float result = 0.0f;
		result += pGen.noise(0.0009f * x, 0.0009f, 0.0009f * z) * 256.0f;
		return result;
	}

	/**
	 * Returns the roughness for the base terrain.
	 */
	private float calcTerrainRoughness(float x, float z) {
		float result = 0.0f;
		result += pGen.noise(0.001f * x, 0.001f, 0.001f * z);
		return result;
	}

	/**
	 * Returns the detail level for the base terrain.
	 */
	private float calcTerrainDetail(float x, float z) {
		float result = 0.0f;
		result += pGen.noise(0.09f * x, 0.09f, 0.09f * z);
		return result;
	}

	/**
	 * Returns the cave density for the base terrain.
	 */
	private float getCaveDensityAt(float x, float y, float z) {
		float result = 0.0f;
		result += pGen.noise(0.02f * x, 0.02f * y, 0.02f * z);
		return result;
	}

	public float getDaylight() {
		return daylight;
	}

	/**
	 * @return the player
	 */
	public Player getPlayer() {
		return player;
	}
}