package org.okd;

import jakarta.inject.Inject;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

@Path("/memory-leak")
public class MemoryLeakResource {

    @Inject
    MemoryLeakService memoryLeakService;

    @GET
    public String simulateMemoryLeak() {
        memoryLeakService.simulateMemoryLeak();
        return "Memory leak simulation started!";
    }
}