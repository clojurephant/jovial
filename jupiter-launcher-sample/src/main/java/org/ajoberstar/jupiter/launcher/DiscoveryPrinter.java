package org.ajoberstar.jupiter.launcher;

import org.junit.gen5.engine.discovery.ClasspathSelector;
import org.junit.gen5.launcher.Launcher;
import org.junit.gen5.launcher.TestDiscoveryRequest;
import org.junit.gen5.launcher.TestIdentifier;
import org.junit.gen5.launcher.TestPlan;
import org.junit.gen5.launcher.main.LauncherFactory;
import org.junit.gen5.launcher.main.TestDiscoveryRequestBuilder;

import java.io.File;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class DiscoveryPrinter {

    public static void main(String[] args) {
        Launcher launcher = LauncherFactory.create();

        Set<File> dirs = Arrays.stream(args)
            .map(File::new)
            .collect(Collectors.toSet());

        TestDiscoveryRequest discoveryRequest = TestDiscoveryRequestBuilder.request()
            .selectors(ClasspathSelector.selectClasspathRoots(dirs))
            .build();

        TestPlan plan = launcher.discover(discoveryRequest);
        plan.getRoots().forEach(root -> printDescriptor(plan, root, 0));
    }

    private static void printDescriptor(TestPlan plan, TestIdentifier id, int level) {
        IntStream.range(0, level).forEach(i -> System.out.print("\t"));
        System.out.println(id.getDisplayName());
        plan.getChildren(id).forEach(child -> printDescriptor(plan, child, level + 1));
    }
}
