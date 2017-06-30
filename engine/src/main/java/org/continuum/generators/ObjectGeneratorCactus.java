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

import org.continuum.world.WorldProvider;

public class ObjectGeneratorCactus extends ObjectGenerator {

	/**
	 * @param w
	 * @param seed
	 */
	public ObjectGeneratorCactus(WorldProvider w, String seed) {
		super(w, seed);
	}

	/**
	 * Generates the cactus.
	 *
	 * @param posX Origin on the x-axis
	 * @param posY Origin on the y-axis
	 * @param posZ Origin on the z-axis
	 */
	@Override
	public void generate(int posX, int posY, int posZ, boolean update) {
		for (int y = posY; y < posY + 3; y++) {
			_worldProvider.setBlock(posX, y, posZ, (byte) 0x18, update, false);
		}
	}
}
