package org.ajoberstar.jupiter.launcher.gradle;

import org.gradle.jvm.test.JvmTestSuiteBinarySpec;

public interface JupiterTestSuiteBinarySpec extends JvmTestSuiteBinarySpec {
    String getJupiterVersion();

    void setJupiterVersion(String version);

    String getjUnitVersion();

    void setjUnitVersion(String version);

    @Override
    JupiterTestSuiteSpec getTestSuite();
}
