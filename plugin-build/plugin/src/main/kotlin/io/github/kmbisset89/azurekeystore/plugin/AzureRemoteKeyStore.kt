package io.github.kmbisset89.azurekeystore.plugin

import io.github.kmbisset89.azurekeystore.plugin.logic.PropertyResolver
import io.github.kmbisset89.azurekeystore.plugin.logic.RetrieveKeyStoreInformationUseCase
import org.gradle.api.Plugin
import org.gradle.api.Project
import java.util.*
import java.util.Properties

/**
 * A Gradle plugin that configures the project to interact with an Azure Blob Storage to manage a remote keystore.
 * This plugin sets up tasks for downloading a keystore and its support document from Azure Blob Storage based on
 * configuration provided through the plugin's extension.
 */
class AzureRemoteKeyStore : Plugin<Project> {

    /**
     * Applies this plugin to the given project. The method sets up the plugin's extension and tasks.
     *
     * @param project The project to which this plugin is applied. The project provides a context for accessing
     *        Gradle's features such as tasks and extensions.
     */
    override fun apply(project: Project) {
        project.logger.lifecycle("Starting AzureRemoteKeyStore plugin configuration.")

        try {
            val localProps = Properties().also {
                if (project.rootProject.file("local.properties").exists()) {
                    it.load(project.rootProject.file("local.properties").inputStream())
                    project.logger.info("Loaded local properties from root project.")
                }
            }

            val pr = PropertyResolver(localProps, project.rootProject.properties)

            project.logger.lifecycle("Setting up tasks to manage remote keystore from Azure Blob Storage.")
            // Initial retrieval to set up the project
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
                project.logger.lifecycle("Temporary keystore and support document removed after evaluation.")
            }

            // Register a task to download keystore files from Azure Blob Storage. The task's configuration
            // is derived from the plugin extension.
            val download = project.tasks.register("downloadKeystore") {
                it.doFirst {
                    project.logger.info("Downloading keystore from Azure Blob Storage.")
                    RetrieveKeyStoreInformationUseCase().invoke(
                        connectionString = pr.resolveProperty("connectionString"),
                        containerName = pr.resolveProperty("containerName"),
                        keyStoreFileName = pr.resolveProperty("keyStoreFileName"),
                        supportDocumentFilename = pr.resolveProperty("supportDocumentFilename"),
                        project
                    )
                }
            }

            val remove = project.tasks.register("cleanupKeystore") {
                it.doLast {
                    project.rootProject.projectDir.resolve(pr.resolveProperty("keyStoreFileName")).delete()
                    project.rootProject.projectDir.resolve(pr.resolveProperty("supportDocumentFilename")).delete()
                    project.logger.info("Cleanup task executed: Keystore and support document deleted.")
                }
            }

            project.tasks.matching { it.name == "validateSigningRelease" }.configureEach {
                it.dependsOn(download)
                project.logger.lifecycle("validateSigningRelease task now depends on downloadKeystore.")
            }

            project.tasks.matching { it.name == "packageRelease" }.configureEach {
                it.finalizedBy(remove)
                project.logger.lifecycle("packageRelease task now finalized by cleanupKeystore.")
            }

            project.tasks.matching { it.name.contains("ReleaseWithR8") }.configureEach {
                it.dependsOn(download)
                project.logger.lifecycle("validateSigningRelease task now depends on downloadKeystore.")
                it.finalizedBy(remove)
            }

        } catch (e: Exception) {
            project.logger.error("Error in AzureRemoteKeyStore plugin: ${e.message}")
            project.logger.error(e.stackTraceToString())
        }

        project.logger.lifecycle("AzureRemoteKeyStore plugin setup complete.")
    }
}
