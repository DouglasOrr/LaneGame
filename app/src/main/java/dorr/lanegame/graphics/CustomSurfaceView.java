package dorr.lanegame.graphics;

import android.content.Context;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.util.AttributeSet;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class CustomSurfaceView extends GLSurfaceView {
    private static class Renderer implements GLSurfaceView.Renderer {
        @Override
        public void onSurfaceCreated(GL10 gl10, EGLConfig eglConfig) {
            GLES20.glClearColor(0, 0.25f, 0.5f, 1);
        }
        @Override
        public void onSurfaceChanged(GL10 gl10, int width, int height) {
            GLES20.glViewport(0, 0, width, height);
        }
        @Override
        public void onDrawFrame(GL10 gl10) {
            GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);
        }
    }

    public CustomSurfaceView(Context context) {
        super(context);
    }
    public CustomSurfaceView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }
    {
        setEGLContextClientVersion(2);
        setRenderer(new Renderer());
        setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
    }
}
