package dorr.lanegame.core;

import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import static java.util.Arrays.asList;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.sameInstance;
import static org.junit.Assert.assertThat;

public class GameTest {
    // A simple testing spec
    private static final Game.GameSpec TEST_SPEC = new Game.GameSpec(
            3,
            10000,
            asList(
                    new Game.ObjectiveSpec(0, 2000, 200),
                    new Game.ObjectiveSpec(0, 8000, 200),
                    new Game.ObjectiveSpec(1, 5000, 200),
                    new Game.ObjectiveSpec(2, 5000, 200)
            ),
            2000,
            200,
            asList(
                    new Game.UnitSpec("sword", 1000, 2000, 10000,
                            4000, 1000, 1000,
                            true, 0, false),
                    new Game.UnitSpec("arrow", 1000, 2000, 10000,
                            4000, 1000, 1000,
                            false, 3000, false),
                    new Game.UnitSpec("horse", 1000, 4000, 10000,
                            5000, 2000, 1400,
                            false, 0, true)
            )
    );

    private static List<String> noPlacement() {
        return asList(null, null, null);
    }

    @Test
    public void emptyTicks() {
        Game game = new Game(TEST_SPEC);
        assertThat(game.spec, sameInstance(TEST_SPEC));
        assertThat(game.player(Game.Owner.FRIENDLY).balance, is(TEST_SPEC.startingBalance));
        assertThat(game.player(Game.Owner.ENEMY).balance, is(TEST_SPEC.startingBalance));
        assertThat(game.lanes.get(0).units, empty());

        // Tick without placement - no change
        game.tick(0.0f, null, null);
        assertThat(game.player(Game.Owner.FRIENDLY).balance, is(TEST_SPEC.startingBalance));
        assertThat(game.player(Game.Owner.ENEMY).balance, is(TEST_SPEC.startingBalance));
        assertThat(game.lanes.get(0).units, empty());
    }

    @Test
    public void income() {
        Game game = new Game(TEST_SPEC);
        int balance = game.player(Game.Owner.FRIENDLY).balance;
        assertThat(game.player(Game.Owner.ENEMY).balance, is(balance));

        game.tick(0.1f, null, null);
        balance += 20;
        assertThat(game.player(Game.Owner.FRIENDLY).balance, is(balance));
        assertThat(game.player(Game.Owner.ENEMY).balance, is(balance));

        // Fake out capturing some objectives
        game.lanes.get(0).objectives.get(0).owner = Game.Owner.FRIENDLY;
        game.lanes.get(0).objectives.get(1).owner = Game.Owner.FRIENDLY;
        game.lanes.get(1).objectives.get(0).owner = Game.Owner.ENEMY;
        game.tick(0.1f, null, null);
        assertThat(game.player(Game.Owner.FRIENDLY).balance, is(balance + 60));
        assertThat(game.player(Game.Owner.ENEMY).balance, is(balance + 40));
    }

    @Test
    public void placement() {
        Game game = new Game(TEST_SPEC);
        assertThat(game.lanes.get(0).units, empty());
        assertThat(game.player(Game.Owner.FRIENDLY).balance, is(2000));

        game.tick(0.0f, new Game.Placement("sword", 0), null);
        assertThat(game.lanes.get(0).units, hasSize(1));
        assertThat(game.player(Game.Owner.FRIENDLY).balance, is(1000));

        // horse is too expensive
        game.tick(0.0f, new Game.Placement("horse", 1), null);
        assertThat(game.lanes.get(1).units, empty());
        assertThat(game.player(Game.Owner.FRIENDLY).balance, is(1000));

        // cannot stack units (at placement time)
        game.tick(0.0f, new Game.Placement("sword", 0), null);
        assertThat(game.lanes.get(0).units, hasSize(1));
        assertThat(game.player(Game.Owner.FRIENDLY).balance, is(1000));

        // enemy can place (at the other end of the lane
        game.tick(0.0f, null, new Game.Placement("sword", 0));
        assertThat(game.lanes.get(0).units, hasSize(2));
        assertThat(game.player(Game.Owner.ENEMY).balance, is(1000));
    }

    @Test
    public void invert() {
        Game game = new Game(TEST_SPEC);
        game.tick(0.0f, new Game.Placement("sword", 0), new Game.Placement("horse", 2));
        assertThat(game.player(Game.Owner.FRIENDLY).balance, is(1000));
        assertThat(game.player(Game.Owner.ENEMY).balance, is(600));
        Game.Unit sword = game.lanes.get(0).units.get(0);
        assertThat(sword.owner, is(Game.Owner.FRIENDLY));
        assertThat(sword.position, is(0));
        Game.Unit horse = game.lanes.get(2).units.get(0);
        assertThat(horse.owner, is(Game.Owner.ENEMY));
        assertThat(horse.position, is(8999));

        game.invert();
        assertThat(game.player(Game.Owner.ENEMY).balance, is(1000));
        assertThat(game.player(Game.Owner.FRIENDLY).balance, is(600));
        sword = game.lanes.get(2).units.get(0);
        assertThat(sword.owner, is(Game.Owner.ENEMY));
        assertThat(sword.position, is(8999));
        horse = game.lanes.get(0).units.get(0);
        assertThat(horse.owner, is(Game.Owner.FRIENDLY));
        assertThat(horse.position, is(0));
    }

    private static String render(Game game) {
        int height = game.spec.units.get(0).height;
        char[] row = new char[game.spec.length / height];

        StringBuilder sb = new StringBuilder();
        sb.append("+");
        Arrays.fill(row, '-');
        sb.append(row);
        sb.append("+\n");
        for (Game.Lane lane : game.lanes) {
            Arrays.fill(row, ' ');
            int n = 0;
            for (Game.Objective objective : lane.objectives) {
                row[(objective.spec.position + height / 2) / height] = '\'';
            }
            for (Game.Unit unit : lane.units) {
                char label = unit.owner == Game.Owner.FRIENDLY ? '@' : '#';
                row[(unit.position + height / 2) / height] = label;
            }
            sb.append("|");
            sb.append(row);
            sb.append("|\n");
        }
        sb.append("+");
        Arrays.fill(row, '-');
        sb.append(row);
        sb.append("+\n");
        return sb.toString();
    }

    @Test
    public void demo() {
        Game game = new Game(TEST_SPEC);
        game.tick(0.5f, new Game.Placement("sword", 1), new Game.Placement("sword", 1));
        for (int i = 0; i < 2; ++i) {
            System.out.println(render(game));
            game.tick(0.5f, null, null);
        }
        System.out.println(render(game));
        game.tick(0.5f, new Game.Placement("sword", 1), new Game.Placement("sword", 1));
        for (int i = 0; i < 10; ++i) {
            System.out.println(render(game));
            game.tick(0.5f, null, null);
        }
        for (Game.Unit unit : game.lanes.get(1).units) {
            System.out.println(unit.position + " " + unit.health);
        }
    }
}
