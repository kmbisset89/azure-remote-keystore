package io.github.kmbisset89.azurekeystore.plugin

import io.github.kmbisset89.azurekeystore.plugin.logic.PropertyResolver
import io.github.kmbisset89.azurekeystore.plugin.logic.RetrieveKeyStoreInformationUseCase
import org.gradle.api.Plugin
import org.gradle.api.Project
import java.util.*


/**
 * A Gradle plugin that configures the project to interact with an Azure Blob Storage to manage a remote keystore.
 * This plugin sets up tasks for downloading a keystore and its support document from Azure Blob Storage based on
 * configuration provided through the plugin's extension.
 */
abstract class AzureRemoteKeyStore : Plugin<Project> {

    /**
     * Applies this plugin to the given project. The method sets up the plugin's extension and tasks.
     *
     * @param project The project to which this plugin is applied. The project provides a context for accessing
     *        Gradle's features such as tasks and extensions.
     */
    override fun apply(project: Project) {

        try {
            val localProps = Properties().also {
                if (project.rootProject.file("local.properties").exists()) {
                    it.load(project.rootProject.file("local.properties").inputStream())
                }
            }

            val pr = PropertyResolver(localProps, project.rootProject.properties)

            RetrieveKeyStoreInformationUseCase().invoke(
                connectionString = pr.resolveProperty("connectionString"),
                containerName = pr.resolveProperty("containerName"),
                keyStoreFileName = pr.resolveProperty("keyStoreFileName"),
                supportDocumentFilename = pr.resolveProperty("supportDocumentFilename"),
                project
            )

            project.afterEvaluate {
                project.rootProject.projectDir.resolve(pr.resolveProperty("keyStoreFileName")).delete()
                project.rootProject.projectDir.resolve(pr.resolveProperty("supportDocumentFilename")).delete()
            }

            // Register a task to download keystore files from Azure Blob Storage. The task's configuration
            // is derived from the plugin extension.
            val download = project.task("Test") {

                it.doFirst {
                    RetrieveKeyStoreInformationUseCase().invoke(
                        connectionString = pr.resolveProperty("connectionString"),
                        containerName = pr.resolveProperty("containerName"),
                        keyStoreFileName = pr.resolveProperty("keyStoreFileName"),
                        supportDocumentFilename = pr.resolveProperty("supportDocumentFilename"),
                        project
                    )
                }
            }

            val remove = project.task("Remove") {
                it.doLast {
                    project.rootProject.projectDir.resolve(pr.resolveProperty("keyStoreFileName")).delete()
                    project.rootProject.projectDir.resolve(pr.resolveProperty("supportDocumentFilename")).delete()
                }
            }


            project.tasks.matching { it.name == "validateSigningRelease" }.configureEach {
                it.dependsOn(download)
            }

            project.tasks.matching { it.name == "packageRelease" }.configureEach {
                it.finalizedBy(remove)
            }

        } catch (e: Exception) {
            project.logger.error("Error in AzureRemoteKeyStore plugin: ${e.message}")
            project.logger.error(e.stackTraceToString())
        }
    }
}
