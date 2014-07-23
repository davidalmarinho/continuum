package com.continuum.generators;

import com.continuum.main.Configuration;
import com.continuum.utilities.FastRandom;
import com.continuum.world.chunk.Chunk;

/**
 * Generates some trees, flowers and high grass.
 */
public class ChunkGeneratorForest extends ChunkGeneratorTerrain {

    /**
     * Init. the forest generator.
     *
     * @param seed
     */
    public ChunkGeneratorForest(String seed) {
        super(seed);
    }

    /**
     * Apply the generation process to the given chunk.
     *
     * @param c
     */
    @Override
    public void generate(Chunk c) {
        for (int y = 0; y < Configuration.CHUNK_DIMENSIONS.y; y++) {
            for (int x = 0; x < Configuration.CHUNK_DIMENSIONS.x; x++) {
                for (int z = 0; z < Configuration.CHUNK_DIMENSIONS.z; z++) {
                    generateGrassAndFlowers(c, x, y, z);
                }
            }
        }

        FastRandom rand = new FastRandom(c.getChunkId());

        for (int y = 32; y < Configuration.CHUNK_DIMENSIONS.y; y++) {
            for (int x = 0; x < Configuration.CHUNK_DIMENSIONS.x; x += 4) {
                for (int z = 0; z < Configuration.CHUNK_DIMENSIONS.z; z += 4) {
                    double forestDens = calcForestDensity(c.getBlockWorldPosX(x), c.getBlockWorldPosZ(z));

                    if (forestDens > 0.01) {

                        int randX = x + rand.randomInt() % 12 + 4;
                        int randZ = z + rand.randomInt() % 12 + 4;

                        if (c.getBlock(randX, y, randZ) == 0x1 || c.getBlock(randX, y, randZ) == 0x17) {
                            generateTree(c, randX, y, randZ);
                        } else if (c.getBlock(randX, y, randZ) == 0x7) {
                            c.getParent().getObjectGenerator("cactus").generate(c.getBlockWorldPosX(randX), y + 1, c.getBlockWorldPosZ(randZ), false);
                        }
                    }
                }
            }
        }
    }

    /**
     * @param c
     * @param x
     * @param y
     * @param z
     */

    void generateGrassAndFlowers(Chunk c, int x, int y, int z) {

        if (c.getBlock(x, y, z) == 0x1) {
            double grassDens = calcGrassDensity(c.getBlockWorldPosX(x), c.getBlockWorldPosZ(z));

            if (grassDens > 0.0) {
				// Generate high grass.
                double rand = _rand.standNormalDistrDouble();
                if (rand > -0.4 && rand < 0.4) {
                    c.setBlock(x, y + 1, z, (byte) 0xB);
                } else if (rand > -0.8 && rand < -0.8) {
                    c.setBlock(x, y + 1, z, (byte) 0xC);
                }

                // Generate flowers
                if (_rand.standNormalDistrDouble() < -2) {
                    if (_rand.randomBoolean()) {
                        c.setBlock(x, y + 1, z, (byte) 0x9);
                    } else {
                        c.setBlock(x, y + 1, z, (byte) 0xA);
                    }

                }
            }
        }
    }

    /**
     * @param c
     * @param x
     * @param y
     * @param z
     */
    void generateTree(Chunk c, int x, int y, int z) {
        // Trees should only be placed in direct sunlight
        if (!c.canBlockSeeTheSky(x, y + 1, z))
            return;

        double r2 = _rand.standNormalDistrDouble();
        if (r2 > -2 && r2 < -1) {
            c.setBlock(x, y + 1, z, (byte) 0x0);
            c.getParent().getObjectGenerator("pineTree").generate(c.getBlockWorldPosX(x), y + 1, c.getBlockWorldPosZ(z), false);
        } else if (r2 > 1 && r2 < 2) {
            c.setBlock(x, y + 1, z, (byte) 0x0);
            c.getParent().getObjectGenerator("firTree").generate(c.getBlockWorldPosX(x), y + 1, c.getBlockWorldPosZ(z), false);
        } else {
            c.setBlock(x, y + 1, z, (byte) 0x0);
            c.getParent().getObjectGenerator("tree").generate(c.getBlockWorldPosX(x), y + 1, c.getBlockWorldPosZ(z), false);
        }
    }

    /**
     * Returns the cave density for the base terrain.
     *
     * @param x
     * @param z
     * @return
     */
    double calcForestDensity(double x, double z) {
        double result = 0.0;
		result += _pGen1.fBm(0.01 * x, 0, 0.01 * z, 7, 2.3614521, 0.85431);
        return result;
    }

    /**
     * @param x
     * @param z
     * @return
     */
    double calcGrassDensity(double x, double z) {
        double result = 0.0;
		result += _pGen3.fBm(0.05 * x, 0, 0.05 * z, 4, 2.37152, 0.8571);
        return result;
    }
}