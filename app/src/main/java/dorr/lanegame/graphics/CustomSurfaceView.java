package dorr.lanegame.graphics;

import android.content.Context;
import android.opengl.GLSurfaceView;
import android.util.AttributeSet;
import android.util.Log;
import android.widget.GridLayout;

import java.util.Timer;
import java.util.TimerTask;

import dorr.lanegame.core.Game;

public class CustomSurfaceView extends GLSurfaceView {
    public final dorr.lanegame.graphics.Renderer renderer;
    public CustomSurfaceView(Context context) {
        super(context);
    }
    public CustomSurfaceView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }
    {
        setEGLContextClientVersion(2);
        renderer = new dorr.lanegame.graphics.Renderer();
        setRenderer(renderer);
        setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);
//        setRenderer(new Examples.Basic());
//        setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
//        setRenderer(new Examples.Particles(256));  // 131072
//        setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);
    }
}
