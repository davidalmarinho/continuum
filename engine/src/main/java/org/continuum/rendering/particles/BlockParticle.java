/*
 * Copyright 2014-2017 Gil Mendes
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.continuum.rendering.particles;

import org.continuum.blocks.Block;
import org.continuum.world.chunk.Chunk;
import org.lwjgl.opengl.GL11;
import org.lwjgl.util.vector.Vector3f;

import static org.lwjgl.opengl.GL11.*;

/**
 * Block particle class
 */
public class BlockParticle extends Particle {

	private float _lightOffset = 1.0f;
	private float _size;
	private byte _blockType = 0x1;

	public BlockParticle(int lifeTime, Vector3f position, byte blockType, BlockParticleEmitter parent) {
		super(lifeTime, position, parent);
		_blockType = blockType;
		_size = (float) ((_rand.randomDouble() + 1.0) / 2.0) * 0.08f;
		_lightOffset = (float) ((_rand.randomDouble() + 1.0) / 2.0) * 0.6f + 0.4f;

		_position.x += _rand.randomDouble() * 0.4;
		_position.y += _rand.randomDouble() * 0.4;
		_position.z += _rand.randomDouble() * 0.4;

		_lifetime *= (_rand.randomDouble() + 1.0) / 2.0;
	}

	@Override
	public boolean canMove() {
		BlockParticleEmitter pE = (BlockParticleEmitter) getParent();

		// Very simple "collision detection" for particles.
		if (pE.getParent().getBlockAtPosition(new Vector3f(_position.x + 2 * ((_velocity.x >= 0) ? _size : -_size), _position.y + 2 * ((_velocity.y >= 0) ? _size : -_size), _position.z + 2 * ((_velocity.z >= 0) ? _size : -_size))) != 0x0) {
			return false;
		}

		return true;
	}

	@Override
	protected void renderParticle() {
		glDisable(GL11.GL_CULL_FACE);
		glEnable(GL_TEXTURE_2D);
		glEnable(GL_BLEND);
		glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);

		BlockParticleEmitter pE = (BlockParticleEmitter) getParent();
		double lightValueSun = pE.getParent().getDaylight() * ((double) pE.getParent().getLightAtPosition(_position, Chunk.LIGHT_TYPE.SUN));
		lightValueSun = Math.pow(0.8, 15 - lightValueSun);
		double lightValueBlock = pE.getParent().getLightAtPosition(_position, Chunk.LIGHT_TYPE.BLOCK);
		lightValueBlock = Math.pow(0.8, 15 - lightValueBlock);
		float lightValue = (float) Math.max(lightValueSun, lightValueBlock) * _lightOffset;

		glBegin(GL_QUADS);
		GL11.glColor3f(lightValue, lightValue, lightValue);
		GL11.glTexCoord2f(Block.getBlockForType(_blockType).getTextureOffsetFor(Block.SIDE.FRONT).x, Block.getBlockForType(_blockType).getTextureOffsetFor(Block.SIDE.FRONT).y);
		GL11.glVertex3f(-_size, _size, -_size);
		GL11.glTexCoord2f(Block.getBlockForType(_blockType).getTextureOffsetFor(Block.SIDE.FRONT).x + 0.0624f, Block.getBlockForType(_blockType).getTextureOffsetFor(Block.SIDE.FRONT).y);
		GL11.glVertex3f(_size, _size, -_size);
		GL11.glTexCoord2f(Block.getBlockForType(_blockType).getTextureOffsetFor(Block.SIDE.FRONT).x + 0.0624f, Block.getBlockForType(_blockType).getTextureOffsetFor(Block.SIDE.FRONT).y + 0.0624f);
		GL11.glVertex3f(_size, -_size, -_size);
		GL11.glTexCoord2f(Block.getBlockForType(_blockType).getTextureOffsetFor(Block.SIDE.FRONT).x, Block.getBlockForType(_blockType).getTextureOffsetFor(Block.SIDE.FRONT).y + 0.0624f);
		GL11.glVertex3f(-_size, -_size, -_size);
		glEnd();

		glDisable(GL_BLEND);
		glDisable(GL11.GL_TEXTURE_2D);
		glEnable(GL11.GL_CULL_FACE);
	}
}
