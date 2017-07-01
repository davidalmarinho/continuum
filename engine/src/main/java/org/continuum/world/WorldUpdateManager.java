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

import org.continuum.main.Continuum;
import org.continuum.world.chunk.Chunk;
import javolution.util.FastList;
import javolution.util.FastSet;

import java.util.concurrent.*;

public final class WorldUpdateManager {
    private static final int MAX_THREADS = Math.max(Runtime.getRuntime().availableProcessors() - 2, 1);
    private static final ExecutorService threadPoll = Executors.newFixedThreadPool(MAX_THREADS);

    private final LinkedBlockingQueue<Chunk> _vboUpdates = new LinkedBlockingQueue<Chunk>();
    private final FastSet<Chunk> _currentlyProcessedChunks = new FastSet<Chunk>();

    private double averageUpdateDuration = 0.0;

    public boolean queueChunkUpdate(Chunk c) {
        final Chunk chunkToProcess = c;

        if (!_currentlyProcessedChunks.contains(chunkToProcess) && _currentlyProcessedChunks.size() < MAX_THREADS) {
            _currentlyProcessedChunks.add(chunkToProcess);

            Runnable r = () -> {
                long timeStart = Continuum.getInstance().getTime();

                processChunkUpdate(chunkToProcess);
                _currentlyProcessedChunks.remove(chunkToProcess);

                averageUpdateDuration += Continuum.getInstance().getTime() - timeStart;
                averageUpdateDuration /= 2;
            };

            threadPoll.execute(r);
            return true;
        }

        return false;
    }

    private void processChunkUpdate(Chunk c) {
        // If the chunk was changed, update the VBOs
        if (c.processChunk()) {
            _vboUpdates.add(c);
        }
    }

    /**
     * Updates the VBOs of all currently queued chunks.
     */
    public void updateVBOs() {
        Chunk c = _vboUpdates.poll();

        if (c != null) {
            c.generateVBOs();
        }
    }

    public int getVboUpdatesSize() {
        return _vboUpdates.size();
    }

    public double getAverageUpdateDuration() {
        return averageUpdateDuration;
    }
}
