package aurum.aurum.worldgen.Dimension;

import java.util.Random;

public class NoiseGenerator {

    private static final int[] P = new int[512];
    private static final double[] GRADIENT = new double[512 * 3];

    public NoiseGenerator(long seed) {
        Random rand = new Random(seed);
        int[] permutation = new int[256];
        for (int i = 0; i < 256; i++) {
            permutation[i] = i;
        }

        for (int i = 0; i < 256; i++) {
            int pos = rand.nextInt(256 - i) + i;
            int temp = permutation[i];
            permutation[i] = permutation[pos];
            permutation[pos] = temp;
        }

        for (int i = 0; i < 256; i++) {
            P[i] = P[i + 256] = permutation[i];
        }

        for (int i = 0; i < 512; i++) {
            double angle = rand.nextDouble() * 2 * Math.PI;
            GRADIENT[i * 3] = Math.cos(angle);
            GRADIENT[i * 3 + 1] = Math.sin(angle);
            GRADIENT[i * 3 + 2] = 0; // For 2D noise, Z component is 0
        }
    }

    private double fade(double t) {
        return t * t * t * (t * (t * 6 - 15) + 10);
    }

    private double lerp(double t, double a, double b) {
        return a + t * (b - a);
    }

    private double grad(int hash, double x, double y, double z) {
        int h = hash & 15;
        double u = h < 8 ? x : y;
        double v = h < 4 ? y : h == 12 || h == 14 ? x : z;
        return ((h & 1) == 0 ? u : -u) + ((h & 2) == 0 ? v : -v);
    }

    public double perlinNoise(double x, double y, double z) {
        int X = (int) Math.floor(x) & 255;
        int Y = (int) Math.floor(y) & 255;
        int Z = (int) Math.floor(z) & 255;

        x -= Math.floor(x);
        y -= Math.floor(y);
        z -= Math.floor(z);

        double u = fade(x);
        double v = fade(y);
        double w = fade(z);

        int A = P[X] + Y;
        int AA = P[A] + Z;
        int AB = P[A + 1] + Z;
        int B = P[X + 1] + Y;
        int BA = P[B] + Z;
        int BB = P[B + 1] + Z;

        return lerp(w, lerp(v, lerp(u, grad(P[AA], x, y, z), grad(P[BA], x - 1, y, z)),
                        lerp(u, grad(P[AB], x, y - 1, z), grad(P[BB], x - 1, y - 1, z))),
                lerp(v, lerp(u, grad(P[AA + 1], x, y, z - 1), grad(P[BA + 1], x - 1, y, z - 1)),
                        lerp(u, grad(P[AB + 1], x, y - 1, z - 1), grad(P[BB + 1], x - 1, y - 1, z - 1))));
    }
}



