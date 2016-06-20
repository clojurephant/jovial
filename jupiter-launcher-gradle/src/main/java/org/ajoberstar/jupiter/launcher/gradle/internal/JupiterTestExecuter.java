package org.ajoberstar.jupiter.launcher.gradle.internal;

import org.ajoberstar.jupiter.launcher.gradle.plugins.JupiterPlugin;
import org.gradle.api.UncheckedIOException;
import org.gradle.api.internal.tasks.testing.DefaultTestOutputEvent;
import org.gradle.api.internal.tasks.testing.TestCompleteEvent;
import org.gradle.api.internal.tasks.testing.TestDescriptorInternal;
import org.gradle.api.internal.tasks.testing.TestResultProcessor;
import org.gradle.api.internal.tasks.testing.TestStartEvent;
import org.gradle.api.internal.tasks.testing.detection.TestExecuter;
import org.gradle.api.tasks.testing.Test;
import org.gradle.api.tasks.testing.TestOutputEvent;
import org.gradle.api.tasks.testing.TestResult;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class JupiterTestExecuter implements TestExecuter {
    @Override
    public void execute(Test test, TestResultProcessor testResultProcessor) {
        

        PipedOutputStream os = new PipedOutputStream();

        Thread handler = new JupiterOutputHandler(os, testResultProcessor);
        handler.start();

        test.getProject().javaexec(spec -> {
            test.copyTo(spec);
            spec.setClasspath(test.getClasspath());
            spec.setMain("org.junit.gen5.console.ConsoleRunner");
            spec.args("--all", "--disable-ansi-colors");
            spec.setStandardOutput(os);
            spec.setErrorOutput(os);
        });

        try {
            handler.join();
        } catch (InterruptedException e) {
            // continue
        }
    }

    private static class JupiterOutputHandler extends Thread {
        private final PipedOutputStream os;
        private final TestResultProcessor testResultProcessor;

        public JupiterOutputHandler(PipedOutputStream os, TestResultProcessor testResultProcessor) {
            this.os = os;
            this.testResultProcessor = testResultProcessor;
        }

        public void run() {
            try (
                PipedInputStream is = new PipedInputStream(os);
                InputStreamReader isr = new InputStreamReader(is);
                BufferedReader reader = new BufferedReader(isr)
            ) {
                JupiterTestDescriptor currentTest = null;
                boolean testComplete = false;
                List<String> failureOutput = null;
                for (String line = ""; line != null; line = reader.readLine()) {
                    System.out.println(line);
                    if (line.startsWith("Started:")) {
                        if (currentTest != null) {
                            // send off the finish from the previous test
                            String failMessage = String.join(System.lineSeparator(), failureOutput);
                            if (failMessage.isEmpty()) {
                                testResultProcessor.completed(currentTest.getId(), new TestCompleteEvent(Instant.now().toEpochMilli(), TestResult.ResultType.SUCCESS));
                            } else {
                                testResultProcessor.failure(currentTest.getId(), new AssertionError(failMessage));
                            }
                        }

                        // set up next test
                        currentTest = JupiterTestDescriptor.fromUniqueId(line);
                        testComplete = false;
                        testResultProcessor.started(currentTest, new TestStartEvent(Instant.now().toEpochMilli(), null));
                    } else if (line.startsWith("Finished:")) {
                        testComplete = true;
                        failureOutput = new ArrayList<>();
                    } else if (testComplete) {
                        failureOutput.add(line);
                    } else if (currentTest != null) {
                        testResultProcessor.output(currentTest.getId(), new DefaultTestOutputEvent(TestOutputEvent.Destination.StdOut, line));
                    }
                }
            } catch (IOException e) {
                throw new UncheckedIOException("Problem reading Jupiter output", e);
            }
        }
    }

    private static class JupiterTestDescriptor implements TestDescriptorInternal {
        private static final Pattern ID_COMPONENT_PATTERN = Pattern.compile("(?:(\\[.+\\])/)?\\[.+?:(.+?)\\]");
        private final String id;
        private final JupiterTestDescriptor parent;
        private final String name;
        private final boolean composite;

        private JupiterTestDescriptor(String id, JupiterTestDescriptor parent, String name, boolean composite) {
            this.id = id;
            this.parent = parent;
            this.name = name;
            this.composite = composite;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public String getClassName() {
            return null;
        }

        @Override
        public boolean isComposite() {
            return composite;
        }

        @Override
        public TestDescriptorInternal getParent() {
            return parent;
        }

        @Override
        public Object getId() {
            return id;
        }

        @Override
        public Object getOwnerBuildOperationId() {
            return null;
        }

        public static JupiterTestDescriptor fromUniqueId(String uniqueId) {
            return fromUniqueId(uniqueId, false);
        }

        private static JupiterTestDescriptor fromUniqueId(String uniqueId, boolean composite) {
            if (uniqueId == null) {
                return null;
            } else {
                Matcher matcher = ID_COMPONENT_PATTERN.matcher(uniqueId);
                if (matcher.find()) {
                    String id = matcher.group(0);
                    JupiterTestDescriptor parent = fromUniqueId(matcher.group(1), true);
                    String name = matcher.group(2);
                    return new JupiterTestDescriptor(id, parent, name, composite);
                } else {
                    return null;
                }
            }
        }
    }
}
