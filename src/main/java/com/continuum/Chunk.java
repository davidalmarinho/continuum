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
public class Chunk extends RenderObject implements Comparable<Chunk> {
	public static int maxChunkID = 0;
	private static final float MAX_LIGHT = 0.9f;
	private static final float MAX_LUMINANCE = 1f;
	private static final float MIN_LIGHT = 0.1f;
	private static final float DIMMING_INTENS = 0.1f;
	private static final float LUMINANCE_INTENS = 0.075f;
	private static final float DIM_BLOCK_SIDES = 0.15f;

	// TODO
	private List<Float> quads 	= new ArrayList<Float>();
	private List<Float> tex 	= new ArrayList<Float>();
	private List<Float> color 	= new ArrayList<Float>();

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

	@Override
	public int compareTo(Chunk o) {
		return new Double(calcDistanceToOrigin()).compareTo(o.calcDistanceToOrigin())*-1;
	}

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
		} catch (Exception e) {}
	}

	public Chunk(World p, Vector3f position) {
		this.position = position;

		if (Chunk.parent == null) {
			Chunk.parent = p;
		}

		blocks = new int[(int) chunkDimensions.x][(int) chunkDimensions.y][(int) chunkDimensions.z];
		light = new float[(int) chunkDimensions.x][(int) chunkDimensions.y][(int) chunkDimensions.z];
	}

	public void generate() {

		clear();

		int xOffset = (int) position.x * (int) chunkDimensions.x;
		int yOffset = (int) position.y * (int) chunkDimensions.y;
		int zOffset = (int) position.z * (int) chunkDimensions.z;

		for (int x = 0; x < Chunk.chunkDimensions.x; x++) {
			for (int z = 0; z < Chunk.chunkDimensions.z; z++) {
				setBlock(x, 0, z, 0x3);
			}
		}

		for (int x = 0; x < Chunk.chunkDimensions.x; x++) {
			for (int z = 0; z < Chunk.chunkDimensions.z; z++) {
				float height = (calcTerrainElevation(x + xOffset, z + zOffset) + (calcTerrainRoughness(x + xOffset, z + zOffset) * calcTerrainDetail(x + xOffset, z + zOffset)) * 64) + 64f;

				float y = height;

				// Block has air "on top" => Dirt!
				setBlock(x, (int) y, z, 0x1);

				y--;

				while (y > 0) {
					setBlock(x, (int) y, z, 0x2);

					if (height-y < height * 0.75f) {
						setBlock(x, (int) y, z, 0x3);
					}
					y--;
				}

				if (height < 64) {
					// Generate water
					for (int i = 64; i > 0; i--) {
						if (getBlock(x, i, z) == 0) {
							setBlock(x, i, z, 0x4);
						}
					}
				}
			}
		}
	}

	public void populate() {
		for (int x = 0; x < Chunk.chunkDimensions.x; x++) {
			for (int z = 0; z < Chunk.chunkDimensions.z; z++) {
				for (int y = 64; y < Chunk.chunkDimensions.y; y++) {
					if (parent.getBlock(getBlockWorldPosX(x), getBlockWorldPosY(y), getBlockWorldPosZ(z)) == 0x1 && rand.nextFloat() < 0.002f) {
						parent.generateTree(getBlockWorldPosX(x), getBlockWorldPosY((int) y) + 1, getBlockWorldPosZ(z));
					}
				}
			}
		}
	}

	@Override
	public String toString() {
		return String.format("Chunk (%d) cotaining %d Blocks.", chunkID, blockCount());
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
		try {
			return blocks[x][y][z];
		} catch (Exception e) {
			return 0;
		}

	}

	public void setBlock(int x, int y, int z, int type) {
		blocks[x][y][z] = type;
	}

	public int getBlockWorldPosX(int x) {
		return x + (int) position.x * (int) chunkDimensions.x;
	}

	public int getBlockWorldPosY(int y) {
		return y + (int) position.y * (int) chunkDimensions.y;
	}

	public int getBlockWorldPosZ(int z) {
		return z + (int) position.z * (int) chunkDimensions.z;
	}

	public void calcSunlight() {

		light = new float[(int) chunkDimensions.x][(int) chunkDimensions.y][(int) chunkDimensions.z];

		for (int x = 0; x < (int) chunkDimensions.x; x++) {
			for (int z = 0; z < (int) chunkDimensions.z; z++) {
				boolean covered = false;
				for (int y = (int) chunkDimensions.y - 1; y > 0; y--) {

					if (blocks[x][y][z] == 0 && !covered) {
						float dimming = 0.0f;
						dimming += (parent.getBlock(getBlockWorldPosX(x + 1), getBlockWorldPosY(y), getBlockWorldPosZ(z)) > 0) ? DIMMING_INTENS : 0.0f;
						dimming += (parent.getBlock(getBlockWorldPosX(x - 1), getBlockWorldPosY(y), getBlockWorldPosZ(z)) > 0) ? DIMMING_INTENS : 0.0f;
						dimming += (parent.getBlock(getBlockWorldPosX(x), getBlockWorldPosY(y), getBlockWorldPosZ(z + 1)) > 0) ? DIMMING_INTENS : 0.0f;
						dimming += (parent.getBlock(getBlockWorldPosX(x), getBlockWorldPosY(y), getBlockWorldPosZ(z - 1)) > 0) ? DIMMING_INTENS : 0.0f;

						dimming += (parent.getBlock(getBlockWorldPosX(x + 1), getBlockWorldPosY(y), getBlockWorldPosZ(z + 1)) > 0) ? DIMMING_INTENS : 0.0f;
						dimming += (parent.getBlock(getBlockWorldPosX(x - 1), getBlockWorldPosY(y), getBlockWorldPosZ(z - 1)) > 0) ? DIMMING_INTENS : 0.0f;
						dimming += (parent.getBlock(getBlockWorldPosX(x - 1), getBlockWorldPosY(y), getBlockWorldPosZ(z + 1)) > 0) ? DIMMING_INTENS : 0.0f;
						dimming += (parent.getBlock(getBlockWorldPosX(x + 1), getBlockWorldPosY(y), getBlockWorldPosZ(z - 1)) > 0) ? DIMMING_INTENS : 0.0f;

						setLight(x, y, z, Math.max(MAX_LIGHT - dimming, MIN_LIGHT));

					} else if (blocks[x][y][z] == 0 && covered) {

						float luminance = parent.getLight(getBlockWorldPosX(x - 1), getBlockWorldPosY(y), getBlockWorldPosZ(z)) * LUMINANCE_INTENS;
						luminance += parent.getLight(getBlockWorldPosX(x + 1), getBlockWorldPosY(y), getBlockWorldPosZ(z)) * LUMINANCE_INTENS;
						luminance += parent.getLight(getBlockWorldPosX(x), getBlockWorldPosY(y), getBlockWorldPosZ(z + 1)) * LUMINANCE_INTENS;
						luminance += parent.getLight(getBlockWorldPosX(x), getBlockWorldPosY(y), getBlockWorldPosZ(z - 1)) * LUMINANCE_INTENS;

						luminance += parent.getLight(getBlockWorldPosX(x + 1), getBlockWorldPosY(y), getBlockWorldPosZ(z + 1)) * LUMINANCE_INTENS;
						luminance += parent.getLight(getBlockWorldPosX(x - 1), getBlockWorldPosY(y), getBlockWorldPosZ(z - 1)) * LUMINANCE_INTENS;
						luminance += parent.getLight(getBlockWorldPosX(x + 1), getBlockWorldPosY(y), getBlockWorldPosZ(z - 1)) * LUMINANCE_INTENS;
						luminance += parent.getLight(getBlockWorldPosX(x - 1), getBlockWorldPosY(y), getBlockWorldPosZ(z + 1)) * LUMINANCE_INTENS;

						setLight(x, y, z, (float) Math.min(luminance, MAX_LUMINANCE));
					} else {
						covered = true;
					}
				}
			}
		}
	}

	private boolean checkBlockTypeToDraw(int blockToCheck, int currentBlock) {
		return (blockToCheck == 0 || (Helper.getInstance().isBlockTypeTranslucent(blockToCheck) && !Helper.getInstance().isBlockTypeTranslucent(currentBlock)));
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

						blockToCheck = parent.getBlock(getBlockWorldPosX(x), getBlockWorldPosY(y + 1), getBlockWorldPosZ(z));
						drawTop = checkBlockTypeToDraw(blockToCheck, block);

						if (drawTop) {
							Vector3f colorOffset = Helper.getInstance().getColorOffsetFor(block, Helper.SIDE.TOP);
							float shadowIntens = parent.getLight(getBlockWorldPosX(x), getBlockWorldPosY(y + 1), getBlockWorldPosZ(z));


							float texOffsetX = Helper.getInstance().getTextureOffsetFor(block, Helper.SIDE.TOP).x;
							float texOffsetY = Helper.getInstance().getTextureOffsetFor(block, Helper.SIDE.TOP).y;

							color.add(colorOffset.x * shadowIntens * parent.getDaylight() + MIN_LIGHT);
							color.add(colorOffset.y * shadowIntens * parent.getDaylight() + MIN_LIGHT);
							color.add(colorOffset.z * shadowIntens * parent.getDaylight() + MIN_LIGHT);
							tex.add(texOffsetX);
							tex.add(texOffsetY);
							quads.add(-0.5f + x + offset.x);
							quads.add(0.5f + y + offset.y);
							quads.add(0.5f + z + offset.z);

							color.add(colorOffset.x * shadowIntens * parent.getDaylight() + MIN_LIGHT);
							color.add(colorOffset.y * shadowIntens * parent.getDaylight() + MIN_LIGHT);
							color.add(colorOffset.z * shadowIntens * parent.getDaylight() + MIN_LIGHT);
							tex.add(texOffsetX + 0.0625f);
							tex.add(texOffsetY);
							quads.add(0.5f + x + offset.x);
							quads.add(0.5f + y + offset.y);
							quads.add(0.5f + z + offset.z);

							color.add(colorOffset.x * shadowIntens * parent.getDaylight() + MIN_LIGHT);
							color.add(colorOffset.y * shadowIntens * parent.getDaylight() + MIN_LIGHT);
							color.add(colorOffset.z * shadowIntens * parent.getDaylight() + MIN_LIGHT);
							tex.add(texOffsetX + 0.0625f);
							tex.add(texOffsetY + 0.0625f);
							quads.add(0.5f + x + offset.x);
							quads.add(0.5f + y + offset.y);
							quads.add(-0.5f + z + offset.z);

							color.add(colorOffset.x * shadowIntens * parent.getDaylight() + MIN_LIGHT);
							color.add(colorOffset.y * shadowIntens * parent.getDaylight() + MIN_LIGHT);
							color.add(colorOffset.z * shadowIntens * parent.getDaylight() + MIN_LIGHT);
							tex.add(texOffsetX);
							tex.add(texOffsetY + 0.0625f);
							quads.add(-0.5f + x + offset.x);
							quads.add(0.5f + y + offset.y);
							quads.add(-0.5f + z + offset.z);
						}

						blockToCheck = parent.getBlock(getBlockWorldPosX(x), getBlockWorldPosY(y), getBlockWorldPosZ(z - 1));
						drawFront = checkBlockTypeToDraw(blockToCheck, block);

						if (drawFront) {
							Vector3f colorOffset = Helper.getInstance().getColorOffsetFor(block, Helper.SIDE.FRONT);
							float shadowIntens = parent.getLight(getBlockWorldPosX(x), getBlockWorldPosY(y), getBlockWorldPosZ(z - 1));


							float texOffsetX = Helper.getInstance().getTextureOffsetFor(block, Helper.SIDE.FRONT).x;
							float texOffsetY = Helper.getInstance().getTextureOffsetFor(block, Helper.SIDE.FRONT).y;

							color.add(colorOffset.x * shadowIntens * parent.getDaylight() + MIN_LIGHT - DIM_BLOCK_SIDES);
							color.add(colorOffset.y * shadowIntens * parent.getDaylight() + MIN_LIGHT - DIM_BLOCK_SIDES);
							color.add(colorOffset.z * shadowIntens * parent.getDaylight() + MIN_LIGHT - DIM_BLOCK_SIDES);
							tex.add(texOffsetX);
							tex.add(texOffsetY);
							quads.add(-0.5f + x + offset.x);
							quads.add(0.5f + y + offset.y);
							quads.add(-0.5f + z + offset.z);

							color.add(colorOffset.x * shadowIntens * parent.getDaylight() + MIN_LIGHT - DIM_BLOCK_SIDES);
							color.add(colorOffset.y * shadowIntens * parent.getDaylight() + MIN_LIGHT - DIM_BLOCK_SIDES);
							color.add(colorOffset.z * shadowIntens * parent.getDaylight() + MIN_LIGHT - DIM_BLOCK_SIDES);
							tex.add(texOffsetX + 0.0625f);
							tex.add(texOffsetY);
							quads.add(0.5f + x + offset.x);
							quads.add(0.5f + y + offset.y);
							quads.add(-0.5f + z + offset.z);

							color.add(colorOffset.x * shadowIntens * parent.getDaylight() + MIN_LIGHT - DIM_BLOCK_SIDES);
							color.add(colorOffset.y * shadowIntens * parent.getDaylight() + MIN_LIGHT - DIM_BLOCK_SIDES);
							color.add(colorOffset.z * shadowIntens * parent.getDaylight() + MIN_LIGHT - DIM_BLOCK_SIDES);
							tex.add(texOffsetX + 0.0625f);
							tex.add(texOffsetY + 0.0625f);
							quads.add(0.5f + x + offset.x);
							quads.add(-0.5f + y + offset.y);
							quads.add(-0.5f + z + offset.z);

							color.add(colorOffset.x * shadowIntens * parent.getDaylight() + MIN_LIGHT - DIM_BLOCK_SIDES);
							color.add(colorOffset.y * shadowIntens * parent.getDaylight() + MIN_LIGHT - DIM_BLOCK_SIDES);
							color.add(colorOffset.z * shadowIntens * parent.getDaylight() + MIN_LIGHT - DIM_BLOCK_SIDES);
							tex.add(texOffsetX);
							tex.add(texOffsetY + 0.0625f);
							quads.add(-0.5f + x + offset.x);
							quads.add(-0.5f + y + offset.y);
							quads.add(-0.5f + z + offset.z);
						}

						blockToCheck = parent.getBlock(getBlockWorldPosX(x), getBlockWorldPosY(y), getBlockWorldPosZ(z + 1));
						drawBack = checkBlockTypeToDraw(blockToCheck, block);

						if (drawBack) {
							Vector3f colorOffset = Helper.getInstance().getColorOffsetFor(block, Helper.SIDE.BACK);
							float shadowIntens = parent.getLight(getBlockWorldPosX(x), getBlockWorldPosY(y), getBlockWorldPosZ(z + 1));


							float texOffsetX = Helper.getInstance().getTextureOffsetFor(block, Helper.SIDE.BACK).x;
							float texOffsetY = Helper.getInstance().getTextureOffsetFor(block, Helper.SIDE.BACK).y;

							color.add(colorOffset.x * shadowIntens * parent.getDaylight() + MIN_LIGHT - DIM_BLOCK_SIDES);
							color.add(colorOffset.y * shadowIntens * parent.getDaylight() + MIN_LIGHT - DIM_BLOCK_SIDES);
							color.add(colorOffset.z * shadowIntens * parent.getDaylight() + MIN_LIGHT - DIM_BLOCK_SIDES);
							tex.add(texOffsetX);
							tex.add(texOffsetY + 0.0625f);
							quads.add(-0.5f + x + offset.x);
							quads.add(-0.5f + y + offset.y);
							quads.add(0.5f + z + offset.z);

							color.add(colorOffset.x * shadowIntens * parent.getDaylight() + MIN_LIGHT - DIM_BLOCK_SIDES);
							color.add(colorOffset.y * shadowIntens * parent.getDaylight() + MIN_LIGHT - DIM_BLOCK_SIDES);
							color.add(colorOffset.z * shadowIntens * parent.getDaylight() + MIN_LIGHT - DIM_BLOCK_SIDES);
							tex.add(texOffsetX + 0.0625f);
							tex.add(texOffsetY + 0.0625f);
							quads.add(0.5f + x + offset.x);
							quads.add(-0.5f + y + offset.y);
							quads.add(0.5f + z + offset.z);

							color.add(colorOffset.x * shadowIntens * parent.getDaylight() + MIN_LIGHT - DIM_BLOCK_SIDES);
							color.add(colorOffset.y * shadowIntens * parent.getDaylight() + MIN_LIGHT - DIM_BLOCK_SIDES);
							color.add(colorOffset.z * shadowIntens * parent.getDaylight() + MIN_LIGHT - DIM_BLOCK_SIDES);
							tex.add(texOffsetX + 0.0625f);
							tex.add(texOffsetY);

							quads.add(0.5f + x + offset.x);
							quads.add(0.5f + y + offset.y);
							quads.add(0.5f + z + offset.z);

							color.add(colorOffset.x * shadowIntens * parent.getDaylight() + MIN_LIGHT - DIM_BLOCK_SIDES);
							color.add(colorOffset.y * shadowIntens * parent.getDaylight() + MIN_LIGHT - DIM_BLOCK_SIDES);
							color.add(colorOffset.z * shadowIntens * parent.getDaylight() + MIN_LIGHT - DIM_BLOCK_SIDES);
							tex.add(texOffsetX);
							tex.add(texOffsetY);

							quads.add(-0.5f + x + offset.x);
							quads.add(0.5f + y + offset.y);
							quads.add(0.5f + z + offset.z);
						}

						blockToCheck = parent.getBlock(getBlockWorldPosX(x - 1), getBlockWorldPosY(y), getBlockWorldPosZ(z));
						drawLeft = checkBlockTypeToDraw(blockToCheck, block);

						if (drawLeft) {
							Vector3f colorOffset = Helper.getInstance().getColorOffsetFor(block, Helper.SIDE.LEFT);
							float shadowIntens = parent.getLight(getBlockWorldPosX(x - 1), getBlockWorldPosY(y), getBlockWorldPosZ(z));

							float texOffsetX = Helper.getInstance().getTextureOffsetFor(block, Helper.SIDE.LEFT).x;
							float texOffsetY = Helper.getInstance().getTextureOffsetFor(block, Helper.SIDE.LEFT).y;

							color.add(colorOffset.x * shadowIntens * parent.getDaylight() + MIN_LIGHT - DIM_BLOCK_SIDES);
							color.add(colorOffset.y * shadowIntens * parent.getDaylight() + MIN_LIGHT - DIM_BLOCK_SIDES);
							color.add(colorOffset.z * shadowIntens * parent.getDaylight() + MIN_LIGHT - DIM_BLOCK_SIDES);
							tex.add(texOffsetX);
							tex.add(texOffsetY + 0.0625f);
							quads.add(-0.5f + x + offset.x);
							quads.add(-0.5f + y + offset.y);
							quads.add(-0.5f + z + offset.z);

							color.add(colorOffset.x * shadowIntens * parent.getDaylight() + MIN_LIGHT - DIM_BLOCK_SIDES);
							color.add(colorOffset.y * shadowIntens * parent.getDaylight() + MIN_LIGHT - DIM_BLOCK_SIDES);
							color.add(colorOffset.z * shadowIntens * parent.getDaylight() + MIN_LIGHT - DIM_BLOCK_SIDES);
							tex.add(texOffsetX + 0.0625f);
							tex.add(texOffsetY + 0.0625f);
							quads.add(-0.5f + x + offset.x);
							quads.add(-0.5f + y + offset.y);
							quads.add(0.5f + z + offset.z);

							color.add(colorOffset.x * shadowIntens * parent.getDaylight() + MIN_LIGHT - DIM_BLOCK_SIDES);
							color.add(colorOffset.y * shadowIntens * parent.getDaylight() + MIN_LIGHT - DIM_BLOCK_SIDES);
							color.add(colorOffset.z * shadowIntens * parent.getDaylight() + MIN_LIGHT - DIM_BLOCK_SIDES);
							tex.add(texOffsetX + 0.0625f);
							tex.add(texOffsetY);

							quads.add(-0.5f + x + offset.x);
							quads.add(0.5f + y + offset.y);
							quads.add(0.5f + z + offset.z);

							color.add(colorOffset.x * shadowIntens * parent.getDaylight() + MIN_LIGHT - DIM_BLOCK_SIDES);
							color.add(colorOffset.y * shadowIntens * parent.getDaylight() + MIN_LIGHT - DIM_BLOCK_SIDES);
							color.add(colorOffset.z * shadowIntens * parent.getDaylight() + MIN_LIGHT - DIM_BLOCK_SIDES);
							tex.add(texOffsetX);
							tex.add(texOffsetY);

							quads.add(-0.5f + x + offset.x);
							quads.add(0.5f + y + offset.y);
							quads.add(-0.5f + z + offset.z);
						}

						blockToCheck = parent.getBlock(getBlockWorldPosX(x + 1), getBlockWorldPosY(y), getBlockWorldPosZ(z));
						drawRight = checkBlockTypeToDraw(blockToCheck, block);

						if (drawRight) {
							Vector3f colorOffset = Helper.getInstance().getColorOffsetFor(block, Helper.SIDE.RIGHT);
							float shadowIntens = parent.getLight(getBlockWorldPosX(x + 1), getBlockWorldPosY(y), getBlockWorldPosZ(z));

							float texOffsetX = Helper.getInstance().getTextureOffsetFor(block, Helper.SIDE.RIGHT).x;
							float texOffsetY = Helper.getInstance().getTextureOffsetFor(block, Helper.SIDE.RIGHT).y;

							color.add(colorOffset.x * shadowIntens * parent.getDaylight() + MIN_LIGHT - DIM_BLOCK_SIDES);
							color.add(colorOffset.y * shadowIntens * parent.getDaylight() + MIN_LIGHT - DIM_BLOCK_SIDES);
							color.add(colorOffset.z * shadowIntens * parent.getDaylight() + MIN_LIGHT - DIM_BLOCK_SIDES);
							tex.add(texOffsetX);
							tex.add(texOffsetY);
							quads.add(0.5f + x + offset.x);
							quads.add(0.5f + y + offset.y);
							quads.add(-0.5f + z + offset.z);

							color.add(colorOffset.x * shadowIntens * parent.getDaylight() + MIN_LIGHT - DIM_BLOCK_SIDES);
							color.add(colorOffset.y * shadowIntens * parent.getDaylight() + MIN_LIGHT - DIM_BLOCK_SIDES);
							color.add(colorOffset.z * shadowIntens * parent.getDaylight() + MIN_LIGHT - DIM_BLOCK_SIDES);
							tex.add(texOffsetX + 0.0625f);
							tex.add(texOffsetY);
							quads.add(0.5f + x + offset.x);
							quads.add(0.5f + y + offset.y);
							quads.add(0.5f + z + offset.z);

							color.add(colorOffset.x * shadowIntens * parent.getDaylight() + MIN_LIGHT - DIM_BLOCK_SIDES);
							color.add(colorOffset.y * shadowIntens * parent.getDaylight() + MIN_LIGHT - DIM_BLOCK_SIDES);
							color.add(colorOffset.z * shadowIntens * parent.getDaylight() + MIN_LIGHT - DIM_BLOCK_SIDES);
							tex.add(texOffsetX + 0.0625f);
							tex.add(texOffsetY + 0.0625f);
							quads.add(0.5f + x + offset.x);
							quads.add(-0.5f + y + offset.y);
							quads.add(0.5f + z + offset.z);

							color.add(colorOffset.x * shadowIntens * parent.getDaylight() + MIN_LIGHT - DIM_BLOCK_SIDES);
							color.add(colorOffset.y * shadowIntens * parent.getDaylight() + MIN_LIGHT - DIM_BLOCK_SIDES);
							color.add(colorOffset.z * shadowIntens * parent.getDaylight() + MIN_LIGHT - DIM_BLOCK_SIDES);
							tex.add(texOffsetX);
							tex.add(texOffsetY + 0.0625f);
							quads.add(0.5f + x + offset.x);
							quads.add(-0.5f + y + offset.y);
							quads.add(-0.5f + z + offset.z);
						}

						blockToCheck = parent.getBlock(getBlockWorldPosX(x), getBlockWorldPosY(y - 1), getBlockWorldPosZ(z));
						drawBottom = checkBlockTypeToDraw(blockToCheck, block);

						if (drawBottom) {
							Vector3f colorOffset = Helper.getInstance().getColorOffsetFor(block, Helper.SIDE.BOTTOM);
							float shadowIntens = parent.getLight(getBlockWorldPosX(x), getBlockWorldPosY(y - 1), getBlockWorldPosZ(z));


							float texOffsetX = Helper.getInstance().getTextureOffsetFor(block, Helper.SIDE.BOTTOM).x;
							float texOffsetY = Helper.getInstance().getTextureOffsetFor(block, Helper.SIDE.BOTTOM).y;

							color.add(colorOffset.x * shadowIntens * parent.getDaylight() + MIN_LIGHT);
							color.add(colorOffset.y * shadowIntens * parent.getDaylight() + MIN_LIGHT);
							color.add(colorOffset.z * shadowIntens * parent.getDaylight() + MIN_LIGHT);
							tex.add(texOffsetX);
							tex.add(texOffsetY);
							quads.add(-0.5f + x + offset.x);
							quads.add(-0.5f + y + offset.y);
							quads.add(-0.5f + z + offset.z);

							color.add(colorOffset.x * shadowIntens * parent.getDaylight() + MIN_LIGHT);
							color.add(colorOffset.y * shadowIntens * parent.getDaylight() + MIN_LIGHT);
							color.add(colorOffset.z * shadowIntens * parent.getDaylight() + MIN_LIGHT);
							tex.add(texOffsetX + 0.0625f);
							tex.add(texOffsetY);
							quads.add(0.5f + x + offset.x);
							quads.add(-0.5f + y + offset.y);
							quads.add(-0.5f + z + offset.z);

							color.add(colorOffset.x * shadowIntens * parent.getDaylight() + MIN_LIGHT);
							color.add(colorOffset.y * shadowIntens * parent.getDaylight() + MIN_LIGHT);
							color.add(colorOffset.z * shadowIntens * parent.getDaylight() + MIN_LIGHT);
							tex.add(texOffsetX + 0.0625f);
							tex.add(texOffsetY + 0.0625f);
							quads.add(0.5f + x + offset.x);
							quads.add(-0.5f + y + offset.y);
							quads.add(0.5f + z + offset.z);

							color.add(colorOffset.x * shadowIntens * parent.getDaylight() + MIN_LIGHT);
							color.add(colorOffset.y * shadowIntens * parent.getDaylight() + MIN_LIGHT);
							color.add(colorOffset.z * shadowIntens * parent.getDaylight() + MIN_LIGHT);
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
	}

	public void generateDisplayList() {

		if (chunkID == -1) {
			chunkID = maxChunkID + 1;
			maxChunkID++;
			displayList = glGenLists(1);
		}

		FloatBuffer cb = null;
		FloatBuffer tb = null;
		FloatBuffer vb = null;

		vb = BufferUtils.createFloatBuffer(quads.size());

		for (Float f : quads) {
			vb.put(f);
		}

		tb = BufferUtils.createFloatBuffer(tex.size());

		for (Float f : tex) {
			tb.put(f);
		}

		tex.clear();

		cb = BufferUtils.createFloatBuffer(color.size());

		for (Float f : color) {
			cb.put(f);
		}

		color.clear();

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
		glDrawArrays(GL_QUADS, 0, quads.size() / 3);
		glDisableClientState(GL_COLOR_ARRAY);
		glDisableClientState(GL_TEXTURE_COORD_ARRAY);
		glDisableClientState(GL_VERTEX_ARRAY);
		glEndList();

		quads.clear();

	}

	public void clear() {
		for (int x = 0; x < chunkDimensions.x; x++) {
			for (int y = 0; y < chunkDimensions.y; y++) {
				for (int z = 0; z < chunkDimensions.z; z++) {
					setBlock(x, y, z, 0x0);
				}
			}
		}
	}

	/**
	 * Returns the base elevation for the terrain.
	 */
	private float calcTerrainElevation(float x, float z) {
		float result = 0.0f;
		result += parent.getpGen1().noise(0.002f * x, 0.002f, 0.002f * z) * 64;
		return result;
	}

	/**
	 * Returns the roughness for the base terrain.
	 */
	private float calcTerrainRoughness(float x, float z) {
		float result = 0.0f;
		result += parent.getpGen1().noise(0.01f * x, 0.01f, 0.01f * z);
		return Math.abs(result);
	}

	/**
	 * Returns the detail level for the base terrain.
	 */
	private float calcTerrainDetail(float x, float z) {
		float result = 0.0f;
		result += parent.getpGen1().noise(0.04f * x, 0.04f, 0.04f * z);
		return Math.abs(result);
	}

	/**
	 * Returns the cave density for the base terrain.
	 */
	private float getCaveDensityAt(float x, float y, float z) {
		float result = 0.0f;
		result += parent.getpGen2().noise(0.01f * x, 0.01f * y, 0.01f * z);
		return result;
	}

	/**
	 * Returns the cave density for the base terrain.
	 */
	private float getCanyonDensityAt(float x, float y, float z) {
		float result = 0.0f;
		result += parent.getpGen3().noise(0.006f * x, 0.006f * y, 0.006f * z);
		return result;
	}

	public int blockCount() {
		int counter = 0;

		for (int x = 0; x < (int) chunkDimensions.x; x++) {
			for (int z = 0; z < (int) chunkDimensions.z; z++) {
				for (int y = 0; y < (int) chunkDimensions.y; y++) {
					if (blocks[x][y][z] > 0) {
						counter++;
					}
				}
			}
		}
		return counter;
	}

	public void destroy() {
		chunkID = -1;
		glDeleteLists(displayList, -1);
		displayList = -1;
	}

	public double calcDistanceToOrigin() {
		Vector3f pcv = Vector3f.sub(position, parent.getPlayer().getPosition(), null);
		return Math.sqrt(Math.pow(pcv.x, 2) + Math.pow(pcv.z, 2));
	}
}