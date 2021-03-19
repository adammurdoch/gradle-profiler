package org.gradle.profiler.asyncprofiler;

import org.gradle.profiler.GradleScenarioDefinition;
import org.gradle.profiler.ScenarioSettings;

import java.io.File;

public enum AsyncProfilerOutputType {
    STACKS("collapsed") {
        @Override
        File outputFileFor(ScenarioSettings settings) {
            return settings.getProfilerOutputLocation(".stacks.txt");
        }
    },
    JFR("jfr") {
        @Override
        File outputFileFor(ScenarioSettings settings) {
            return settings.getJfrProfilerOutputLocation();
        }
    };

    public static AsyncProfilerOutputType from(AsyncProfilerConfig config, GradleScenarioDefinition scenarioDefinition) {
        return (config.getEvents().size() > 1 || scenarioDefinition.createsMultipleProcesses())
            ? AsyncProfilerOutputType.JFR
            : AsyncProfilerOutputType.STACKS;
    }

    private final String commandLineOption;

    AsyncProfilerOutputType(String commandLineOption) {
        this.commandLineOption = commandLineOption;
    }

    abstract File outputFileFor(ScenarioSettings settings);

    public String getCommandLineOption() {
        return commandLineOption;
    }
}