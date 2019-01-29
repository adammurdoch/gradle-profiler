package org.gradle.profiler;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import static org.gradle.profiler.Phase.MEASURE;
import static org.gradle.profiler.Phase.WARM_UP;

public class BazelScenarioInvoker extends ScenarioInvoker<BazelScenarioDefinition> {
    @Override
    void run(BazelScenarioDefinition scenario, InvocationSettings settings, BenchmarkResultCollector benchmarkResults) {
        String bazelHome = System.getenv("BAZEL_HOME");
        String bazelExe = bazelHome == null ? "bazel" : bazelHome + "/bin/bazel";

        List<String> targets = new ArrayList<>(scenario.getTargets());

        System.out.println();
        System.out.println("* Bazel targets: " + targets);

        List<String> commandLine = new ArrayList<>();
        commandLine.add(bazelExe);
        commandLine.add("build");
        commandLine.addAll(targets);

        BuildMutator mutator = scenario.getBuildMutator().get();
        mutator.beforeScenario();
        try {
            Consumer<BuildInvocationResult> resultConsumer = benchmarkResults.version(scenario);
            for (int i = 0; i < scenario.getWarmUpCount(); i++) {
                String displayName = WARM_UP.displayBuildNumber(i + 1);
                runMeasured(displayName, mutator, measureCommandLineExecution(displayName, commandLine, settings.getProjectDir()), resultConsumer);
            }
            for (int i = 0; i < scenario.getBuildCount(); i++) {
                String displayName = MEASURE.displayBuildNumber(i + 1);
                runMeasured(displayName, mutator, measureCommandLineExecution(displayName, commandLine, settings.getProjectDir()), resultConsumer);
            }
        } finally {
            mutator.afterScenario();
        }
    }
}
