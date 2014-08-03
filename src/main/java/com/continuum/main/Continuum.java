package com.continuum.main;

import java.awt.Font;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.util.glu.GLU.*;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

import com.continuum.rendering.ShaderManager;
import com.continuum.world.characters.Player;
import com.continuum.utilities.FastRandom;
import com.continuum.world.World;
import com.continuum.world.chunk.Chunk;
import javolution.util.FastList;
import org.lwjgl.LWJGLException;
import org.lwjgl.Sys;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.openal.AL;
import org.lwjgl.opengl.Display;

import org.lwjgl.opengl.GL11;
import org.lwjgl.util.vector.Vector3f;
import org.newdawn.slick.Color;
import org.newdawn.slick.TrueTypeFont;

/**
 * The heart and soul of Continuum.
 */
public final class Continuum {

	/* ------- */
	private static final int TICKS_PER_SECOND = 60;
	private static final int SKIP_TICKS = 1000 / TICKS_PER_SECOND;
	/* ------- */
	private static TrueTypeFont _font1;
	private long _lastLoopTime;
	private long _lastFpsTime;
	private int _fps;
	private final StringBuffer _consoleInput = new StringBuffer();
	private boolean _pauseGame = false;
	private boolean _runGame = true;
	private boolean _saveWorldOnExit = true;
	/* ------- */
	private float _meanFps;
	private float _memoryUsage;
	/* ------- */
	private Player _player;
	private World _world;
    /* ------- */
    private final FastRandom _rand = new FastRandom();
    /* ------- */
    private long _timeTicksPerSecond;
    /* ------- */
    private static Continuum _instance;
    /* ------- */
    private  final Logger _logger = Logger.getLogger("continuum");
    /* ------- */
    private boolean _sandbox = false;
    /* ------- */

    /**
     * Get game instance
     *
     * @return
     */
    public static Continuum getInstance() {
        if (_instance == null) {
            _instance = new Continuum();
        }

        return _instance;
    }

	/**
	 * Entry point of the application.
	 *
	 * @param args Arguments
	 */
	public static void main(String[] args) {
        Continuum.getInstance().addLogFileHandler("continuum.log", Level.SEVERE);
		Continuum.getInstance().getLogger().log(Level.INFO, "Welcome to {0}!", Configuration.GAME_TITLE);

        // Load native libraries
        try {
            loadNativeLibs();
        } catch (Exception ex) {
            Continuum.getInstance().getLogger().log(Level.SEVERE, "Couldn't link static libraries. Sorry: " + ex);
        }

		Continuum continuum = null;

		try {
			continuum = Continuum.getInstance();

			continuum.initDisplay();
            continuum.initControllers();

			continuum.initGame();

            continuum.startGame();
		} catch (Exception ex) {
			Continuum.getInstance().getLogger().log(Level.SEVERE, ex.toString()
            , ex);
		} finally {
			if (continuum != null) {
				continuum.destroy();
			}
		}

		System.exit(0);
	}

    private static void loadNativeLibs() throws Exception {
        if (System.getProperty("os.name").equals("Mac OS X")) {
            addLibraryPath("natives/macosx");
        } else if (System.getProperty("os.name").equals("Linux")) {
            addLibraryPath("natives/linux");
        } else {
            addLibraryPath("natives/windows");
        }
    }

    private static void addLibraryPath(String s) throws Exception {
        final Field usrPathsFields = ClassLoader.class.getDeclaredField("usr_paths");
        usrPathsFields.setAccessible(true);

        final String[] paths = (String[]) usrPathsFields.get(null);

        for (String path : paths){
            if (path.equals(s)) {
                return;
            }
        }

        final String[] newPaths = Arrays.copyOf(paths, paths.length + 1);
        newPaths[newPaths.length - 1] = s;
        usrPathsFields.set(null, newPaths);
    }

	/**
	 * Returns the system time in milliseconds.
	 *
	 * @return The system time in milliseconds.
	 */
	public long getTime() {
		if (_timeTicksPerSecond == 0) {
			return 0;
		}

		return (Sys.getTime() * 1000) / _timeTicksPerSecond;
	}

    /**
     * Init. the display.
     *
     * @throws LWJGLException
     */
    public void initDisplay() throws LWJGLException {
        Continuum.getInstance().getLogger().log(Level.INFO, "Loading Continuum. Please stand by...");

        // Display
        if (Configuration.FULLSCREEN){
            Display.setDisplayMode(Display.getDesktopDisplayMode());
            Display.setFullscreen(true);
        } else {
            Display.setDisplayMode(Configuration.DISPLAY_MODE);
        }

        Display.setTitle(Configuration.GAME_TITLE);
        Display.create(Configuration.PIXEL_FORMAT);
    }

    /**
     * Init. game inputs.
     *
     * @throws LWJGLException
     */
    public void initControllers() throws LWJGLException {
        // Keyboard
        Keyboard.create();
        Keyboard.enableRepeatEvents(true);

        // Mouse
        Mouse.setGrabbed(true);
        Mouse.create();
    }

	/**
	 * Clean up before exiting the application.
	 */
	private void destroy() {
        AL.destroy();
        Mouse.destroy();
		Keyboard.destroy();
		Display.destroy();
	}

	public void initGame() {
        _timeTicksPerSecond = Sys.getTimerResolution();

		// Init. fonts
		_font1 = new TrueTypeFont(new Font("Arial", Font.PLAIN, 12), true);

        /*
         * Load shaders.
         */
		ShaderManager.getInstance();

        /*
         * Init. OpenGL
         */
		glEnable(GL_CULL_FACE);
		glEnable(GL_DEPTH_TEST);
		glDepthFunc(GL_LEQUAL);
		GL11.glHint(GL11.GL_PERSPECTIVE_CORRECTION_HINT, GL11.GL_NICEST);
        glShadeModel(GL11.GL_SMOOTH);

		// Generate a world with a random seed value
		String worldSeed = Configuration.DEFAULT_SEED;

		if (worldSeed.length() == 0) {
			worldSeed = _rand.randomCharacterString(16);
		}

		initNewWorldAndPlayer("World1", worldSeed);
	}

	/**
	 * Renders the scene.
	 */
	private void render() {
        glFogi(GL_FOG_MODE, GL_LINEAR);

		// Update the viewing distance
		double minDist = Math.min(Configuration.getSettingNumeric("V_DIST_X") * Configuration.CHUNK_DIMENSIONS.x, Configuration.getSettingNumeric("V_DIST_Z") * Configuration.CHUNK_DIMENSIONS.z);
        double viewingDistance = minDist / 2f;
		glFogf(GL_FOG_START, (float)(viewingDistance * 0.05));
		glFogf(GL_FOG_END, (float)viewingDistance);

        /*
         * Render the player, world and HUD.
         */
		glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
		glLoadIdentity();

		_world.render();

        renderHUD();
	}

	/**
	 * Resizes the viewport according to the chosen display width and height.
	 */
	private void resizeViewport() {
		glViewport(0, 0, Display.getDisplayMode().getWidth(), Display.getDisplayMode().getHeight());

		glMatrixMode(GL_PROJECTION);
		glLoadIdentity();
		gluPerspective(80.0f, (float) Display.getDisplayMode().getWidth() / (float) Display.getDisplayMode().getHeight(), 0.1f, 756f);
		glPushMatrix();

		glMatrixMode(GL_MODELVIEW);
		glLoadIdentity();
		glPushMatrix();
	}

	/**
	 * Starts the render loop.
	 */
    public void startGame() {
		Continuum.getInstance().getLogger().log(Level.INFO, "Starting Continuum...");
		_lastLoopTime =getTime();

		double nextGameTick = getTime();
		int loopCounter;

        resizeViewport();

        /*
         * Main game loop.
         */
		while (_runGame && !Display.isCloseRequested()) {
			updateStatistics();
			processKeyboardInput();
			processMouseInput();

			// Pause the game while the debug console is being shown
			loopCounter = 0;
			while (getTime() > nextGameTick && loopCounter < Configuration.FRAME_SKIP_MAX_FRAMES) {
				if (!_pauseGame) {
					update();
				}
				nextGameTick += SKIP_TICKS;
				loopCounter++;
			}
			render();

			// Clear dirty flag and swap buffer
			Display.update();
		}

        /*
         * Save the world and exit the application.
         */
		if (_saveWorldOnExit) {
			_world.dispose();
		}

		Display.destroy();
	}

    public void stopGame() {
        _runGame = false;
    }

    public void pauseGame(){
        if (_world != null) {
            _world.suspendUpdateThread();
        }

        Mouse.setGrabbed(false);
        _pauseGame = true;
    }

    public void unpauseGame() {
        _pauseGame = false;
        Mouse.setGrabbed(false);

        if (_world != null) {
            _world.resumeUpdateThread();
        }
    }

	/**
	 * Updates the world.
	 */
	private void update() {
		_world.update();
	}

	/**
	 * Renders the HUD on the screen.
	 */
	private void renderHUD() {
		glMatrixMode(GL_PROJECTION);
		glPushMatrix();
		glLoadIdentity();
		glOrtho(0, Display.getDisplayMode().getWidth(), Display.getDisplayMode().getHeight(), 0, -5, 1);
		glMatrixMode(GL_MODELVIEW);
		glPushMatrix();
		glLoadIdentity();

		glDisable(GL_DEPTH_TEST);

		// Draw the crosshair
		if (Configuration.getSettingBoolean("CROSSHAIR")) {
			glColor4f(1f, 1f, 1f, 1f);
			glLineWidth(2f);

			glBegin(GL_LINES);
			glVertex2d(Display.getDisplayMode().getWidth() / 2f - 8f, Display.getDisplayMode().getHeight() / 2f);
			glVertex2d(Display.getDisplayMode().getWidth() / 2f + 8f, Display.getDisplayMode().getHeight() / 2f);
			glVertex2d(Display.getDisplayMode().getWidth() / 2f, Display.getDisplayMode().getHeight() / 2f - 8f);
			glVertex2d(Display.getDisplayMode().getWidth() / 2f, Display.getDisplayMode().getHeight() / 2f + 8f);
			glEnd();
		}

        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);

        /*
         * Draw debugging information.
         */
		if (Configuration.getSettingBoolean("DEBUG")) {
			_font1.drawString(4, 4, String.format("%s (fps: %.2f, mem usage: %.2f MB)", Configuration.GAME_TITLE, _meanFps, _memoryUsage));
			_font1.drawString(4, 22, String.format("%s", _player));
			_font1.drawString(4, 38, String.format("%s", _world));
			_font1.drawString(4, 54, String.format("total vus: %s", Chunk.getVertexArrayUpdateCount()));
		}

		if (_pauseGame) {
			// Display the console input text
			_font1.drawString(4, Display.getDisplayMode().getHeight() - 16 - 4, String.format("%s_", _consoleInput), Color.red);
		}

		glDisable(GL_BLEND);
		glEnable(GL_DEPTH_TEST);

		glMatrixMode(GL_PROJECTION);
		glPopMatrix();
		glMatrixMode(GL_MODELVIEW);
		glPopMatrix();
	}

	/*
	 * Process mouse input.
	 */
	private void processMouseInput() {
		while (Mouse.next()) {
			int button = Mouse.getEventButton();
			_player.processMouseInput(button, Mouse.getEventButtonState());
		}
	}

	/**
	 * Processes keyboard input.
	 */
	private void processKeyboardInput() {
		while (Keyboard.next()) {
			int key = Keyboard.getEventKey();

			if (key == Keyboard.KEY_ESCAPE && !Keyboard.isRepeatEvent() && Keyboard.getEventKeyState()) {
				toggleDebugConsole();
			}

			if (_pauseGame) {
				if (!Keyboard.isRepeatEvent() && Keyboard.getEventKeyState()) {
					if (key == Keyboard.KEY_BACK) {
						int length = _consoleInput.length() - 1;

						if (length < 0) {
							length = 0;
						}
						_consoleInput.setLength(length);

					} else if (key == Keyboard.KEY_RETURN) {
						processConsoleString();
					}

					char c = Keyboard.getEventCharacter();

					if (c >= 'a' && c < 'z' + 1 || c >= '0' && c < '9' + 1 || c >= 'A' && c < 'A' + 1 || c == ' ' || c == '_' || c == '.' || c == '!') {
						_consoleInput.append(c);
					}
				}
			} else {
				_player.processKeyboardInput(key, Keyboard.getEventKeyState(), Keyboard.isRepeatEvent());
			}
		}
	}

	/**
	 * Parses the console string and executes the command.
	 */
    private void processConsoleString() {
        boolean success = false;

        FastList<String> parsingResult = new FastList<String>();
        String temp = "";

        for (int i = 0; i < _consoleInput.length(); i++) {
            char c = _consoleInput.charAt(i);

            if (c != ' ') {
                temp = temp.concat(String.valueOf(c));
            }

            if (c == ' ' || i == _consoleInput.length() - 1) {
                parsingResult.add(temp);
                temp = "";
            }
        }

        // Try to parse the input
        try {
            if (parsingResult.get(0).equals("place")) {
                if (parsingResult.get(1).equals("tree")) {
                    _player.plantTree(Integer.parseInt(parsingResult.get(2)));
                    success = true;
                } else if (parsingResult.get(1).equals("block")) {
                    _player.placeBlock(Byte.parseByte(parsingResult.get(2)));
                    success = true;
                }
            } else if (parsingResult.get(0).equals("set")) {
                if (parsingResult.get(1).equals("time")) {
                    _world.setTime(Float.parseFloat(parsingResult.get(2)));
                    success = true;
                    // Otherwise try lookup the given variable within the settings
                } else {

                    Boolean bRes = Configuration.getSettingBoolean(parsingResult.get(1).toUpperCase());

                    if (bRes != null) {
                        Configuration.setSetting(parsingResult.get(1).toUpperCase(), Boolean.parseBoolean(parsingResult.get(2)));
                        success = true;
                    } else {
                        Double fRes = Configuration.getSettingNumeric(parsingResult.get(1).toUpperCase());
                        if (fRes != null) {
                            Configuration.setSetting(parsingResult.get(1).toUpperCase(), Double.parseDouble(parsingResult.get(2)));
                            success = true;
                        }
                    }
                }

            } else if (parsingResult.get(0).equals("respawn")) {
                _world.resetPlayer();
                success = true;
            } else if (parsingResult.get(0).equals("goto")) {
                int x = Integer.parseInt(parsingResult.get(1));
                int y = Integer.parseInt(parsingResult.get(2));
                int z = Integer.parseInt(parsingResult.get(3));
                _player.setPosition(new Vector3f(x, y, z));
                success = true;
            } else if (parsingResult.get(0).equals("exit")) {
                _saveWorldOnExit = true;
                _runGame = false;
                success = true;
            } else if (parsingResult.get(0).equals("exit!")) {
                _saveWorldOnExit = false;
                _runGame = false;
                success = true;
            } else if (parsingResult.get(0).equals("load")) {
                String worldSeed = _rand.randomCharacterString(16);

                if (parsingResult.size() > 1) {
                    worldSeed = parsingResult.get(1);
                }

                initNewWorldAndPlayer(worldSeed, worldSeed);
                success = true;
            } else if (parsingResult.get(0).equals("set_spawn")) {
                _world.setSpawningPoint();
                success = true;
            }
        } catch (Exception e) {
            Continuum.getInstance().getLogger().log(Level.INFO, e.getMessage());
        }

        if (success) {
            Continuum.getInstance().getLogger().log(Level.INFO, "Console command \"{0}\" accepted.", _consoleInput);
        } else {
            Continuum.getInstance().getLogger().log(Level.WARNING, "Console command \"{0}\" is invalid.", _consoleInput);
        }

        toggleDebugConsole();
    }

	/**
	 * Disables/enables the debug console.
	 */
	private void toggleDebugConsole() {
		if (!_pauseGame) {
			_world.suspendUpdateThread();
			_consoleInput.setLength(0);
			_pauseGame = true;
		} else {
			_pauseGame = false;
			_world.resumeUpdateThread();
		}
	}

	/**
	 * Prepares a new world with a given name and seed value.
	 *
	 * @param title Title of the world
	 * @param seed  Seed value used for the generators
	 */
	private void initNewWorldAndPlayer(String title, String seed) {
		Continuum.getInstance().getLogger().log(Level.INFO, "Creating new World with seed \"{0}\"", seed);

		// Get rid of the old world
		if (_world != null) {
			_world.dispose();
		}

		// Init some world
		_world = new World(title, seed);

		// Init. a new player
		_player = new Player(_world);
        _world.setPlayer(_player);

		_world.startUpdateThread();

		// Reset the delta value
		_lastLoopTime = getTime();
	}

	/**
	 * Updates the game statistics like FPS and memory usage.
	 */
	private void updateStatistics() {
		// Measure a delta value and the frames per second
		long delta = getTime() - _lastLoopTime;
		_lastLoopTime = getTime();
		_lastFpsTime += delta;
		_fps++;

		// Update the FPS and calculate the mean for displaying
		if (_lastFpsTime >= 1000) {
			_lastFpsTime = 0;

			_meanFps += _fps;
			_meanFps /= 2;

			// Calculate the current memory usage in MB
			_memoryUsage = (Runtime.getRuntime().maxMemory() - Runtime.getRuntime().freeMemory()) / 1048576;

			_fps = 0;
		}
	}

    public void addLogFileHandler(String s, Level logLevel)
    {
        try {
            FileHandler fh = new FileHandler(s, true);
            fh.setLevel(logLevel);
            fh.setFormatter(new SimpleFormatter());
            _logger.addHandler(fh);
        } catch (IOException ex) {
            _logger.log(Level.WARNING, ex.toString(), ex);
        }
    }

    public Logger getLogger() {
        return _logger;
    }

    public void setSandbox(boolean b) {
        _sandbox = b;
    }

    public boolean isSandboxed() {
        return _sandbox;
    }
}