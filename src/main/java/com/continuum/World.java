package com.continuum;

import static org.lwjgl.opengl.GL11.*;

import org.lwjgl.util.vector.Vector3f;

import java.util.*;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.lwjgl.util.glu.*;

/**
 * World class
 * <p/>
 * Created by gil0mendes on 12/07/14.
 */
public class World extends RenderObject {
	private long daytime = Helper.getInstance().getTime();
	private boolean disableChunkUpdates = false;

	// Has world generated
	private boolean worldGenerated;

	// Daylight intensity
	private float daylight = 0.8f;

	private Random rand;

	// Used for updating/generating the world
	private Thread updateThread;
	private Thread worldThread;

	// The chunks to display
	private Chunk[][][] chunks;

	// Update queue for generating the light and vertex arrays
	private final LinkedBlockingQueue<Chunk> chunkUpdateQueue = new LinkedBlockingQueue<Chunk>();

	// Update queue for generating the display lists
	private final LinkedBlockingQueue<Chunk> chunkUpdateQueueDL = new LinkedBlockingQueue<Chunk>();

	// Logger
	private static final Logger LOGGER = Logger.getLogger(Main.class.getName());

	// Player
	private Player player = null;

	private int displayListSun = -1;

	/**
	 * Init. world
	 *
	 * @param title
	 * @param seed
	 */
	public World(String title, String seed, Player p) {
		this.player = p;
		rand = new Random(seed.hashCode());
		final World currentWorld = this;

		chunks = new Chunk[(int) Configuration.viewingDistanceInChunks.x][(int) Configuration.viewingDistanceInChunks.y][(int) Configuration.viewingDistanceInChunks.z];

		updateThread = new Thread(new Runnable() {

			@Override
			public void run() {

				long timeStart = System.currentTimeMillis();

				Logger.getLogger(this.getClass().getName()).log(Level.INFO, "Generating chunks. Please wait.");

				for (int x = 0; x < Configuration.viewingDistanceInChunks.x; x++) {
					for (int y = 0; y < Configuration.viewingDistanceInChunks.y; y++) {
						for (int z = 0; z < Configuration.viewingDistanceInChunks.z; z++) {
							Chunk c = new Chunk(currentWorld, new Vector3f(x, y, z));
							chunks[x][y][z] = c;
						}
					}
				}

				setWorldGenerated(true);
				player.resetPlayer();

				Logger.getLogger(this.getClass().getName()).log(Level.INFO, "Calculating sunlight. Please wait.");

				for (int x = 0; x < Configuration.viewingDistanceInChunks.x; x++) {
					for (int y = 0; y < Configuration.viewingDistanceInChunks.y; y++) {
						for (int z = 0; z < Configuration.viewingDistanceInChunks.z; z++) {
							Chunk c = chunks[x][y][z];
							c.calcSunlight();
						}
					}
				}

				Logger.getLogger(this.getClass().getName()).log(Level.INFO, "World updated ({0}s).", (System.currentTimeMillis() - timeStart) / 1000d);

				for (int x = 0; x < Configuration.viewingDistanceInChunks.x; x++) {
					for (int y = 0; y < Configuration.viewingDistanceInChunks.y; y++) {
						for (int z = 0; z < Configuration.viewingDistanceInChunks.z; z++) {
							Chunk c = chunks[x][y][z];
							c.generateVertexArray();

							if (!chunkUpdateQueueDL.contains(c)) {
								chunkUpdateQueueDL.add(c);
							}
						}
					}
				}

				while (true) {
					while (chunkUpdateQueue.size() > 0 && !disableChunkUpdates) {

						Chunk c = null;

						synchronized (chunkUpdateQueue) {
							c = chunkUpdateQueue.element();
						}

						c.calcSunlight();
						c.generateVertexArray();

						synchronized (chunkUpdateQueueDL) {
							if (!chunkUpdateQueueDL.contains(c)) {
								chunkUpdateQueueDL.add(c);
								chunkUpdateQueue.remove(c);
							}
						}
					}


					if (Helper.getInstance().getTime() - daytime > 120000) {

						if (chunkUpdateQueue.size() == 0) {
							daylight -= 0.1f;

							if (daylight < 0.3f) {
								daylight = 0.8f;
							}

							updateAllChunks();
							daytime = Helper.getInstance().getTime();
						}
					}
				}
			}
		});

		worldThread = new Thread(new Runnable() {
			@Override
			public void run() {
				while (true) {
					updateInfWorld();
				}
			}
		});
	}

	public void init() {
		updateThread.start();
		worldThread.start();

		/**
		 * Generates the display list used for displaying the sun.
		 */
		Sphere s = new Sphere();
		displayListSun = glGenLists(1);
		glNewList(displayListSun, GL_COMPILE);
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
		glTranslatef(Configuration.viewingDistanceInChunks.x * Chunk.chunkDimensions.x * 1.5f + player.getPosition().x, Configuration.viewingDistanceInChunks.y * Chunk.chunkDimensions.y + player.getPosition().y, Configuration.viewingDistanceInChunks.z * Chunk.chunkDimensions.z * 1.5f + player.getPosition().z);
		glCallList(displayListSun);
		glEnable(GL_FOG);
		glPopMatrix();

		/**
		 * Render all active chunks.
		 */
		for (int x = 0; x < Configuration.viewingDistanceInChunks.x; x++) {
			for (int y = 0; y < Configuration.viewingDistanceInChunks.y; y++) {
				for (int z = 0; z < Configuration.viewingDistanceInChunks.z; z++) {
					Chunk c = chunks[x][y][z];

					if (c != null) {
						c.render();
					}
				}
			}
		}
	}

	/*
	 * Update everything within the world (e.q. the chunks).
	 */
	@Override
	public void update(long delta) {
		int chunkUpdates = 0;

		while (chunkUpdateQueueDL.size() > 0) {
			if (chunkUpdates < 4) {
				Chunk c = null;

				synchronized (chunkUpdateQueueDL) {
					c = chunkUpdateQueueDL.element();
				}

				c.generateDisplayList();

				synchronized (chunkUpdateQueueDL) {
					chunkUpdateQueueDL.remove(c);
				}
				chunkUpdates++;
			} else {
				break;
			}
		}
	}

	public void generateForest() {

		Logger.getLogger(this.getClass().getName()).log(Level.INFO, "Generating a forest. Please stand by.");

		disableChunkUpdates = true;
		for (int x = 0; x < Configuration.viewingDistanceInChunks.x * Chunk.chunkDimensions.x; x++) {
			for (int y = 0; y < Configuration.viewingDistanceInChunks.y * Chunk.chunkDimensions.y; y++) {
				for (int z = 0; z < Configuration.viewingDistanceInChunks.z * Chunk.chunkDimensions.z; z++) {
					if (getBlock(x, y, z) == 0x1) {
						if (rand.nextFloat() > 0.9984f) {
							generateTree(x, y + 1, z);
						}
					}
				}
			}
		}
		disableChunkUpdates = false;

		Logger.getLogger(this.getClass().getName()).log(Level.INFO, "Finished generating forest.");
	}

	public void generateTree(int posX, int posY, int posZ) {

		int height = rand.nextInt() % 6 + 12;

		// Generate tree trunk
		for (int i = 0; i < height; i++) {
			setBlock(posX, posY + i, posZ, 0x5);
		}

		// Generate the treetop
		for (int y = height / 4; y
				< height + 2; y += 2) {
			for (int x = -(height / 2 - y / 2); x
					<= (height / 2 - y / 2); x++) {
				for (int z = -(height / 2 - y / 2); z
						<= (height / 2 - y / 2); z++) {
					if (rand.nextFloat() < 0.95 && !(x == 0 && z == 0)) {
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
		int chunkPosX = calcChunkPosX(x);
		int chunkPosY = calcChunkPosY(y);
		int chunkPosZ = calcChunkPosZ(z);

		int blockPosX = calcBlockPosX(x, chunkPosX);
		int blockPosY = calcBlockPosY(y, chunkPosY);
		int blockPosZ = calcBlockPosZ(z, chunkPosZ);

		try {
			Chunk c = chunks[chunkPosX][chunkPosY][chunkPosZ];
			return (c.getBlock(blockPosX, blockPosY, blockPosZ) > 0);
		} catch (Exception e) {
			return false;
		}
	}

	private int calcChunkPosX(int x) {
		return (x / (int) Chunk.chunkDimensions.x) % (int) Configuration.viewingDistanceInChunks.x;
	}

	private int calcChunkPosY(int y) {
		return (y / (int) Chunk.chunkDimensions.y) % (int) Configuration.viewingDistanceInChunks.y;
	}

	private int calcChunkPosZ(int z) {
		return (z / (int) Chunk.chunkDimensions.z) % (int) Configuration.viewingDistanceInChunks.z;
	}

	private int calcBlockPosX(int x1, int x2) {
		x1 = x1 % ((int) Configuration.viewingDistanceInChunks.x * (int) Chunk.chunkDimensions.x);
		return (x1 - (x2 * (int) Chunk.chunkDimensions.x));
	}

	private int calcBlockPosY(int y1, int y2) {
		y1 = y1 % ((int) Configuration.viewingDistanceInChunks.y * (int) Chunk.chunkDimensions.y);
		return (y1 - (y2 * (int) Chunk.chunkDimensions.y));
	}

	private int calcBlockPosZ(int z1, int z2) {
		z1 = z1 % ((int) Configuration.viewingDistanceInChunks.z * (int) Chunk.chunkDimensions.z);
		return (z1 - (z2 * (int) Chunk.chunkDimensions.z));
	}

	public float getDaylight() {
		return daylight;
	}

	/**
	 * TODO
	 */
	public Player getPlayer() {
		return player;
	}

	public Vector3f getDaylightColor() {
		return new Vector3f((getDaylight() - 0.5f), getDaylight() - 0.25f, getDaylight());
	}

	/**
	 * @return the worldGenerated
	 */
	public boolean isWorldGenerated() {
		return worldGenerated;
	}

	/**
	 * @param worldGenerated the worldGenerated to set
	 */
	public void setWorldGenerated(boolean worldGenerated) {
		this.worldGenerated = worldGenerated;
	}

	/**
	 * Sets the type of a block at a given position.
	 */
	public final void setBlock(int x, int y, int z, int type) {
		int chunkPosX = calcChunkPosX(x);
		int chunkPosY = calcChunkPosY(y);
		int chunkPosZ = calcChunkPosZ(z);

		int blockPosX = calcBlockPosX(x, chunkPosX);
		int blockPosY = calcBlockPosY(y, chunkPosY);
		int blockPosZ = calcBlockPosZ(z, chunkPosZ);

		try {
			Chunk c = chunks[chunkPosX][chunkPosY][chunkPosZ];
			// Generate or update the corresponding chunk
			c.setBlock(blockPosX, blockPosY, blockPosZ, type);

			synchronized (chunkUpdateQueue) {
				if (!chunkUpdateQueue.contains(c)) {
					chunkUpdateQueue.add(c);
				}
			}
		} catch (Exception e) {
			return;
		}
	}

	/**
	 * Returns a block at a position by looking up the containing chunk.
	 */
	public final int getBlock(int x, int y, int z) {
		int chunkPosX = calcChunkPosX(x);
		int chunkPosY = calcChunkPosY(y);
		int chunkPosZ = calcChunkPosZ(z);

		int blockPosX = calcBlockPosX(x, chunkPosX);
		int blockPosY = calcBlockPosY(y, chunkPosY);
		int blockPosZ = calcBlockPosZ(z, chunkPosZ);

		try {
			Chunk c = chunks[chunkPosX][chunkPosY][chunkPosZ];
			return c.getBlock(blockPosX, blockPosY, blockPosZ);
		} catch (Exception e) {
			return 0;
		}
	}

	/**
	 * TODO.
	 */
	public final float getLight(int x, int y, int z) {
		int chunkPosX = calcChunkPosX(x);
		int chunkPosY = calcChunkPosY(y);
		int chunkPosZ = calcChunkPosZ(z);

		int blockPosX = calcBlockPosX(x, chunkPosX);
		int blockPosY = calcBlockPosY(y, chunkPosY);
		int blockPosZ = calcBlockPosZ(z, chunkPosZ);

		try {
			Chunk c = chunks[chunkPosX][chunkPosY][chunkPosZ];
			return c.getLight(blockPosX, blockPosY, blockPosZ);
		} catch (Exception e) {
			return 0.0f;
		}
	}

	private int calcPlayerChunkOffsetX() {
		return (int) ((player.getPosition().x - Helper.getInstance().calcPlayerOrigin().x) / Chunk.chunkDimensions.x);
	}

	private int calcPlayerChunkOffsetY() {
		return (int) ((player.getPosition().y - Helper.getInstance().calcPlayerOrigin().y) / Chunk.chunkDimensions.y);
	}

	private int calcPlayerChunkOffsetZ() {
		return (int) ((player.getPosition().z - Helper.getInstance().calcPlayerOrigin().z) / Chunk.chunkDimensions.z);
	}

	private void debugOutput() {
//        System.out.printf("1:%d 2:%d\n", chunkUpdateQueue.size(), chunkUpdateQueueDL.size());
//        System.out.println(String.format("OffsetX: %d, MultX: %d", calcPlayerChunkOffsetX(), calcPlayerChunkMultX()));
	}

	private void updateInfWorld() {

		for (int x = 0; x < Configuration.viewingDistanceInChunks.x; x++) {
			for (int y = 0; y < Configuration.viewingDistanceInChunks.y; y++) {
				for (int z = 0; z < Configuration.viewingDistanceInChunks.z; z++) {
					Chunk c = chunks[x][y][z];

					if (c != null) {
						Vector3f pos = new Vector3f(x, y, z);

						int multZ = (int) calcPlayerChunkOffsetZ() / (int) Configuration.viewingDistanceInChunks.z + 1;

						if (z < calcPlayerChunkOffsetZ() % Configuration.viewingDistanceInChunks.z) {
							pos.z += Configuration.viewingDistanceInChunks.z * multZ;
						} else {
							pos.z += Configuration.viewingDistanceInChunks.z * (multZ - 1);
						}

						int multX = (int) calcPlayerChunkOffsetX() / (int) Configuration.viewingDistanceInChunks.x + 1;

						if (x < calcPlayerChunkOffsetX() % Configuration.viewingDistanceInChunks.x) {
							pos.x += Configuration.viewingDistanceInChunks.x * multX;
						} else {
							pos.x += Configuration.viewingDistanceInChunks.x * (multX - 1);
						}

						if (c.getPosition().x != pos.x || c.getPosition().z != pos.z) {
							c.setPosition(pos);
							c.generate();
							synchronized (chunkUpdateQueue) {
								if (!chunkUpdateQueue.contains(c)) {
									chunkUpdateQueue.add(c);
								}
							}
						}

					}
				}
			}
		}
	}

	public void updateAllChunks() {
		for (int x = 0; x < Configuration.viewingDistanceInChunks.x; x++) {
			for (int y = 0; y < Configuration.viewingDistanceInChunks.y; y++) {
				for (int z = 0; z < Configuration.viewingDistanceInChunks.z; z++) {
					Chunk c = chunks[x][y][z];

					synchronized (chunkUpdateQueue) {
						if (!chunkUpdateQueue.contains(c)) {
							chunkUpdateQueue.add(c);
						}
					}
				}
			}
		}
	}
}