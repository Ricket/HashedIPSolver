package org.richardcarter;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class IpAddressTest {

    IpAddress ip(int i1, int i2, int i3, int i4) {
        return new IpAddress(i1, i2, i3, i4);
    }

    @Test
    void toBytes() {
        IpAddress ip = new IpAddress(15, 200, 0, 255);
        byte[] actual = ip.toBytes();
        byte[] expected = new byte[] {15, -56, 0, -1};
        assertArrayEquals(expected, actual);
    }

    @Test
    void toLongPositive() {
        IpAddress ip = IpAddress.MIN_VALUE;
        assertEquals(0, ip.toLong());

        long last = ip.toLong();
        do {
            IpAddress nextIp = ip.increment();
            long next = nextIp.toLong();
            assertTrue(next > last);
            assertTrue(nextIp.compareTo(ip) > 0);
            ip = nextIp;
            last = next;
        } while (!ip.equals(IpAddress.MAX_VALUE));
    }

    @Test
    void toAndFromLong() {
        IpAddress ip = new IpAddress(15, 200, 0, 255);
        long l = ip.toLong();
        IpAddress actual = IpAddress.fromLong(l);
        assertEquals(ip, actual);
    }

    @Test
    void increment() {
        assertEquals(ip(0, 0, 0, 1), ip(0, 0, 0, 0).increment());
        assertEquals(ip(0, 0, 0, 255), ip(0, 0, 0, 254).increment());
        assertEquals(ip(0, 0, 1, 0), ip(0, 0, 0, 255).increment());
        assertEquals(ip(0, 0, 255, 1), ip(0, 0, 255, 0).increment());
        assertEquals(ip(0, 1, 0, 0), ip(0, 0, 255, 255).increment());
        assertEquals(ip(1, 0, 0, 0), ip(0, 255, 255, 255).increment());
        assertEquals(ip(255, 255, 255, 255), ip(255, 255, 255, 254).increment());
        assertThrows(IndexOutOfBoundsException.class, () -> ip(255, 255, 255, 255).increment());
    }
}