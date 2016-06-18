package org.ajoberstar.jupiter.launcher;

import org.junit.gen5.engine.discovery.ClasspathSelector;
import org.junit.gen5.launcher.Launcher;
import org.junit.gen5.launcher.TestDiscoveryRequest;
import org.junit.gen5.launcher.listeners.LoggingListener;
import org.junit.gen5.launcher.listeners.SummaryGeneratingListener;
import org.junit.gen5.launcher.main.LauncherFactory;
import org.junit.gen5.launcher.main.TestDiscoveryRequestBuilder;

import java.io.File;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

public class ExecutionPrinter {
    public static void main(String[] args) {
        Launcher launcher = LauncherFactory.create();

        Set<File> dirs = Arrays.stream(args)
            .map(File::new)
            .collect(Collectors.toSet());

        TestDiscoveryRequest discoveryRequest = TestDiscoveryRequestBuilder.request()
            .selectors(ClasspathSelector.selectClasspathRoots(dirs))
            .build();

        SummaryGeneratingListener listener = new SummaryGeneratingListener();
        launcher.registerTestExecutionListeners(listener);
        launcher.registerTestExecutionListeners(new LoggingListener((e, msg) -> {
            System.out.println(msg.get());
            if (e != null) {
                e.printStackTrace();
            }
        }));

        launcher.execute(discoveryRequest);

        try (PrintWriter writer = new PrintWriter(System.out)) {
            listener.getSummary().printTo(writer);
            listener.getSummary().printFailuresTo(writer);
        }
    }
}
