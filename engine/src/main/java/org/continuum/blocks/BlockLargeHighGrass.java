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
package org.continuum.blocks;

import org.continuum.utilities.Helper;
import org.lwjgl.util.vector.Vector2f;
import org.lwjgl.util.vector.Vector4f;

/**
 * A large high grass billboard block.
 */
public class BlockLargeHighGrass extends Block {
    @Override
    public boolean isBlockTypeTranslucent() {
        return true;
    }

    @Override
    public Vector4f getColorOffsetFor(SIDE side, double temperature, double humidity) {
        Vector4f foliageColor = foliageColorForTemperatureAndHumidity(temperature, humidity);
        return new Vector4f(foliageColor.x, foliageColor.y, foliageColor.z, 1.0f);
    }

    @Override
    public Vector2f getTextureOffsetFor(Block.SIDE side) {
        return Helper.calcOffsetForTextureAt(15, 11);
    }

    @Override
    public boolean isPenetrable() {
        return true;
    }

    @Override
    public boolean isCastingShadows() {
        return true;
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
