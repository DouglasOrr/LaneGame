package dorr.lanegame.graphics;

import android.app.Activity;
import android.os.Bundle;

import dorr.lanegame.R;
import dorr.lanegame.core.Agent;
import dorr.lanegame.core.Game;
import dorr.lanegame.core.Simulation;

public class MainActivity extends Activity {
    private Simulation mSimulation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    @Override
    protected void onStart() {
        super.onStart();
        Game.GameSpec spec = Game.EXAMPLE;
        mSimulation = new Simulation(0.01f, spec,
                new Agent.RandomAgent(spec), new Agent.RandomAgent(spec));
    }

    @Override
    protected void onStop() {
        mSimulation.stop();
        mSimulation = null;
        super.onStop();
    }
}
