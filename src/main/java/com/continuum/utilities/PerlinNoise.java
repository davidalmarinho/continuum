package com.continuum.utilities;

import java.util.ArrayList;

/**
 * Improved Perlin Noise based on the reference implementation by Ken Perlin.
 */
public class PerlinNoise
{
	private final int[] noisePerm;

	/**
	 * @param seed
	 */
	public PerlinNoise(int seed) {
		FastRandom rand = new FastRandom(seed);
		noisePerm = new int[256];

		ArrayList<Integer> permutations = new ArrayList<Integer>();

		for (int i = 0; i < 256; i++) {
			permutations.add(i);
		}

		rand.shuffle(permutations);

		for (int i = 0; i < 256; i++) {
			noisePerm[i] = permutations.get(i);
		}
	}

	/**
	 * @param x
	 * @param y
	 * @param z
	 * @return
	 */
	public double noise(double x, double y, double z) {
		int X = (int) Math.floor(x) & 255, Y = (int) Math.floor(y) & 255, Z = (int) Math.floor(z) & 255;

		x -= Math.floor(x);
		y -= Math.floor(y);
		z -= Math.floor(z);

		double u = fade(x), v = fade(y), w = fade(z);
		int A = noisePerm[X % 255] + Y, AA = noisePerm[A % 255] + Z, AB = noisePerm[(A + 1) % 255] + Z,
				B = noisePerm[(X + 1) % 255] + Y, BA = noisePerm[B % 255] + Z, BB = noisePerm[(B + 1) % 255] + Z;

		return lerp(w, lerp(v, lerp(u, grad(noisePerm[AA % 255], x, y, z),
								grad(noisePerm[BA % 255], x - 1, y, z)),
						lerp(u, grad(noisePerm[AB % 255], x, y - 1, z),
								grad(noisePerm[BB % 255], x - 1, y - 1, z))),
				lerp(v, lerp(u, grad(noisePerm[(AA + 1) % 255], x, y, z - 1),
								grad(noisePerm[(BA + 1) % 255], x - 1, y, z - 1)),
						lerp(u, grad(noisePerm[(AB + 1) % 255], x, y - 1, z - 1),
								grad(noisePerm[(BB + 1) % 255], x - 1, y - 1, z - 1))));
	}

	/**
	 * @param t
	 * @return
	 */
	private static double fade(double t) {
		return t * t * t * (t * (t * 6 - 15) + 10);
	}

	/**
	 * @param t
	 * @param a
	 * @param b
	 * @return
	 */
	private static double lerp(double t, double a, double b) {
		return a + t * (b - a);
	}

	/**
	 * @param hash
	 * @param x
	 * @param y
	 * @param z
	 * @return
	 */
	private static double grad(int hash, double x, double y, double z) {
		int h = hash & 15;
		double u = h < 8 ? x : y, v = h < 4 ? y : h == 12 || h == 14 ? x : z;
		return ((h & 1) == 0 ? u : -u) + ((h & 2) == 0 ? v : -v);
	}

	/**
	 * @param x
	 * @param y
	 * @param z
	 * @param octaves
	 * @param lacunarity
	 * @return
	 */
	public double multiFractalNoise(double x, double y, double z, int octaves, double lacunarity) {
		double result = 0;

		for (int i = 1; i <= octaves; i++) {
			result += noise(x, y, z) * Math.pow(lacunarity, -0.76471 * i);

			x *= lacunarity;
			y *= lacunarity;
			z *= lacunarity;
		}

		return result;
	}

	/**
	 * @param x
	 * @param y
	 * @param z
	 * @param octaves
	 * @param lacunarity
	 * @param gain
	 * @param offset
	 * @return
	 */
	public double ridgedMultiFractalNoise(double x, double y, double z, int octaves, double lacunarity, double gain, double offset) {
		double frequency = 1f;
		double signal;

        /*
         * Fetch the first noise octave.
         */
		signal = ridge(noise(x, y, z), offset);
		double result = signal;
		double weight;

		for (int i = 1; i <= octaves; i++) {
			x *= lacunarity;
			y *= lacunarity;
			z *= lacunarity;

			weight = gain * signal;

			if (weight > 1.0f) {
				weight = 1.0f;
			} else if (weight < 0.0f) {
				weight = 0.0f;
			}

			signal = ridge(noise(x, y, z), offset);

			signal *= weight;
			result += signal * Math.pow(frequency, -0.86461f);
			frequency *= lacunarity;
		}


		return result;
	}

	private double ridge(double n, double offset) {
		n = Math.abs(n);
		n = offset - n;
		n = n * n;
		return n;
	}
}