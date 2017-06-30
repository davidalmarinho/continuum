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
package org.continuum.datastructures;

import org.lwjgl.util.vector.Vector3f;

/**
 * Represents the position of a block. This class is used within the
 * collision detection process.
 */
public final class BlockPosition implements Comparable<BlockPosition> {

    public final int x;
    public final int y;
    public final int z;
    private final Vector3f _origin;

    public BlockPosition(int x, int y, int z, Vector3f origin) {
        this.x = x;
        this.y = y;
        this.z = z;
        this._origin = origin;
    }

    double getDistance() {
        return new Vector3f(x - _origin.x, y - _origin.y, z - _origin.z).length();
    }

    public int compareTo(BlockPosition o) {
        double distance = getDistance();
        double oDistance = o.getDistance();

        if (oDistance > distance)
            return -1;

        if (oDistance < distance)
            return 1;

        return 0;
    }
}
