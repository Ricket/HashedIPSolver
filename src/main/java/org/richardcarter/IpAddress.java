package org.richardcarter;

import lombok.EqualsAndHashCode;

import java.nio.charset.StandardCharsets;

@EqualsAndHashCode
public class IpAddress implements Comparable<IpAddress> {
    public static final IpAddress MIN_VALUE = new IpAddress(0, 0, 0, 0);
    public static final IpAddress MAX_VALUE = new IpAddress(255, 255, 255, 255);

    private final int oct1;
    private final int oct2;
    private final int oct3;
    private final int oct4;

    public IpAddress(int oct1, int oct2, int oct3, int oct4) {
        if (oct1 < 0 || oct1 > 255 || oct2 < 0 || oct2 > 255 || oct3 < 0 || oct3 > 255 || oct4 < 0 || oct4 > 255) {
            throw new IllegalArgumentException(oct1 + "." + oct2 + "." + oct3 + "." + oct4);
        }
        this.oct1 = oct1;
        this.oct2 = oct2;
        this.oct3 = oct3;
        this.oct4 = oct4;
    }

    public IpAddress increment() {
        if (oct4 < 255) {
            return new IpAddress(oct1, oct2, oct3, oct4 + 1);
        } else if (oct3 < 255) {
            return new IpAddress(oct1, oct2, oct3 + 1, 0);
        } else if (oct2 < 255) {
            return new IpAddress(oct1, oct2 + 1, 0, 0);
        } else if (oct1 < 255) {
            return new IpAddress(oct1 + 1, 0, 0, 0);
        } else {
            throw new IndexOutOfBoundsException();
        }
    }

    public long toLong() {
        return ((oct1 << 24) + (oct2 << 16) + (oct3 << 8) + oct4) & 0xFFFFFFFFL;
    }

    public byte[] toBytes() {
        return new byte[] {(byte) oct1, (byte) oct2, (byte) oct3, (byte) oct4};
    }

    public byte[] toBytesPlusSalt(String salt) {
        return (this + salt).getBytes(StandardCharsets.UTF_8);
    }

    public static IpAddress fromLong(long l) {
        return new IpAddress(
                (int) ((l >> 24) & 0xFF),
                (int) ((l >> 16) & 0xFF),
                (int) ((l >> 8) & 0xFF),
                (int) (l & 0xFF)
        );
    }

    public static IpAddress fromByteArray(byte[] ip) {
        return new IpAddress(ip[0] & 0xFF, ip[1] & 0xFF, ip[2] & 0xFF, ip[3] & 0xFF);
    }

    @Override
    public String toString() {
        return oct1 + "." + oct2 + "." + oct3 + "." + oct4;
    }

    @Override
    public int compareTo(IpAddress o) {
        return Long.compare(toLong(), o.toLong());
    }
}
