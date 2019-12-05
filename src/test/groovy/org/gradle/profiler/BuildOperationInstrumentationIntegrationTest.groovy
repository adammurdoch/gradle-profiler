package org.gradle.profiler

import spock.lang.Unroll

import static org.hamcrest.CoreMatchers.*
import static org.junit.Assert.assertThat
import static org.junit.Assert.assertTrue

class BuildOperationInstrumentationIntegrationTest extends AbstractProfilerIntegrationTest {

    @Unroll
    def "can benchmark configuration time for build using #gradleVersion (instant-execution: #instantExecution)"() {
        given:
        instrumentedBuildScript()

        and:
        String[] args = [
            "--project-dir", projectDir.absolutePath,
            "--output-dir", outputDir.absolutePath,
            "--gradle-version", gradleVersion,
            "--benchmark",
            "--measure-config-time",
            "assemble"
        ]
        if (instantExecution) {
            args += "-Dorg.gradle.unsafe.instant-execution=true"
        }

        when:
        new Main().run(*args)

        then:
        def lines = resultFile.lines
        lines.size() == totalLinesForExecutions(16)
        lines.get(0) == "scenario,default,default"
        lines.get(1) == "version,${gradleVersion},${gradleVersion}"
        lines.get(2) == "tasks,assemble,assemble"
        lines.get(3) == "value,execution,task start"
        lines.get(4).matches("warm-up build #1,\\d+,\\d+")
        lines.get(9).matches("warm-up build #6,\\d+,\\d+")
        lines.get(10).matches("measured build #1,\\d+,\\d+")
        lines.get(20).matches("mean,\\d+\\.\\d+,\\d+\\.\\d+")
        lines.get(23).matches("median,\\d+\\.\\d+,\\d+\\.\\d+")
        lines.get(26).matches("stddev,\\d+\\.\\d+,\\d+\\.\\d+")
        lines.get(27).matches("confidence,\\d+\\.\\d+,\\d+\\.\\d+")

        and:
        def taskStart = lines.get(10) =~ /measured build #1,\d+,(\d+)/
        taskStart.matches()
        Long.valueOf(taskStart[0][1]) > 0

        where:
        [gradleVersion, instantExecution] << [
            ["6.1-milestone-3", latestSupportedGradleVersion] as Set, // simplify this once the latest version is > 6.1-milestone-3
            [true, false]
        ].combinations()
    }

    @Unroll
    def "can benchmark snapshotting build operation time via #via for build using #gradleVersion (instant-execution: #instantExecution)"() {
        given:
        instrumentedBuildForSnapshottingBenchmark()

        and:
        String[] args = [
            "--project-dir", projectDir.absolutePath,
            "--output-dir", outputDir.absolutePath,
            "--gradle-version", gradleVersion,
            "--benchmark"
        ]
        if (scenarioConfiguration) {
            def scenarioFile = file("performance.scenarios")
            scenarioFile.text = """
            default {
                tasks = ["assemble"]
                ${scenarioConfiguration}
            }
            """
            args += ["--scenario-file", scenarioFile.absolutePath]
        }
        args += commandLine
        if (instantExecution) {
            args += "-Dorg.gradle.unsafe.instant-execution=true"
        }
        args += scenarioConfiguration ? "default" : "assemble"

        when:
        new Main().run(*args)

        then:
        def lines = resultFile.lines
        lines.size() == totalLinesForExecutions(16)
        lines.get(0) == "scenario,default,default"
        lines.get(1) == "version,${gradleVersion},${gradleVersion}"
        lines.get(2) == "tasks,assemble,assemble"
        lines.get(3) == "value,execution,SnapshotTaskInputsBuildOperationType"

        def firstWarmup = lines.get(4)
        def snapshottingDuration = firstWarmup =~ /warm-up build #1,\d+,(\d+)/
        snapshottingDuration.matches()
        Long.valueOf(snapshottingDuration[0][1]) > 0

        lines.get(9).matches("warm-up build #6,\\d+,\\d+")
        lines.get(10).matches("measured build #1,\\d+,\\d+")
        lines.get(20).matches("mean,\\d+\\.\\d+,\\d+\\.\\d+")
        lines.get(23).matches("median,\\d+\\.\\d+,\\d+\\.\\d+")
        lines.get(26).matches("stddev,\\d+\\.\\d+,\\d+\\.\\d+")

        where:
        [via, commandLine, scenarioConfiguration, gradleVersion, instantExecution] << [
            [
                [
                    'command line',
                    ["--measure-build-op", "org.gradle.api.internal.tasks.SnapshotTaskInputsBuildOperationType"],
                    null
                ],
                [
                    'scenario file',
                    [],
                    'measured-build-ops = ["org.gradle.api.internal.tasks.SnapshotTaskInputsBuildOperationType"]'
                ],
                [
                    'command line and scenario file',
                    ["--measure-build-op", "org.gradle.api.internal.tasks.SnapshotTaskInputsBuildOperationType"],
                    'measured-build-ops = ["org.gradle.api.internal.tasks.SnapshotTaskInputsBuildOperationType"]'
                ]
            ],
            ["6.1-milestone-3", latestSupportedGradleVersion] as Set
        ].combinations().collectMany {
            def scenario = it[0]
            def gradleVersion = it[1]
            if (gradleVersion == latestSupportedGradleVersion) {
                [scenario + gradleVersion + false, scenario + gradleVersion + true]
            } else {
                [scenario + gradleVersion + false]
            }
        }
    }

    @Unroll
    def "can combine measuring configuration time and build operation using #gradleVersion (instant-execution: #instantExecution)"() {
        given:
        instrumentedBuildForSnapshottingBenchmark()

        and:
        String[] args = [
            "--project-dir", projectDir.absolutePath,
            "--output-dir", outputDir.absolutePath,
            "--gradle-version", gradleVersion,
            "--benchmark",
            "--measure-config-time",
            "--measure-build-op", "org.gradle.api.internal.tasks.SnapshotTaskInputsBuildOperationType",
            "assemble"
        ]
        if (instantExecution) {
            args += "-Dorg.gradle.unsafe.instant-execution=true"
        }

        when:
        new Main().run(*args)

        then:
        def lines = resultFile.lines
        assertThat(lines.size(), equalTo(totalLinesForExecutions(16)))
        lines.get(0) == "scenario,default,default,default"
        lines.get(1) == "version,${gradleVersion},${gradleVersion},${gradleVersion}"
        lines.get(2) == "tasks,assemble,assemble,assemble"
        lines.get(3) == "value,execution,task start,SnapshotTaskInputsBuildOperationType"
        lines.get(4).matches("warm-up build #1,\\d+,\\d+,\\d+")
        lines.get(9).matches("warm-up build #6,\\d+,\\d+,\\d+")
        lines.get(10).matches("measured build #1,\\d+,\\d+,\\d+")
        lines.get(20).matches("mean,\\d+\\.\\d+,\\d+\\.\\d+,\\d+\\.\\d+")
        lines.get(23).matches("median,\\d+\\.\\d+,\\d+\\.\\d+,\\d+\\.\\d+")
        lines.get(26).matches("stddev,\\d+\\.\\d+,\\d+\\.\\d+,\\d+\\.\\d+")
        lines.get(27).matches("confidence,\\d+\\.\\d+,\\d+\\.\\d+,\\d+\\.\\d+")

        and:
        def buildLines = lines.subList(10, 19).collect { it.tokenize(',') }
        def executions = buildLines.collect { Long.valueOf(it.get(1)) } as Set
        def taskStarts = buildLines.collect { Long.valueOf(it.get(2)) } as Set
        def buildOps = buildLines.collect { Long.valueOf(it.get(3)) } as Set
        assertThat("non zero execution times", executions, hasItem(not(equalTo(0L))))
        assertThat("non zero task start times", taskStarts, hasItem(not(equalTo(0L))))
        assertThat("non zero build-op times", buildOps, hasItem(not(equalTo(0L))))
        assertTrue("different execution times", executions.size() > 1)
        assertTrue("different task start times", taskStarts.size() > 1)
        assertTrue("different build-op times", buildOps.size() > 1)

        where:
        gradleVersion                | instantExecution
        "6.1-milestone-3"            | false
        "6.1-milestone-3"            | true
    }

    private void instrumentedBuildForSnapshottingBenchmark() {
        instrumentedBuildScript()
        buildFile << """
            apply plugin: 'java'
        """

        // We don't capture snapshotting time (yet) if the build cache is not enabled
        file("gradle.properties").text = "org.gradle.caching=true"

        def sourceFile = file("src/main/java/A.java")
        sourceFile.parentFile.mkdirs()
        sourceFile.text = "class A {}"
    }

    @Unroll
    def "complains when attempting to benchmark configuration time for build using #gradleVersion"() {
        given:
        instrumentedBuildScript()

        when:
        new Main().run("--project-dir", projectDir.absolutePath, "--output-dir", outputDir.absolutePath, "--gradle-version", gradleVersion, "--benchmark", "--measure-config-time", "assemble")

        then:
        thrown(IllegalArgumentException)

        and:
        output.contains("Scenario using Gradle ${gradleVersion}: Measuring build configuration is only supported for Gradle 6.1-milestone-3 and later")

        where:
        gradleVersion << [minimalSupportedGradleVersion, "4.0", "4.10", "6.0"]
    }

    def "complains when attempting to benchmark configuration time for build using unsupported Gradle version from scenario file"() {
        given:
        instrumentedBuildScript()
        def scenarioFile = file("performance.scenarios")
        scenarioFile.text = """
            assemble { 
                versions = ["${minimalSupportedGradleVersion}", "4.0", "4.10", "6.0", "${latestSupportedGradleVersion}"]
            }
        """

        when:
        new Main().run("--project-dir", projectDir.absolutePath, "--output-dir", outputDir.absolutePath, "--scenario-file", scenarioFile.absolutePath, "--benchmark", "--measure-config-time", "assemble")

        then:
        thrown(IllegalArgumentException)

        and:
        output.contains("Scenario assemble using Gradle ${minimalSupportedGradleVersion}: Measuring build configuration is only supported for Gradle 6.1-milestone-3 and later")
        output.contains("Scenario assemble using Gradle 4.0: Measuring build configuration is only supported for Gradle 6.1-milestone-3 and later")
        output.contains("Scenario assemble using Gradle 4.10: Measuring build configuration is only supported for Gradle 6.1-milestone-3 and later")
    }
}
