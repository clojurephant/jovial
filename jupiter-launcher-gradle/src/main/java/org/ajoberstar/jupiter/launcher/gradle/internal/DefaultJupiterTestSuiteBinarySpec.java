package org.ajoberstar.jupiter.launcher.gradle.internal;

import org.ajoberstar.jupiter.launcher.gradle.JupiterTestSuiteBinarySpec;
import org.ajoberstar.jupiter.launcher.gradle.JupiterTestSuiteSpec;
import org.gradle.api.tasks.testing.Test;
import org.gradle.jvm.JvmBinarySpec;
import org.gradle.jvm.internal.DefaultJvmBinarySpec;
import org.gradle.jvm.internal.DependencyResolvingClasspath;
import org.gradle.jvm.internal.WithDependencies;
import org.gradle.jvm.internal.WithJvmAssembly;
import org.gradle.jvm.test.JvmTestSuiteBinarySpec;
import org.gradle.jvm.test.internal.JvmTestSuiteBinarySpecInternal;
import org.gradle.platform.base.BinaryTasksCollection;
import org.gradle.platform.base.DependencySpec;
import org.gradle.platform.base.internal.BinaryTasksCollectionWrapper;

import java.util.Collection;
import java.util.LinkedList;

public class DefaultJupiterTestSuiteBinarySpec extends DefaultJvmBinarySpec implements JupiterTestSuiteBinarySpec, JvmTestSuiteBinarySpecInternal, WithJvmAssembly, WithDependencies {
    private String jupiterVersion;
    private String jUnitVersion;
    private Collection<DependencySpec> binaryLevelDependencies = new LinkedList<>();
    private JvmBinarySpec testedBinary;
    private final DefaultTasksCollection tasks = new DefaultTasksCollection(super.getTasks());
    private DependencyResolvingClasspath runtimeClasspath;

    @Override
    public String getJupiterVersion() {
        return jupiterVersion;
    }

    @Override
    public void setJupiterVersion(String version) {
        this.jupiterVersion = version;
    }

    @Override
    public String getjUnitVersion() {
        return jUnitVersion;
    }

    @Override
    public void setjUnitVersion(String version) {
        this.jUnitVersion = version;
    }

    @Override
    public JupiterTestSuiteSpec getTestSuite() {
        return getComponentAs(JupiterTestSuiteSpec.class);
    }

    @Override
    public JvmBinarySpec getTestedBinary() {
        return testedBinary;
    }

    @Override
    public void setTestedBinary(JvmBinarySpec testedBinary) {
        this.testedBinary = testedBinary;
    }

    @Override
    public DependencyResolvingClasspath getRuntimeClasspath() {
        return runtimeClasspath;
    }

    @Override
    public void setRuntimeClasspath(DependencyResolvingClasspath runtimeClasspath) {
        this.runtimeClasspath = runtimeClasspath;
    }

    @Override
    public void setDependencies(Collection<DependencySpec> binaryLevelDependencies) {
        this.binaryLevelDependencies = binaryLevelDependencies;
    }

    @Override
    public Collection<DependencySpec> getDependencies() {
        return binaryLevelDependencies;
    }

    @Override
    public JvmTestSuiteBinarySpec.JvmTestSuiteTasks getTasks() {
        return tasks;
    }

    private static class DefaultTasksCollection extends BinaryTasksCollectionWrapper implements JvmTestSuiteBinarySpec.JvmTestSuiteTasks {
        public DefaultTasksCollection(BinaryTasksCollection delegate) {
            super(delegate);
        }

        @Override
        public Test getRun() {
            return findSingleTaskWithType(Test.class);
        }
    }
}
