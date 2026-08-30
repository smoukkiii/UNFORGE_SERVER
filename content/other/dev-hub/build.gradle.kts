plugins {
    id("base-conventions")
    id("integration-test-suite")
}

dependencies {
    implementation(projects.api.pluginCommons)
    integrationImplementation(projects.engine.annotations)
    integrationImplementation(projects.engine.coroutine)
}
