package com.continuum.blocks;

import com.continuum.Helper;
import org.lwjgl.util.vector.Vector2f;
import org.lwjgl.util.vector.Vector4f;

/**
 * A stone block.
 */
public class BlockStone extends Block {

	@Override
	public boolean isBlockTypeTranslucent() {
		return false;
	}

	@Override
	public Vector4f getColorOffsetFor(Block.SIDE side) {
		return new Vector4f(1f, 1f, 1f, 1.0f);
	}

	@Override
	public Vector2f getTextureOffsetFor(Block.SIDE side) {
		return Helper.getInstance().calcOffsetForTextureAt(1, 0);
	}
}