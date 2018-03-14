package dorr.lanegame.core;

import android.util.Log;

import java.util.List;

import dorr.lanegame.BuildConfig;

public class Utility {
    public static void check(boolean condition, String message) {
        if (BuildConfig.DEBUG && !condition) {
            throw new IllegalStateException(message);
        }
    }
    public static <T> T getOrNull(List<T> items, int index) {
        return (index < 0 || items.size() <= index) ? null : items.get(index);
    }
    public static void debug(String fmt, Object... args) {
        Log.d("LaneGame", String.format(fmt, args));
    }
}
