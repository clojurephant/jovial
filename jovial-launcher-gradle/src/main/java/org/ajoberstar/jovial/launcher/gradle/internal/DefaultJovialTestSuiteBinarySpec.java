package org.ajoberstar.jovial.launcher.gradle.internal;

import org.ajoberstar.jovial.launcher.gradle.JovialTestSuiteBinarySpec;
import org.ajoberstar.jovial.launcher.gradle.JovialTestSuiteSpec;
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

public class DefaultJovialTestSuiteBinarySpec extends DefaultJvmBinarySpec implements JovialTestSuiteBinarySpec, JvmTestSuiteBinarySpecInternal, WithJvmAssembly, WithDependencies {
    private String jovialVersion;
    private String jUnitVersion;
    private Collection<DependencySpec> binaryLevelDependencies = new LinkedList<>();
    private JvmBinarySpec testedBinary;
    private final DefaultTasksCollection tasks = new DefaultTasksCollection(super.getTasks());
    private DependencyResolvingClasspath runtimeClasspath;

    @Override
    public String getJovialVersion() {
        return jovialVersion;
    }

    @Override
    public void setJovialVersion(String version) {
        this.jovialVersion = version;
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
    public JovialTestSuiteSpec getTestSuite() {
        return getComponentAs(JovialTestSuiteSpec.class);
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
