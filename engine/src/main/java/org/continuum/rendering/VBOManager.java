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
package org.continuum.rendering;

import gnu.trove.list.array.TIntArrayList;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL15;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;

/**
 * Provides support for creating and buffering Vertex Buffer Objects.
 *
 * @author Benjamin Glatzel <benjamin.glatzel@me.com>
 */
public class VBOManager {

    private static VBOManager _instance = null;
    private TIntArrayList _vertexBufferObjectPool = new TIntArrayList(32000);

    public static VBOManager getInstance() {
        if (_instance == null) {
            _instance = new VBOManager();
        }

        return _instance;
    }

    public VBOManager() {
        IntBuffer buffer = createVbos(32000);
        for (int i = 0; i < 32000; i++) {
            _vertexBufferObjectPool.add(buffer.get(i));
        }
    }

    private IntBuffer createVbos(int size) {
        IntBuffer buffer = BufferUtils.createIntBuffer(size);
        GL15.glGenBuffers(buffer);
        return buffer;
    }

    public synchronized Integer getVboId() {
        if (_vertexBufferObjectPool.size() > 0)
            return _vertexBufferObjectPool.removeAt(_vertexBufferObjectPool.size() - 1);
        return createVbos(1).get(0);
    }

    public synchronized void putVboId(int vboId) {
        if (vboId > 0) {
            _vertexBufferObjectPool.add(vboId);
        }
    }

    public void bufferVboData(int id, FloatBuffer buffer, int drawMode) {
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, id);
        GL15.glBufferData(GL15.GL_ARRAY_BUFFER, buffer, drawMode);
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
    }

    public void bufferVboElementData(int id, IntBuffer buffer, int drawMode) {
        GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, id);
        GL15.glBufferData(GL15.GL_ELEMENT_ARRAY_BUFFER, buffer, drawMode);
        GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, 0);
    }
}
