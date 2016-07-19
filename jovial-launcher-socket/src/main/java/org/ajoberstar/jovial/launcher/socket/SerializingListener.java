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
        writeData(testIdentifier, data);
    }

    @Override
    public void executionSkipped(TestIdentifier testIdentifier, String reason) {
        Map<String, Object> data = new HashMap<>();
        data.put("type", "executionSkipped");
        data.put("reason", reason);
        writeData(testIdentifier, data);
    }

    @Override
    public void executionStarted(TestIdentifier testIdentifier) {
        Map<String, Object> data = new HashMap<>();
        data.put("type", "executionStarted");
        writeData(testIdentifier, data);
    }

    @Override
    public void executionFinished(TestIdentifier testIdentifier, TestExecutionResult testExecutionResult) {
        Map<String, Object> data = new HashMap<>();
        data.put("type", "executionFinished");
        data.put("success", testExecutionResult.getStatus() == TestExecutionResult.Status.SUCCESSFUL);
        data.put("throwable", testExecutionResult.getThrowable().orElse(null));
        writeData(testIdentifier, data);
    }

    private void writeData(TestIdentifier id, Map<String, Object> data) {
        try {
            data.put("uniqueId", id.getUniqueId());
            data.put("parentId", id.getParentId().orElse(null));
            data.put("displayName", id.getDisplayName());
            data.put("container", id.isContainer());
            stream.writeObject(data);
        } catch (IOException e) {
            System.err.println("Could not write event: " + data);
        }
    }
}
