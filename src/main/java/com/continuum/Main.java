package com.continuum;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.util.glu.GLU.*;

import java.io.IOException;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.lwjgl.LWJGLException;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.DisplayMode;

/**
 * Created by gil0mendes on 11/07/14.
 */
public class Main
{

	// Window settings
	public static final float DISPLAY_WIDTH = 1152.0f;
	public static final float DISPLAY_HEIGHT = 648.0f;

	// Logger
	public static final Logger LOGGER = Logger.getLogger(Main.class.getName());

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
		// Start debug window
		if (Configuration.displayDebug) {
			java.awt.EventQueue.invokeLater(new Runnable() {
				@Override
				public void run() {
					new DebugWindow().setVisible(true);
				}
			});
		}

		Main main = null;

		// Try create the main window
		try {
			main = new Main();

			main.create();
			main.run();
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
	public Main() {}

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
		Display.create();

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
	public void run() {
		// Start main loop
		while (!Display.isCloseRequested() && !Keyboard.isKeyDown(Keyboard.KEY_ESCAPE)) {
			if (Display.isVisible()) {
				// Process inputs
				processKeyboard();
				processMouse();

				// Update and rend scene
				update();
				render();
			} else {
				if (Display.isDirty()) {
					render();
				}

				try {
					Thread.sleep(100);
				} catch (InterruptedException ex) {}
			}

			Display.update();
		}
	}

	/**
	 * Initialize OpenGL
	 */
	private void initGL() {
		glClearColor(0.5f, 0.75f, 1.0f, 1.0f);
		glLineWidth(2.0f);

		// Enable OpenGL features
		glEnable(GL_CULL_FACE);
		glEnable(GL_DEPTH_TEST);
		glEnable(GL_TEXTURE_2D);
		//glEnable(GL_FOG);

		// Disable OpenGL features
		glDisable(GL_LIGHTING);
		glDisable(GL_COLOR_MATERIAL);

		// Configure some features
		//glFogi(GL_FOG_MODE, GL_LINEAR);
		//glFogf(GL_FOG_DENSITY, 1.0f);
		//glHint(GL_FOG_HINT, GL_DONT_CARE);
		//glFogf(GL_FOG_START, 512.0f);
		//glFogf(GL_FOG_END, 1024.0f);

		// Initialize world
		this.world = new World();

		// Initialize player
		this.player = new Player(this.world);

		// Initialize Chunks
		Chunk.init();
	}

	/**
	 * Setup OpenGL for defined resolution
	 */
	private void resizeGL() {
		glViewport(0, 0, (int) DISPLAY_WIDTH, (int) DISPLAY_HEIGHT);

		glMatrixMode(GL_PROJECTION);
		glLoadIdentity();
		gluPerspective(64.0f, DISPLAY_WIDTH / DISPLAY_HEIGHT, 1f, 1024f);
		glPushMatrix();

		glMatrixMode(GL_MODELVIEW);
		glLoadIdentity();
		glPushMatrix();
	}

	/**
	 * Render the scene
	 */
	private void render() {
		glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
		glLoadIdentity();

		glPushMatrix();

		// Render player
		this.player.render();

		// Render world
		this.world.render();

		// Draw coordinate axis
		glBegin(GL_LINES);
		glColor3f(255.0f, 0.0f, 0.0f);
		glVertex3f(0.0f, 0.0f, 0.0f);
		glVertex3f(1000.0f, 0.0f, 0.0f);
		glEnd();

		glBegin(GL_LINES);
		glColor3f(0.0f, 255.0f, 0.0f);
		glVertex3f(0.0f, 0.0f, 0.0f);
		glVertex3f(0.0f, 1000.0f, 0.0f);
		glEnd();

		glBegin(GL_LINES);
		glColor3f(0.0f, 0.0f, 255.0f);
		glVertex3f(0.0f, 0.0f, 0.0f);
		glVertex3f(0.0f, 0.0f, 1000.0f);
		glEnd();

		glPopMatrix();

		Helper.getInstance().frameRendered();
	}

	/**
	 * Update main char
	 */
	private void update() {
		this.world.update();
		this.player.update();
	}

	// --- PROCESS

	/**
	 * Process Mouse data
	 */
	private void processMouse() {}

	/**
	 * Process Keyboard data
	 */
	private void processKeyboard() {
		if (Keyboard.isKeyDown(Keyboard.KEY_W))//move forward
		{
			this.player.walkForward();
		}
		if (Keyboard.isKeyDown(Keyboard.KEY_S))//move backwards
		{
			this.player.walkBackwards();
		}
		if (Keyboard.isKeyDown(Keyboard.KEY_A))//strafe left
		{
			this.player.strafeLeft();
		}
		if (Keyboard.isKeyDown(Keyboard.KEY_D))//strafe right
		{
			this.player.strafeRight();
		}
		if (Keyboard.isKeyDown(Keyboard.KEY_SPACE)) {
			this.player.jump();
		}
	}


}