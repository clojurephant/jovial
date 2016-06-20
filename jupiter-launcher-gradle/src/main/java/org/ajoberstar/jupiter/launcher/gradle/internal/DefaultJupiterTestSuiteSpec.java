package org.ajoberstar.jupiter.launcher.gradle.internal;

import org.ajoberstar.jupiter.launcher.gradle.JupiterTestSuiteSpec;
import org.gradle.jvm.JvmComponentSpec;
import org.gradle.jvm.internal.DefaultJvmLibrarySpec;
import org.gradle.platform.base.DependencySpecContainer;
import org.gradle.platform.base.TransformationFileType;
import org.gradle.platform.base.internal.DefaultDependencySpecContainer;
import org.gradle.platform.base.internal.HasIntermediateOutputsComponentSpec;
import org.gradle.testing.base.internal.BaseTestSuiteSpec;

import java.util.Set;

public class DefaultJupiterTestSuiteSpec extends BaseTestSuiteSpec implements JupiterTestSuiteSpec, HasIntermediateOutputsComponentSpec {
    private String jupiterVersion;
    private String jUnitVersion;
    private final DependencySpecContainer dependencies = new DefaultDependencySpecContainer();

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
    public JvmComponentSpec getTestedComponent() {
        return (JvmComponentSpec) super.getTestedComponent();
    }

    @Override
    public DependencySpecContainer getDependencies() {
        return dependencies;
    }

    @Override
    public Set<? extends Class<? extends TransformationFileType>> getIntermediateTypes() {
        return DefaultJvmLibrarySpec.defaultJvmComponentInputTypes();
    }
}
