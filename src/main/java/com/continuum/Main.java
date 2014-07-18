package com.continuum;

import java.awt.Font;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.util.glu.GLU.*;

import java.io.IOException;
import java.nio.FloatBuffer;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.lwjgl.BufferUtils;
import org.lwjgl.LWJGLException;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.DisplayMode;
import org.newdawn.slick.Color;
import org.newdawn.slick.TrueTypeFont;

/**
 * The heart and soul of Continuum.
 *
 * @author Gil Mendes <gil00mendes@gmail.com>
 */
public class Main {

	private static TrueTypeFont _font1;

	// Logger
	public static final Logger LOGGER = Logger.getLogger(Main.class.getName());

	// Time at the start of the last render loop
	private long _lastLoopTime = Helper.getInstance().getTime();

	// Time at last _fps measurement.
	private long _lastFpsTime;

	// Measured frames per second.
	private int _fps;
	private float _meanFps;

	// World
	private World world;

	// Player
	private Player player;

	FastRandom rand = new FastRandom();

	/**
	 * Init the logger.
	 */
	static {
		try {
			LOGGER.addHandler(new FileHandler("continuum.log", true));
		} catch (IOException ex) {
			LOGGER.log(Level.WARNING, ex.toString(), ex);
		}
	}

	/**
	 * Entry point of the application.
	 *
	 * @param args Arguments
	 */
	public static void main(String[] args) {
		LOGGER.log(Level.INFO, "Welcome to {0}!", Configuration.GAME_TITLE);

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
	 * Init. the display and mouse/keyboard input.
	 *
	 * @throws LWJGLException
	 */
	private void create() throws LWJGLException {
		LOGGER.log(Level.INFO, "Initializing display, input devices and OpenGL.");

		// Display
		Display.setDisplayMode(new DisplayMode(Configuration.DISPLAY_WIDTH, Configuration.DISPLAY_HEIGHT));
		Display.setFullscreen(Configuration.FULLSCREEN);
		Display.setTitle(Configuration.GAME_TITLE);
		Display.create(Configuration.PIXEL_FORMAT);

		// Keyboard
		Keyboard.create();
		Keyboard.enableRepeatEvents(true);

		// Mouse
		Mouse.setGrabbed(true);
		Mouse.create();

		// OpenGL
		this.initGL();
		this.resizeGL();
	}

	/**
	 * Clean up before exiting the application.
	 */
	private void destroy() {
		Mouse.destroy();
		Keyboard.destroy();
		Display.destroy();
	}

	/**
	 * Initializes OpenGL.
	 */
	private void initGL() {
		// Init. the fonts
		_font1 = new TrueTypeFont(new Font("Arial", Font.PLAIN, 12), true);

		// Init. OpenGL
		glEnable(GL_CULL_FACE);
		glEnable(GL_DEPTH_TEST);
		glEnable(GL_FOG);
		glDepthFunc(GL_LEQUAL);
		glEnable(GL_BLEND);
		glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
		glEnable(GL_ALPHA_TEST);
		glAlphaFunc(GL_GREATER, 0.1f);

		// Enable FOG
		glHint(GL_FOG_HINT, GL_NICEST);
		glFogi(GL_FOG_MODE, GL_LINEAR);
		glFogf(GL_FOG_DENSITY, 1.0f);
		float viewingDistance = (Configuration.VIEWING_DISTANCE_IN_CHUNKS.x * Configuration.CHUNK_DIMENSIONS.x) / 2f;
		glFogf(GL_FOG_START, viewingDistance - 64f);
		glFogf(GL_FOG_END, viewingDistance);

		try {
			// Init. textures and more
			Class.forName("com.continuum.Chunk");
			Chunk.init();
		} catch (ClassNotFoundException ex) {
			Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
		}
		try {
			Class.forName("com.continuum.World");
			World.init();
		} catch (ClassNotFoundException ex) {
			Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
		}

		// Init. the player and a world
		player = new Player();

		// Generate a world with a "random" seed value
		String worldSeed = rand.randomCharacterString(16);
		LOGGER.log(Level.INFO, "Creating new World with random seed \"{0}\"", worldSeed);
		world = new World("WORLD1", worldSeed, player);

		// Link the player to the world
		player.setParent(world);
	}

	/**
	 * Render the scene, player and HUD
	 */
	public void render() {
		// Use the color of the sky for clearing
		glClearColor(world.getDaylightColor().x, world.getDaylightColor().y, world.getDaylightColor().z, 1.0f);

		// Color the fog like the sky
		float[] fogColor = {world.getDaylightColor().x, world.getDaylightColor().y, world.getDaylightColor().z, 1.0f};
		FloatBuffer fogColorBuffer = BufferUtils.createFloatBuffer(4);
		fogColorBuffer.put(fogColor);
		fogColorBuffer.rewind();
		glFog(GL_FOG_COLOR, fogColorBuffer);

		// Render the player, world and HUD
		glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
		glLoadIdentity();
		player.render();
		world.render();
		renderHUD();
	}

	/**
	 * Resizes the viewport according to the chosen display with and height.
	 */
	private void resizeGL() {
		glViewport(0, 0, Configuration.DISPLAY_WIDTH, Configuration.DISPLAY_HEIGHT);

		glMatrixMode(GL_PROJECTION);
		glLoadIdentity();
		gluPerspective(84.0f, (float) Configuration.DISPLAY_WIDTH / (float) Configuration.DISPLAY_HEIGHT, 0.1f, 256f);
		glPushMatrix();

		glMatrixMode(GL_MODELVIEW);
		glLoadIdentity();
		glPushMatrix();
	}

	/**
	 * Start the render loop. The application can be terminated by pressing
	 * the ESCAPE key.
	 */
	public void start() {
		LOGGER.log(Level.INFO, "Starting the game...");

		while (!Keyboard.isKeyDown(Keyboard.KEY_ESCAPE)) {
			// Sync. at 120 FPS
			Display.sync(120);

			// Measure a delta value and the frames per second
			long delta = Helper.getInstance().getTime() - _lastLoopTime;
			_lastLoopTime = Helper.getInstance().getTime();
			_lastFpsTime += delta;
			_fps++;

			// Update the FPS and calculate the mean for display
			if (_lastFpsTime >= 1000) {
				_lastFpsTime = 0;

				_meanFps += _fps;
				_meanFps /= 2;

				_fps = 0;
			}

			// Updating and rendering of the scene. The delta
			// value is used within the updating process.
			update(delta);
			render();

			// Clear dirty flag and swap buffer
			Display.update();
		}

		Display.destroy();
	}

	/**
	 * Updates the player and the world
	 */
	private void update(long delta) {
		if (world.isWorldGenerated()) {
			world.update(delta);
			player.update(delta);
		}
	}

	/**
	 * Render the HUD on the screen.
	 */
	private void renderHUD() {
		glMatrixMode(GL_PROJECTION);
		glPushMatrix();
		glLoadIdentity();
		glOrtho(0, Display.getDisplayMode().getWidth(),
				Display.getDisplayMode().getHeight(), 0, -1, 1);
		glMatrixMode(GL_MODELVIEW);
		glPushMatrix();
		glLoadIdentity();

		glDisable(GL_DEPTH_TEST);
		glEnable(GL_BLEND);
		glEnable(GL_TEXTURE_2D);

		// Draw debugging information
		_font1.drawString(4, 4, String.format("%s (fps: %.2f, free heap space: %d MB)", Configuration.GAME_TITLE, _meanFps, Runtime.getRuntime().freeMemory() / 1048576), Color.white);
		_font1.drawString(4, 22, String.format("%s", player, Color.white));
		_font1.drawString(4, 38, String.format("%s", world, Color.white));

		glDisable(GL_TEXTURE_2D);

		glColor3f(1f, 1f, 1f);
		glLineWidth(2f);
		// Draw the crosshair
		glBegin(GL_LINES);
		glVertex2d(Display.getDisplayMode().getWidth() / 2f - 8f, Display.getDisplayMode().getHeight() / 2f);
		glVertex2d(Display.getDisplayMode().getWidth() / 2f + 8f, Display.getDisplayMode().getHeight() / 2f);

		glVertex2d(Display.getDisplayMode().getWidth() / 2f, Display.getDisplayMode().getHeight() / 2f - 8f);
		glVertex2d(Display.getDisplayMode().getWidth() / 2f, Display.getDisplayMode().getHeight() / 2f + 8f);
		glEnd();


		glEnable(GL_DEPTH_TEST);

		glMatrixMode(GL_PROJECTION);
		glPopMatrix();
		glMatrixMode(GL_MODELVIEW);
		glPopMatrix();
		glLoadIdentity();
	}

}
