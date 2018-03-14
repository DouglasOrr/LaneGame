package dorr.lanegame.core;

import java.util.Random;

public abstract class Agent {
    public abstract Game.Placement place(Game game);

    /**
     * Place units of random types on random lanes (doesn't do any checking).
     */
    public static class RandomAgent extends Agent {
        private final Random mRandom = new Random();
        private final Game.GameSpec mSpec;
        public RandomAgent(Game.GameSpec spec) {
            mSpec = spec;
        }
        @Override
        public Game.Placement place(Game game) {
            return new Game.Placement(
                    mSpec.units.get(mRandom.nextInt(mSpec.units.size())).name,
                    mRandom.nextInt(mSpec.lanes));
        }
    }
}
