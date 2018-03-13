package dorr.lanegame.graphics;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;

import java.util.Arrays;
import java.util.Timer;
import java.util.TimerTask;

import dorr.lanegame.R;
import dorr.lanegame.core.Game;

public class MainActivity extends Activity {
    private static class Simulation extends TimerTask {
        private final Game mGame;
        private final Timer mTimer;
        private final float mTimestep;
        Simulation(Game.GameSpec spec, float dt) {
            mGame = new Game(spec);
            mTimestep = dt;
            mTimer = new Timer("simulation", true);
            mTimer.schedule(this, 0, (int)(dt * 1000));
        }
        void stop() {
            mTimer.cancel();
        }
        @Override
        public void run() {
            // TODO
            mGame.tick(mTimestep,
                    Arrays.<String> asList(null, null, "sword", null, null),
                    Arrays.<String> asList(null, null, "sword", null, null));
        }
    }

    private Simulation mSimulation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    @Override
    protected void onStart() {
        super.onStart();
        mSimulation = new Simulation(Game.EXAMPLE, 0.01f);
    }

    @Override
    protected void onStop() {
        mSimulation.stop();
        mSimulation = null;
        super.onStop();
    }
}
