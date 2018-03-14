package dorr.lanegame.graphics;

import android.opengl.GLES20;
import android.util.Log;

/**
 * A few wrappers around GLES20 APIs, to make them more usable.
 */
class GlUtility {
    static class GlException extends RuntimeException {
        final Integer error;
        final String detail;
        GlException(String message, Integer error, String detail) {
            super(message);
            this.error = error;
            this.detail = detail;
        }
        @Override
        public String toString() {
            return String.format("GlException(%s, %d) {\n%s}", getMessage(), error, detail);
        }
    }

    private static void checkError(String what) {
        int error = GLES20.glGetError();
        if (error != GLES20.GL_NO_ERROR) {
            throw new GlException(what, error, "");
        }
    }

    static int loadShader(int type, String source) {
        checkError("unknown");
        int shader = GLES20.glCreateShader(type);
        GLES20.glShaderSource(shader, source);
        GLES20.glCompileShader(shader);
        checkError("glCompileShader");
        int[] error = new int[1];
        GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, error, 0);
        if (error[0] == GLES20.GL_FALSE) {
            throw new GlException("glCompileShader", null, GLES20.glGetShaderInfoLog(shader));
        }
        return shader;
    }

    static int loadProgram(int vertexShader, int fragmentShader) {
        checkError("unknown");
        int program = GLES20.glCreateProgram();
        GLES20.glAttachShader(program, vertexShader);
        GLES20.glAttachShader(program, fragmentShader);
        GLES20.glLinkProgram(program);
        checkError("glLinkProgram");
        int[] error = new int[1];
        GLES20.glGetProgramiv(program, GLES20.GL_LINK_STATUS, error, 0);
        if (error[0] == GLES20.GL_FALSE) {
            throw new GlException("glLinkProgram", null, GLES20.glGetProgramInfoLog(program));
        }
        return program;
    }

    private static int checkLocation(int location, String what, String name) {
        if (location < 0) {
            throw new GlException(String.format("%s(%s) failed", what, name), location, "");
        }
        return location;
    }

    static int getAttribLocation(int program, String name) {
        return checkLocation(
                GLES20.glGetAttribLocation(program, name),
                "glGetAttribLocation", name);
    }

    static int getUniformLocation(int program, String name) {
        return checkLocation(
                GLES20.glGetUniformLocation(program, name),
                "getUniformLocation", name);
    }

    static class FpsLogger {
        private final long mLogInterval;
        private final int mFrameSmoothing;
        private long mLastNanoTime = Long.MIN_VALUE;
        private long mLastLogged = Long.MIN_VALUE;
        private float mFrameTime = 0;
        private int mFrames = 0;

        FpsLogger(float interval, int frameSmoothing) {
            mLogInterval = (long)(1e9 * interval);
            mFrameSmoothing = frameSmoothing;
        }

        void tick() {
            long time = System.nanoTime();
            if (mLastNanoTime != Long.MIN_VALUE) {
                float interval = (time - mLastNanoTime) / 1.0e9f;
                mFrames = Math.min(mFrameSmoothing, mFrames + 1);
                mFrameTime = ((mFrames - 1) * mFrameTime + interval) / mFrames;
                if (mLastLogged == Long.MIN_VALUE || mLogInterval <= time - mLastLogged) {
                    Log.d("FpsLogger", String.format("%.2g FPS", 1 / mFrameTime));
                    mLastLogged = time;
                }
            }
            mLastNanoTime = time;
        }
    }
}
