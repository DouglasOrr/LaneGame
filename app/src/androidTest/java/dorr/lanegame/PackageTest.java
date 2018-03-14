package dorr.lanegame;

import android.content.Context;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class PackageTest {
    @Test
    public void packageName() throws Exception {
        Context appContext = InstrumentationRegistry.getTargetContext();
        assertEquals("dorr.lanegame", appContext.getPackageName());
    }
}
