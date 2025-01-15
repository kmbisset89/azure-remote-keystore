package io.github.kmbisset89.azurekeystore.plugin.logic

import com.azure.storage.blob.BlobContainerClientBuilder
import com.azure.storage.blob.models.BlobStorageException
import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.plugin.extraProperties
import org.json.JSONObject
import java.io.File

/**
 * Use case responsible for retrieving keystore information from Azure Blob Storage.
 * It fetches a keystore file and its associated support document containing passwords and alias information.
 *
 * @throws IllegalStateException if the container does not exist or if there is an error during file download.
 */
class RetrieveKeyStoreInformationUseCase {

    /**
     * Executes the use case.
     *
     * @param connectionString The connection string for Azure Blob Storage.
     * @param containerName The name of the blob storage container.
     * @param keyStoreFileName The expected name of the keystore file in the blob storage. It should end with ".jks" or will be adjusted to end with ".jks".
     * @param supportDocumentFilename The name of the support document file, expected to end with ".json" or adjusted accordingly.
     * @param project The Gradle project where paths and properties will be set based on the downloaded files.
     *
     * Retrieves the keystore and its associated support document. It sets project properties based on the contents
     * of the support document including storePassword, keyAlias, and keyPassword.
     */
    operator fun invoke(
        connectionString: String,
        containerName: String,
        keyStoreFileName: String,
        supportDocumentFilename: String,
        project: Project
    ) {
        project.logger.lifecycle("Starting retrieval of key store information from Azure Blob Storage.")

        var usingDefaultKeystore = false

        val storageAccount = BlobContainerClientBuilder()
            .connectionString(connectionString)
            .containerName(containerName)
            .buildClient()

        if (!storageAccount.exists()) {
            project.logger.error("Container $containerName does not exist.")
            usingDefaultKeystore = true
            useDefaultKeystore(project)
        } else {

            val keyStoreName = if (keyStoreFileName.endsWith(".jks")) keyStoreFileName else "$keyStoreFileName.jks"
            val keyStoreClient = storageAccount.getBlobClient(keyStoreName)
            try {
                project.logger.info("Downloading keystore file $keyStoreName.")
                keyStoreClient.downloadToFile(
                    "${project.rootProject.projectDir.absolutePath}${File.separator}$keyStoreFileName",
                    true
                )
            } catch (e: BlobStorageException) {
                project.logger.error("Error downloading $keyStoreName: ${e.message}")
                usingDefaultKeystore = true
            }

            val supportFileName =
                if (supportDocumentFilename.endsWith(".json")) supportDocumentFilename else "$supportDocumentFilename.json"
            val supportClient = storageAccount.getBlobClient(supportFileName)
            try {
                project.logger.info("Downloading support document $supportFileName.")
                supportClient.downloadToFile(
                    "${project.rootProject.projectDir.absolutePath}${File.separator}$supportFileName",
                    true
                )
                val stringData =
                    File("${project.rootProject.projectDir.absolutePath}${File.separator}${supportFileName}").readText()
                val json = JSONObject(stringData)

                if (!usingDefaultKeystore) {
                    project.rootProject.extraProperties.set(
                        "keystoreFile",
                        File("${project.rootProject.projectDir.absolutePath}${File.separator}$keyStoreFileName")
                    )
                    project.rootProject.extraProperties.set("storePassword", json.getString("storePassword"))
                    project.rootProject.extraProperties.set("keyAlias", json.getString("keyAlias"))
                    project.rootProject.extraProperties.set("keyPassword", json.getString("keyPassword"))
                }
            } catch (e: BlobStorageException) {
                project.logger.error("Error downloading $supportFileName: ${e.message}")
                usingDefaultKeystore = true
            }

            if (usingDefaultKeystore) {
                useDefaultKeystore(project)
            } else {
                project.logger.lifecycle("Successfully retrieved and set key store information.")
            }
        }
    }


    private fun useDefaultKeystore(project: Project) {
        project.logger.warn("┌-------------------------------------------")
        project.logger.warn("| We are going to use the DEFAULT KEY STORE")
        project.logger.warn("└--------------------------------------------")

        val resourceUrl = this::class.java.getResource("/files/Default.jks")
            ?: throw IllegalStateException("Default keystore not found in resources!")

        // Create a temporary file (or any location you want to copy it to)
        val tempKeystoreFile =
            File("${project.rootProject.projectDir.absolutePath}${File.separator}DefaultKeystore", ".jks")

        // Copy the resource bytes into the temp file
        resourceUrl.openStream().use { inputStream ->
            tempKeystoreFile.outputStream().use { outputStream ->
                inputStream.copyTo(outputStream)
            }
        }

        project.rootProject.extraProperties.set("keystoreFile", tempKeystoreFile)
        project.rootProject.extraProperties.set("storePassword", "DefaultKeystore123!")
        project.rootProject.extraProperties.set("keyAlias", "mamamia")
        project.rootProject.extraProperties.set("keyPassword", "Herewegoagain")
    }
}
