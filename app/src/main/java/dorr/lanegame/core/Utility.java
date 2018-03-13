package dorr.lanegame.core;

import java.util.List;

import dorr.lanegame.BuildConfig;

class Utility {
    static void check(boolean condition, String message) {
        if (BuildConfig.DEBUG && !condition) {
            throw new IllegalStateException(message);
        }
    }
    static <T> T getOrNull(List<T> items, int index) {
        return (index < 0 || items.size() <= index) ? null : items.get(index);
    }
}
