package org.ajoberstar.jovial.launcher.gradle.internal;

import org.gradle.api.UncheckedIOException;
import org.gradle.api.file.FileCollection;
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
import java.io.EOFException;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamClass;
import java.io.Reader;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.channels.Channels;
import java.nio.channels.Pipe;
import java.nio.channels.SocketChannel;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class JovialTestExecuter implements TestExecuter {
    @Override
    public void execute(Test test, TestResultProcessor testResultProcessor) {
        Map<String, List<String>> config = new HashMap<>();
        List<String> classpathRoots = test.getClasspath().getFiles().stream()
            .filter(File::isDirectory)
            .map(File::getAbsolutePath)
            .collect(Collectors.toList());
        config.put("classpathRoots", classpathRoots);
        InputStream is = createInput(config);

        Pipe pipe;
        try {
            pipe = Pipe.open();
        } catch (IOException e) {
            throw new UncheckedIOException("Could not open pipe.", e);
        }
        OutputStream os = Channels.newOutputStream(pipe.sink());

        Thread handler = new JovialOutputHandler(test.getClasspath(), pipe, testResultProcessor);
        handler.start();

        test.getProject().javaexec(spec -> {
            test.copyTo(spec);
            spec.setClasspath(test.getClasspath());
            spec.setMain("org.ajoberstar.jovial.launcher.socket.JovialSocketLauncher");
            spec.setStandardInput(is);
            spec.setStandardOutput(os);
        });

        try {
            handler.join();
        } catch (InterruptedException e) {
            // continue
        }
    }

    private InputStream createInput(Map<String, List<String>> config) {
        try {
            Pipe pipe = Pipe.open();
            OutputStream rawOutput = Channels.newOutputStream(pipe.sink());
            ObjectOutputStream output = new ObjectOutputStream(rawOutput);
            output.writeObject(config);
            return Channels.newInputStream(pipe.source());
        } catch (IOException e) {
            throw new UncheckedIOException("Could not open pipe.", e);
        }
    }

    private static class JovialOutputHandler extends Thread {
        private final FileCollection classpath;
        private final Pipe pipe;
        private final TestResultProcessor testResultProcessor;
        private final Map<String, JovialTestDescriptor> descriptors = new HashMap<>();

        public JovialOutputHandler(FileCollection classpath, Pipe pipe, TestResultProcessor testResultProcessor) {
            this.classpath = classpath;
            this.pipe = pipe;
            this.testResultProcessor = testResultProcessor;
        }

        @Override
        public void run() {
            ClassLoader oldClassLoader = this.getContextClassLoader();
            try {
                URL[] urls = classpath.getFiles().stream()
                    .map(File::toURI)
                    .map(uri -> {
                        try {
                            return uri.toURL();
                        } catch (MalformedURLException e) {
                            throw new RuntimeException("Bad uri: " + uri, e);
                        }
                    }).toArray(size -> new URL[size]);
                ClassLoader classLoader = new URLClassLoader(urls);
                this.setContextClassLoader(classLoader);

                InputStream portInput = Channels.newInputStream(pipe.source());
                Scanner scanner = new Scanner(portInput);
                int port = scanner.nextInt();

                InetSocketAddress addr = new InetSocketAddress(port);
                SocketChannel channel = SocketChannel.open(addr);
                InputStream rawInputStream = Channels.newInputStream(channel);
                ObjectInputStream inputStream = new ContextObjectInputStream(rawInputStream);

                Map<String, Object> event = (Map<String, Object>) inputStream.readObject();
                while (event != null) {
                    handleEvent(event);
                    event = (Map<String, Object>) inputStream.readObject();
                }
            } catch (EOFException ignored) {
                // we're done
            } catch (IOException e) {
                throw new UncheckedIOException("Problem reading Jovial output", e);
            } catch (ClassNotFoundException e) {
                throw new RuntimeException("Could not find class." ,e);
            } finally {
                this.setContextClassLoader(oldClassLoader);
            }
        }

        private void handleEvent(Map<String, Object> event) {
            // Gradle seems to only allow two tiers of tests, so drop the roots
            if (event.get("parentId") == null) {
                return;
            }

            JovialTestDescriptor descriptor = descriptors.computeIfAbsent((String) event.get("uniqueId"), uniqueId -> {
                boolean container = (boolean) event.get("container");
                JovialTestDescriptor parent = container ? null : descriptors.get((String) event.get("parentId"));
                return new JovialTestDescriptor(uniqueId, parent, (String) event.get("displayName"), container);
            });

            switch ((String) event.get("type")) {
                case "dynamicTestRegistered":
                    // do nothing, for now
                    break;
                case "executionSkipped":
                    testResultProcessor.completed(descriptor.getId(), new TestCompleteEvent(Instant.now().toEpochMilli(), TestResult.ResultType.SKIPPED));
                    break;
                case "executionStarted":
                    testResultProcessor.started(descriptor, new TestStartEvent(Instant.now().toEpochMilli(), null));
                    break;
                case "executionFinished":
                    boolean success = (boolean) event.get("success");
                    TestResult.ResultType result = success ? TestResult.ResultType.SUCCESS : TestResult.ResultType.FAILURE;
                    Throwable cause = (Throwable) event.get("throwable");
                    if (cause != null) {
                        testResultProcessor.failure(descriptor.getId(), cause);
                    }
                    testResultProcessor.completed(descriptor.getId(), new TestCompleteEvent(Instant.now().toEpochMilli(), result));
                    break;
            }
        }
    }

    private static class ContextObjectInputStream extends ObjectInputStream {
        public ContextObjectInputStream(InputStream is) throws IOException {
            super(is);
        }

        @Override
        public Class resolveClass(ObjectStreamClass desc) throws IOException, ClassNotFoundException {
            try {
                return Thread.currentThread().getContextClassLoader().loadClass(desc.getName());
            } catch (ClassNotFoundException e) {
                return super.resolveClass(desc);
            }
        }
    }

    private static class JovialTestDescriptor implements TestDescriptorInternal {
        private final Object id;
        private final JovialTestDescriptor parent;
        private final String name;
        private final boolean composite;

        public JovialTestDescriptor(Object id, JovialTestDescriptor parent, String name, boolean composite) {
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

        @Override
        public String toString() {
            return id.toString();
        }

        @Override
        public boolean equals(Object other) {
            if (other instanceof JovialTestDescriptor) {
                JovialTestDescriptor that = (JovialTestDescriptor) other;
                return this.id.equals(that.id);
            } else {
                return false;
            }
        }

        @Override
        public int hashCode() {
            return id.hashCode();
        }
    }
}
