package com.velox.jewelvault.data

data class UpdateInfo(
    val forceUpdate: Boolean = false,
    val latestVersionCode: Int = 1,
    val latestVersionName: String = "1.0",
    val playStoreUrl: String = "",
    val updateMessage: String = "",
    val updateTitle: String = "Update Available",
    val minRequiredVersion: Int = 1
) {
    override fun toString(): String {
        return "UpdateInfo(forceUpdate=$forceUpdate, latestVersionCode=$latestVersionCode, latestVersionName='$latestVersionName', playStoreUrl='$playStoreUrl', updateMessage='$updateMessage', updateTitle='$updateTitle', minRequiredVersion=$minRequiredVersion)"
    }
    companion object {
        fun fromRemoteConfig(
            forceUpdate: Boolean,
            latestVersionCode: Long,
            latestVersionName: String,
            playStoreUrl: String,
            updateMessage: String,
            updateTitle: String,
            minRequiredVersion: Long
        ): UpdateInfo {
            return UpdateInfo(
                forceUpdate = forceUpdate,
                latestVersionCode = latestVersionCode.toInt(),
                latestVersionName = latestVersionName,
                playStoreUrl = playStoreUrl,
                updateMessage = updateMessage,
                updateTitle = updateTitle,
                minRequiredVersion = minRequiredVersion.toInt()
            )
        }
    }
} 