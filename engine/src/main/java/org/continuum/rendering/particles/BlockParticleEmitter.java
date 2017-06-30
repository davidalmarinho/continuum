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

import org.continuum.rendering.TextureManager;
import org.continuum.world.World;

public class BlockParticleEmitter extends ParticleEmitter {

    private World _parent;
    private byte _currentBlockType = 0x1;

    public BlockParticleEmitter(World parent) {
        _parent = parent;
    }

    public void emitParticles(int amount, byte blockType) {
        _currentBlockType = blockType;
        super.emitParticles(amount);
    }

    public void render() {
        TextureManager.getInstance().bindTexture("terrain");
        super.render();
    }

    public World getParent() {
        return _parent;
    }

    @Override
    protected Particle createParticle() {
        return new BlockParticle(100, _origin, _currentBlockType, this);
    }
}
