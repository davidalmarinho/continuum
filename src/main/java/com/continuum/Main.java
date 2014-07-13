package com.continuum;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.util.glu.GLU.*;

import java.io.IOException;
import java.nio.FloatBuffer;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.lwjgl.BufferUtils;
import org.lwjgl.LWJGLException;
import org.lwjgl.Sys;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.DisplayMode;
import org.lwjgl.opengl.PixelFormat;
import org.lwjgl.util.vector.Vector3f;

/**
 * Entry point class
 * <p/>
 * Created by gil0mendes on 11/07/14.
 */
public class Main {
	// Constant values
	public static final String GAME_TITLE = "Continuum (Pre) Alpha";
	public static final float DISPLAY_WIDTH = 1024.0f;
	public static final float DISPLAY_HEIGHT = 768.0f;

	// Logger
	public static final Logger LOGGER = Logger.getLogger(Main.class.getName());

	// Time at the start of the last render loop
	private long lastLoopTime = Helper.getInstance().getTime();

	// Time at last fps measurement.
	private long lastFpsTime;

	// Measured frames per second.
	private int fps;

	// World
	private World world;

	// Player
	private Player player;

	// Start logger
	static {
		try {
			LOGGER.addHandler(new FileHandler("errors.log", true));
		} catch (IOException ex) {
			LOGGER.log(Level.WARNING, ex.toString(), ex);
		}
	}

	public static void main(String[] args) {
		Main main = null;

		// Try create the main window
		try {
			main = new Main();

			main.create();
			main.start();
		} catch (Exception ex) {
			LOGGER.log(Level.SEVERE, ex.toString(), ex);
		} finally {
			// After crash or end render loop should destroy the main window
			if (main != null) {
				main.destroy();
			}
		}

		System.exit(0);
	}

	/**
	 * Empty constructor
	 */
	public Main() {
	}

	/**
	 * Create and initialize Display, Keyboard, Mouse and main loop.
	 *
	 * @throws LWJGLException
	 */
	public void create() throws LWJGLException {
		// Display
		Display.setDisplayMode(new DisplayMode((int) DISPLAY_WIDTH, (int) DISPLAY_HEIGHT));
		Display.setFullscreen(false);
		Display.setTitle("Continuum");
		Display.create(new PixelFormat().withDepthBits(24).withSamples(4));

		// Keyboard
		Keyboard.create();

		// Mouse
		Mouse.setGrabbed(true);
		Mouse.create();

		// OpenGL
		this.initGL();
		this.resizeGL();
	}

	/**
	 * Destroy window
	 */
	public void destroy() {
		Mouse.destroy();
		Keyboard.destroy();
		Display.destroy();
	}

	/**
	 * Start main loop
	 */
	public void start() {
		while (!Display.isCloseRequested()) {
			// Process the keyboard and mouse input
			processKeyboard();
			processMouse();

			// Sync. at 60 FPS
			Display.sync(60);

			long delta = Helper.getInstance().getTime() - lastLoopTime;
			lastLoopTime = Helper.getInstance().getTime();
			lastFpsTime += delta;
			fps++;

			// Update the FPS display in the title bar each second passed
			if (lastFpsTime >= 1000) {
				Display.setTitle(String.format("%s (FPS: %d, MEM: %d MB)", GAME_TITLE, fps, Runtime.getRuntime().freeMemory() / 1024 / 1024));
				lastFpsTime = 0;
				fps = 0;
			}

			// Update the scene
			update(delta);

			// Render the scene
			render();

			// Update Display
			Display.update();
		}

		Display.destroy();
	}

	/**
	 * Initialize OpenGL
	 */
	private void initGL() {
		// Enable OpenGL features
		glEnable(GL_CULL_FACE);
		glCullFace(GL_BACK);
		glEnable(GL_DEPTH_TEST);
		glEnable(GL_FOG);
		glDepthFunc(GL_LEQUAL);
		//glEnable(GL_BLEND);
		//glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);

		// Configure FOG
		glHint(GL_FOG_HINT, GL_NICEST);
		glFogi(GL_FOG_MODE, GL_LINEAR);
		glFogf(GL_FOG_DENSITY, 1.0f);
		glFogf(GL_FOG_START, 64);
		glFogf(GL_FOG_END, 256);

		// Initialize player
		this.player = new Player();

		// Initialize world
		this.world = new World("WORLD1", "123892", this.player);

		// Set parent for player
		this.player.setParent(this.world);

		// Initialize Chunks
		Chunk.init();

		// Initialize world
		world.init();
	}

	/**
	 * Setup OpenGL for defined resolution
	 */
	private void resizeGL() {
		glViewport(0, 0, (int) DISPLAY_WIDTH, (int) DISPLAY_HEIGHT);

		glMatrixMode(GL_PROJECTION);
		glLoadIdentity();
		gluPerspective(64.0f, DISPLAY_WIDTH / DISPLAY_HEIGHT, 0.01f, 1000f);
		glPushMatrix();

		glMatrixMode(GL_MODELVIEW);
		glLoadIdentity();
		glPushMatrix();
	}

	/**
	 * Render the scene
	 */
	public void render() {
		if (world.isWorldGenerated()) {
			glClearColor(world.getDaylightColor().x, world.getDaylightColor().y, world.getDaylightColor().z, 1.0f);

			// Color the fog like the sky
			float[] fogColor = {world.getDaylightColor().x, world.getDaylightColor().y, world.getDaylightColor().z, 1.0f};
			FloatBuffer fogColorBuffer = BufferUtils.createFloatBuffer(4);
			fogColorBuffer.put(fogColor);
			fogColorBuffer.rewind();
			glFog(GL_FOG_COLOR, fogColorBuffer);

			glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

			glLoadIdentity();

			player.render();
			world.render();

			// Display the currently looked at block
			Vector3f blockPosition = player.calcViewBlockPosition();

			if (blockPosition != null) {

				int bpX = (int) blockPosition.x;
				int bpY = (int) blockPosition.y;
				int bpZ = (int) blockPosition.z;

				glColor3f(1.0f, 0.0f, 0.0f);
				glLineWidth(4.0f);

				glBegin(GL_LINES);
				glVertex3f(bpX - 0.5f, bpY - 0.5f, bpZ - 0.5f);
				glVertex3f(bpX + 0.5f, bpY - 0.5f, bpZ - 0.5f);

				glVertex3f(bpX - 0.5f, bpY - 0.5f, bpZ + 0.5f);
				glVertex3f(bpX + 0.5f, bpY - 0.5f, bpZ + 0.5f);

				glVertex3f(bpX - 0.5f, bpY + 0.5f, bpZ + 0.5f);
				glVertex3f(bpX + 0.5f, bpY + 0.5f, bpZ + 0.5f);

				glVertex3f(bpX - 0.5f, bpY + 0.5f, bpZ - 0.5f);
				glVertex3f(bpX + 0.5f, bpY + 0.5f, bpZ - 0.5f);

				glVertex3f(bpX - 0.5f, bpY - 0.5f, bpZ - 0.5f);
				glVertex3f(bpX - 0.5f, bpY - 0.5f, bpZ + 0.5f);

				glVertex3f(bpX + 0.5f, bpY - 0.5f, bpZ - 0.5f);
				glVertex3f(bpX + 0.5f, bpY - 0.5f, bpZ + 0.5f);

				glVertex3f(bpX - 0.5f, bpY + 0.5f, bpZ - 0.5f);
				glVertex3f(bpX - 0.5f, bpY + 0.5f, bpZ + 0.5f);

				glVertex3f(bpX + 0.5f, bpY + 0.5f, bpZ - 0.5f);
				glVertex3f(bpX + 0.5f, bpY + 0.5f, bpZ + 0.5f);

				glVertex3f(bpX - 0.5f, bpY - 0.5f, bpZ - 0.5f);
				glVertex3f(bpX - 0.5f, bpY + 0.5f, bpZ - 0.5f);

				glVertex3f(bpX + 0.5f, bpY - 0.5f, bpZ - 0.5f);
				glVertex3f(bpX + 0.5f, bpY + 0.5f, bpZ - 0.5f);

				glVertex3f(bpX - 0.5f, bpY - 0.5f, bpZ + 0.5f);
				glVertex3f(bpX - 0.5f, bpY + 0.5f, bpZ + 0.5f);

				glVertex3f(bpX + 0.5f, bpY - 0.5f, bpZ + 0.5f);
				glVertex3f(bpX + 0.5f, bpY + 0.5f, bpZ + 0.5f);

				glEnd();

			}

		} else {
			glClearColor(0.0f, 0.0f, 0.0f, 1.0f);
			glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
		}
	}

	/**
	 * Updates the player and the world
	 */
	public void update(long delta) {
		if (world.isWorldGenerated()) {
			world.update(delta);
			player.update(delta);
		}
	}

	// --- PROCESS

	/**
	 * Process Mouse data
	 */
	private void processMouse() {
	}

	/**
	 * Process Keyboard data
	 */
	private void processKeyboard() {
	}


}
