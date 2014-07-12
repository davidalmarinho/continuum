package com.continuum;

import org.lwjgl.util.vector.Vector3f;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.newdawn.slick.opengl.Texture;
import java.io.FileInputStream;
import org.newdawn.slick.opengl.TextureLoader;
import static org.lwjgl.opengl.GL11.*;

/**
 * Class for chunk.
 *
 * Created by gil0mendes on 12/07/14.
 */
public class Chunk extends RenderObject {

	// The actual block ids for the chunk
	int[][][] blocks;

	// Create an unique id for each chunk
	static int maxChunkID = 0;
	int chunkID;

	// Count the overall number of quads
	public static int quadCounter = 0;

	// After init. display lists are used for rendering the chunks
	int displayListID = -1;
	int displayListDebugID = -1;

	// Texture map
	static Texture textureMap;

	// Size of one single chunk
	public static final Vector3f chunkDimensions = new Vector3f(64, 128, 64);

	static {
		try {
			textureMap = TextureLoader.getTexture("PNG", new FileInputStream(Chunk.class.getResource("Terrain.png").getPath()), GL_NEAREST);
		} catch (IOException ex) {
			Logger.getLogger(Chunk.class.getName()).log(Level.SEVERE, null, ex);
		}
	}

	public Chunk(Vector3f position) {
		this.position = position;

		// Generate and assign the chunk id
		chunkID = maxChunkID;
		maxChunkID++;

		blocks = new int[(int) chunkDimensions.x][(int) chunkDimensions.y][(int) chunkDimensions.z];

		System.out.println("Chunk created. ID = " + chunkID);
	}

	@Override
	public String toString() {
		int counter = 0;

		for (int x = 0; x < chunkDimensions.x; x++) {
			for (int y = 0; y < chunkDimensions.y; y++) {
				for (int z = 0; z < chunkDimensions.z; z++) {
					if (blocks[x][y][z] > 0) {
						counter++;
					}
				}
			}
		}

		return counter + " Blocks in this chunk.";
	}

	public boolean updateDisplayList() {

		if (this.displayListDebugID == -1) {
			this.displayListDebugID = glGenLists(1);

			glNewList(displayListDebugID, GL_COMPILE);
			glColor3f(255.0f, 0.0f, 0.0f);
			glBegin(GL_LINE_LOOP);
			glVertex3f(0.0f, 0.0f, 0.0f);
			glVertex3f(chunkDimensions.x, 0.0f, 0.0f);
			glVertex3f(chunkDimensions.x, chunkDimensions.y, 0.0f);
			glVertex3f(0.0f, chunkDimensions.y, 0.0f);
			glEnd();

			glBegin(GL_LINE_LOOP);
			glVertex3f(0.0f, 0.0f, 0.0f);
			glVertex3f(0.0f, 0.0f, chunkDimensions.z);
			glVertex3f(0.0f, chunkDimensions.y, chunkDimensions.z);
			glVertex3f(0.0f, chunkDimensions.y, 0.0f);
			glVertex3f(0.0f, 0.0f, 0.0f);
			glEnd();

			glBegin(GL_LINE_LOOP);
			glVertex3f(0.0f, 0.0f, chunkDimensions.z);
			glVertex3f(chunkDimensions.x, 0.0f, chunkDimensions.z);
			glVertex3f(chunkDimensions.x, chunkDimensions.y, chunkDimensions.z);
			glVertex3f(0.0f, chunkDimensions.y, chunkDimensions.z);
			glVertex3f(0.0f, 0.0f, chunkDimensions.z);
			glEnd();

			glBegin(GL_LINE_LOOP);
			glVertex3f(chunkDimensions.x, 0.0f, 0.0f);
			glVertex3f(chunkDimensions.x, 0.0f, chunkDimensions.z);
			glVertex3f(chunkDimensions.x, chunkDimensions.y, chunkDimensions.z);
			glVertex3f(chunkDimensions.x, chunkDimensions.y, 0.0f);
			glVertex3f(chunkDimensions.x, 0.0f, 0.0f);
			glEnd();
			glEndList();
		}

		if (displayListID == -1) {
			displayListID = glGenLists(1);

			textureMap.bind();

			glNewList(displayListID, GL_COMPILE);
			glBegin(GL_QUADS);

			for (int x = 0; x < chunkDimensions.x; x++) {
				for (int y = 0; y < chunkDimensions.y; y++) {
					for (int z = 0; z < chunkDimensions.z; z++) {

						if (blocks[x][y][z] > 0) {


							boolean drawFront = true, drawBack = true, drawLeft = true, drawRight = true, drawTop = true, drawBottom = true;

							if (z - 1 >= 0) {
								if (blocks[x][y][z - 1] != 0) {
									drawFront = false;
								}
							} else {
								drawFront = false;
							}

							if (z + 1 < chunkDimensions.z) {
								if (blocks[x][y][z + 1] != 0) {
									drawBack = false;
								}
							} else {
								drawBack = false;
							}

							if (x - 1 >= 0) {
								if (blocks[x - 1][y][z] != 0) {
									drawLeft = false;
								}
							} else {
								drawLeft = false;
							}

							if (x + 1 < chunkDimensions.x) {
								if (blocks[x + 1][y][z] != 0) {
									drawRight = false;
								}
							} else {
								drawRight = false;
							}

							if (y + 1 < chunkDimensions.y) {
								if (blocks[x][y + 1][z] != 0) {
									drawTop = false;
								}
							} else {
								drawTop = false;
							}

							if (y - 1 >= 0) {
								if (blocks[x][y - 1][z] != 0) {
									drawBottom = false;
								}
							} else {
								drawBottom = false;
							}

							glColor3f(1.0f, 1.0f, 1.0f);

							if (drawFront) {

								float texOffsetX = BlockHelper.getTextureOffsetFor(blocks[x][y][z], BlockHelper.SIDE.FRONT).x;
								float texOffsetY = BlockHelper.getTextureOffsetFor(blocks[x][y][z], BlockHelper.SIDE.FRONT).y;

								quadCounter++;
								glTexCoord2f(texOffsetX, texOffsetY);
								glVertex3f(-0.5f + x, 0.5f + y, -0.5f + z);

								glTexCoord2f(texOffsetX + 0.0625f, texOffsetY);
								glVertex3f(0.5f + x, 0.5f + y, -0.5f + z);

								glTexCoord2f(texOffsetX + 0.0625f, texOffsetY + 0.0625f);
								glVertex3f(0.5f + x, -0.5f + y, -0.5f + z);

								glTexCoord2f(texOffsetX, texOffsetY + 0.0625f);
								glVertex3f(-0.5f + x, -0.5f + y, -0.5f + z);

							}

							glColor3f(1.0f, 1.0f, 1.0f);

							if (drawBack) {
								float texOffsetX = BlockHelper.getTextureOffsetFor(blocks[x][y][z], BlockHelper.SIDE.BACK).x;
								float texOffsetY = BlockHelper.getTextureOffsetFor(blocks[x][y][z], BlockHelper.SIDE.BACK).y;

								quadCounter++;

								glTexCoord2f(texOffsetX, texOffsetY + 0.0625f);
								glVertex3f(-0.5f + x, -0.5f + y, 0.5f + z);

								glTexCoord2f(texOffsetX + 0.0625f, texOffsetY + 0.0625f);
								glVertex3f(0.5f + x, -0.5f + y, 0.5f + z);

								glTexCoord2f(texOffsetX + 0.0625f, texOffsetY);
								glVertex3f(0.5f + x, 0.5f + y, 0.5f + z);

								glTexCoord2f(texOffsetX, texOffsetY);
								glVertex3f(-0.5f + x, 0.5f + y, 0.5f + z);
							}

							glColor3f(1.0f, 1.0f, 1.0f);

							if (drawLeft) {
								float texOffsetX = BlockHelper.getTextureOffsetFor(blocks[x][y][z], BlockHelper.SIDE.LEFT).x;
								float texOffsetY = BlockHelper.getTextureOffsetFor(blocks[x][y][z], BlockHelper.SIDE.LEFT).y;

								quadCounter++;

								glTexCoord2f(texOffsetX, texOffsetY + 0.0625f);
								glVertex3f(-0.5f + x, -0.5f + y, -0.5f + z);

								glTexCoord2f(texOffsetX + 0.0625f, texOffsetY + 0.0625f);
								glVertex3f(-0.5f + x, -0.5f + y, 0.5f + z);

								glTexCoord2f(texOffsetX + 0.0625f, texOffsetY);
								glVertex3f(-0.5f + x, 0.5f + y, 0.5f + z);

								glTexCoord2f(texOffsetX, texOffsetY);
								glVertex3f(-0.5f + x, 0.5f + y, -0.5f + z);
							}

							glColor3f(1.0f, 1.0f, 1.0f);

							if (drawRight) {
								float texOffsetX = BlockHelper.getTextureOffsetFor(blocks[x][y][z], BlockHelper.SIDE.RIGHT).x;
								float texOffsetY = BlockHelper.getTextureOffsetFor(blocks[x][y][z], BlockHelper.SIDE.RIGHT).y;

								quadCounter++;

								glTexCoord2f(texOffsetX, texOffsetY);
								glVertex3f(0.5f + x, 0.5f + y, -0.5f + z);

								glTexCoord2f(texOffsetX + 0.0625f, texOffsetY);
								glVertex3f(0.5f + x, 0.5f + y, 0.5f + z);


								glTexCoord2f(texOffsetX + 0.0625f, texOffsetY + 0.0625f);
								glVertex3f(0.5f + x, -0.5f + y, 0.5f + z);

								glTexCoord2f(texOffsetX, texOffsetY + 0.0625f);
								glVertex3f(0.5f + x, -0.5f + y, -0.5f + z);
							}

							int rayLeft = x - 1;
							int rayRight = x + 1;

							int rayFront = z + 1;
							int rayBack = z - 1;

							int rayTop = y + 1;
							float shadowIntens = 0.0f;

							while (rayLeft > 0) {

								if (blocks[rayLeft][y + 1][z] > 0) {
									shadowIntens += (1.0f / 5.0f) / (x - rayLeft);
									break;
								}

								rayLeft--;
							}

							while (rayRight < chunkDimensions.x) {
								if (blocks[rayRight][y + 1][z] > 0) {
									shadowIntens += (1.0f / 5.0f) / (rayRight - x);
									break;
								}

								rayRight++;
							}

							while (rayTop < chunkDimensions.y) {

								if (blocks[x][rayTop][z] > 0) {
									shadowIntens += (1.0f / 5.0f) / (rayTop - y);
									break;
								}

								rayTop++;
							}

							while (rayFront < chunkDimensions.z) {

								if (blocks[x][y + 1][rayFront] > 0) {
									shadowIntens += (1.0f / 5.0f) / (rayFront - z);
									break;
								}

								rayFront++;
							}

							while (rayBack > 0) {

								if (blocks[x][y + 1][rayBack] > 0) {
									shadowIntens += (1.0f / 5.0f) / (z - rayBack);
									break;
								}

								rayBack--;
							}

							glColor3f(1.0f - shadowIntens, 1.0f - shadowIntens, 1.0f - shadowIntens);

							if (drawTop) {
								quadCounter++;

								glTexCoord2f(0.0f, 0.0f);
								glVertex3f(-0.5f + x, 0.5f + y, 0.5f + z);

								glTexCoord2f(0.0625f, 0.0f);
								glVertex3f(0.5f + x, 0.5f + y, 0.5f + z);

								glTexCoord2f(0.0625f, 0.0625f);
								glVertex3f(0.5f + x, 0.5f + y, -0.5f + z);

								glTexCoord2f(0.0f, 0.0625f);
								glVertex3f(-0.5f + x, 0.5f + y, -0.5f + z);
							}

							glColor3f(1.0f, 1.0f, 1.0f);

							if (drawBottom) {
								quadCounter++;

								glTexCoord2f(0.0f, 0.0625f);
								glVertex3f(-0.5f + x, -0.5f + y, -0.5f + z);

								glTexCoord2f(0.0625f, 0.0625f);
								glVertex3f(0.5f + x, -0.5f + y, -0.5f + z);

								glTexCoord2f(0.0625f, 0.0f);
								glVertex3f(0.5f + x, -0.5f + y, 0.5f + z);

								glTexCoord2f(0.0f, 0.0f);
								glVertex3f(-0.5f + x, -0.5f + y, 0.5f + z);
							}

						}
					}
				}
			}

			glEnd();
			glEndList();

			return true;

		}

		return false;
	}

	@Override
	public void render() {

		if (displayListID >= 0) {

			glPushMatrix();
			glTranslatef(position.x * (int) chunkDimensions.x, position.y * (int) chunkDimensions.y, position.z * (int) chunkDimensions.z);

			glCallList(displayListID);

			if (Configuration.displayDebug) {
				glCallList(this.displayListDebugID);
			}

			glPopMatrix();

		}
	}

	public int getBlock(int x, int y, int z) {
		return blocks[x][y][z];
	}

	public void setBlock(int x, int y, int z, int type) {
		blocks[x][y][z] = type;
	}
}