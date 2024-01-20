package org.richardcarter;

import lombok.Getter;
import lombok.SneakyThrows;

import java.io.*;

public class StorageWriter implements AutoCloseable {

    private final BufferedOutputStream output;
    @Getter
    private long fileSize;

    @SneakyThrows
    public StorageWriter(String filename) {
        output = new BufferedOutputStream(new FileOutputStream(filename));
        fileSize = 0;
    }

    @Override
    public void close() throws Exception {
        output.close();
    }

    @SneakyThrows
    public void write(IpAddress ip, byte[] hash) {
        byte[] ipBytes = ip.toBytes();
        output.write(ipBytes);
        output.write(hash);
        fileSize += ipBytes.length + hash.length;
    }
}
