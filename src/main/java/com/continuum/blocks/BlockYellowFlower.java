package com.continuum.blocks;

import com.continuum.utilities.Helper;
import org.lwjgl.util.vector.Vector2f;
import org.lwjgl.util.vector.Vector4f;

/**
 * A flower billboard block.
 */
public class BlockYellowFlower extends Block {

	@Override
	public boolean isBlockTypeTranslucent() {
		return true;
	}

	@Override
	public Vector2f getTextureOffsetFor(Block.SIDE side) {
		return Helper.calcOffsetForTextureAt(12, 0);
	}

	@Override
	public boolean isPenetrable() {
		return true;
	}

	@Override
	public boolean isCastingShadows() {
		return false;
	}

	@Override
	public boolean shouldRenderBoundingBox() {
		return false;
	}

	@Override
	public BLOCK_FORM getBlockForm() {
		return BLOCK_FORM.BILLBOARD;
	}
}