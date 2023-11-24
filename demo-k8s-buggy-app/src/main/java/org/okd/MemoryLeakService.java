package org.okd;
import java.util.ArrayList;
import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class MemoryLeakService {

    private static final List<byte[]> memoryLeakList = new ArrayList<>();

    public void simulateMemoryLeak() {
        while (true) {
            memoryLeakList.add(new byte[1024 * 1024]); // 1 MB byte array
            System.out.println("Allocationg 1MB from memory");
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}