package dorr.lanegame.core;

import org.jetbrains.annotations.Nullable;

import java.util.Timer;
import java.util.TimerTask;

public class Simulation extends TimerTask {
    private final Game mGame;
    private final Object mGameLock = new Object();
    private final Timer mTimer;
    private final float mTimestep;
    private final Agent mFriendlyAgent;
    private final Agent mEnemyAgent;

    public Simulation(float dt, Game.GameSpec spec, Agent friendly, Agent enemy) {
        mGame = new Game(spec);
        mTimestep = dt;
        mTimer = new Timer("simulation", true);
        mFriendlyAgent = friendly;
        mEnemyAgent = enemy;
        start();
    }

    public Game.GameSpec spec() {
        return mGame.spec;
    }

    /**
     * Read the current state into scratch & return it.
     * <p>
     * If scratch is null, create a new Game object (don't do this every frame!).
     */
    public Game getState(@Nullable Game scratch) {
        if (scratch == null) {
            scratch = new Game(mGame.spec);
        }
        synchronized (mGameLock) {
            scratch.copyFrom(mGame);
        }
        return scratch;
    }

    public void start() {
        mTimer.schedule(this, 0, (int) (mTimestep * 1000));
    }

    public void stop() {
        mTimer.cancel();
    }

    @Override
    public void run() {
        synchronized (mGameLock) {
            mGame.tick(mTimestep, mFriendlyAgent.place(mGame), mEnemyAgent.place(mGame));
        }
    }
}
