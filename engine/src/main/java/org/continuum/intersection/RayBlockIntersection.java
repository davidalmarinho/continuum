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
package org.continuum.intersection;

import org.continuum.blocks.Block;
import org.continuum.world.World;
import javolution.util.FastList;
import org.lwjgl.util.vector.Vector3f;

import java.util.Collections;

/**
 * Helper class for ray-box intersection tests.
 */
public class RayBlockIntersection {

    /**
     * Represents an intersection of a ray with the face of a block.
     *
     * @author Benjamin Glatzel <benjamin.glatzel@me.com>
     */
    public static class Intersection implements Comparable<Intersection> {

        private final double _d;
        private final double _t;
        private final Vector3f _rayOrigin, _intersectionPoint, _surfaceNormal, _blockPosition, _rayDirection;

        public Intersection(Vector3f blockPosition, Vector3f normal, double d, double t, Vector3f rayOrigin, Vector3f rayDirection, Vector3f intersectionPoint) {
            this._d = d;
            this._t = t;
            this._rayOrigin = rayOrigin;
            this._rayDirection = rayDirection;
            this._intersectionPoint = intersectionPoint;
            this._surfaceNormal = normal;
            this._blockPosition = blockPosition;
        }

        /**
         * @param o
         * @return
         */
        public int compareTo(Intersection o) {
            if (o == null) {
                return 0;
            }

            double distance = _t;
            double distance2 = o._t;

            if (distance == distance2)
                return 0;

            return distance2 > distance ? -1 : 1;
        }

        /**
         * @return
         */
        Vector3f getSurfaceNormal() {
            return _surfaceNormal;
        }

        /**
         * @return
         */
        public Vector3f calcAdjacentBlockPos() {
            return Vector3f.add(getBlockPosition(), getSurfaceNormal(), null);
        }

        /**
         * @return the blockPos
         */
        public Vector3f getBlockPosition() {
            return _blockPosition;
        }

        /**
         * @return
         */
        @Override
        public String toString() {
            return String.format("x: %.2f y: %.2f z: %.2f", _blockPosition.x, _blockPosition.y, _blockPosition.z);
        }
    }

    /**
     * Calculates the intersection of a given ray originating from a specified point with
     * a block. Returns a list of intersections ordered by the distance to the player.
     *
     * @param x
     * @param y
     * @param z
     * @param rayOrigin
     * @param rayDirection
     * @return Distance-ordered list of ray-face-intersections
     */
    public static FastList<Intersection> executeIntersection(World w, int x, int y, int z, Vector3f rayOrigin, Vector3f rayDirection) {
        /*
* Ignore invisible blocks.
*/
        if (Block.getBlockForType(w.getBlock(x, y, z)).isBlockInvisible()) {
            return null;
        }

        FastList<Intersection> result = new FastList<Intersection>();

        /*
* Fetch all vertices of the specified block.
*/
        Vector3f[] vertices = Block.AABBForBlockAt(x, y, z).getVertices();
        Vector3f blockPos = new Vector3f(x, y, z);

        /*
* Generate a new intersection for each side of the block.
*/

        // Front
        Intersection is = executeBlockFaceIntersection(blockPos, vertices[0], vertices[1], vertices[3], rayOrigin, rayDirection);
        if (is != null) {
            result.add(is);
        }

        // Back
        is = executeBlockFaceIntersection(blockPos, vertices[4], vertices[7], vertices[5], rayOrigin, rayDirection);
        if (is != null) {
            result.add(is);
        }

        // Left
        is = executeBlockFaceIntersection(blockPos, vertices[4], vertices[0], vertices[7], rayOrigin, rayDirection);
        if (is != null) {
            result.add(is);
        }

        // Right
        is = executeBlockFaceIntersection(blockPos, vertices[5], vertices[6], vertices[1], rayOrigin, rayDirection);
        if (is != null) {
            result.add(is);
        }

        // Top
        is = executeBlockFaceIntersection(blockPos, vertices[7], vertices[3], vertices[6], rayOrigin, rayDirection);
        if (is != null) {
            result.add(is);
        }

        // Bottom
        is = executeBlockFaceIntersection(blockPos, vertices[4], vertices[5], vertices[0], rayOrigin, rayDirection);
        if (is != null) {
            result.add(is);
        }

        /*
        * Sort the intersections by distance.
        */
        Collections.sort(result);
        return result;
    }

    /**
     * Calculates an intersection with the face of a block defined by 3 points.
     *
     * @param blockPos The position of the block to intersect with
     * @param v0 Point 1
     * @param v1 Point 2
     * @param v2 Point 3
     * @param origin Origin of the intersection ray
     * @param ray Direction of the intersection ray
     * @return Ray-face-intersection
     */
    private static Intersection executeBlockFaceIntersection(Vector3f blockPos, Vector3f v0, Vector3f v1, Vector3f v2, Vector3f origin, Vector3f ray) {

        // Calculate the plane to intersect with
        Vector3f a = new Vector3f();
        Vector3f.sub(v1, v0, a);
        Vector3f b = new Vector3f();
        Vector3f.sub(v2, v0, b);
        Vector3f norm = new Vector3f();
        Vector3f.cross(a, b, norm);

        double d = -(norm.x * v0.x + norm.y * v0.y + norm.z * v0.z);

        /**
         * Calculate the distance on the ray, where the intersection occurs.
         */
        double t = -(norm.x * origin.x + norm.y * origin.y + norm.z * origin.z + d) / (Vector3f.dot(ray, norm));

        if (t < 0)
            return null;

        /**
         * Calc. the point of intersection.
         */
        Vector3f intersectPoint = new Vector3f((float) (ray.x * t), (float) (ray.y * t), (float) (ray.z * t));
        Vector3f.add(intersectPoint, origin, intersectPoint);

        if (intersectPoint.x >= v0.x && intersectPoint.x <= Math.max(v1.x, v2.x) && intersectPoint.y >= v0.y && intersectPoint.y <= Math.max(v1.y, v2.y) && intersectPoint.z >= v0.z && intersectPoint.z <= Math.max(v1.z, v2.z)) {
            return new Intersection(blockPos, norm, d, t, origin, ray, intersectPoint);
        }

        return null;
    }
}
