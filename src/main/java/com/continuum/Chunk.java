package com.continuum;

import org.lwjgl.BufferUtils;
import org.lwjgl.util.vector.Vector3f;

import java.io.IOException;
import java.nio.FloatBuffer;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.newdawn.slick.opengl.Texture;

import java.io.FileInputStream;

import org.newdawn.slick.opengl.TextureLoader;

import static org.lwjgl.opengl.GL11.*;

/**
 * Class for chunk.
 */
public class Chunk extends RenderObject {
	// Some constantes
	private static final float DIMMING_INTENS = 0.1f;
	private static final float LUMINANCE_INTENS = 0.05f;
	private static final float DIM_BLOCK_SIDES = 0.2f;
	boolean dirty = false;

	// TODO
	private List<Float> quads = new ArrayList<Float>();
	private List<Float> tex = new ArrayList<Float>();
	private List<Float> color = new ArrayList<Float>();

	// TODO
	Random rand = new Random();

	// The actual block ids for the chunk
	int[][][] blocks;
	private float[][][] light;

	// Chunk
	int chunkID = -1;

	// Create an unique id for each chunk
	int displayList = -1;

	// Texture map
	static Texture textureMap;

	// Size of one single chunk
	public static final Vector3f chunkDimensions = new Vector3f(16, 128, 16);

	// The parent world
	static World parent = null;

	// Perlin noise generator
	static PerlinNoise pGen1 = new PerlinNoise("WALTER");
	static PerlinNoise pGen2 = new PerlinNoise("OTTO");

	/**
	 * @return the light
	 */
	public float getLight(int x, int y, int z) {
		float result;
		try {
			result = light[x][y][z];
		} catch (Exception e) {
			return 0.0f;
		}

		return result;
	}

	public void setLight(int x, int y, int z, float intens) {
		try {
			light[x][y][z] = intens;
			return;
		} catch (Exception e) {
			return;
		}
	}

	enum SIDE {

		LEFT, RIGHT, TOP, BOTTOM, FRONT, BACK;
	};

	public Chunk(World p, Vector3f position) {
		this.position = position;

		if (Chunk.parent == null) {
			Chunk.parent = p;
		}

		blocks = new int[(int) chunkDimensions.x][(int) chunkDimensions.y][(int) chunkDimensions.z];
		light = new float[(int) chunkDimensions.x][(int) chunkDimensions.y][(int) chunkDimensions.z];

		generate();
	}

	private void generate() {
		int xOffset = (int) position.x * (int) chunkDimensions.x;
		int zOffset = (int) position.z * (int) chunkDimensions.z;

		for (int x = 0; x < Chunk.chunkDimensions.x; x++) {
			for (int z = 0; z < Chunk.chunkDimensions.z; z++) {
				setBlock(new Vector3f(x, 0, z), 0x3, false);
			}

		}

		for (int x = 0; x < Chunk.chunkDimensions.x; x++) {
			for (int z = 0; z < Chunk.chunkDimensions.z; z++) {
				float height = calcTerrainElevation(x + xOffset, z + zOffset) + (calcTerrainRoughness(x + xOffset, z + zOffset) * calcTerrainDetail(x + xOffset, z + zOffset)) * 64 + 64;

				float y = height;

				while (y > 0) {
					if (getCaveDensityAt(x + xOffset, y, z + zOffset) < 0.45) {

						if (height == y) {
							setBlock(new Vector3f(x, y, z), 0x1, false);
						} else {
							setBlock(new Vector3f(x, y, z), 0x2, false);
						}
					} else if (getCaveDensityAt(x + xOffset, y, z + zOffset) < 0.46) {
						setBlock(new Vector3f(x, y, z), 0x3, false);
					}

					if (getStoneDensity(x + xOffset, y, z + zOffset) > 0.46) {
						setBlock(new Vector3f(x, y, z), 0x3, false);
					}

					y--;
				}

				// Generate water
				for (int i = 32; i > 0; i--) {
					if (getBlock(x, i, z) == 0) {
						setBlock(new Vector3f(x, i, z), 0x4, false);
					}
				}
			}
		}
	}

	@Override
	public String toString() {
		int counter = 0;

		for (int x = 0; x < chunkDimensions.x; x++) {
			for (int y = 0; y < chunkDimensions.y; y++) {
				for (int z = 0; z < chunkDimensions.z; z++) {
					if (blocks[x][y][z] > 0) {
						counter++;
					}
				}
			}
		}

		return counter + " Blocks in this chunk.";
	}

	@Override
	public void render() {
		glEnable(GL_TEXTURE_2D);
		glCallList(displayList);
		glDisable(GL_TEXTURE_2D);
	}

	public static void init() {
		try {
			textureMap = TextureLoader.getTexture("PNG", new FileInputStream(Chunk.class.getResource("images/Terrain.png").getPath()), GL_NEAREST);
			textureMap.bind();

		} catch (IOException ex) {
			Logger.getLogger(Chunk.class.getName()).log(Level.SEVERE, null, ex);
		}
	}

	public int getBlock(int x, int y, int z) {
		return blocks[x][y][z];
	}

	public void setBlock(Vector3f pos, int type, boolean dirty) {
		blocks[(int) pos.x][(int) pos.y][(int) pos.z] = type;
		this.dirty = dirty;
	}

	public Vector3f getBlockWorldPos(Vector3f pos) {
		Vector3f v = new Vector3f(pos.x + position.x * chunkDimensions.x, pos.y + position.y * chunkDimensions.y, pos.z + position.z * chunkDimensions.z);
		return v;
	}

	public void calcSunlight() {

		for (int x = 0; x < (int) chunkDimensions.x; x++) {
			for (int z = 0; z < (int) chunkDimensions.z; z++) {
				boolean covered = false;
				for (int y = (int) chunkDimensions.y - 1; y >= 0; y--) {

					if (blocks[x][y][z] == 0 && !covered) {
						float dimming = 0.0f;
						dimming += (parent.getBlock(new Vector3f(getBlockWorldPos(new Vector3f(x + 1, y, z)))) > 0) ? DIMMING_INTENS : 0.0f;
						dimming += (parent.getBlock(new Vector3f(getBlockWorldPos(new Vector3f(x - 1, y, z)))) > 0) ? DIMMING_INTENS : 0.0f;
						dimming += (parent.getBlock(new Vector3f(getBlockWorldPos(new Vector3f(x, y, z + 1)))) > 0) ? DIMMING_INTENS : 0.0f;
						dimming += (parent.getBlock(new Vector3f(getBlockWorldPos(new Vector3f(x, y, z - 1)))) > 0) ? DIMMING_INTENS : 0.0f;

						dimming += (parent.getBlock(new Vector3f(getBlockWorldPos(new Vector3f(x + 1, y, z + 1)))) > 0) ? DIMMING_INTENS : 0.0f;
						dimming += (parent.getBlock(new Vector3f(getBlockWorldPos(new Vector3f(x - 1, y, z - 1)))) > 0) ? DIMMING_INTENS : 0.0f;
						dimming += (parent.getBlock(new Vector3f(getBlockWorldPos(new Vector3f(x + 1, y, z - 1)))) > 0) ? DIMMING_INTENS : 0.0f;
						dimming += (parent.getBlock(new Vector3f(getBlockWorldPos(new Vector3f(x - 1, y, z + 1)))) > 0) ? DIMMING_INTENS : 0.0f;


						setLight(x, y, z, 1.0f - dimming);
					} else if (blocks[x][y][z] == 0 && covered) {

						float luminance = getLight(x - 1, y, z) * LUMINANCE_INTENS;
						luminance += getLight(x + 1, y, z) * LUMINANCE_INTENS;
						luminance += getLight(x, y, z + 1) * LUMINANCE_INTENS;
						luminance += getLight(x, y, z - 1) * LUMINANCE_INTENS;

						luminance += getLight(x + 1, y, z + 1) * LUMINANCE_INTENS;
						luminance += getLight(x - 1, y, z - 1) * LUMINANCE_INTENS;
						luminance += getLight(x + 1, y, z + 1) * LUMINANCE_INTENS;
						luminance += getLight(x + 1, y, z - 1) * LUMINANCE_INTENS;


						setLight(x, y, z, 0.05f + (float) Math.min(luminance, 1.0f));
					} else {
						covered = true;
					}
				}
			}
		}


	}

	private boolean checkBlockTypeToDraw(int blockToCheck, int currentBlock) {
		return (blockToCheck == 0 || (BlockHelper.isBlockTypeTranslucent(blockToCheck) && !BlockHelper.isBlockTypeTranslucent(currentBlock)));
	}

	public void generateVertexArray() {
		color.clear();
		quads.clear();
		tex.clear();

		Vector3f offset = new Vector3f(position.x * chunkDimensions.x, position.y * chunkDimensions.y, position.z * chunkDimensions.z);

		for (int x = 0; x < chunkDimensions.x; x++) {
			for (int y = 0; y < chunkDimensions.y; y++) {
				for (int z = 0; z < chunkDimensions.z; z++) {

					int block = blocks[x][y][z];

					if (block > 0) {

						boolean drawFront, drawBack, drawLeft, drawRight, drawTop, drawBottom;
						int blockToCheck = 0;

						blockToCheck = parent.getBlock(new Vector3f(getBlockWorldPos(new Vector3f(x, y + 1, z))));
						drawTop = checkBlockTypeToDraw(blockToCheck, block);

						if (drawTop) {
							Vector3f colorOffset = BlockHelper.getColorOffsetFor(block, BlockHelper.SIDE.TOP);
							float shadowIntens = parent.getLight(new Vector3f(getBlockWorldPos(new Vector3f(x, y + 1, z))));


							float texOffsetX = BlockHelper.getTextureOffsetFor(block, BlockHelper.SIDE.TOP).x;
							float texOffsetY = BlockHelper.getTextureOffsetFor(block, BlockHelper.SIDE.TOP).y;

							color.add(colorOffset.x * shadowIntens * parent.getDaylight());
							color.add(colorOffset.y * shadowIntens * parent.getDaylight());
							color.add(colorOffset.z * shadowIntens * parent.getDaylight());
							tex.add(texOffsetX);
							tex.add(texOffsetY);
							quads.add(-0.5f + x + offset.x);
							quads.add(0.5f + y + offset.y);
							quads.add(0.5f + z + offset.z);

							color.add(colorOffset.x * shadowIntens * parent.getDaylight());
							color.add(colorOffset.y * shadowIntens * parent.getDaylight());
							color.add(colorOffset.z * shadowIntens * parent.getDaylight());
							tex.add(texOffsetX + 0.0625f);
							tex.add(texOffsetY);
							quads.add(0.5f + x + offset.x);
							quads.add(0.5f + y + offset.y);
							quads.add(0.5f + z + offset.z);

							color.add(colorOffset.x * shadowIntens * parent.getDaylight());
							color.add(colorOffset.y * shadowIntens * parent.getDaylight());
							color.add(colorOffset.z * shadowIntens * parent.getDaylight());
							tex.add(texOffsetX + 0.0625f);
							tex.add(texOffsetY + 0.0625f);
							quads.add(0.5f + x + offset.x);
							quads.add(0.5f + y + offset.y);
							quads.add(-0.5f + z + offset.z);

							color.add(colorOffset.x * shadowIntens * parent.getDaylight());
							color.add(colorOffset.y * shadowIntens * parent.getDaylight());
							color.add(colorOffset.z * shadowIntens * parent.getDaylight());
							tex.add(texOffsetX);
							tex.add(texOffsetY + 0.0625f);
							quads.add(-0.5f + x + offset.x);
							quads.add(0.5f + y + offset.y);
							quads.add(-0.5f + z + offset.z);
						}

						blockToCheck = parent.getBlock(new Vector3f(getBlockWorldPos(new Vector3f(x, y, z - 1))));
						drawFront = checkBlockTypeToDraw(blockToCheck, block);

						if (drawFront) {
							Vector3f colorOffset = BlockHelper.getColorOffsetFor(block, BlockHelper.SIDE.FRONT);
							float shadowIntens = parent.getLight(new Vector3f(getBlockWorldPos(new Vector3f(x, y, z - 1))));


							float texOffsetX = BlockHelper.getTextureOffsetFor(block, BlockHelper.SIDE.FRONT).x;
							float texOffsetY = BlockHelper.getTextureOffsetFor(block, BlockHelper.SIDE.FRONT).y;

							color.add(colorOffset.x * shadowIntens * parent.getDaylight() - DIM_BLOCK_SIDES);
							color.add(colorOffset.y * shadowIntens * parent.getDaylight() - DIM_BLOCK_SIDES);
							color.add(colorOffset.z * shadowIntens * parent.getDaylight() - DIM_BLOCK_SIDES);
							tex.add(texOffsetX);
							tex.add(texOffsetY);
							quads.add(-0.5f + x + offset.x);
							quads.add(0.5f + y + offset.y);
							quads.add(-0.5f + z + offset.z);

							color.add(colorOffset.x * shadowIntens * parent.getDaylight() - DIM_BLOCK_SIDES);
							color.add(colorOffset.y * shadowIntens * parent.getDaylight() - DIM_BLOCK_SIDES);
							color.add(colorOffset.z * shadowIntens * parent.getDaylight() - DIM_BLOCK_SIDES);
							tex.add(texOffsetX + 0.0625f);
							tex.add(texOffsetY);
							quads.add(0.5f + x + offset.x);
							quads.add(0.5f + y + offset.y);
							quads.add(-0.5f + z + offset.z);

							color.add(colorOffset.x * shadowIntens * parent.getDaylight() - DIM_BLOCK_SIDES);
							color.add(colorOffset.y * shadowIntens * parent.getDaylight() - DIM_BLOCK_SIDES);
							color.add(colorOffset.z * shadowIntens * parent.getDaylight() - DIM_BLOCK_SIDES);
							tex.add(texOffsetX + 0.0625f);
							tex.add(texOffsetY + 0.0625f);
							quads.add(0.5f + x + offset.x);
							quads.add(-0.5f + y + offset.y);
							quads.add(-0.5f + z + offset.z);

							color.add(colorOffset.x * shadowIntens * parent.getDaylight() - DIM_BLOCK_SIDES);
							color.add(colorOffset.y * shadowIntens * parent.getDaylight() - DIM_BLOCK_SIDES);
							color.add(colorOffset.z * shadowIntens * parent.getDaylight() - DIM_BLOCK_SIDES);
							tex.add(texOffsetX);
							tex.add(texOffsetY + 0.0625f);
							quads.add(-0.5f + x + offset.x);
							quads.add(-0.5f + y + offset.y);
							quads.add(-0.5f + z + offset.z);
						}

						blockToCheck = parent.getBlock(new Vector3f(getBlockWorldPos(new Vector3f(x, y, z + 1))));
						drawBack = checkBlockTypeToDraw(blockToCheck, block);

						if (drawBack) {
							Vector3f colorOffset = BlockHelper.getColorOffsetFor(block, BlockHelper.SIDE.BACK);
							float shadowIntens = parent.getLight(new Vector3f(getBlockWorldPos(new Vector3f(x, y, z + 1))));


							float texOffsetX = BlockHelper.getTextureOffsetFor(block, BlockHelper.SIDE.BACK).x;
							float texOffsetY = BlockHelper.getTextureOffsetFor(block, BlockHelper.SIDE.BACK).y;

							color.add(colorOffset.x * shadowIntens * parent.getDaylight() - DIM_BLOCK_SIDES);
							color.add(colorOffset.y * shadowIntens * parent.getDaylight() - DIM_BLOCK_SIDES);
							color.add(colorOffset.z * shadowIntens * parent.getDaylight() - DIM_BLOCK_SIDES);
							tex.add(texOffsetX);
							tex.add(texOffsetY + 0.0625f);
							quads.add(-0.5f + x + offset.x);
							quads.add(-0.5f + y + offset.y);
							quads.add(0.5f + z + offset.z);

							color.add(colorOffset.x * shadowIntens * parent.getDaylight() - DIM_BLOCK_SIDES);
							color.add(colorOffset.y * shadowIntens * parent.getDaylight() - DIM_BLOCK_SIDES);
							color.add(colorOffset.z * shadowIntens * parent.getDaylight() - DIM_BLOCK_SIDES);
							tex.add(texOffsetX + 0.0625f);
							tex.add(texOffsetY + 0.0625f);
							quads.add(0.5f + x + offset.x);
							quads.add(-0.5f + y + offset.y);
							quads.add(0.5f + z + offset.z);

							color.add(colorOffset.x * shadowIntens * parent.getDaylight() - DIM_BLOCK_SIDES);
							color.add(colorOffset.y * shadowIntens * parent.getDaylight() - DIM_BLOCK_SIDES);
							color.add(colorOffset.z * shadowIntens * parent.getDaylight() - DIM_BLOCK_SIDES);
							tex.add(texOffsetX + 0.0625f);
							tex.add(texOffsetY);

							quads.add(0.5f + x + offset.x);
							quads.add(0.5f + y + offset.y);
							quads.add(0.5f + z + offset.z);

							color.add(colorOffset.x * shadowIntens * parent.getDaylight() - DIM_BLOCK_SIDES);
							color.add(colorOffset.y * shadowIntens * parent.getDaylight() - DIM_BLOCK_SIDES);
							color.add(colorOffset.z * shadowIntens * parent.getDaylight() - DIM_BLOCK_SIDES);
							tex.add(texOffsetX);
							tex.add(texOffsetY);

							quads.add(-0.5f + x + offset.x);
							quads.add(0.5f + y + offset.y);
							quads.add(0.5f + z + offset.z);
						}


						blockToCheck = parent.getBlock(new Vector3f(getBlockWorldPos(new Vector3f(x - 1, y, z))));
						drawLeft = checkBlockTypeToDraw(blockToCheck, block);

						if (drawLeft) {
							Vector3f colorOffset = BlockHelper.getColorOffsetFor(block, BlockHelper.SIDE.LEFT);
							float shadowIntens = parent.getLight(new Vector3f(getBlockWorldPos(new Vector3f(x - 1, y, z))));

							float texOffsetX = BlockHelper.getTextureOffsetFor(block, BlockHelper.SIDE.LEFT).x;
							float texOffsetY = BlockHelper.getTextureOffsetFor(block, BlockHelper.SIDE.LEFT).y;

							color.add(colorOffset.x * shadowIntens * parent.getDaylight() - DIM_BLOCK_SIDES);
							color.add(colorOffset.y * shadowIntens * parent.getDaylight() - DIM_BLOCK_SIDES);
							color.add(colorOffset.z * shadowIntens * parent.getDaylight() - DIM_BLOCK_SIDES);
							tex.add(texOffsetX);
							tex.add(texOffsetY + 0.0625f);
							quads.add(-0.5f + x + offset.x);
							quads.add(-0.5f + y + offset.y);
							quads.add(-0.5f + z + offset.z);

							color.add(colorOffset.x * shadowIntens * parent.getDaylight() - DIM_BLOCK_SIDES);
							color.add(colorOffset.y * shadowIntens * parent.getDaylight() - DIM_BLOCK_SIDES);
							color.add(colorOffset.z * shadowIntens * parent.getDaylight() - DIM_BLOCK_SIDES);
							tex.add(texOffsetX + 0.0625f);
							tex.add(texOffsetY + 0.0625f);
							quads.add(-0.5f + x + offset.x);
							quads.add(-0.5f + y + offset.y);
							quads.add(0.5f + z + offset.z);

							color.add(colorOffset.x * shadowIntens * parent.getDaylight() - DIM_BLOCK_SIDES);
							color.add(colorOffset.y * shadowIntens * parent.getDaylight() - DIM_BLOCK_SIDES);
							color.add(colorOffset.z * shadowIntens * parent.getDaylight() - DIM_BLOCK_SIDES);
							tex.add(texOffsetX + 0.0625f);
							tex.add(texOffsetY);

							quads.add(-0.5f + x + offset.x);
							quads.add(0.5f + y + offset.y);
							quads.add(0.5f + z + offset.z);

							color.add(colorOffset.x * shadowIntens * parent.getDaylight() - DIM_BLOCK_SIDES);
							color.add(colorOffset.y * shadowIntens * parent.getDaylight() - DIM_BLOCK_SIDES);
							color.add(colorOffset.z * shadowIntens * parent.getDaylight() - DIM_BLOCK_SIDES);
							tex.add(texOffsetX);
							tex.add(texOffsetY);

							quads.add(-0.5f + x + offset.x);
							quads.add(0.5f + y + offset.y);
							quads.add(-0.5f + z + offset.z);
						}

						blockToCheck = parent.getBlock(new Vector3f(getBlockWorldPos(new Vector3f(x + 1, y, z))));
						drawRight = checkBlockTypeToDraw(blockToCheck, block);

						if (drawRight) {
							Vector3f colorOffset = BlockHelper.getColorOffsetFor(block, BlockHelper.SIDE.RIGHT);
							float shadowIntens = parent.getLight(new Vector3f(getBlockWorldPos(new Vector3f(x + 1, y, z))));

							float texOffsetX = BlockHelper.getTextureOffsetFor(block, BlockHelper.SIDE.RIGHT).x;
							float texOffsetY = BlockHelper.getTextureOffsetFor(block, BlockHelper.SIDE.RIGHT).y;

							color.add(colorOffset.x * shadowIntens * parent.getDaylight() - DIM_BLOCK_SIDES);
							color.add(colorOffset.y * shadowIntens * parent.getDaylight() - DIM_BLOCK_SIDES);
							color.add(colorOffset.z * shadowIntens * parent.getDaylight() - DIM_BLOCK_SIDES);
							tex.add(texOffsetX);
							tex.add(texOffsetY);
							quads.add(0.5f + x + offset.x);
							quads.add(0.5f + y + offset.y);
							quads.add(-0.5f + z + offset.z);

							color.add(colorOffset.x * shadowIntens * parent.getDaylight() - DIM_BLOCK_SIDES);
							color.add(colorOffset.y * shadowIntens * parent.getDaylight() - DIM_BLOCK_SIDES);
							color.add(colorOffset.z * shadowIntens * parent.getDaylight() - DIM_BLOCK_SIDES);
							tex.add(texOffsetX + 0.0625f);
							tex.add(texOffsetY);
							quads.add(0.5f + x + offset.x);
							quads.add(0.5f + y + offset.y);
							quads.add(0.5f + z + offset.z);

							color.add(colorOffset.x * shadowIntens * parent.getDaylight() - DIM_BLOCK_SIDES);
							color.add(colorOffset.y * shadowIntens * parent.getDaylight() - DIM_BLOCK_SIDES);
							color.add(colorOffset.z * shadowIntens * parent.getDaylight() - DIM_BLOCK_SIDES);
							tex.add(texOffsetX + 0.0625f);
							tex.add(texOffsetY + 0.0625f);
							quads.add(0.5f + x + offset.x);
							quads.add(-0.5f + y + offset.y);
							quads.add(0.5f + z + offset.z);

							color.add(colorOffset.x * shadowIntens * parent.getDaylight() - DIM_BLOCK_SIDES);
							color.add(colorOffset.y * shadowIntens * parent.getDaylight() - DIM_BLOCK_SIDES);
							color.add(colorOffset.z * shadowIntens * parent.getDaylight() - DIM_BLOCK_SIDES);
							tex.add(texOffsetX);
							tex.add(texOffsetY + 0.0625f);
							quads.add(0.5f + x + offset.x);
							quads.add(-0.5f + y + offset.y);
							quads.add(-0.5f + z + offset.z);
						}

						blockToCheck = parent.getBlock(new Vector3f(getBlockWorldPos(new Vector3f(x, y - 1, z))));
						drawBottom = checkBlockTypeToDraw(blockToCheck, block);

						if (drawBottom) {
							Vector3f colorOffset = BlockHelper.getColorOffsetFor(block, BlockHelper.SIDE.BOTTOM);
							float shadowIntens = parent.getLight(new Vector3f(getBlockWorldPos(new Vector3f(x, y - 1, z))));


							float texOffsetX = BlockHelper.getTextureOffsetFor(block, BlockHelper.SIDE.BOTTOM).x;
							float texOffsetY = BlockHelper.getTextureOffsetFor(block, BlockHelper.SIDE.BOTTOM).y;

							color.add(colorOffset.x * shadowIntens * parent.getDaylight());
							color.add(colorOffset.y * shadowIntens * parent.getDaylight());
							color.add(colorOffset.z * shadowIntens * parent.getDaylight());
							tex.add(texOffsetX);
							tex.add(texOffsetY);
							quads.add(-0.5f + x + offset.x);
							quads.add(-0.5f + y + offset.y);
							quads.add(-0.5f + z + offset.z);

							color.add(colorOffset.x * shadowIntens * parent.getDaylight());
							color.add(colorOffset.y * shadowIntens * parent.getDaylight());
							color.add(colorOffset.z * shadowIntens * parent.getDaylight());
							tex.add(texOffsetX + 0.0625f);
							tex.add(texOffsetY);
							quads.add(0.5f + x + offset.x);
							quads.add(-0.5f + y + offset.y);
							quads.add(-0.5f + z + offset.z);

							color.add(colorOffset.x * shadowIntens * parent.getDaylight());
							color.add(colorOffset.y * shadowIntens * parent.getDaylight());
							color.add(colorOffset.z * shadowIntens * parent.getDaylight());
							tex.add(texOffsetX + 0.0625f);
							tex.add(texOffsetY + 0.0625f);
							quads.add(0.5f + x + offset.x);
							quads.add(-0.5f + y + offset.y);
							quads.add(0.5f + z + offset.z);

							color.add(colorOffset.x * shadowIntens * parent.getDaylight());
							color.add(colorOffset.y * shadowIntens * parent.getDaylight());
							color.add(colorOffset.z * shadowIntens * parent.getDaylight());
							tex.add(texOffsetX);
							tex.add(texOffsetY + 0.0625f);
							quads.add(-0.5f + x + offset.x);
							quads.add(-0.5f + y + offset.y);
							quads.add(0.5f + z + offset.z);
						}

					}

				}
			}
		}

		dirty = false;
	}

	public void generateDisplayList() {

		if (chunkID == -1) {
			chunkID = rand.nextInt();
			displayList = glGenLists(1);
		}

		int qSize = quads.size();
		int tSize = tex.size();
		int cSize = color.size();


		FloatBuffer vb = BufferUtils.createFloatBuffer(qSize);

		for (Float f : quads) {
			vb.put(f);
		}

		FloatBuffer tb = BufferUtils.createFloatBuffer(tSize);

		for (Float f : tex) {
			tb.put(f);
		}

		FloatBuffer cb = BufferUtils.createFloatBuffer(cSize);

		for (Float f : color) {
			cb.put(f);
		}

		vb.flip();
		tb.flip();
		cb.flip();

		glNewList(displayList, GL_COMPILE);
		glEnableClientState(GL_VERTEX_ARRAY);
		glEnableClientState(GL_TEXTURE_COORD_ARRAY);
		glEnableClientState(GL_COLOR_ARRAY);
		glTexCoordPointer(2, 0, tb);
		glColorPointer(3, 0, cb);
		glVertexPointer(3, 0, vb);
		glDrawArrays(GL_QUADS, 0, qSize / 3);
		glDisableClientState(GL_COLOR_ARRAY);
		glDisableClientState(GL_TEXTURE_COORD_ARRAY);
		glDisableClientState(GL_VERTEX_ARRAY);
		glEndList();

	}

	public void clear() {
		for (int x = 0; x < chunkDimensions.x; x++) {
			for (int y = 0; y < chunkDimensions.y; y++) {
				for (int z = 0; z < chunkDimensions.z; z++) {
					setBlock(new Vector3f(x, y, z), 0x0, true);
				}
			}
		}
	}

	/**
	 * Returns the base elevation for the terrain.
	 */
	private float calcTerrainElevation(float x, float z) {
		float result = 0.0f;
		result += pGen1.noise(0.0009f * x, 0.0009f, 0.00009f * z) * 64.0f;
		return Math.abs(result);
	}

	/**
	 * Returns the roughness for the base terrain.
	 */
	private float calcTerrainRoughness(float x, float z) {
		float result = 0.0f;
		result += pGen1.noise(0.009f * x, 0.009f, 0.009f * z);
		return Math.abs(result);
	}

	/**
	 * Returns the detail level for the base terrain.
	 */
	private float calcTerrainDetail(float x, float z) {
		float result = 0.0f;
		result += pGen1.noise(0.05f * x, 0.05f, 0.05f * z);
		return Math.abs(result);
	}

	/**
	 * Returns the cave density for the base terrain.
	 */
	private float getCaveDensityAt(float x, float y, float z) {
		float result = 0.0f;
		result += pGen2.noise(0.01f * x, 0.01f * y, 0.01f * z);
		return result;
	}

	/**
	 * TODO
	 */
	private float getStoneDensity(float x, float y, float z) {
		float result = 0.0f;
		result += pGen2.noise(0.1f * x, 0.1f * y, 0.1f * z);
		return result;
	}
}