package sample;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

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
