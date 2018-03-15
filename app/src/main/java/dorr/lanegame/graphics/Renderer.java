package dorr.lanegame.graphics;

import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;

import java.nio.FloatBuffer;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import dorr.lanegame.core.Game;
import dorr.lanegame.core.Simulation;
import dorr.lanegame.core.Utility;

import static dorr.lanegame.core.Utility.debug;

public class Renderer implements GLSurfaceView.Renderer {
    /**
     * A simple single-color background for the game world.
     */
    static class Background {
        private final int mProgram;
        private final int mProgramPosition, mProgramProjection, mProgramColor;
        private final FloatBuffer mVertexBuffer;
        private final float[] mColor = new float[] { 0.8f, 0.8f, 0.8f, 1 };
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
            mProgramPosition = GlUtility.getAttribLocation(mProgram, "vPosition");
            mProgramProjection = GlUtility.getUniformLocation(mProgram, "uProjection");
            mProgramColor = GlUtility.getUniformLocation(mProgram, "uColor");
            mVertexBuffer = GlUtility.allocateFloatBuffer(4 * 2);
            mVertexBuffer.put(new float[] {
                    0, 0,
                    width, 0,
                    0, height,
                    width, height,
            });
            mVertexBuffer.position(0);
        }
        void draw(float[] projection) {
            GLES20.glUseProgram(mProgram);
            GLES20.glEnableVertexAttribArray(mProgramPosition);
            GLES20.glVertexAttribPointer(mProgramPosition, 2, GLES20.GL_FLOAT, false, 0, mVertexBuffer);
            GLES20.glUniformMatrix4fv(mProgramProjection, 1, false, projection, 0);
            GLES20.glUniform4fv(mProgramColor, 1, mColor, 0);
            GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, mVertexBuffer.limit() / 2);
            GLES20.glDisableVertexAttribArray(mProgramPosition);
        }
    }

    /**
     * A particle system.
     */
    static class Particles {
        static class Particle {
            int offset;
            int npoints;
            int xposition;
            int yposition;
            boolean parity;
            public Particle(int offset, int npoints, int xposition, int yposition, boolean parity) {
                this.offset = offset;
                this.npoints = npoints;
                this.xposition = xposition;
                this.yposition = yposition;
                this.parity = parity;
            }
        }

        // Constants
        private static final int MAX_VERTICES = 10000;
        private static final int POINTS_PER_UNIT = 40;
        private static final float RANDOM_DELTA_SCALE = 0.2f; // tile widths per second
        private static final float MARGIN = 0.1f; // tile widths
        private static final float POINT_SIZE = 4.0f; // pixels

        // Logical
        private final Game mGame;
        private final Game.Owner mOwner;

        // Drawing
        private final int mLaneWidth;
        private final int mMargin;
        private float[] mData, mOldData;
        private final FloatBuffer mVertexBuffer;
        private final float[] mColor;
        private final int mProgram;
        private final int mProgramPosition, mProgramProjection, mProgramColor, mProgramPointSize;

        // State
        private boolean mParity = false;
        private float mLastGameTime;
        private final Map<Integer, Particle> mParticles = new HashMap<>();
        private final Utility.FastRandom mRandom = new Utility.FastRandom();

        Particles(Game game, Game.Owner owner, int laneWidth, float[] color) {
            mGame = game;
            mOwner = owner;
            mLastGameTime = game.time;

            mLaneWidth = laneWidth;
            mMargin = (int) (MARGIN * laneWidth);
            mColor = color;
            int bufferSize = MAX_VERTICES * 2;
            mVertexBuffer = GlUtility.allocateFloatBuffer(bufferSize);
            mData = new float[bufferSize];
            mOldData = new float[bufferSize];

            mProgram = GlUtility.loadProgram(
                    GlUtility.loadShader(GLES20.GL_VERTEX_SHADER,
                            "attribute vec4 vPosition;\n" +
                                    "uniform mat4 uProjection;\n" +
                                    "uniform float uPointSize;\n" +
                                    "void main() {\n" +
                                    "  gl_Position = uProjection * vPosition;\n" +
                                    "  gl_PointSize = uPointSize;\n" +
                                    "}\n"),
                    GlUtility.loadShader(GLES20.GL_FRAGMENT_SHADER,
                            "precision mediump float;\n" +
                                    "uniform vec4 uColor;\n" +
                                    "void main() {\n" +
                                    "  gl_FragColor = uColor;\n" +
                                    "}\n")
            );
            mProgramPosition = GlUtility.getAttribLocation(mProgram, "vPosition");
            mProgramProjection = GlUtility.getUniformLocation(mProgram, "uProjection");
            mProgramColor = GlUtility.getUniformLocation(mProgram, "uColor");
            mProgramPointSize = GlUtility.getUniformLocation(mProgram, "uPointSize");
        }
        private static int countPoints(Game.Unit unit) {
            return (int) Math.ceil((POINTS_PER_UNIT * unit.health) / unit.spec.health);
        }
        private float randomDelta() {
            return (2 * mRandom.nextFloat() - 1) *
                    (mLastGameTime - mGame.time) *
                    RANDOM_DELTA_SCALE * mLaneWidth;
        }
        private int updateParticle(Game.Unit unit, int lane, int offset) {
            Particle particle = mParticles.get(unit.id);
            if (particle == null) {
                particle = new Particle(0, 0, lane * mLaneWidth, unit.position, false);
                mParticles.put(unit.id, particle);
            }

            // TODO: MAX_VERTICES overflow handling
            int nPoints = countPoints(unit);
            int copyPoints = Math.min(nPoints, particle.npoints);
            int dx = (lane * mLaneWidth) - particle.xposition;
            int dy = unit.position - particle.yposition;
            int cursor = particle.offset;
            particle.offset = offset;
            particle.npoints = nPoints;
            particle.xposition = lane * mLaneWidth;
            particle.yposition = unit.position;
            particle.parity = mParity;
            final int xmin = particle.xposition + mMargin;
            final int xmax = particle.xposition + mLaneWidth - mMargin;
            final int ymin = particle.yposition + mMargin;
            final int ymax = particle.yposition + unit.spec.height - mMargin;
            // Update existing points
            for (int i = 0; i < copyPoints; ++i) {
                mData[offset] = Utility.clamp(mOldData[cursor] + dx + randomDelta(), xmin, xmax);
                mData[offset+1] = Utility.clamp(mOldData[cursor+1] + dy + randomDelta(), ymin, ymax);
                offset += 2;
                cursor += 2;
            }
            // Add new points
            for (int i = copyPoints; i < nPoints; ++i) {
                mData[offset] = xmin + mRandom.nextFloat() * (xmax - xmin);
                mData[offset+1] = ymin + mRandom.nextFloat() * (ymax - ymin);
                offset += 2;
            }
            return offset;
        }
        void draw(float[] projection) {
            // 1. update vertex buffer
            int offset = 0;
            for (int laneIndex = 0; laneIndex < mGame.lanes.size(); ++laneIndex) {
                for (Game.Unit unit : mGame.lanes.get(laneIndex).units) {
                    if (unit.owner == mOwner) {
                        offset = updateParticle(unit, laneIndex, offset);
                    }
                }
            }
            final int nvertices = offset / 2;
            mVertexBuffer.position(0);
            mVertexBuffer.put(mData, 0, 2 * nvertices);
            mVertexBuffer.position(0);

            // 2. draw the scene
            GLES20.glUseProgram(mProgram);
            GLES20.glEnableVertexAttribArray(mProgramPosition);
            GLES20.glVertexAttribPointer(mProgramPosition, 2, GLES20.GL_FLOAT, false, 0, mVertexBuffer);
            GLES20.glUniformMatrix4fv(mProgramProjection, 1, false, projection, 0);
            GLES20.glUniform4fv(mProgramColor, 1, mColor, 0);
            GLES20.glUniform1f(mProgramPointSize, POINT_SIZE);
            GLES20.glDrawArrays(GLES20.GL_POINTS, 0, nvertices);
            GLES20.glDisableVertexAttribArray(mProgramPosition);

            // 3. swap old & new vertex buffers & delete unused particles
            float[] tmp = mOldData;
            mOldData = mData;
            mData = tmp;
            Iterator<Particle> values = mParticles.values().iterator();
            while (values.hasNext()) {
                if (values.next().parity != mParity) {
                    values.remove();
                }
            }
            mParity = !mParity;
            mLastGameTime = mGame.time;
        }
    }

    private Simulation mSimulation;
    private Game mGame;
    private float[] mProjection;
    private Background mBackground;
    private Particles mFriendlyParticles, mEnemyParticles;

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

        mGame = mSimulation.getState(null);
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
        mFriendlyParticles = new Particles(mGame, Game.Owner.FRIENDLY, laneWidth, new float[] { 0, 0, 1, 1 });
        mEnemyParticles = new Particles(mGame, Game.Owner.ENEMY, laneWidth, new float[] { 1, 0, 0, 1 });
    }

    @Override
    public void onDrawFrame(GL10 gl10) {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);
        mSimulation.getState(mGame);
        mBackground.draw(mProjection);
        mFriendlyParticles.draw(mProjection);
        mEnemyParticles.draw(mProjection);
    }
}
