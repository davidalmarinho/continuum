package com.continuum.rendering;

import com.continuum.main.Continuum;
import javolution.util.FastMap;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.ARBShaderObjects;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;
import org.newdawn.slick.util.ResourceLoader;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.logging.Level;

public class ShaderManager {

    private final FastMap<String, Integer> _shaderPrograms = new FastMap<String, Integer>(32);
    private final FastMap<String, Integer> _fragmentShader = new FastMap<String, Integer>(32);
    private final FastMap<String, Integer> _vertexShader = new FastMap<String, Integer>(32);
    private static ShaderManager _instance = null;

    /**
     * Returns (and creates – if necessary) the static instance
     * of this helper class.
     *
     * @return The instance
     */
    public static ShaderManager getInstance() {
        if (_instance == null) {
            _instance = new ShaderManager();
        }

        return _instance;
    }

    private ShaderManager() {
        initShader();

        Continuum.getInstance().getLogger().log(Level.INFO, "Loading Continuum shader manager...");
        Continuum.getInstance().getLogger().log(Level.INFO, "GL_VERSION: {0}", GL11.glGetString(GL11.GL_VERSION));
        Continuum.getInstance().getLogger().log(Level.INFO, "SHADING_LANGUAGE VERSION: {0}", GL11.glGetString(GL20.GL_SHADING_LANGUAGE_VERSION));
        Continuum.getInstance().getLogger().log(Level.INFO, "EXTENSIONS: {0}", GL11.glGetString(GL11.GL_EXTENSIONS));
    }

    private void initShader() {
		createVertexShader("sky_vert.glsl", "sky");
		createFragShader("sky_frag.glsl", "sky");
        createVertexShader("chunk_vert.glsl", "chunk");
        createFragShader("chunk_frag.glsl", "chunk");
        createVertexShader("cloud_vert.glsl", "cloud");
        createFragShader("cloud_frag.glsl", "cloud");

        for (FastMap.Entry<String, Integer> e = _fragmentShader.head(), end = _fragmentShader.tail(); (e = e.getNext()) != end; ) {
            int shaderProgram = GL20.glCreateProgram();

            GL20.glAttachShader(shaderProgram, _fragmentShader.get(e.getKey()));
            GL20.glAttachShader(shaderProgram, _vertexShader.get(e.getKey()));
            GL20.glLinkProgram(shaderProgram);
            GL20.glValidateProgram(shaderProgram);

            _shaderPrograms.put(e.getKey(), shaderProgram);
        }
    }

    private int createFragShader(String filename, String title) {

        _fragmentShader.put(title, GL20.glCreateShader(GL20.GL_FRAGMENT_SHADER));

        if (_fragmentShader.get(title) == 0) {
            return 0;
        }

        String fragCode = "";
        String line;
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(ResourceLoader.getResource("com/github/begla/blockmania/data/shaders/" + filename).openStream()));
            while ((line = reader.readLine()) != null) {
                fragCode += line + "\n";
            }
        } catch (Exception e) {
            Continuum.getInstance().getLogger().log(Level.SEVERE, "Failed reading fragment shading code.");
            return 0;
        }

        GL20.glShaderSource(_fragmentShader.get(title), fragCode);
        GL20.glCompileShader(_fragmentShader.get(title));

        printLogInfo(_fragmentShader.get(title));

        return _fragmentShader.get(title);
    }

    private int createVertexShader(String filename, String title) {

        _vertexShader.put(title, GL20.glCreateShader(GL20.GL_VERTEX_SHADER));

        if (_vertexShader.get(title) == 0) {
            return 0;
        }

        String fragCode = "";
        String line;
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(ResourceLoader.getResource("com/github/begla/blockmania/data/shaders/" + filename).openStream()));
            while ((line = reader.readLine()) != null) {
                fragCode += line + "\n";
            }
        } catch (Exception e) {
            Continuum.getInstance().getLogger().log(Level.SEVERE, "Failed reading vertex shading code.");
            return 0;
        }

        GL20.glShaderSource(_vertexShader.get(title), fragCode);
        GL20.glCompileShader(_vertexShader.get(title));

        printLogInfo(_vertexShader.get(title));

        return _vertexShader.get(title);
    }

    private static void printLogInfo(int obj) {
        IntBuffer intBuffer = BufferUtils.createIntBuffer(1);
        ARBShaderObjects.glGetObjectParameterARB(obj, ARBShaderObjects.GL_OBJECT_INFO_LOG_LENGTH_ARB, intBuffer);

        int length = intBuffer.get();

        if (length <= 1) {
            return;
        }

        ByteBuffer infoBuffer = ByteBuffer.allocateDirect(length);
        intBuffer.flip();

        GL20.glGetShaderInfoLog(obj, intBuffer, infoBuffer);

        int actualLength = intBuffer.get();
        byte[] infoBytes = new byte[actualLength];
        infoBuffer.get(infoBytes);

        Continuum.getInstance().getLogger().log(Level.INFO, "{0}", new String(infoBytes));
    }

    /**
     * @param s Name of the shader to activate
     */
    public void enableShader(@org.jetbrains.annotations.Nullable String s) {
        if (s == null) {
            GL20.glUseProgram(0);
            return;
        }

        int shader = getShader(s);
        GL20.glUseProgram(shader);
    }

    /**
     * @param s Nave of the shader to return
     * @return The id of the requested shader
     */
    public int getShader(String s) {
        return _shaderPrograms.get(s);
    }
}