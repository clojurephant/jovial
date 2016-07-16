package org.ajoberstar.jovial.launcher.gradle;

import org.gradle.jvm.test.JvmTestSuiteSpec;
import org.gradle.model.Managed;

public interface JovialTestSuiteSpec extends JvmTestSuiteSpec {
    String getJovialVersion();

    void setJovialVersion(String version);

    String getjUnitVersion();

    void setjUnitVersion(String version);
}
