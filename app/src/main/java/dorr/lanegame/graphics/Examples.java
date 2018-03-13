package dorr.lanegame.graphics;

import android.opengl.GLES20;
import android.opengl.GLSurfaceView;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.Random;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

class Examples {
    static class Particles implements GLSurfaceView.Renderer {
        private final Random mRandom = new Random();
        private final FloatBuffer mVertices;
        private final float[] mVerticesCopy;
        private final GlUtility.FpsLogger mFps = new GlUtility.FpsLogger(1, 100);
        private int mProgram;
        private int mProgramPosition;
        private int mProgramColor;
        private float[] mColor = new float[] { 1, 0, 0, 1 };
        private float[] mRngBuffer = new float[256];
        private long mRngState = System.nanoTime();

        public Particles(int n) {
            ByteBuffer bvertices = ByteBuffer.allocateDirect(n * 2 * 4);
            bvertices.order(ByteOrder.nativeOrder());
            mVertices = bvertices.asFloatBuffer();
            for (int i = 0; i < 2 * n; ++i) {
                mVertices.put(i, 2 * mRandom.nextFloat() - 1);
            }
            mVertices.position(0);
            mVerticesCopy = new float[mVertices.limit()];
            mVertices.get(mVerticesCopy);
        }

        @Override
        public void onSurfaceCreated(GL10 gl10, EGLConfig eglConfig) {
            GLES20.glClearColor(0, 0.25f, 0.5f, 1);
        }

        @Override
        public void onSurfaceChanged(GL10 gl10, int width, int height) {
            GLES20.glViewport(0, 0, width, height);

            mProgram = GlUtility.loadProgram(
                    GlUtility.loadShader(GLES20.GL_VERTEX_SHADER,
                            "attribute vec4 vPosition;\n" +
                                    "void main() {\n" +
                                    "  gl_Position = vPosition;\n" +
                                    "  gl_PointSize = 1.0f;\n" +
                                    "}\n"),
                    GlUtility.loadShader(GLES20.GL_FRAGMENT_SHADER,
                            "precision mediump float;\n" +
                                    "uniform vec4 vColor;\n" +
                                    "void main() {\n" +
                                    "  gl_FragColor = vColor;\n" +
                                    "}\n")
            );
            mProgramPosition = GLES20.glGetAttribLocation(mProgram, "vPosition");
            mProgramColor = GLES20.glGetUniformLocation(mProgram, "vColor");
        }

        private void movePoints() {
            int nVertices = mVertices.limit();
            /////// METHOD 1
//            for (int i = 0; i < nVertices; ++i) {
//                float v = mVertices.get(i);
//                v += (mRandom.nextFloat() - 0.5f) / 100;
//                if (v < 0) v = 0;
//                if (1 <= v) v = 1;
//                mVertices.put(i, v);
//            }
            /////// METHOD 2
//            for (int i = 0; i < nVertices; ++i) {
//                float v = mVerticesCopy[i];
//                v += (mRandom.nextFloat() - 0.5f) / 100;
//                if (v < -1) v = -1;
//                if (1 <= v) v = 1;
//                mVerticesCopy[i] = v;
//            }
//            mVertices.position(0);
//            mVertices.put(mVerticesCopy);
            /////// METHOD 3
//            for (int i = 0; i < mRngBuffer.length; ++i) {
//                mRngBuffer[i] = (mRandom.nextFloat() - 0.5f) / 100;
//            }
//            int offset = mRandom.nextInt(mRngBuffer.length);
//            int multiplier = 1 + mRandom.nextInt(mRngBuffer.length);
//            for (int i = 0; i < nVertices; ++i) {
//                float v = mVerticesCopy[i];
//                v += mRngBuffer[(multiplier * i + offset) % mRngBuffer.length];
//                if (v < -1) v = -1;
//                if (1 <= v) v = 1;
//                mVerticesCopy[i] = v;
//            }
//            mVertices.position(0);
//            mVertices.put(mVerticesCopy);
            /////// METHOD 4
            for (int i = 0; i < nVertices; ++i) {
                float v = mVerticesCopy[i];
                // https://www.javamex.com/tutorials/random_numbers/xorshift.shtml
                mRngState ^= (mRngState << 21);
                mRngState ^= (mRngState >>> 35);
                mRngState ^= (mRngState << 4);
                v += mRngState / (100.0f * Long.MAX_VALUE);
                if (v < -1) v = -1;
                if (1 <= v) v = 1;
                mVerticesCopy[i] = v;
            }
            mVertices.position(0);
            mVertices.put(mVerticesCopy);
        }

        @Override
        public void onDrawFrame(GL10 gl10) {
            movePoints();

            GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);

            GLES20.glUseProgram(mProgram);
            GLES20.glEnableVertexAttribArray(mProgramPosition);
            GLES20.glVertexAttribPointer(mProgramPosition, 2, GLES20.GL_FLOAT, false, 2 * 4, mVertices);
            GLES20.glUniform4fv(mProgramColor, 1, mColor, 0);
            GLES20.glDrawArrays(GLES20.GL_POINTS, 0, mVertices.limit() / 2);
            GLES20.glDisableVertexAttribArray(mProgramPosition);

            mFps.tick();
        }
    }

    static class Basic implements GLSurfaceView.Renderer {
        private int mProgram;
        @Override
        public void onSurfaceCreated(GL10 gl10, EGLConfig eglConfig) {
            GLES20.glClearColor(0, 0.25f, 0.5f, 1);
        }
        @Override
        public void onSurfaceChanged(GL10 gl10, int width, int height) {
            GLES20.glViewport(0, 0, width, height);

            mProgram = GlUtility.loadProgram(
                    GlUtility.loadShader(GLES20.GL_VERTEX_SHADER,
                            "attribute vec4 vPosition;\n" +
                                    "void main() {\n" +
                                    "  gl_Position = vPosition;\n" +
                                    "}\n"),
                    GlUtility.loadShader(GLES20.GL_FRAGMENT_SHADER,
                            "precision mediump float;\n" +
                                    "uniform vec4 vColor;\n" +
                                    "void main() {\n" +
                                    "  gl_FragColor = vColor;\n" +
                                    "}\n")
            );
        }
        @Override
        public void onDrawFrame(GL10 gl10) {
            GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);

            GLES20.glUseProgram(mProgram);

            // TODO - factor this out as required
            ByteBuffer bvertices = ByteBuffer.allocateDirect(3 * 2 * 4);
            bvertices.order(ByteOrder.nativeOrder());
            FloatBuffer vertices = bvertices.asFloatBuffer();
            vertices.put(new float[] {
                    0.0f, 0.5f,
                    -0.5f, -0.5f,
                    0.5f, -0.5f,
            });
            vertices.position(0);
            int position = GLES20.glGetAttribLocation(mProgram, "vPosition");
            GLES20.glEnableVertexAttribArray(position);
            GLES20.glVertexAttribPointer(position, 2, GLES20.GL_FLOAT, false, 2 * 4, vertices);

            int color = GLES20.glGetUniformLocation(mProgram, "vColor");
            GLES20.glUniform4fv(color, 1, new float[] { 1, 0, 0, 1 }, 0);

            GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, 3);
            GLES20.glDisableVertexAttribArray(position);
        }
    }
}
