package dorr.lanegame.core;

import org.hamcrest.Matchers;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;

public class UtilityTest {
    @Test(expected = IllegalStateException.class)
    public void checkFails() {
        Utility.check(false, "something went wrong");
    }

    @Test
    public void checkPasses() {
        Utility.check(true, "nothing went wrong");
    }

    @Test
    public void getOrNull() {
        assertThat(Utility.getOrNull(Collections.EMPTY_LIST, 0), nullValue());
        List<String> items = Arrays.asList("one", "two", "three");
        assertThat(Utility.getOrNull(items, -1), nullValue());
        assertThat(Utility.getOrNull(items, 0), is("one"));
        assertThat(Utility.getOrNull(items, 2), is("three"));
        assertThat(Utility.getOrNull(items, 3), nullValue());
    }

    @Test
    public void clamp() {
        assertThat(Utility.clamp(0.5f, 0, 1), is(0.5f));
        assertThat(Utility.clamp(-0.5f, 0, 1), is(0.0f));
        assertThat(Utility.clamp(1.5f, 0, 1), is(1.0f));
    }

    @Test
    public void fastRandom() {
        Utility.FastRandom random = new Utility.FastRandom(1234567890L);
        final int n = 1000;
        float sum = 0, sumsq = 0;
        for (int i = 0; i < n; ++i) {
            float f = random.nextFloat();
            sum += f;
            sumsq += f * f;
        }
        float mean = sum / n;
        float std = (float) Math.sqrt(sumsq / n - mean * mean);
        // No gross biases
        assertThat((double) mean, Matchers.closeTo(0.5, 0.01));
        assertThat((double) std, Matchers.closeTo(Math.sqrt(1./12), 0.01));
    }
}
