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
package org.continuum.world;

import org.continuum.generators.ChunkGeneratorTerrain;
import org.continuum.main.Configuration;
import org.continuum.main.Continuum;
import org.continuum.rendering.ShaderManager;
import org.continuum.rendering.TextureManager;
import org.continuum.rendering.particles.BlockParticleEmitter;
import org.continuum.world.characters.Player;
import org.continuum.world.chunk.Chunk;
import org.continuum.world.chunk.ChunkMesh;
import org.continuum.world.horizon.Clouds;
import org.continuum.world.horizon.Skysphere;
import javolution.util.FastList;
import org.lwjgl.opengl.GL20;
import org.lwjgl.util.vector.Vector3f;


import java.util.Collection;
import java.util.Collections;
import java.util.logging.Level;

import static org.lwjgl.opengl.GL11.*;

/**
 * The world of Continuum. At its most basic the world contains chunks (consisting of a fixed amount of blocks)
 * and the player.
 * <p>
 * The world is randomly generated by using a bunch of Perlin noise generators initialized
 * with a favored seed value.
 */
public final class World extends WorldProvider {
    /* PLAYER */
    private Player _player;
    /* RENDERING */
    private FastList<Chunk> _chunksInProximity = new FastList();
    /* PARTICLE EMITTERS */
    private final BlockParticleEmitter _blockParticleEmitter = new BlockParticleEmitter(this);
    /* HORIZON */
    private final Clouds _clouds;
    //private final SunMoon _sunMoon;
    private final Skysphere _skysphere;
    protected double _daylight = 1.0f;
    /* WATER AND LAVA ANIMATION */
    private int _tick = 0;
    private long _lastTick;
    /* UPDATING */
    private final Thread _updateThread;
    private final WorldUpdateManager _worldUpdateManager;
    private boolean _updatingEnabled = false, _updateThreadAlive = true;
    private int prevChunkPosX = 0, prevChunkPosZ = 0;

    /**
     * Initializes a new world for the single player mode.
     *
     * @param title The title/description of the world
     * @param seed  The seed string used to generate the terrain
     */
    public World(String title, String seed) {
        super(title, seed);

        // Init. horizon
        _clouds = new Clouds(this);
        _skysphere = new Skysphere(this);

        _worldUpdateManager = new WorldUpdateManager(this);
        _updateThread = new Thread(new Runnable() {

            public void run() {
                while (true) {
                    /*
                     * Checks if the thread should be killed.
                     */
                    if (!_updateThreadAlive) {
                        return;
                    }

                    /*
                     * Puts the thread to sleep
                     * if updating is disabled.
                     */
                    if (!_updatingEnabled) {
                        synchronized (_updateThread) {
                            try {
                                _updateThread.wait();
                            } catch (InterruptedException ex) {
                                Continuum.getInstance().getLogger().log(Level.SEVERE, ex.toString());
                            }
                        }
                    }

                    updateChunksInProximity();
                    _chunkCache.freeCacheSpace();
                }
            }
        });
    }

    /**
     * Renders the world.
     */
    public void render() {
        // Skysphere
        _skysphere.render();

        // World rendering
        _player.applyPlayerModelViewMatrix();
        _player.render();

        renderChunks();

        // clouds
        _clouds.render();

        // particle effects
        _blockParticleEmitter.render();
    }

    private void updateChunksInProximity() {
        if (prevChunkPosX != calcPlayerChunkOffsetX() || prevChunkPosZ != calcPlayerChunkOffsetZ()) {
            prevChunkPosX = calcPlayerChunkOffsetX();
            prevChunkPosZ = calcPlayerChunkOffsetZ();

            FastList<Chunk> newChunksInProximity = new FastList<Chunk>();

            for (int x = -(Configuration.getSettingNumeric("V_DIST_X").intValue() / 2); x < (Configuration.getSettingNumeric("V_DIST_X").intValue() / 2); x++) {
                for (int z = -(Configuration.getSettingNumeric("V_DIST_Z").intValue() / 2); z < (Configuration.getSettingNumeric("V_DIST_Z").intValue() / 2); z++) {
                    Chunk c = getChunkCache().loadOrCreateChunk(calcPlayerChunkOffsetX() + x, calcPlayerChunkOffsetZ() + z);
                    newChunksInProximity.add(c);
                }
            }

            Collections.sort(newChunksInProximity);
            _chunksInProximity = newChunksInProximity;
        }
    }


    private void updateDaylight() {
        // Sunrise
        if (getTime() < 0.1f && getTime() > 0.0f) {
            _daylight = getTime() / 0.1f;
        } else if (getTime() >= 0.1 && getTime() <= 0.5f) {
            _daylight = 1.0f;
        }

        // Sunset
        if (getTime() > 0.5f && getTime() < 0.6f) {
            _daylight = 1.0f - (getTime() - 0.5f) / 0.1f;
        } else if (getTime() >= 0.6f && getTime() <= 1.0f) {
            _daylight = 0.0f;
        }
    }

    private FastList<Chunk> fetchVisibleChunks() {
        FastList<Chunk> result = new FastList<Chunk>();
        FastList<Chunk> chunksInPromity = _chunksInProximity;

        for (FastList.Node<Chunk> n = chunksInPromity.head(), end = chunksInPromity.tail(); (n = n.getNext()) != end; ) {
            Chunk c = n.getValue();

            if (isChunkVisible(c)) {
                c.setVisible(true);
                result.add(c);
                continue;
            }

            c.setVisible(false);
        }

        return result;
    }

    private void renderChunks() {
        ShaderManager.getInstance().enableShader("chunk");
        int daylight = GL20.glGetUniformLocation(ShaderManager.getInstance().getShader("chunk"), "daylight");
        int swimmimg = GL20.glGetUniformLocation(ShaderManager.getInstance().getShader("chunk"), "swimming");
        int animationOffset = GL20.glGetUniformLocation(ShaderManager.getInstance().getShader("chunk"), "animationOffset");
        int animationType = GL20.glGetUniformLocation(ShaderManager.getInstance().getShader("chunk"), "animationType");
        GL20.glUniform1f(daylight, (float) getDaylight());
        GL20.glUniform1i(animationType, 0);
        GL20.glUniform1i(swimmimg, _player.isHeadUnderWater() ? 1 : 0);

        FastList<Chunk> visibleChunks = fetchVisibleChunks();

        glEnable(GL_TEXTURE_2D);

        // OPAQUE ELEMENTS
        for (FastList.Node<Chunk> n = visibleChunks.head(), end = visibleChunks.tail(); (n = n.getNext()) != end; ) {
            Chunk c = n.getValue();

            GL20.glUniform1i(animationType, 0);
            TextureManager.getInstance().bindTexture("terrain");
            c.render(ChunkMesh.RENDER_TYPE.OPAQUE);

            // ANIMATED LAVA
            GL20.glUniform1i(animationType, 1);
            GL20.glUniform1f(animationOffset, ((float) (_tick % 16)) * (1.0f / 16f));
            TextureManager.getInstance().bindTexture("custom_lava_still");
            visibleChunks.valueOf(n).render(ChunkMesh.RENDER_TYPE.LAVA);

            if (Configuration.getSettingBoolean("CHUNK_OUTLINES")) {
                c.getAABB().render();
            }
        }

        GL20.glUniform1i(animationType, 0);
        TextureManager.getInstance().bindTexture("terrain");

        // BILLBOARDS AND TRANSLUCENT ELEMENTS
        for (FastList.Node<Chunk> n = visibleChunks.head(), end = visibleChunks.tail(); (n = n.getNext()) != end; ) {
            Chunk c = n.getValue();
            c.render(ChunkMesh.RENDER_TYPE.BILLBOARD_AND_TRANSLUCENT);
        }

        GL20.glUniform1i(animationType, 1);

        for (int i = 0; i < 2; i++) {
            // ANIMATED WATER
            for (FastList.Node<Chunk> n = visibleChunks.head(), end = visibleChunks.tail(); (n = n.getNext()) != end; ) {
                Chunk c = n.getValue();

                if (i == 0) {
                    glColorMask(false, false, false, false);
                } else {
                    glColorMask(true, true, true, true);
                }

                GL20.glUniform1f(animationOffset, ((float) (_tick / 2 % 12)) * (1.0f / 16f));
                TextureManager.getInstance().bindTexture("custom_water_still");
                c.render(ChunkMesh.RENDER_TYPE.WATER);
            }
        }

        ShaderManager.getInstance().enableShader(null);
        glDisable(GL_TEXTURE_2D);
    }

    public void update() {
        updateDaylight();
        updateTicks();
        _skysphere.update();

        // Update the player
        _player.update();
        // Generate new VBOs if available
        _worldUpdateManager.updateVBOs();
        // Update the clouds
        _clouds.update();

        FastList<Chunk> visibleChunks = fetchVisibleChunks();

        // Update chunks
        for (FastList.Node<Chunk> n = visibleChunks.head(), end = visibleChunks.tail(); (n = n.getNext()) != end; ) {
            n.getValue().update();
        }

        _worldUpdateManager.queueChunkUpdates(visibleChunks);

        // Update the particle emitters
        _blockParticleEmitter.update();
    }

    private void updateTicks() {
        if (Continuum.getInstance().getTime() - _lastTick >= 200) {
            _tick++;
            _lastTick = Continuum.getInstance().getTime();
        }
    }

    /**
     * Chunk position of the player.
     *
     * @return The player offset on the x-axis
     */
    private int calcPlayerChunkOffsetX() {
        return (int) (_player.getPosition().x / Configuration.CHUNK_DIMENSIONS.x);
    }

    /**
     * Chunk position of the player.
     *
     * @return The player offset on the z-axis
     */
    private int calcPlayerChunkOffsetZ() {
        return (int) (_player.getPosition().z / Configuration.CHUNK_DIMENSIONS.z);
    }

    public void setPlayer(Player p) {
        _player = p;
        // Reset the player's position
        resetPlayer();
    }

    public void resetPlayer() {
        _player.resetEntity();
        _player.setPosition(getSpawningPoint());
    }

    /**
     * Stops the updating thread and writes all chunks to disk.
     */
    public void dispose() {
        Continuum.getInstance().getLogger().log(Level.INFO, "Disposing world {0} and saving all chunks.", getTitle());

        synchronized (_updateThread) {
            _updateThreadAlive = false;
            _updateThread.notify();
        }

        try {
            _updateThread.join();
        } catch (InterruptedException e) {
        }

        saveMetaData();
        getChunkCache().saveAndDisposeAllChunks();
    }

    /**
     * Displays some information about the world formatted as a string.
     *
     * @return String with world information
     */
    @Override
    public String toString() {
        return String.format("world (biome: %s, time: %f, sun: %f, cdl: %d, cache: %d, ud: %fs, seed: \"%s\", title: \"%s\")", getActiveBiome(), getTime(), _skysphere.getSunPosAngle(), _worldUpdateManager.getVboUpdatesSize(), _chunkCache.size(), _worldUpdateManager.getMeanUpdateDuration() / 1000d, _seed, _title);
    }

    /**
     * Starts the updating thread.
     */
    public void startUpdateThread() {
        _updatingEnabled = true;
        _updateThread.start();
    }

    /**
     * Resumes the updating thread.
     */
    public void resumeUpdateThread() {
        _updatingEnabled = true;
        synchronized (_updateThread) {
            _updateThread.notify();
        }
    }

    /**
     * Safely suspends the updating thread.
     */
    public void suspendUpdateThread() {
        _updatingEnabled = false;
    }

    public void setSpawningPointToPlayerPosition() {
        _spawningPoint = new Vector3f(_player.getPosition());
    }

    public Player getPlayer() {
        return _player;
    }

    @Override
    public Vector3f getOrigin() {
        return _player.getPosition();
    }

    public boolean isChunkVisible(Chunk c) {
        return _player.getViewFrustum().intersects(c.getAABB());
    }

    /**
     * Returns the daylight value.
     *
     * @return The daylight value
     */
    public double getDaylight() {
        return _daylight;
    }

    public FastList<Chunk> getChunksInProximity() {
        return _chunksInProximity;
    }

    public BlockParticleEmitter getBlockParticleEmitter() {
        return _blockParticleEmitter;
    }

    /**
     * Returns the active biome at the player's position.
     */
    public ChunkGeneratorTerrain.BIOME_TYPE getActiveBiome() {
        return ((ChunkGeneratorTerrain) _chunkGenerators.get("terrain")).calcBiomeTypeForGlobalPosition((int) _player.getPosition().x, (int) _player.getPosition().z);
    }

    /**
     * Returns the humidity at the player's position;
     *
     * @return
     */
    public double getActiveHumidity() {
        return getHumidityAt((int) _player.getPosition().x, (int) _player.getPosition().z);
    }

    /**
     * Returns the temperature at the player's position.
     *
     * @return
     */
    public double getActiveTemperature() {
        return getTemperatureAt((int) _player.getPosition().x, (int) _player.getPosition().z);
    }
}
