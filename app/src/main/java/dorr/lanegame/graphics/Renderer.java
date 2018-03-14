package dorr.lanegame.graphics;

import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import dorr.lanegame.core.Game;
import dorr.lanegame.core.Simulation;

import static dorr.lanegame.core.Utility.debug;

public class Renderer implements GLSurfaceView.Renderer {
    /**
     * A simple single-color background for the game world.
     */
    class Background {
        private final int mProgram;
        private final int mProgramPosition, mProgramProjection, mProgramColor;
        private final FloatBuffer mVertices;
        private final float[] mColor;
        Background(int width, int height) {
            mProgram = GlUtility.loadProgram(
                    GlUtility.loadShader(GLES20.GL_VERTEX_SHADER,
                            "attribute vec4 vPosition;\n" +
                                    "uniform mat4 uProjection;\n" +
                                    "void main() {\n" +
                                    "  gl_Position = uProjection * vPosition;\n" +
                                    "}\n"),
                    GlUtility.loadShader(GLES20.GL_FRAGMENT_SHADER,
                            "precision mediump float;\n" +
                                    "uniform vec4 uColor;\n" +
                                    "void main() {\n" +
                                    "  gl_FragColor = uColor;\n" +
                                    "}\n")
            );
            mProgramPosition = GLES20.glGetAttribLocation(mProgram, "vPosition");
            mProgramProjection = GlUtility.getUniformLocation(mProgram, "uProjection");
            mProgramColor = GLES20.glGetUniformLocation(mProgram, "uColor");
            ByteBuffer bvertices = ByteBuffer.allocateDirect(4 * 2 * 4);
            bvertices.order(ByteOrder.nativeOrder());
            mVertices = bvertices.asFloatBuffer();
            mVertices.put(new float[] {
                    0, 0,
                    width, 0,
                    0, height,
                    width, height,
            });
            mVertices.position(0);
            mColor = new float[] { 0.7f, 0.8f, 1.0f, 1 };
        }
        void draw(float[] projection) {
            GLES20.glUseProgram(mProgram);
            GLES20.glEnableVertexAttribArray(mProgramPosition);
            GLES20.glVertexAttribPointer(mProgramPosition, 2, GLES20.GL_FLOAT, false, 2 * 4, mVertices);
            GLES20.glUniform4fv(mProgramColor, 1, mColor, 0);
            GLES20.glUniformMatrix4fv(mProgramProjection, 1, false, projection, 0);
            GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);
            GLES20.glDisableVertexAttribArray(mProgramPosition);
        }
    }

    private Background mBackground;
    private Simulation mSimulation;
    private float[] mProjection;

    void setup(Simulation simulation) {
        debug("setup(%s)", simulation);
        mSimulation = simulation;
    }

    private static int getLaneWidth(Game.GameSpec spec) {
        int maxHeight = 0;
        for (Game.UnitSpec unitSpec : spec.units) {
            maxHeight = Math.max(maxHeight, unitSpec.height);
        }
        return maxHeight;
    }

    @Override
    public void onSurfaceCreated(GL10 gl10, EGLConfig eglConfig) {
        debug("onSurfaceCreated()");
        GLES20.glClearColor(0, 0f, 0f, 1);
        GLES20.glEnable(GLES20.GL_CULL_FACE);
    }

    @Override
    public void onSurfaceChanged(GL10 gl10, int width, int height) {
        debug("onSurfaceChanged()");
        GLES20.glViewport(0, 0, width, height);
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);

        Game.GameSpec spec = mSimulation.spec();
        int laneWidth = getLaneWidth(spec);
        int gameWidth = laneWidth * spec.lanes;
        int gameHeight = spec.length;
        float screenScale = Math.min(width / (float) gameWidth, height / (float) gameHeight);
        // It's neater to specify using the transpose of the projection matrix, as in this case
        // each row below represents the calculation from input -> output
        // coordinates
        mProjection = new float[16];
        Matrix.transposeM(mProjection, 0,
                new float[] {
                    2 * screenScale / width, 0, 0, -1,
                    0, 2 * screenScale / height, 0, -1,
                    0, 0, 1, 0,
                    0, 0, 0, 1},
                0);

        mBackground = new Background(gameWidth, gameHeight);
    }

    @Override
    public void onDrawFrame(GL10 gl10) {
        mBackground.draw(mProjection);
    }
}
