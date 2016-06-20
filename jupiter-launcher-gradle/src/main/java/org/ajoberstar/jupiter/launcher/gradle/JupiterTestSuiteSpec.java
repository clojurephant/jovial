package org.ajoberstar.jupiter.launcher.gradle;

import org.gradle.jvm.test.JvmTestSuiteSpec;
import org.gradle.model.Managed;

public interface JupiterTestSuiteSpec extends JvmTestSuiteSpec {
    String getJupiterVersion();

    void setJupiterVersion(String version);

    String getjUnitVersion();

    void setjUnitVersion(String version);
}
