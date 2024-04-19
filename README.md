# Azure Upload Plugin

[![License](https://img.shields.io/github/license/cortinico/kotlin-android-template.svg)](LICENSE) ![Language](https://img.shields.io/github/languages/top/cortinico/kotlin-android-template?color=blue&logo=kotlin)

# Azure KeyStore Gradle Plugin

The Azure KeyStore Gradle Plugin facilitates managing key stores for Android or Java applications by allowing seamless integration with Azure Blob Storage to download and configure keystores and their support documents.

## Features

- Download keystores and their support documents from Azure Blob Storage.
- Automatically configures project properties for use in build configurations, particularly for signing Android applications.
- Cleans up keystore files post-build to ensure sensitive data is not kept around longer than necessary.

## Getting Started

To use the Azure KeyStore Plugin, include it in your build script.

### Prerequisites

Ensure you have the following:
- An Azure Blob Storage account.
- Connection details for the Azure Blob Storage account.
- Gradle 6.0 or newer.


### Installation

To use the plugin, add the following to your project's `build.gradle.kts` file:

```kotlin
plugins {
    id("io.github.kmbisset89.azurekeystore.plugin") version "1.0.0"
}
```

For libs.toml:

```toml
[versions]
  azure-upload = "1.0.0"

[plugins]
azure-upload-plugin = { id = "io.github.kmbisset89.azurekeystore.plugin", version.ref = "azure-upload" }
```

### Configuration

Configure the plugin by specifying the connection string, container name, and file names for your keystore and support document. You can place these settings in the local.properties file or as project properties:
```properties
connectionString=your_connection_string_here
containerName=your_container_name_here
keyStoreFileName=keystore.jks
supportDocumentFilename=support_document.json
```
## Tasks
The plugin adds tasks to your project:

- downloadKeyStore: Downloads the keystore and support document from Azure Blob Storage.
- removeKeyStore: Removes the keystore and support document files after use.

These tasks are automatically configured to run at appropriate times during the build process.

## Usage

Below is an example configuration for an Android project:

```kotlin
android {
    compileSdk = 30
    defaultConfig {
        applicationId = "com.example"
        minSdk = 21
        versionCode = 1
        versionName = "1.0"
        signingConfig = signingConfigs.create("config").apply {
            storeFile = rootProject.ext.get("keystoreFile") as File
            storePassword = rootProject.ext.get("storePassword") as String
            keyAlias = rootProject.ext.get("keyAlias") as String
            keyPassword = rootProject.ext.get("keyPassword") as String
        }
    }
}
```
This will ensure that the Android application is signed using the credentials downloaded from Azure Blob Storage.

## Troubleshooting
- Ensure the Azure Blob Storage container exists and the names of the files are correctly specified in the properties.
- Check network permissions and connection strings if there are issues connecting to Azure Blob Storage.

## Contributing 🤝

Feel free to open an issue or submit a pull request for any bugs/improvements.

## License 📄

This template is licensed under the MIT License - see the [License](License) file for details.
Please note that the generated template is offering to start with a MIT license but you can change it to whatever you
wish, as long as you attribute under the MIT terms that you're using the template.
