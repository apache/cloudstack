package com.cloud.utils;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

public class PairTest {

    @Test
    public void shouldReturnAValidInstanceFromPairWithStrings() {
        final String expetedFirst = "A";
        final String expetedSecond = "B";
        final Pair<String, String> actual = Pair.of("A", "B");

        assertNotNull("the pair is null",actual);
        assertEquals(expetedFirst, actual.first());
        assertEquals(expetedSecond, actual.second());
    }

    @Test
    public void shouldReturnAValidInstanceFromPairWithIntegers() {
        final Integer expetedFirst = 1;
        final Integer expetedSecond = 2;
        final Pair<Integer, Integer> actual = Pair.of(1, 2);

        assertNotNull("the pair is null",actual);
        assertEquals(expetedFirst, actual.first());
        assertEquals(expetedSecond, actual.second());
    }

    @Test
    public void shouldReturnAValidInstanceFromPairWithLongs() {
        final Long expetedFirst = 1L;
        final Long expetedSecond = 2L;
        final Pair<Long, Long> actual = Pair.of(1L, 2L);

        assertNotNull("the pair is null",actual);
        assertEquals(expetedFirst, actual.first());
        assertEquals(expetedSecond, actual.second());
    }

    @Test
    public void shouldReturnAValidInstanceFromPairWithDiferentsTypes() {
        final String expetedFirst = "A";
        final Long expetedSecond = 2L;
        final Pair<String, Long> actual = Pair.of("A", 2L);

        assertNotNull("the pair is null",actual);
        assertEquals(expetedFirst, actual.first());
        assertEquals(expetedSecond, actual.second());
    }

    @Test
    public void shouldReturnAValidInstanceFromPairWithNulls() {
        final Pair<String, Long> actual = Pair.of(null,null);

        assertNotNull("the pair is null",actual);
        assertNull(actual.first());
        assertNull(actual.second());
    }

    @Test
    public void testToString_whenTAndUAreNull() {
        final Pair<Object, Object> p = new Pair<>(null, null);
        final String expected = "P[null:null]";
        final String actual = p.toString();
        assertEquals(expected, actual);
    }

    @Test
    public void testToString_whenTNotNullAndUIsNull() {
        final Pair<String, String> p = new Pair<>("apple", null);
        final String expected = "P[apple:null]";
        final String actual = p.toString();
        assertEquals(expected, actual);
    }

    @Test
    public void testToString_whenTIsNullAndUNotNull() {
        final Pair<String, String> p = new Pair<>(null, "orange");
        final String expected = "P[null:orange]";
        final String actual = p.toString();
        assertEquals(expected, actual);
    }
}
