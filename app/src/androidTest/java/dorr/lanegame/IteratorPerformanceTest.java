package dorr.lanegame;

import android.support.annotation.NonNull;
import android.support.test.runner.AndroidJUnit4;
import android.util.Log;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

@RunWith(AndroidJUnit4.class)
public class IteratorPerformanceTest {
    private static final int REPETITIONS = 1000000;
    private static final int LENGTH = 10;

    private static List<Integer> ints() {
        List<Integer> collection = new ArrayList<>();
        for (int i = 0; i < LENGTH; ++i) {
            collection.add(i);
        }
        return collection;
    }

    @Test
    public void testIteratorPerformance() {
        List<Integer> collection = ints();
        long t0 = System.nanoTime();
        long total = 0;
        for (int i = 0; i < REPETITIONS; ++i) {
            for (Integer item : collection) {
                total += item;
            }
        }
        long t1 = System.nanoTime();
        Log.d("DOUG", "total = " + total);
        Log.d("DOUG", String.format("Iterator: %d x %d (= %d ops) in %.2g s",
                REPETITIONS, LENGTH, REPETITIONS * LENGTH, (t1 - t0) * 1e-9));
    }

    class ResettableIterator<T> implements Iterator<T>, Iterable<T> {
        private final List<T> mItems;
        private final T[] mItemsArray;
        private final int mSize;
        private int mIndex = -1;
        public ResettableIterator(List<T> items) {
            mItems = items;
            mItemsArray = (T[]) items.toArray();
            mSize = mItems.size();
        }
        public void reset() {
            mIndex = -1;
        }
        @Override
        public boolean hasNext() {
            //return mIndex < mItems.size() - 1;
            return mIndex < mSize - 1;
            //return mIndex < mItemsArray.length - 1;
        }
        @Override
        public T next() {
            ++mIndex;
            //return mItems.get(mIndex);
            return mItemsArray[mIndex];
        }
        @NonNull
        @Override
        public Iterator<T> iterator() {
            return this;
        }
    }

    @Test
    public void testResettableIteratorPerformance() {
        List<Integer> collection = ints();
        long t0 = System.nanoTime();
        long total = 0;
        ResettableIterator<Integer> it = new ResettableIterator<>(collection);
        for (int i = 0; i < REPETITIONS; ++i) {
            while (it.hasNext()) {
                total += it.next();
            }
            it.reset();
        }
        long t1 = System.nanoTime();
        Log.d("DOUG", "total = " + total);
        Log.d("DOUG", String.format("ResettableIterator: %d x %d (= %d ops) in %.2g s",
                REPETITIONS, LENGTH, REPETITIONS * LENGTH, (t1 - t0) * 1e-9));
    }
}
