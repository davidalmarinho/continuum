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
package org.continuum.world.entity;

import org.continuum.rendering.RenderableObject;
import org.continuum.datastructures.AABB;
import org.lwjgl.util.vector.Vector3f;

public abstract class Entity implements RenderableObject {

    protected Vector3f _position = new Vector3f();

    /**
     * Returns the position of the entity.
     *
     * @return The position
     */
    public Vector3f getPosition() {
        return _position;
    }

    /**
     * Sets the position of the entity.
     *
     * @param position The position
     */
    public void setPosition(Vector3f position) {
        _position.set(position);
    }

    /**
     * @return The AABB of the entity
     */
    public abstract AABB getAABB();
}
