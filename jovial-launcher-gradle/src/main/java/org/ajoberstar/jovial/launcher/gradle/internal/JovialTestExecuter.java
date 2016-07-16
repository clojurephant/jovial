package org.ajoberstar.jovial.launcher.gradle.internal;

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
import java.io.OutputStream;
import java.io.Reader;
import java.nio.channels.Channels;
import java.nio.channels.Pipe;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class JovialTestExecuter implements TestExecuter {
    @Override
    public void execute(Test test, TestResultProcessor testResultProcessor) {
        Pipe pipe;
        try {
            pipe = Pipe.open();
        } catch (IOException e) {
            throw new UncheckedIOException("Could not open pipe.", e);
        }
        OutputStream os = Channels.newOutputStream(pipe.sink());

        Thread handler = new JovialOutputHandler(pipe, testResultProcessor);
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

    private static class JovialOutputHandler extends Thread {
        private final Pipe pipe;
        private final TestResultProcessor testResultProcessor;

        public JovialOutputHandler(Pipe pipe, TestResultProcessor testResultProcessor) {
            this.pipe = pipe;
            this.testResultProcessor = testResultProcessor;
        }

        public void run() {

            try {
                Reader channelReader = Channels.newReader(pipe.source(), "UTF-8");
                BufferedReader reader = new BufferedReader(channelReader);

                JovialTestDescriptor currentTest = null;
                for (String line = ""; line != null; line = reader.readLine()) {
                    System.out.println(line);
                    JovialTestDescriptor maybeTest = JovialTestDescriptor.fromUniqueId(line);
                    System.out.println("Processing line for: " + (maybeTest != null ? maybeTest.getId() : null));
                    if (line.startsWith("Started:")) {
                        if (maybeTest.getParent() != null && maybeTest.getParent().getParent() != null) {
                            if (currentTest == null || !maybeTest.getParent().getId().equals(currentTest.getParent().getId())) {
                                if (currentTest != null) {
                                    System.out.println("Jovial Completed Suite: " + currentTest.getParent().getId());
                                    testResultProcessor.completed(currentTest.getParent().getId(), new TestCompleteEvent(Instant.now().toEpochMilli()));
                                }
                                System.out.println("Jovial Starting Suite: " + maybeTest.getParent().getId());
                                testResultProcessor.started(maybeTest.getParent(), new TestStartEvent(Instant.now().toEpochMilli()));
                            }
                            currentTest = maybeTest;
                            System.out.println("Jovial Starting Test: " + currentTest.getId());
                            testResultProcessor.started(currentTest, new TestStartEvent(Instant.now().toEpochMilli(), null));
                        }
                    } else if (line.startsWith("Finished:") && maybeTest.getId().equals(currentTest.getId())) {
                        System.out.println("Jovial Completed Test: " + currentTest.getId());
                        testResultProcessor.completed(currentTest.getId(), new TestCompleteEvent(Instant.now().toEpochMilli()));
                    }
                }
                System.out.println("Jovial Completed Suite: " + currentTest.getParent().getId());
                testResultProcessor.completed(currentTest.getParent().getId(), new TestCompleteEvent(Instant.now().toEpochMilli()));
            } catch (IOException e) {
                throw new UncheckedIOException("Problem reading Jovial output", e);
            }
        }
    }

    private static class JovialTestDescriptor implements TestDescriptorInternal {
        private static final Pattern ID_COMPONENT_PATTERN = Pattern.compile("(?:(\\[.+\\])/)?\\[.+?:(.+?)\\]");
        private final String id;
        private final JovialTestDescriptor parent;
        private final String name;
        private final boolean composite;

        private JovialTestDescriptor(String id, JovialTestDescriptor parent, String name, boolean composite) {
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
            return isComposite() || parent == null ? this.getName() : parent.getName();
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

        public static JovialTestDescriptor fromUniqueId(String uniqueId) {
            return fromUniqueId(uniqueId, false);
        }

        private static JovialTestDescriptor fromUniqueId(String uniqueId, boolean composite) {
            if (uniqueId == null) {
                return null;
            } else {
                Matcher matcher = ID_COMPONENT_PATTERN.matcher(uniqueId);
                if (matcher.find()) {
                    String id = matcher.group(0);
                    JovialTestDescriptor parent = fromUniqueId(matcher.group(1), true);
                    String name = matcher.group(2);
                    return new JovialTestDescriptor(id, parent, name, composite);
                } else {
                    return null;
                }
            }
        }
    }
}
