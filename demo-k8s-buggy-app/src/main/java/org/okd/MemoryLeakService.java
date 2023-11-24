package org.okd;
import java.util.ArrayList;
import java.util.List;
import java.sql.Timestamp;

import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class MemoryLeakService {

    private static final List<byte[]> memoryLeakList = new ArrayList<>();
    public static String ts() {
        return "Timestamp: " + new Timestamp(new java.util.Date().getTime());
    }
    public void simulateMemoryLeak() {
        while (true) {
            memoryLeakList.add(new byte[1024 * 1024]); // 1 MB byte array
            System.out.println("Allocationg 1MB from memory at " + ts());
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}