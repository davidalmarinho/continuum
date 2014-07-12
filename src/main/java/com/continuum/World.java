package com.continuum;

import org.lwjgl.util.vector.Vector3f;
import org.lwjgl.util.vector.Vector4f;

import java.util.ArrayList;
import java.util.Random;

/**
 * World class
 *
 * Created by gil0mendes on 12/07/14.
 */
public class World extends RenderObject
{
	// The title of the world
	private String title = "World 1";

	// The seed used for terrain generation
	private String seed = "CONTINUUM";

	// List of world blocks
	private ArrayList<Vector4f> blocks = new ArrayList<Vector4f>();

	// The chunks to display
	Chunk[][][] chunks;

	// Random number generator
	private Random rand = new Random();

	// Noise generator
	private PerlinNoiseGenerator pGen = new PerlinNoiseGenerator();

	// World dimensions
	private static final Vector3f worldDimensions = new Vector3f(512, 256, 512);

	// Time since last chunk update
	long timeSinceLastChunkUpdate = 0;

	// Players
	private Player[] players = new Player[32];

	public World(Player mainChar) {
		chunks = new Chunk[(int) Configuration.viewingDistanceInChunks.x][2][(int) Configuration.viewingDistanceInChunks.y];

		// Save main char
		this.players[0] = mainChar;

		Thread t = new Thread(new Runnable() {
			@Override
			public void run() {
				generateTestWorld();
				updateWorld();
			}
		});

		t.start();
	}

	@Override
	public void render() {
		for (int x = 0; x < (int) Configuration.viewingDistanceInChunks.x; x++) {
			for (int y = 0; y < 2; y++) {
				for (int z = 0; z < (int) Configuration.viewingDistanceInChunks.y; z++) {
					if (chunks[x][y][z] != null) {
						// Render active chunks only
						chunks[x][y][z].render();

						if (System.currentTimeMillis() - timeSinceLastChunkUpdate > 25) {
							if (chunks[x][y][z].updateDisplayList()) {
								timeSinceLastChunkUpdate = System.currentTimeMillis();
							}
						}
					}


				}
			}
		}
	}

	/**
	 * Update world
	 */
	public final void updateWorld() {

		long timeStart = System.currentTimeMillis();

		for (Vector4f v : blocks) {
			int chunkX = (int) Math.floor(v.x / (int) Chunk.chunkDimensions.x);
			int chunkY = (int) Math.floor(v.y / (int) Chunk.chunkDimensions.y);
			int chunkZ = (int) Math.floor(v.z / (int) Chunk.chunkDimensions.z);

			// Create a new chunk if needed
			if (chunks[chunkX][chunkY][chunkZ] == null) {
				System.out.println("Generating chunk at X: " + chunkX + ", Y: " + chunkY + ", Z: " + chunkZ);
				chunks[chunkX][chunkY][chunkZ] = new Chunk(new Vector3f(chunkX, chunkY, chunkZ));
			}

			Vector3f blockCoord = new Vector3f(v.x - (chunkX * Chunk.chunkDimensions.x), v.y - (chunkY * Chunk.chunkDimensions.y), v.z - (chunkZ * Chunk.chunkDimensions.z));

			// Generate or update the corresponding chunk
			chunks[chunkX][chunkY][chunkZ].setBlock((int) blockCoord.x, (int) blockCoord.y, (int) blockCoord.z, (int) v.w);
		}

		System.out.println(chunks[0][0][0]);

		System.out.println("World updated (" + (System.currentTimeMillis() - timeStart) / 1000d + "s).");

	}

	/**
	 * Generate a test world
	 */
	public final void generateTestWorld() {
		long timeStart = System.currentTimeMillis();

		for (int x = 0; x < worldDimensions.x; x++) {
			for (int z = 0; z < worldDimensions.z; z++) {
				float height = pGen.getTerrainHeightAt(x / 2, z / 2);

				if (height < 0) {
					height = 0;
				}

				float y = height * 512 + 64;

				if (y > 126) {
					y = 126;
				}

				blocks.add(new Vector4f(x, y, z, 1.0f));
				y--;

				// Fill with dirt
				while (y >= 0) {
					blocks.add(new Vector4f(x, y, z, 2.0f));
					y--;
				}
			}
		}

		System.out.println("World generated (" + (System.currentTimeMillis() - timeStart) / 1000d + "s).");
	}
}
