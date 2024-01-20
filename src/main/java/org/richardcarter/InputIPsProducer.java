package org.richardcarter;

import lombok.RequiredArgsConstructor;

import java.util.concurrent.BlockingQueue;

@RequiredArgsConstructor
public class InputIPsProducer implements Runnable {

    private final IpAddress startIp;
    private final IpAddress maxIp;
    private final BlockingQueue<IpAddress> ipQueue;

    @Override
    public void run() {
        IpAddress ip = startIp;
        while (ip != maxIp) {
            try {
                ipQueue.put(ip);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
            ip = ip.increment();
        }
    }
}
