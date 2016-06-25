package sample;

import static org.junit.Assert.*;

import org.junit.Test;

public class MyTest {
    @Test
    public void itWorks() {
        assertEquals(2, 1 + 1);
    }

    @Test
    public void itFails() {
        assertEquals(5, 2 + 2);
    }
}
