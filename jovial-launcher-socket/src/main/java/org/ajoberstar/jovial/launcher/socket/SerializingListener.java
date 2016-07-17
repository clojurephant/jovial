package org.ajoberstar.jovial.launcher.socket;

import org.junit.platform.engine.TestExecutionResult;
import org.junit.platform.launcher.TestExecutionListener;
import org.junit.platform.launcher.TestIdentifier;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.HashMap;
import java.util.Map;

public class SerializingListener implements TestExecutionListener {
    private final ObjectOutputStream stream;

    public SerializingListener(ObjectOutputStream stream) {
        this.stream = stream;
    }

    @Override
    public void dynamicTestRegistered(TestIdentifier testIdentifier) {
        Map<String, Object> data = new HashMap<>();
        data.put("type", "dynamicTestRegistered");
        data.put("id", testIdentifier);
        writeData(data);
    }

    @Override
    public void executionSkipped(TestIdentifier testIdentifier, String reason) {
        Map<String, Object> data = new HashMap<>();
        data.put("type", "executionSkipped");
        data.put("id", testIdentifier);
        data.put("reason", reason);
        writeData(data);
    }

    @Override
    public void executionStarted(TestIdentifier testIdentifier) {
        Map<String, Object> data = new HashMap<>();
        data.put("type", "executionStarted");
        data.put("id", testIdentifier);
        writeData(data);
    }

    @Override
    public void executionFinished(TestIdentifier testIdentifier, TestExecutionResult testExecutionResult) {
        Map<String, Object> data = new HashMap<>();
        data.put("type", "executionFinished");
        data.put("id", testIdentifier);
        data.put("status", testExecutionResult.getStatus());
        data.put("throwable", testExecutionResult.getThrowable().orElse(null));
        writeData(data);
    }

    private void writeData(Map<String, Object> data) {
        try {
            stream.writeObject(data);
        } catch (IOException e) {
            System.err.println("Could not write event: " + data);
        }
    }
}
