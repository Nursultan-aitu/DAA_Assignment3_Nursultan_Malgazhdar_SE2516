package edu.daa.algorithms;

import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

class DisjointSetUnionTest {
    @Test
    void randomUnionsAgreeWithSimpleReference() {
        int n = 200;
        DisjointSetUnion dsu = DisjointSetUnion.makeSet(n);
        int[] label = new int[n];
        for (int i = 0; i < n; i++) label[i] = i;
        Random random = new Random(42);
        for (int operation = 0; operation < 5_000; operation++) {
            int a = random.nextInt(n);
            int b = random.nextInt(n);
            boolean expectedMerge = label[a] != label[b];
            assertEquals(expectedMerge, dsu.union(a, b));
            if (expectedMerge) {
                int old = label[b], replacement = label[a];
                for (int i = 0; i < n; i++) if (label[i] == old) label[i] = replacement;
            }
            int x = random.nextInt(n), y = random.nextInt(n);
            assertEquals(label[x] == label[y], dsu.find(x) == dsu.find(y));
            assertEquals(distinct(label), dsu.components());
        }
        assertEquals(dsu.findCalls() + dsu.successfulUnions(), dsu.operations());
    }

    @Test
    void validatesIndices() {
        DisjointSetUnion dsu = new DisjointSetUnion(3);
        assertThrows(IndexOutOfBoundsException.class, () -> dsu.find(-1));
        assertThrows(IndexOutOfBoundsException.class, () -> dsu.union(0, 3));
    }

    private static int distinct(int[] values) {
        boolean[] seen = new boolean[values.length];
        int count = 0;
        for (int value : values) if (!seen[value]) { seen[value] = true; count++; }
        return count;
    }
}
