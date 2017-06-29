package com.continuum.world.chunk;

import com.continuum.rendering.VBOManager;
import gnu.trove.list.array.TFloatArrayList;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL15;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import static org.lwjgl.opengl.GL11.*;

public class ChunkMesh {

	public class VertexElements {
		public VertexElements() {
			quads = new TFloatArrayList();
			tex = new TFloatArrayList();
			color = new TFloatArrayList();
		}

		public final TFloatArrayList quads;
		public final TFloatArrayList tex;
		public final TFloatArrayList color;

		public FloatBuffer vertices;
		public IntBuffer indices;
	}

	public enum RENDER_TYPE {
		OPAQUE, BILLBOARD_AND_TRANSLUCENT, WATER, LAVA
	}

	private static final int STRIDE = (3 + 2 + 2 + 4) * 4;
	private static final int OFFSET_VERTEX = 0;
	private static final int OFFSET_TEX_0 = (3 * 4);
	private static final int OFFSET_TEX_1 = ((2 + 3) * 4);
	private static final int OFFSET_COLOR = ((2 + 3 + 2) * 4);

    /* ------ */

	private final int[] _vertexBuffers = new int[5];
	private final int[] _idxBuffers = new int[5];
	private final int[] _idxBufferCount = new int[5];
	public VertexElements[] _vertexElements = new VertexElements[5];

	private boolean _generated;

	public ChunkMesh() {
		_vertexElements[0] = new VertexElements();
		_vertexElements[1] = new VertexElements();
		_vertexElements[2] = new VertexElements();
		_vertexElements[3] = new VertexElements();
		_vertexElements[4] = new VertexElements();
	}

	/**
	 * Generates the display lists from the pre calculated arrays.
	 */
	public void generateVBOs() {
		// IMPORTANT: A mesh can only be generated once.
		if (_generated)
			return;

		for (int i = 0; i < _vertexBuffers.length; i++)
			generateVBO(i);

		_vertexElements = null;
		// Make sure this mesh can not be generated again
		_generated = true;
	}

	private void generateVBO(int id) {
		_vertexBuffers[id] = VBOManager.getInstance().getVboId();
		_idxBuffers[id] = VBOManager.getInstance().getVboId();
		_idxBufferCount[id] = _vertexElements[id].indices.limit();

		VBOManager.getInstance().bufferVboElementData(_idxBuffers[id], _vertexElements[id].indices, GL15.GL_STATIC_DRAW);
		VBOManager.getInstance().bufferVboData(_vertexBuffers[id], _vertexElements[id].vertices, GL15.GL_STATIC_DRAW);
	}

	private void renderVbo(int id) {
		if (_vertexBuffers[id] == -1)
			return;

		glEnableClientState(GL_VERTEX_ARRAY);
		glEnableClientState(GL_TEXTURE_COORD_ARRAY);
		glEnableClientState(GL_COLOR_ARRAY);

		GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, _idxBuffers[id]);
		GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, _vertexBuffers[id]);

		glVertexPointer(3, GL11.GL_FLOAT, STRIDE, OFFSET_VERTEX);

		GL13.glClientActiveTexture(GL13.GL_TEXTURE0);
		glTexCoordPointer(2, GL11.GL_FLOAT, STRIDE, OFFSET_TEX_0);

		GL13.glClientActiveTexture(GL13.GL_TEXTURE1);
		glTexCoordPointer(2, GL11.GL_FLOAT, STRIDE, OFFSET_TEX_1);

		glColorPointer(4, GL11.GL_FLOAT, STRIDE, OFFSET_COLOR);

		GL12.glDrawRangeElements(GL11.GL_TRIANGLES, 0, _idxBufferCount[id], _idxBufferCount[id], GL_UNSIGNED_INT, 0);

		glDisableClientState(GL_COLOR_ARRAY);
		glDisableClientState(GL_TEXTURE_COORD_ARRAY);
		glDisableClientState(GL_VERTEX_ARRAY);

		GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
		GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, 0);
	}

	public void render(RENDER_TYPE type) {
		switch (type) {
			case OPAQUE:
				renderVbo(type.ordinal());
				break;
			case BILLBOARD_AND_TRANSLUCENT:
				glEnable(GL_BLEND);
				glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);

				renderVbo(type.ordinal());

				glDisable(GL_CULL_FACE);

				// BILLBOARDS
				renderVbo(type.ordinal() + 1);

				glEnable(GL_CULL_FACE);
				glDisable(GL_BLEND);
				break;
			case WATER:
			case LAVA:
				glEnable(GL_BLEND);
				glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
				glDisable(GL_CULL_FACE);
				renderVbo(type.ordinal() + 1);
				glEnable(GL_CULL_FACE);
				glDisable(GL_BLEND);
				break;
			default:
				return;
		}
	}

	public void freeBuffers() {
		for (int i = 0; i < _vertexBuffers.length; i++) {
			int id = _vertexBuffers[i];

			VBOManager.getInstance().putVboId(id);
			_vertexBuffers[i] = -1;

			id = _idxBuffers[i];

			VBOManager.getInstance().putVboId(id);
			_idxBuffers[i] = -1;
		}
	}

	public boolean isGenerated() {
		return _generated;
	}
}