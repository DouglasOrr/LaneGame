package dorr.lanegame.core;

import android.support.annotation.NonNull;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;

import dorr.lanegame.BuildConfig;

import static dorr.lanegame.core.Utility.check;
import static dorr.lanegame.core.Utility.getOrNull;

public class Game {
    public static GameSpec EXAMPLE = new GameSpec(
            5,
            10000,
            Arrays.asList(
                    new ObjectiveSpec(0, 2000, 200),
                    new ObjectiveSpec(1, 6000, 200),
                    new ObjectiveSpec(2, 5000, 200),
                    new ObjectiveSpec(3, 4000, 200),
                    new ObjectiveSpec(4, 8000, 200)
            ),
            3000,
            200,
            Arrays.asList(
                    new UnitSpec("sword", 1000, 2000, 10000,
                            4000, 1000, 1000,
                            true, 0, false),
                    new UnitSpec("arrow", 1000, 2000, 10000,
                            4000, 1000, 1000,
                            false, 3000, false),
                    new UnitSpec("horse", 1000, 4000, 10000,
                            4000, 1000, 1000,
                            false, 0, true)
            )
    );

    public static class UnitSpec {
        @NotNull final String name;
        // Basic stats
        final int height;
        final int speed;
        final int health;
        final int attack;
        final int minAttack;
        final int cost;
        // Special powers
        final boolean merge;
        final int range;
        final boolean swapLanes;
        UnitSpec(@NotNull String name, int height, int speed, int health,
                 int attack, int minAttack, int cost,
                 boolean merge, int range, boolean swapLanes) {
            this.name = name;
            this.height = height;
            this.speed = speed;
            this.health = health;
            this.attack = attack;
            this.minAttack = minAttack;
            this.cost = cost;
            this.merge = merge;
            this.range = range;
            this.swapLanes = swapLanes;
        }
    }
    public static class ObjectiveSpec {
        final int lane;
        final int position;
        final int income;
        ObjectiveSpec(int lane, int position, int income) {
            this.lane = lane;
            this.position = position;
            this.income = income;
        }
    }
    public static class GameSpec {
        final int lanes;
        final int length;
        @NotNull final List<ObjectiveSpec> objectives;
        final int startingBalance;
        final int income;
        @NotNull final List<UnitSpec> units;
        GameSpec(int lanes, int length, @NotNull List<ObjectiveSpec> objectives,
                 int startingBalance, int income, @NotNull List<UnitSpec> units) {
            this.lanes = lanes;
            this.length = length;
            this.objectives = Collections.unmodifiableList(objectives);
            this.startingBalance = startingBalance;
            this.income = income;
            this.units = Collections.unmodifiableList(units);
        }
    }

    public static class Placement {
        @NotNull final String unit;
        final int lane;
        public Placement(@NotNull String unit, int lane) {
            this.unit = unit;
            this.lane = lane;
        }
    }

    private static class UnitIterator implements Iterator<Unit> {
        private final List<Unit> mItems;
        private final int mStep;
        private int mCurrent;
        UnitIterator(List<Unit> items, int start, int step) {
            mItems = items;
            mCurrent = start - step;
            mStep = step;
        }
        int index() {
            return mCurrent;
        }
        Unit current() {
            return mItems.get(mCurrent);
        }
        Unit peek() {
            return getOrNull(mItems, mCurrent + mStep);
        }
        @Override
        public boolean hasNext() {
            int next = mCurrent + mStep;
            return 0 <= next && next < mItems.size();
        }
        @Override
        public Unit next() {
            if (!hasNext()) {
                throw new NoSuchElementException("Exhausted iterator");
            }
            mCurrent += mStep;
            return mItems.get(mCurrent);
        }
        @Override
        public void remove() {
            mItems.remove(mCurrent);
            if (0 < mStep) {
                // If we're going forwards, we must rewind by one step in order not to skip
                // the next element
                mCurrent -= mStep;
            }
        }
    }
    enum Owner {
        FRIENDLY,
        ENEMY;
        Owner flip() {
            if (this == FRIENDLY) return ENEMY;
            else return FRIENDLY;
        }
        int direction() {
            if (this == FRIENDLY) return 1;
            else return -1;
        }
        UnitIterator forward(List<Unit> units) {
            if (this == FRIENDLY) return new UnitIterator(units, 0, 1);
            else return new UnitIterator(units, units.size() - 1, -1);
        }
        UnitIterator reverse(List<Unit> units) {
            return this.flip().forward(units);
        }
    }
    static class Unit {
        enum State {
            MOVEMENT,
            COMBAT
        }
        // NOTE: updates here must be reflected in Game.copyLane
        @NotNull UnitSpec spec;
        @NotNull Owner owner;
        int position;
        int health;
        @NotNull State state;
        Unit(@NotNull UnitSpec spec, @NotNull Owner owner, int position, int health, @NonNull State state) {
            this.spec = spec;
            this.owner = owner;
            this.position = position;
            this.health = health;
            this.state = state;
        }
    }
    static class Player {
        // NOTE: updates here must be reflected in Game.copyFrom
        int balance;
        Player(int balance) {
            this.balance = balance;
        }
    }
    static class Objective {
        // NOTE: updates here must be reflected in Game.copyLane
        @NotNull ObjectiveSpec spec;
        @Nullable Owner owner;
        int position;
        Objective(@NotNull ObjectiveSpec spec, @Nullable Owner owner, int position) {
            this.spec = spec;
            this.owner = owner;
            this.position = position;
        }
    }
    static class Lane {
        // NOTE: updates here must be reflected in Game.copyLane
        @NotNull final List<Objective> objectives;
        @NotNull final List<Unit> units;
        Lane(@NotNull List<Objective> objectives, @NotNull List<Unit> units) {
            this.objectives = objectives;
            this.units = units;
        }
    }

    @NotNull final GameSpec spec;
    @NotNull final List<Lane> lanes;
    private final List<Player> mPlayers;
    private final Map<String, UnitSpec> mNameToUnitSpec;

    public Game(@NotNull GameSpec spec) {
        this.spec = spec;
        mPlayers = Arrays.asList(
                new Player(spec.startingBalance),
                new Player(spec.startingBalance)
        );

        this.lanes = new ArrayList<>(spec.lanes);
        for (int lane = 0; lane < spec.lanes; ++lane) {
            ArrayList<Objective> objectives = new ArrayList<>();
            for (ObjectiveSpec objectiveSpec : spec.objectives) {
                if (objectiveSpec.lane == lane) {
                    objectives.add(new Objective(objectiveSpec, null, objectiveSpec.position));
                }
            }
            Collections.sort(objectives, new Comparator<Objective>() {
                @Override
                public int compare(Objective a, Objective b) {
                    if (a.spec.position < b.spec.position) {
                        return -1;
                    } else if (b.spec.position < a.spec.position) {
                        return 1;
                    } else {
                        return 0;
                    }
                }
            });
            this.lanes.add(new Lane(objectives, new ArrayList<Unit>()));
        }

        // Build the name -> unit mapping
        Map<String, UnitSpec> nameToUnitSpec = new HashMap<>();
        for (UnitSpec unitSpec : spec.units) {
            if (nameToUnitSpec.containsKey(unitSpec.name)) {
                throw new IllegalArgumentException(
                        "Duplicate unit name \"" + unitSpec.name + "\"");
            }
            nameToUnitSpec.put(unitSpec.name, unitSpec);
        }
        mNameToUnitSpec = Collections.unmodifiableMap(nameToUnitSpec);
    }

    // Basic utilities

    Player player(Owner owner) {
        return mPlayers.get(owner.ordinal());
    }

    private static boolean isOverlapping(Unit a, Unit b) {
        return (a.position < b.position + b.spec.height
                && b.position < a.position + a.spec.height);
    }

    private void checkInvariants() {
        if (BuildConfig.DEBUG) {
            check(lanes.size() == spec.lanes, "wrong number of lanes");
            check(mPlayers.get(0) != mPlayers.get(1), "duplicate player");
            for (Lane lane : lanes) {
                for (int i = 0; i < lane.units.size() - 1; ++i) {
                    Unit a = lane.units.get(i);
                    Unit b = lane.units.get(i + 1);
                    check(a.position < b.position, "mis-ordered lanes");
                    check(!isOverlapping(a, b), "overlapping units");
                }
            }
        }
    }

    // API subfunctions

    private void place(Owner owner, Placement placement) {
        if (placement != null) {
            if (placement.lane < 0 || this.lanes.size() <= placement.lane) {
                throw new IllegalArgumentException(
                        "Bad lane assignment - no lane: " + placement.lane);
            }
            Lane lane = this.lanes.get(placement.lane);
            UnitSpec spec = mNameToUnitSpec.get(placement.unit);
            if (spec.cost <= player(owner).balance) {
                int position, index;
                Unit encumbent;
                if (owner == Owner.FRIENDLY) {
                    position = 0;
                    index = 0;
                    encumbent = getOrNull(lane.units, 0);
                } else {
                    position = this.spec.length - 1 - spec.height;
                    index = lane.units.size();
                    encumbent = getOrNull(lane.units, lane.units.size() - 1);
                }
                Unit unit = new Unit(spec, owner, position, spec.health, Unit.State.MOVEMENT);
                if (encumbent == null || !isOverlapping(encumbent, unit)) {
                    lane.units.add(index, unit);
                    player(owner).balance -= spec.cost;
                }
            }
        }
    }

    private void addIncome(float dt) {
        int baseIncome = (int)(dt * this.spec.income);
        player(Owner.FRIENDLY).balance += baseIncome;
        player(Owner.ENEMY).balance += baseIncome;
        for (Lane lane : this.lanes) {
            for (Objective objective : lane.objectives) {
                for (Unit unit : lane.units) {
                    int d = objective.spec.position - unit.position;
                    if (0 <= d && d < unit.spec.height) {
                        objective.owner = unit.owner; // captured!
                    }
                }
                if (objective.owner != null) {
                    player(objective.owner).balance += (int) (dt * objective.spec.income);
                }
            }
        }
    }

    private static Unit getCombat(Lane lane, Unit unit) {
        // Find the closest enemy unit with a linear scan
        Unit closest = null;
        int closestDistance = Integer.MAX_VALUE;
        for (Unit other : lane.units) {
            if (other.owner != unit.owner) {
                int distance = Math.max(
                        unit.position - other.position - other.spec.height, // below
                        other.position - unit.position - unit.spec.height   // above
                );
                Utility.check(0 <= distance, "bad distance calculation");
                if (distance < closestDistance) {
                    closest = other;
                    closestDistance = distance;
                }
            }
        }
        if (closestDistance <= unit.spec.range) {
            return closest;
        }
        return null;
    }

    private void doCombat(float dt) {
        for (Lane lane : this.lanes) {
            // Pass 1: compute damage
            Map<Unit, Integer> unitDamage = new HashMap<>();
            for (Unit unit : lane.units) {
                Unit enemy = getCombat(lane, unit);
                if (enemy != null) {
                    unit.state = Unit.State.COMBAT;
                    Integer damage = unitDamage.get(enemy);
                    if (damage == null) {
                        damage = 0;
                    }
                    damage += (int)(dt * Math.max(unit.spec.minAttack,
                            (unit.spec.attack * unit.health) / unit.spec.health));
                    unitDamage.put(enemy, damage);
                } else {
                    unit.state = Unit.State.MOVEMENT;
                }
            }
            // Pass 2: reduce health & remove units
            for (Iterator<Unit> iterator = lane.units.iterator(); iterator.hasNext(); ) {
                Unit unit = iterator.next();
                Integer damage = unitDamage.get(unit);
                if (damage != null) {
                    unit.health -= damage;
                    if (unit.health <= 0) {
                        iterator.remove();
                    }
                }
            }
        }
    }

    private static boolean isFlanking(Lane lane, int unitIndex) {
        Unit unit = lane.units.get(unitIndex);
        if (unit.spec.swapLanes) {
            Unit prev = getOrNull(lane.units, unitIndex - unit.owner.direction());
            return prev != null && prev.owner != unit.owner;
        }
        return false;
    }

    private static Integer getFlank(Unit unit, Lane adjacent) {
        if (adjacent == null) {
            return null;
        }
        for (Unit other : adjacent.units) {
            if (isOverlapping(unit, other)) {
                return null;
            }
        }
        for (UnitIterator iterator = unit.owner.forward(adjacent.units); iterator.hasNext(); ) {
            Unit other = iterator.next();
            Unit next = iterator.peek();
            // Test if we've found the gap
            int d = unit.owner.direction();
            if (other.position * d < unit.position * d
                    && (next == null || unit.position * d < next.position * d)) {
                return other.owner != unit.owner ? iterator.index() + Math.max(d, 0) : null;
            }
        }
        return null;
    }

    private void doSwapLanes(Owner owner) {
        for (int laneIndex = 0; laneIndex < this.lanes.size(); ++laneIndex) {
            Lane previous = getOrNull(this.lanes, laneIndex - 1);
            Lane current = this.lanes.get(laneIndex);
            Lane next = getOrNull(this.lanes, laneIndex + 1);

            for (UnitIterator iterator = owner.forward(current.units); iterator.hasNext(); ) {
                Unit unit = iterator.next();
                if (unit.owner == owner
                        && unit.spec.swapLanes
                        && unit.state == Unit.State.MOVEMENT
                        && !isFlanking(current, iterator.index())) {
                    Integer flank = getFlank(iterator.current(), previous);
                    if (flank != null) {
                        previous.units.add(flank, iterator.current());
                        iterator.remove();
                    } else {
                        flank = getFlank(iterator.current(), next);
                        if (flank != null) {
                            next.units.add(flank, iterator.current());
                            iterator.remove();
                        }
                    }
                }
            }
        }
    }

    private void doRefund(UnitIterator iterator) {
        Unit unit = iterator.current();
        if (unit.position < 0 || this.spec.length < unit.position + unit.spec.height) {
            int refund = (unit.spec.cost * unit.health) / unit.spec.health;
            player(unit.owner).balance += refund;
            iterator.remove();
        }
    }

    private static Unit doCollision(UnitIterator iterator, int direction, Lane lane) {
        Unit unit = iterator.current();
        Unit next = getOrNull(lane.units, iterator.index() + direction);
        if (next != null) {
            if (direction == 1 && next.position < unit.position + unit.spec.height) {
                unit.position = next.position - unit.spec.height;
                return next;
            }
            if (direction == -1 && unit.position < next.position + next.spec.height) {
                unit.position = next.position + next.spec.height;
                return next;
            }
        }
        return null;
    }

    private void doUnitMovement(float dt, Lane lane, UnitIterator iterator) {
        Unit unit = iterator.current();
        int dx = (int) (dt * unit.spec.speed);
        int direction = unit.owner.direction();
        if (isFlanking(lane, iterator.index())) {
            direction *= -1;
        }
        unit.position += direction * dx;

        Unit next = doCollision(iterator, direction, lane);
        if (next != null
                && unit.owner == next.owner
                && unit.spec.name.equals(next.spec.name)
                && unit.spec.merge) {
            next.health += unit.health;
            iterator.remove();
        } else {
            doRefund(iterator);
        }
    }

    private void doMovement(float dt, Owner owner) {
        for (Lane lane : this.lanes) {
            // In furthest-to-nearest (reverse) order
            for (UnitIterator iterator = owner.reverse(lane.units); iterator.hasNext(); ) {
                Unit unit = iterator.next();
                if (unit.owner == owner && unit.state == Unit.State.MOVEMENT) {
                    doUnitMovement(dt, lane, iterator);
                }
            }
        }
    }

    private static void copyLane(Lane dest, Lane src) {
        check(dest.objectives.size() == src.objectives.size(),
                "Same spec => same number of objectives");
        for (int i = 0; i < dest.objectives.size(); ++i) {
            Objective destObjective = dest.objectives.get(i);
            Objective srcObjective = src.objectives.get(i);
            destObjective.spec = srcObjective.spec;
            destObjective.position = srcObjective.position;
            destObjective.owner = srcObjective.owner;
        }
        // Remove extra units
        while (src.units.size() < dest.units.size()) {
            dest.units.remove(dest.units.size() - 1);
        }
        // Update existing units
        for (int i = 0; i < src.units.size(); ++i) {
            Unit srcUnit = src.units.get(i);
            Unit destUnit = dest.units.get(i);
            srcUnit.spec = destUnit.spec;
            srcUnit.owner = destUnit.owner;
            srcUnit.position = destUnit.position;
            srcUnit.health = destUnit.health;
            srcUnit.state = destUnit.state;
        }
        // Add new units
        for (int i = src.units.size(); i < dest.units.size(); ++i) {
            Unit unit = dest.units.get(i);
            dest.units.add(new Unit(unit.spec, unit.owner, unit.position, unit.health, unit.state));
        }
    }

    // Public API

    /**
     * Invert the simulation, so that the enemy becomes the friendly & the top of the map becomes
     * the bottom.
     */
    public void invert() {
        Collections.reverse(this.lanes);
        for (Lane lane : this.lanes) {
            Collections.reverse(lane.objectives);
            for (Objective objective : lane.objectives) {
                objective.owner = objective.owner == null ? null : objective.owner.flip();
                objective.position = this.spec.length - 1 - objective.position;
            }
            Collections.reverse(lane.units);
            for (Unit unit : lane.units) {
                unit.owner = unit.owner.flip();
                unit.position = this.spec.length - 1 - unit.position - unit.spec.height;
            }
        }
        Collections.reverse(mPlayers);
        checkInvariants();
    }

    /**
     * Deep copy the state from "game" into the current game.
     *
     * Both games must have been created from the same spec.
     */
    public void copyFrom(Game game) {
        if (this.spec != game.spec) {
            throw new IllegalArgumentException(
                    "Cannot copyFrom a game which has a different spec");
        }
        // As the spec is the same, we don't need to sync {.spec, .mNameToUnitSpec}
        this.mPlayers.get(0).balance = game.mPlayers.get(0).balance;
        this.mPlayers.get(1).balance = game.mPlayers.get(1).balance;
        check(this.lanes.size() == game.lanes.size(),
                "Same spec => same number of lanes");
        for (int i = 0; i < this.lanes.size(); ++i) {
            copyLane(this.lanes.get(i), game.lanes.get(i));
        }
        checkInvariants();
    }

    /**
     * Advance the simulation by a single timestep.
     *
     * The simulation is not guaranteed to be fair. If fairness is required, we recommend inverting
     * the game as follows:
     * <pre>{@code
     * game.invert();
     * game.tick(dt, friendly, enemy);
     * game.invert();
     * }</pre>
     */
    public void tick(float dt, @Nullable Placement friendly, @Nullable Placement enemy) {
        place(Owner.FRIENDLY, friendly);
        place(Owner.ENEMY, enemy);
        addIncome(dt);
        doCombat(dt);
        doSwapLanes(Owner.FRIENDLY);
        doSwapLanes(Owner.ENEMY);
        doMovement(dt, Owner.FRIENDLY);
        doMovement(dt, Owner.ENEMY);

        // Debug
        checkInvariants();
    }
}
