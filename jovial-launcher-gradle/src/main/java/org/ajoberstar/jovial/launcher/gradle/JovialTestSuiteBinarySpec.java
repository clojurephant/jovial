package org.ajoberstar.jovial.launcher.gradle;

import org.gradle.jvm.test.JvmTestSuiteBinarySpec;

public interface JovialTestSuiteBinarySpec extends JvmTestSuiteBinarySpec {
    String getJovialVersion();

    void setJovialVersion(String version);

    String getjUnitVersion();

    void setjUnitVersion(String version);

    @Override
    JovialTestSuiteSpec getTestSuite();
}
