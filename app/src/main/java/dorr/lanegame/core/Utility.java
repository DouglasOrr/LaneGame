package dorr.lanegame.core;

import android.util.Log;

import java.util.List;

import dorr.lanegame.BuildConfig;

public class Utility {
    public static void debug(String fmt, Object... args) {
        Log.d("LaneGame", String.format(fmt, args));
    }
    public static void check(boolean condition, String message) {
        if (BuildConfig.DEBUG && !condition) {
            throw new IllegalStateException(message);
        }
    }
    public static <T> T getOrNull(List<T> items, int index) {
        return (index < 0 || items.size() <= index) ? null : items.get(index);
    }
    public static float clamp(float x, float min, float max) {
        if (x < min) return min;
        if (max < x) return max;
        return x;
    }
    public static class FastRandom {
        private long mRngState;
        public FastRandom() {
            this(System.nanoTime());
        }
        public FastRandom(long seed) {
            mRngState = seed;
        }
        public float nextFloat() {
            mRngState ^= (mRngState << 21);
            mRngState ^= (mRngState >>> 35);
            mRngState ^= (mRngState << 4);
            return Math.abs((int) mRngState) / (float) Integer.MAX_VALUE;
        }
    }
}
