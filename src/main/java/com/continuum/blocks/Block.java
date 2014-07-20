package com.continuum.blocks;

import com.continuum.datastructures.AABB;
import com.continuum.utilities.Helper;
import com.continuum.utilities.VectorPool;
import org.lwjgl.util.vector.Vector2f;
import org.lwjgl.util.vector.Vector4f;

/**
 * Block class
 */
public abstract class Block {

	/**
	 * The six sides of a block.
	 */
	public static enum SIDE {

		/**
		 * Left side.
		 */
		LEFT,
		/**
		 * Right side.
		 */
		RIGHT,
		/**
		 * Top side.
		 */
		TOP,
		/**
		 * Bottom side.
		 */
		BOTTOM,
		/**
		 * Front side.
		 */
		FRONT,
		/**
		 * Back side.
		 */
		BACK
	}

	private static final Block[] _blocks = {new BlockAir(), new BlockGrass(), new BlockDirt(), new BlockStone(), new BlockWater(), new BlockTreeTrunk(), new BlockLeaf(), new BlockSand(), new BlockHardStone(), new BlockRedFlower(), new BlockYellowFlower(), new BlockHighGrass(), new BlockLargeHighGrass(), new BlockTorch(), new BlockLava(), new BlockWood(), new BlockCobbleStone(), new BlockIce(), new BlockGlass(), new BlockBrick(), new BlockCoal(), new BlockGold(), new BlockDarkLeaf()};
	private static final BlockNil nilBlock = new BlockNil();

	/**
	 * Returns the object for the given block type ID.
	 *
	 * @param type Block type ID
	 * @return The object for the given ID
	 */
	public static Block getBlockForType(byte type) {
		if (type < 0 || type >= _blocks.length) {
			return nilBlock;
		}

		return _blocks[type];
	}

	/**
	 * Returns the amount of blocks available.
	 *
	 * @return Amount of blocks available
	 */
	public static int getBlockCount() {
		return _blocks.length;
	}

	/**
	 * Returns true if a given block type is translucent.
	 *
	 * @return True if the block type is translucent
	 */
	public boolean isBlockTypeTranslucent() {
		return false;
	}

	/**
	 * Calculates the color offset for a given block type and a speific
	 * side of the block.
	 *
	 * @param side The block side
	 * @return The color offset
	 */
	public Vector4f getColorOffsetFor(SIDE side) {
		return new Vector4f(1.0f, 1.0f, 1.0f, 1.0f);
	}

	/**
	 * Calculates the texture offset for a given block type and a specific
	 * side of the block.
	 *
	 * @param side The side of the block
	 * @return The texture offset
	 */
	public Vector2f getTextureOffsetFor(SIDE side) {
		return Helper.getInstance().calcOffsetForTextureAt(2, 0);
	}

	/**
	 * Returns true, if the current block is a billboard.
	 *
	 * @return True if billboard
	 */
	public boolean isBlockBillboard() {
		return false;
	}

	/**
	 * Returns true, if the block is invisible.
	 *
	 * @return True if invisible
	 */
	public boolean isBlockInvisible() {
		return false;
	}

	/**
	 * Returns true, if the block should be ignored
	 * within the collision checks.
	 *
	 * @return True if penetrable
	 */
	public boolean isPenetrable() {
		return false;
	}

	/**
	 * Returns true, if the block should be considered
	 * while calculating shadows.
	 *
	 * @return True if casting shadows
	 */
	public boolean isCastingShadows() {
		return true;
	}

	/**
	 * @return
	 */
	public boolean doNotTessellate() {
		return false;
	}

	/**
	 * @return
	 */
	public boolean renderBoundingBox() {
		return true;
	}

	/**
	 * @return
	 */
	public byte getLuminance() {
		return 0;
	}

	/**
	 * @return
	 */
	public boolean isRemovable() {
		return true;
	}

	/**
	 * @param x
	 * @param y
	 * @param z
	 * @return
	 */
	public static AABB AABBForBlockAt(int x, int y, int z) {
		return new AABB(VectorPool.getVector(x, y, z), VectorPool.getVector(0.5f, 0.5f, 0.5f));
	}
}