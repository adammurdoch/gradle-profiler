package org.gradle.profiler;

import java.io.File;
import java.io.PrintStream;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

public class GradleScenarioDefinition extends ScenarioDefinition {

    private final Invoker invoker;
    private final GradleBuildConfiguration buildConfiguration;
    private final BuildAction buildAction;
    private final BuildAction cleanupAction;
    private final List<String> gradleArgs;
    private final Map<String, String> systemProperties;

    public GradleScenarioDefinition(String name, Invoker invoker, GradleBuildConfiguration buildConfiguration, BuildAction buildAction,  BuildAction cleanupAction, List<String> gradleArgs, Map<String, String> systemProperties, Supplier<BuildMutator> buildMutator, int warmUpCount, int buildCount, File outputDir) {
        super(name, buildMutator, warmUpCount, buildCount, outputDir);
        this.invoker = invoker;
        this.buildAction = buildAction;
        this.buildConfiguration = buildConfiguration;
        this.cleanupAction = cleanupAction;
        this.gradleArgs = gradleArgs;
        this.systemProperties = systemProperties;
    }

    @Override
    public String getDisplayName() {
        return getName() + " using " + buildConfiguration.getGradleVersion();
    }

    @Override
    public String getProfileName() {
        return getName() + "-" + buildConfiguration.getGradleVersion().getVersion();
    }

    @Override
    public String getBuildToolDisplayName() {
        return buildConfiguration.getGradleVersion().getVersion();
    }

    @Override
    public String getTasksDisplayName() {
        return buildAction.getShortDisplayName();
    }

    public List<String> getGradleArgs() {
        return gradleArgs;
    }

    public Invoker getInvoker() {
        return invoker;
    }

    public BuildAction getAction() {
        return buildAction;
    }

    public BuildAction getCleanupAction() {
        return cleanupAction;
    }

    public GradleBuildConfiguration getBuildConfiguration() {
        return buildConfiguration;
    }

    public Map<String, String> getSystemProperties() {
        return systemProperties;
    }

    @Override
    protected void printDetail(PrintStream out) {
        out.println("  " + getBuildConfiguration().getGradleVersion() + " (" + getBuildConfiguration().getGradleHome() + ")");
        out.println("  Run using: " + getInvoker());
        out.println("  Run: " + getAction().getDisplayName());
        out.println("  Cleanup: " + getCleanupAction().getDisplayName());
        out.println("  Gradle args: " + getGradleArgs());
        if (!getSystemProperties().isEmpty()) {
            out.println("  System properties:");
            for (Map.Entry<String, String> entry : getSystemProperties().entrySet()) {
                out.println("    " + entry.getKey() + "=" + entry.getValue());
            }
        }
    }
}
