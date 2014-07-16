package com.continuum;

import java.util.ArrayList;
import static org.lwjgl.opengl.GL11.*;

import java.util.Random;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.continuum.noise.PerlinNoise;
import org.lwjgl.util.glu.*;
import org.lwjgl.util.vector.Vector3f;

/**
 *
 * @author Benjamin Glatzel <benjamin.glatzel@me.com>
 */
public class World extends RenderObject {

	private long _daylightTimer = Helper.getInstance().getTime();
	private boolean _worldGenerated;
	private int _displayListSun = -1;
	private Player _player;
	private float _daylight = 1.0f;
	private Random _rand;
	// Used for updating/generating the world
	private Thread _updateThread;
	private Thread _worldThread;
	// The chunks to display
	private Chunk[][][] _chunks;
	// Update queue for generating the light and vertex arrays
	private final PriorityBlockingQueue<Chunk> _chunkUpdateQueue = new PriorityBlockingQueue<Chunk>();
	// Update queue for generating the display lists
	private final PriorityBlockingQueue<Chunk> _chunkUpdateQueueDL = new PriorityBlockingQueue<Chunk>();
	private PerlinNoise _pGen1;
	private PerlinNoise _pGen2;
	private PerlinNoise _pGen3;

	public World(String title, String seed, Player p) {
		this._player = p;
		_rand = new Random(seed.hashCode());
		_pGen1 = new PerlinNoise(_rand.nextInt());
		_pGen2 = new PerlinNoise(_rand.nextInt());
		_pGen3 = new PerlinNoise(_rand.nextInt());
		final World currentWorld = this;

		_chunks = new Chunk[(int) Configuration._viewingDistanceInChunks.x][(int) Configuration._viewingDistanceInChunks.y][(int) Configuration._viewingDistanceInChunks.z];

		_updateThread = new Thread(new Runnable() {

			@Override
			public void run() {

				long timeStart = System.currentTimeMillis();

				Logger.getLogger(this.getClass().getName()).log(Level.INFO, "Generating chunks. Please wait.");

				for (int x = 0; x < Configuration._viewingDistanceInChunks.x; x++) {
					for (int y = 0; y < Configuration._viewingDistanceInChunks.y; y++) {
						for (int z = 0; z < Configuration._viewingDistanceInChunks.z; z++) {
							Chunk c = new Chunk(currentWorld, new Vector3f(x, y, z));
							_chunks[x][y][z] = c;
							c.generate();
							c.populate();
							c.calcSunlight();

							queueChunkForUpdate(c);
						}
					}
				}

				_worldGenerated = true;
				_player.resetPlayer();

				Logger.getLogger(this.getClass().getName()).log(Level.INFO, "World updated ({0}s).", (System.currentTimeMillis() - timeStart) / 1000d);

				while (true) {
					Chunk c = null;
					synchronized (_chunkUpdateQueueDL) {
						c = _chunkUpdateQueue.peek();
						// Do not add a chunk which is beeing generated at the moment
						if (_chunkUpdateQueueDL.contains(c)) {
							c = null;
						} else {
							_chunkUpdateQueue.poll();
						}
					}

					if (c != null) {
						c.calcLight();
						c.generateVertexArray();
						synchronized (_chunkUpdateQueueDL) {
							_chunkUpdateQueueDL.add(c);
						}
					}
				}
			}
		});

		_worldThread = new Thread(new Runnable() {

			@Override
			public void run() {
				while (true) {
					updateInfWorld();

					if (Helper.getInstance().getTime() - _daylightTimer > 120000) {
						_daylight -= 0.2;

						if (_daylight <= 0.4f) {
							_daylight = 1.0f;
						}

						_daylightTimer = Helper.getInstance().getTime();
						updateAllChunks();
					}
				}
			}
		});
	}

	public void init() {
		_updateThread.start();
		_worldThread.start();

		/**
		 * Generates the display list used for displaying the sun.
		 */
		Sphere s = new Sphere();
		_displayListSun = glGenLists(1);
		glNewList(_displayListSun, GL_COMPILE);
		glColor4f(1.0f, 0.8f, 0.0f, 1.0f);
		s.draw(256.0f, 16, 32);
		glEndList();


	}

	@Override
	public void render() {

		/**
		 * Draws the sun.
		 */
		glPushMatrix();
		glDisable(GL_FOG);
		glTranslatef(Configuration._viewingDistanceInChunks.x * Chunk.CHUNK_DIMENSIONS.x * 1.5f + _player.getPosition().x, Configuration._viewingDistanceInChunks.y * Chunk.CHUNK_DIMENSIONS.y + _player.getPosition().y, Configuration._viewingDistanceInChunks.z * Chunk.CHUNK_DIMENSIONS.z * 1.5f + _player.getPosition().z);
		glCallList(_displayListSun);
		glEnable(GL_FOG);
		glPopMatrix();

		/**
		 * Render all active chunks.
		 */
		for (int x = 0; x < Configuration._viewingDistanceInChunks.x; x++) {
			for (int y = 0; y < Configuration._viewingDistanceInChunks.y; y++) {
				for (int z = 0; z < Configuration._viewingDistanceInChunks.z; z++) {
					Chunk c = getChunk(x, y, z);

					if (c != null) {
						c.render();
					}
				}
			}
		}
	}

	/*
	 * Update everything within the world (e.g. the chunks).
	 */
	@Override
	public void update(long delta) {
		Chunk c = null;

		for (int i = 0; i < 32; i++) {
			synchronized (_chunkUpdateQueueDL) {
				c = _chunkUpdateQueueDL.peek();
			}

			if (c != null) {
				c.generateDisplayList();

				synchronized (_chunkUpdateQueueDL) {
					_chunkUpdateQueueDL.remove(c);
				}
			}
		}
	}

	public void generateForest() {

		Logger.getLogger(this.getClass().getName()).log(Level.INFO, "Generating a forest. Please stand by.");

		for (int x = 0; x < Configuration._viewingDistanceInChunks.x * Chunk.CHUNK_DIMENSIONS.x; x++) {
			for (int y = 0; y < Configuration._viewingDistanceInChunks.y * Chunk.CHUNK_DIMENSIONS.y; y++) {
				for (int z = 0; z < Configuration._viewingDistanceInChunks.z * Chunk.CHUNK_DIMENSIONS.z; z++) {
					if (getBlock(x, y, z) == 0x1) {
						if (_rand.nextFloat() > 0.9984f) {
							if (_rand.nextBoolean()) {
								generateTree(x, y + 1, z);
							} else {
								generatePineTree(x, y + 1, z);
							}
						}
					}
				}
			}
		}

		Logger.getLogger(this.getClass().getName()).log(Level.INFO, "Finished generating forest.");
	}

	public void generateTree(int posX, int posY, int posZ) {

		int height = _rand.nextInt() % 2 + 6;

		// Generate tree trunk
		for (int i = 0; i < height; i++) {
			setBlock(posX, posY + i, posZ, 0x5);
		}

		// Generate the treetop
		for (int y = height - 2; y < height + 2; y += 1) {
			for (int x = -2; x < 3; x++) {
				for (int z = -2; z < 3; z++) {
					setBlock(posX + x, posY + y, posZ + z, 0x6);
				}
			}
		}
	}

	public void generatePineTree(int posX, int posY, int posZ) {

		int height = _rand.nextInt() % 2 + 12;

		// Generate tree trunk
		for (int i = 0; i < height; i++) {
			setBlock(posX, posY + i, posZ, 0x5);
		}

		// Generate the treetop
		for (int y = height / 4; y < height; y += 2) {
			for (int x = -(height / 2 - y / 2); x <= (height / 2 - y / 2); x++) {
				for (int z = -(height / 2 - y / 2); z <= (height / 2 - y / 2); z++) {
					if (_rand.nextFloat() < 0.95 && !(x == 0 && z == 0)) {
						setBlock(posX + x, posY + y, posZ + z, 0x6);
					}
				}
			}
		}
	}

	/**
	 * Returns true if the given position is filled with a block.
	 */
	public boolean isHitting(int x, int y, int z) {
		int chunkPosX = calcChunkPosX(x) % (int) Configuration._viewingDistanceInChunks.x;
		int chunkPosY = calcChunkPosY(y) % (int) Configuration._viewingDistanceInChunks.y;
		int chunkPosZ = calcChunkPosZ(z) % (int) Configuration._viewingDistanceInChunks.z;

		int blockPosX = calcBlockPosX(x, chunkPosX);
		int blockPosY = calcBlockPosY(y, chunkPosY);
		int blockPosZ = calcBlockPosZ(z, chunkPosZ);

		try {
			Chunk c = getChunk(chunkPosX, chunkPosY, chunkPosZ);
			return (c.getBlock(blockPosX, blockPosY, blockPosZ) > 0);
		} catch (Exception e) {
			return false;
		}
	}

	private int calcChunkPosX(int x) {
		return (x / (int) Chunk.CHUNK_DIMENSIONS.x);
	}

	private int calcChunkPosY(int y) {
		return (y / (int) Chunk.CHUNK_DIMENSIONS.y);
	}

	private int calcChunkPosZ(int z) {
		return (z / (int) Chunk.CHUNK_DIMENSIONS.z);
	}

	private int calcBlockPosX(int x1, int x2) {
		x1 = x1 % ((int) Configuration._viewingDistanceInChunks.x * (int) Chunk.CHUNK_DIMENSIONS.x);
		return (x1 - (x2 * (int) Chunk.CHUNK_DIMENSIONS.x));
	}

	private int calcBlockPosY(int y1, int y2) {
		y1 = y1 % ((int) Configuration._viewingDistanceInChunks.y * (int) Chunk.CHUNK_DIMENSIONS.y);
		return (y1 - (y2 * (int) Chunk.CHUNK_DIMENSIONS.y));
	}

	private int calcBlockPosZ(int z1, int z2) {
		z1 = z1 % ((int) Configuration._viewingDistanceInChunks.z * (int) Chunk.CHUNK_DIMENSIONS.z);
		return (z1 - (z2 * (int) Chunk.CHUNK_DIMENSIONS.z));
	}

	public float getDaylight() {
		return _daylight;
	}

	/**
	 * TODO
	 */
	public Player getPlayer() {
		return _player;
	}

	public Vector3f getDaylightColor() {
		return new Vector3f((getDaylight() - 0.5f), getDaylight() - 0.25f, getDaylight());
	}

	/**
	 * @return the worldGenerated
	 */
	public boolean isWorldGenerated() {
		return _worldGenerated;
	}

	/**
	 * Sets the type of a block at a given position.
	 */
	public final void setBlock(int x, int y, int z, int type) {
		int chunkPosX = calcChunkPosX(x) % (int) Configuration._viewingDistanceInChunks.x;
		int chunkPosY = calcChunkPosY(y) % (int) Configuration._viewingDistanceInChunks.y;
		int chunkPosZ = calcChunkPosZ(z) % (int) Configuration._viewingDistanceInChunks.z;

		int blockPosX = calcBlockPosX(x, chunkPosX);
		int blockPosY = calcBlockPosY(y, chunkPosY);
		int blockPosZ = calcBlockPosZ(z, chunkPosZ);

		try {
			Chunk c = getChunk(chunkPosX, chunkPosY, chunkPosZ);

			// Check if the chunk is valid
			if (c.getPosition().x != calcChunkPosX(x) || c.getPosition().y != calcChunkPosY(y) || c.getPosition().z != calcChunkPosZ(z)) {
				return;
			}

			// Generate or update the corresponding chunk
			c.setBlock(blockPosX, blockPosY, blockPosZ, type);

			queueChunkForUpdate(c);
		} catch (Exception e) {
			return;
		}
	}

	public final Chunk getChunk(int x, int y, int z) {
		Chunk c = null;

		try {
			c = _chunks[x % (int) Configuration._viewingDistanceInChunks.x][y % (int) Configuration._viewingDistanceInChunks.y][z % (int) Configuration._viewingDistanceInChunks.z];

		} catch (Exception e) {
		}

		return c;
	}

	/**
	 * Returns a block at a position by looking up the containing chunk.
	 */
	public final int getBlock(int x, int y, int z) {
		int chunkPosX = calcChunkPosX(x) % (int) Configuration._viewingDistanceInChunks.x;
		int chunkPosY = calcChunkPosY(y) % (int) Configuration._viewingDistanceInChunks.y;
		int chunkPosZ = calcChunkPosZ(z) % (int) Configuration._viewingDistanceInChunks.z;

		int blockPosX = calcBlockPosX(x, chunkPosX);
		int blockPosY = calcBlockPosY(y, chunkPosY);
		int blockPosZ = calcBlockPosZ(z, chunkPosZ);

		try {
			Chunk c = getChunk(chunkPosX, chunkPosY, chunkPosZ);
			return c.getBlock(blockPosX, blockPosY, blockPosZ);
		} catch (Exception e) {
		}

		return -1;
	}

	/**
	 * TODO.
	 */
	public final float getLight(int x, int y, int z) {
		int chunkPosX = calcChunkPosX(x) % (int) Configuration._viewingDistanceInChunks.x;
		int chunkPosY = calcChunkPosY(y) % (int) Configuration._viewingDistanceInChunks.y;
		int chunkPosZ = calcChunkPosZ(z) % (int) Configuration._viewingDistanceInChunks.z;

		int blockPosX = calcBlockPosX(x, chunkPosX);
		int blockPosY = calcBlockPosY(y, chunkPosY);
		int blockPosZ = calcBlockPosZ(z, chunkPosZ);

		try {
			Chunk c = getChunk(chunkPosX, chunkPosY, chunkPosZ);
			return c.getLight(blockPosX, blockPosY, blockPosZ);
		} catch (Exception e) {
		}

		return -1f;
	}

	/**
	 * TODO.
	 */
	public void setLight(int x, int y, int z, float intens) {
		int chunkPosX = calcChunkPosX(x) % (int) Configuration._viewingDistanceInChunks.x;
		int chunkPosY = calcChunkPosY(y) % (int) Configuration._viewingDistanceInChunks.y;
		int chunkPosZ = calcChunkPosZ(z) % (int) Configuration._viewingDistanceInChunks.z;

		int blockPosX = calcBlockPosX(x, chunkPosX);
		int blockPosY = calcBlockPosY(y, chunkPosY);
		int blockPosZ = calcBlockPosZ(z, chunkPosZ);

		try {
			Chunk c = getChunk(chunkPosX, chunkPosY, chunkPosZ);
			c.setLight(blockPosX, blockPosY, blockPosZ, intens);
		} catch (Exception e) {
		}
	}

	private int calcPlayerChunkOffsetX() {
		return (int) ((_player.getPosition().x - Helper.getInstance().calcPlayerOrigin().x) / Chunk.CHUNK_DIMENSIONS.x);
	}

	private int calcPlayerChunkOffsetY() {
		return (int) ((_player.getPosition().y - Helper.getInstance().calcPlayerOrigin().y) / Chunk.CHUNK_DIMENSIONS.y);
	}

	private int calcPlayerChunkOffsetZ() {
		return (int) ((_player.getPosition().z - Helper.getInstance().calcPlayerOrigin().z) / Chunk.CHUNK_DIMENSIONS.z);
	}

	private void updateInfWorld() {

		ArrayList<Chunk> chunksToUpdate = new ArrayList<Chunk>();

		for (int x = 0; x < Configuration._viewingDistanceInChunks.x; x++) {
			for (int y = 0; y < Configuration._viewingDistanceInChunks.y; y++) {
				for (int z = 0; z < Configuration._viewingDistanceInChunks.z; z++) {
					Chunk c = getChunk(x, y, z);

					if (c != null) {
						Vector3f pos = new Vector3f(x, y, z);

						int multZ = (int) calcPlayerChunkOffsetZ() / (int) Configuration._viewingDistanceInChunks.z + 1;

						if (z < calcPlayerChunkOffsetZ() % Configuration._viewingDistanceInChunks.z) {
							pos.z += Configuration._viewingDistanceInChunks.z * multZ;
						} else {
							pos.z += Configuration._viewingDistanceInChunks.z * (multZ - 1);
						}

						int multX = (int) calcPlayerChunkOffsetX() / (int) Configuration._viewingDistanceInChunks.x + 1;

						if (x < calcPlayerChunkOffsetX() % Configuration._viewingDistanceInChunks.x) {
							pos.x += Configuration._viewingDistanceInChunks.x * multX;
						} else {
							pos.x += Configuration._viewingDistanceInChunks.x * (multX - 1);
						}

						if (c.getPosition().x != pos.x || c.getPosition().z != pos.z) {
							c.setPosition(pos);
							c.generate();
							c.populate();

							chunksToUpdate.add(c);
						}

					}
				}
			}
		}

		for (Chunk c : chunksToUpdate) {
			queueChunkForUpdate(c);
		}
	}

	public void updateAllChunks() {
		for (int x = 0; x < Configuration._viewingDistanceInChunks.x; x++) {
			for (int y = 0; y < Configuration._viewingDistanceInChunks.y; y++) {
				for (int z = 0; z < Configuration._viewingDistanceInChunks.z; z++) {
					Chunk c = getChunk(x, y, z);
					queueChunkForUpdate(c);
				}
			}
		}
	}

	/**
	 * @return the pGen1
	 */
	public PerlinNoise getpGen1() {
		return _pGen1;
	}

	/**
	 * @return the pGen2
	 */
	public PerlinNoise getpGen2() {
		return _pGen2;
	}

	/**
	 * @return the pGen3
	 */
	public PerlinNoise getpGen3() {
		return _pGen3;
	}

	public String chunkUpdateStatus() {
		return String.format("U: %d UDL: %d", _chunkUpdateQueue.size(), _chunkUpdateQueueDL.size());
	}

	private void queueChunkForUpdate(Chunk c) {
		if (c != null) {
			// Add all neighbors
			ArrayList<Chunk> cs = new ArrayList<Chunk>();
			cs.add(c);

			try {
				cs.add(getChunk((int) c.getPosition().x + 1, (int) c.getPosition().y, (int) c.getPosition().z));
			} catch (Exception e) {
			}

			try {
				cs.add(getChunk((int) c.getPosition().x - 1, (int) c.getPosition().y, (int) c.getPosition().z));
			} catch (Exception e) {
			}

			try {
				cs.add(getChunk((int) c.getPosition().x, (int) c.getPosition().y, (int) c.getPosition().z + 1));
			} catch (Exception e) {
			}

			try {
				cs.add(getChunk((int) c.getPosition().x, (int) c.getPosition().y, (int) c.getPosition().z - 1));
			} catch (Exception e) {
			}

			synchronized (_chunkUpdateQueueDL) {
				for (Chunk cc : cs) {
					if (!_chunkUpdateQueue.contains(cc) && cc != null) {
						_chunkUpdateQueue.add(cc);
					}
				}
			}
		}
	}
}