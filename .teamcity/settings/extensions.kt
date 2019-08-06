import jetbrains.buildServer.configs.kotlin.v2018_2.BuildType
import jetbrains.buildServer.configs.kotlin.v2018_2.CheckoutMode
import jetbrains.buildServer.configs.kotlin.v2018_2.DslContext
import jetbrains.buildServer.configs.kotlin.v2018_2.ParametrizedWithType

fun BuildType.agentRequirement(os: Os) {
    requirements {
        contains("teamcity.agent.jvm.os.name", os.requirementName)
    }
}

fun ParametrizedWithType.java8Home(os: Os) {
    param("env.JAVA_HOME", "%${os.name}.java8.oracle.64bit%")
}

fun BuildType.gradleProfilerVcs() {
    vcs {
        root(DslContext.settingsRoot)
        checkoutMode = CheckoutMode.ON_SERVER
    }
}

const val useGradleInternalScansServer = "-I gradle/init-scripts/build-scan-gradle-internal-server.init.gradle.kts"
