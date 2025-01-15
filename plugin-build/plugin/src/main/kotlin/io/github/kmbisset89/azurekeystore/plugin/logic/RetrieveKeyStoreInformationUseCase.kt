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

    private val defaultKeystore = lazy {
        "30820a06020103308209b006092a864886f70d010701a08209a10482099d30820999308205b006092a864886f70d010701a08205a10482059d3082059930820595060b2a864886f70d010c0a0102a08205403082053c306606092a864886f70d01050d3059303806092a864886f70d01050c302b0414e5823d7740f200903750c06089a1f380334a801c02022710020120300c06082a864886f70d02090500301d060960864801650304012a04103ac66a28f3b675a655a51416297d1699048204d0f045a4a01910cc138b5e7f4c42c325a88544ab83f1baa802a6edc523e4c7dc9e88bc837a7caffccacf11a70f75fa106ccfeee5438ee9dfcf4c0aa198d7e8ec8fd244dc669e1579ef8a74253232f46656b95739e13d480d5bdd6e114452f7a9093aaa4caa026a734ffa94d90a5c60a39235049d000a65edcf91994321159fe51905384f4f624beb71ed72719e395f2c7ffc0f1fc24034c6a52a90d90d0e82070a00df2b7780e8c8012c926c6dc729fced5693018513d10165a5568e0bf3c028f220648fe8de0ba5fa5a1dbded3b347ac031ad79926332f9984b0927e316a528d90b6783079c3657de679427400d748d75cb90e3d618f097a830aa534896d87a441b1d2d0adb91440be0cb2c99ea2fafd6463a87752742a05ae14167af5b66d049434ec1cc92a0d066847c7715fd1f6774c7240dc0f1df0d0802f965f3e5cd185575e55c3f7a487da7aa814077382ca3b254d572c00c950a95bc7ef01729341dda3e571ddb2a6582876cdcc863736bda1192a8c5a19fca2a54438991ecf66448319c46fb53190a8c5bc621390b13a7a6a82f6f37e6aff8d74a96525eb8da77809d4b233bd77059202c39713247778cdd1821b4cdee4f8b2448e7ee9d4dc637cf3824f4d2f699e837e444341a6b5eb6fc6949ea6d4d986de58b5509de3d92485349a5d01c09f3a624b9628762e9ece082ea6155cf7152de698587f58b4268041a46d7aa0398c4c4278d0bc04fd47d5fda88b19dcd2f2ec4216f4db80e9710e141225661a877d0fc9f4b163d433a11591cd948defa5ea2232580fca866c1cea5f696b41ea9f54c00af34b78bc2f88dc5ea996e0e729b3e20a7f2b1260b8d8bfb1501625eed71e4ea424b1c40690a1670f79c14926da500d35ec5999be28aa7014c183e805a17cef302f78ecb0b3fd7a06bedb9a019a954dcb964699d45b499838f39d1cee4685ff7411575321645de9682bf9569c9c4bed5b613e7509c6dfc6c7ff455555721b34d190d4017e3e4bf7eca64327ec0d24acc7ee133d2c835bba89a9129ae29c5c23e7338446942fa1cb8dcbe8e005a0128de110ff0bf9c7ed83f3d55128adf04053ca7a29583b2eb508beeb2a30a97cbd14b473d83efb44a63e69772876aa751bfe0fe3669e8bef88fca825ef8e9c48f1f19c6086a93dc34db4994e23753587fb21e4244c316e6fc83000b2ae8220a778bf504c07bf90ad2696dcd250f0e403dabf5dfc880ddba9bea35acbdd8405b13525667700d849862cb42a0a218c7f697611c3ed7a0b0628c18c64ae2447b5b1858d965638ad3dd0205ba2efec0094def8a8cdc568de0a15bc4b39765b09bb342513544bd1951e438adecaee920ffc211ec18473e57a3ccfdebd46ad6c9255f46fbb9087c555095e805cc0263c4ad77cf8833d34be89fc03790670e68110c585b2f8450f29a669259cc4a5c12dd9ddf08e58d1f483a53d93323a54a2d37b649d7017fb21413f216b4e5758fc6e6fd934f732601e6bde8fb94e85a30219286cb1924db6112910ee1294ecbc0a9f3bfaa73149d6dcfa83139a2ce30df2db59a0625f5b5f8ccb1353f8a25b165bffbd8f94b0eaf695fdc972980290d089823c549eaa6c0b9e38a73a946ed255c0937db36219a7e5178068be653986041f91869a15410e5827d636906f48a1d5b6774d7a6cb0050fc54d77dabd929af49e2edfa54d2fc681fbcbc543af213d582aaafb0e1d81bbf31add9731e5f808991093142301d06092a864886f70d01091431101e0e006d0061006d0061006d00690061302106092a864886f70d0109153114041254696d652031373336393636363730383534308203e106092a864886f70d010706a08203d2308203ce020100308203c706092a864886f70d010701306606092a864886f70d01050d3059303806092a864886f70d01050c302b0414c0fba6a3cf4f17daf49a299d886ac357326b35f302022710020120300c06082a864886f70d02090500301d060960864801650304012a04102217fbe41cc78aeabe1f1a1580330b3280820350157b77b3fa1fc5551899d38bc886a30da6c7c5f3cf023bae83e9cc66801920272012e898c8690a1f45e475a8dda1044b6a7af7f7b8a70a3052a7ed657c464a57915c4cae79fec9b4fe69d6702f480c3f636916c9ddb72e4419233fcbfaef0f80cf68ed0ec7c50ad02e8fb3ed4972c3dd6b6f0ec653e8fe5df804e78a4d9b2d45e77c156dab7045b8835ae8d201c435f265a4d28d9a4f94ca53d6ab4a50b5c23476a88a3f7a8be4848a1ae17e7554e6de802aea6243efb2795bd177f29fe9a5f0103a7b0e27a28ba18da9f96e42e54af40f65fc7da65d361fc97baf32f711a5f9731e7ba2af7e277fd20975b3ee1b9b75fa64bfbb5bd21c8a1abba4e07296cd522dfb83f18e0ac01f9cfdb513d0d49fb1723c3c80d77a3a81041793dc9f050d4d1b41bba7b773aad86a9b8bd80ccc96beef65fd84438cc4771ddae171e1c1e84962a1862ad27cb5793bfcad0c75fa5c18b3080559abbcf8ee560be69086950adfef2bd271d9a2cb5def3fba6bccc45aae92c4acca4a4e691dda7368a914cc173f221a3f3cdb9d8cf7da0bdb98de11e786f02f43e0fc959354ba9d8f2d6021071f0ad8063e2cf798252d9c27e57f828915c329725c936231afb00a86541392e0acb3c845465d260d882e897fd91e669c4e8cd4a9f5b63c5768d7afc372f765ff58bcbcb0dd046b765e6f01a7a5d3390dc9c9846bafd6f926c855d199c719b3c976a3200311861d88b57771a2e2cd6236c333e807ece86cabaa76241d52721b4ded61d6c288fc94d47a580ddacaa25295d74492b127dea2884da7c78f94cc29c17d66d29574502eafa794b02a4049e958cdef6fd8b234a29896947a02ba0949b45aae5fb687499e1e3a18f1e01905afb75168a52668406cb15b96c90883fe11053da9b577d73183099ca4e24b6be58f556059432053c9f639e30bd759f3b416e01e45f741b69699065d1d9e51995096320c1ee3b5cd566a92003365d4ec9c4bdc1e3c1f238ca138ac1b4ce3ac708319b44a3eed5ac615cb6d7b285b7287f5b0c3d50cab0e513b5441c2899599f064a6bf142b2ba50ca10aa870a8aa3544ec61040619004563eafa3e8e8829c819874539f81eb43a271382ec7dc793c2c35c7a75758fc14c7f67f4ba1e2831dabcac0406d2eef8667eeb33394827e8f0111b1bfb17eb0c46c35edbe61f25817fab4adf9ed8304d3031300d060960864801650304020105000420412a0cea3db1addafb45ea9281a1e3888b5064eeebe1f9f243f3e8073040c3b804141da0a537bf1b8e9e1d7cec9ae6edbf269cb97b2102022710"
    }

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
        connectionString: String?,
        containerName: String?,
        keyStoreFileName: String?,
        supportDocumentFilename: String?,
        project: Project
    ) {
        project.logger.lifecycle("Starting retrieval of key store information from Azure Blob Storage.")

        var usingDefaultKeystore = false

        val storageAccount = BlobContainerClientBuilder()
            .connectionString(connectionString)
            .containerName(containerName)
            .buildClient()

        if (!storageAccount.exists() || supportDocumentFilename.isNullOrBlank() || keyStoreFileName.isNullOrBlank()) {
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

        // Create a temporary file (or any location you want to copy it to)
        val tempKeystoreFile =
            File("${project.rootProject.projectDir.absolutePath}${File.separator}DefaultKeystore", ".jks")

       tempKeystoreFile.writeBytes(hexStringToByteArray(defaultKeystore.value))

        project.rootProject.extraProperties.set("keystoreFile", tempKeystoreFile)
        project.rootProject.extraProperties.set("storePassword", "DefaultKeystore123!")
        project.rootProject.extraProperties.set("keyAlias", "mamamia")
        project.rootProject.extraProperties.set("keyPassword", "Herewegoagain")
    }


    private fun hexStringToByteArray(hexString: String): ByteArray {
        require(hexString.length % 2 == 0) { "Hex string must have an even length" }

        return hexString.chunked(2)
            .map { it.toInt(16).toByte() }
            .toByteArray()
    }
}
