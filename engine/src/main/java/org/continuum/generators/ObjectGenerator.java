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
package org.continuum.generators;

import org.continuum.utilities.FastRandom;
import org.continuum.world.WorldProvider;

/**
 * Object generators are used to generate objects within the world.
 */
public abstract class ObjectGenerator {

    /**
     *
     */
    final FastRandom _rand;
    /**
     *
     */
    final WorldProvider _worldProvider;

    /**
     * @param w
     * @param seed
     */
    ObjectGenerator(WorldProvider w, String seed) {
        _rand = new FastRandom(seed.hashCode());
        _worldProvider = w;
    }

    /**
     * Generates an object at the given position.
     *
     * @param posX   Position on the x-axis
     * @param posY   Position on the y-axis
     * @param posZ   Position on the z-axis
     * @param update If true, the chunk will be queued for updating
     */
    public abstract void generate(int posX, int posY, int posZ, boolean update);
}
