package org.ajoberstar.jupiter.launcher.gradle.plugins;

import org.ajoberstar.jupiter.launcher.gradle.JupiterTestSuiteBinarySpec;
import org.ajoberstar.jupiter.launcher.gradle.JupiterTestSuiteSpec;
import org.ajoberstar.jupiter.launcher.gradle.internal.DefaultJupiterTestSuiteBinarySpec;
import org.ajoberstar.jupiter.launcher.gradle.internal.DefaultJupiterTestSuiteSpec;
import org.ajoberstar.jupiter.launcher.gradle.internal.JupiterTestExecuter;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.internal.tasks.testing.detection.TestExecuter;
import org.gradle.api.tasks.testing.Test;
import org.gradle.jvm.plugins.JvmComponentPlugin;
import org.gradle.jvm.plugins.JvmTestSuiteBasePlugin;
import org.gradle.jvm.test.internal.JvmTestSuiteBinarySpecInternal;
import org.gradle.jvm.test.internal.JvmTestSuiteRules;
import org.gradle.jvm.toolchain.JavaToolChainRegistry;
import org.gradle.model.Defaults;
import org.gradle.model.Each;
import org.gradle.model.ModelMap;
import org.gradle.model.Mutate;
import org.gradle.model.RuleSource;
import org.gradle.model.Validate;
import org.gradle.platform.base.BinarySpec;
import org.gradle.platform.base.ComponentBinaries;
import org.gradle.platform.base.ComponentType;
import org.gradle.platform.base.InvalidModelException;
import org.gradle.platform.base.TypeBuilder;
import org.gradle.platform.base.internal.DefaultModuleDependencySpec;
import org.gradle.platform.base.internal.HasIntermediateOutputsComponentSpec;
import org.gradle.platform.base.internal.PlatformResolvers;
import org.gradle.testing.base.plugins.TestingModelBasePlugin;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class JupiterPlugin implements Plugin<Project> {
    @Override
    public void apply(Project project) {
        project.getPluginManager().apply(TestingModelBasePlugin.class);
        project.getPluginManager().apply(JvmComponentPlugin.class);
        project.getPluginManager().apply(JvmTestSuiteBasePlugin.class);
    }

    @SuppressWarnings("UnusedDeclaration")
    public static class PluginRules extends RuleSource {
        @ComponentType
        public void registerComponent(TypeBuilder<JupiterTestSuiteSpec> builder) {
            builder.defaultImplementation(DefaultJupiterTestSuiteSpec.class);
            builder.internalView(HasIntermediateOutputsComponentSpec.class);
        }

        @ComponentType
        public void registerBinary(TypeBuilder<JupiterTestSuiteBinarySpec>  builder) {
            builder.defaultImplementation(DefaultJupiterTestSuiteBinarySpec.class);
            builder.internalView(JvmTestSuiteBinarySpecInternal.class);
        }

        @ComponentBinaries
        public void createBinaries(ModelMap<BinarySpec> binaries, JupiterTestSuiteSpec testSuite, PlatformResolvers platforms, JavaToolChainRegistry toolChains) {
            JvmTestSuiteRules.createJvmTestSuiteBinaries(binaries, platforms, testSuite, toolChains, JupiterTestSuiteBinarySpec.class);
        }

        @Mutate
        public void useJupiterExecuter(@Each Test task) {
            try {
                Method setter = Test.class.getDeclaredMethod("setTestExecuter", TestExecuter.class);
                setter.setAccessible(true);
                setter.invoke(task, new JupiterTestExecuter());
            } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException e) {
                throw new AssertionError("This setTestExecuter should exist.", e);
            }
        }

        @Defaults
        public void defaultBinaryVersions(@Each JupiterTestSuiteBinarySpec binary) {
            JupiterTestSuiteSpec testSuite = binary.getTestSuite();
            binary.setJupiterVersion(testSuite.getJupiterVersion());
            binary.setjUnitVersion(testSuite.getjUnitVersion());
            binary.getDependencies().add(new DefaultModuleDependencySpec("org.junit", "junit-console", testSuite.getjUnitVersion()));
            // TODO add dependencies to the binary in a finalize
        }

        @Validate
        public void validateJupiterVersion(@Each JupiterTestSuiteSpec testSuite) {
            if (testSuite.getJupiterVersion() == null) {
                throw new InvalidModelException(String.format("Test suite '%s' doesn't declare Jupiter version. Please specify it with jupiterVersion '0.2.0' for example.", testSuite.getName()));
            }
        }

        @Validate
        public void validateJUnitVersion(@Each JupiterTestSuiteSpec testSuite) {
            if (testSuite.getjUnitVersion() == null) {
                throw new InvalidModelException(String.format("Test suite '%s' doesn't declare JUnit version. Please specify it with jUnitVersion '0.2.0' for example.", testSuite.getName()));
            }
        }
    }
}
