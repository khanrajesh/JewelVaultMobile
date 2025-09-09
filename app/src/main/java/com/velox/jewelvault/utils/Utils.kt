package com.velox.jewelvault.utils

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import kotlin.reflect.KClass
import kotlin.reflect.full.memberProperties
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Parcelable
import androidx.core.content.FileProvider
import java.io.File

fun isAppInstalled(context: Context, packageName: String): Boolean {
    return try {
        context.packageManager.getPackageInfo(packageName, 0)
        true
    } catch (e: PackageManager.NameNotFoundException) {
        false
    }
}

fun <T : Any> getDataClassKeys(clazz: KClass<T>): List<Pair<Int,String>> {
   return clazz.memberProperties.withIndex().map { (index, prop) ->
        index to prop.name
    }
}

@SuppressLint("SimpleDateFormat")
fun generateId(): String {
    val now = java.text.SimpleDateFormat("yyyyMMddHHmmssSSS").format(java.util.Date())
    val random = (0..99).random().toString().padStart(2, '0')
    return now + random
}



object WhatsAppHelper {

    /**
     * Send text OR text + one/multiple files to a specific WhatsApp number.
     *
     * @param context Context required to start activity.
     * @param phoneNumber Full phone number in international format WITHOUT '+' or spaces.
     *                    e.g. for India: "919812345678"
     * @param message Optional text message (may be empty).
     * @param files Optional list of File objects to attach (images/.pdf etc). Can be null or empty.
     * @param authority FileProvider authority, typically "${applicationId}.fileprovider"
     */
    fun sendWhatsApp(
        context: Context,
        phoneNumber: String,
        message: String = "",
        files: List<File>? = null,
        authority: String = "${context.packageName}.fileprovider"
    ) {
        // Normalize phone: remove spaces and '+' just in case
        val cleanedPhone = phoneNumber.replace("\\s+".toRegex(), "").replace("+", "")

        // If no files -> just text
        if (files.isNullOrEmpty()) {
            sendTextOnly(context, cleanedPhone, message)
            return
        }

        // When there are attachments, prepare content Uris via FileProvider
        val uris = files.mapNotNull { file ->
            try {
                // make sure file exists
                if (!file.exists()) return@mapNotNull null
                FileProvider.getUriForFile(context, authority, file)
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }

        if (uris.isEmpty()) {
            // fallback to text only
            sendTextOnly(context, cleanedPhone, message)
            return
        }

        try {
            val intent: Intent
            if (uris.size == 1) {
                // single file
                val singleUri = uris[0]
                intent = Intent(Intent.ACTION_SEND).apply {
                    type = context.contentResolver.getType(singleUri) ?: "*/*"
                    putExtra(Intent.EXTRA_STREAM, singleUri)
                    putExtra(Intent.EXTRA_TEXT, message)
                    `package` = "com.whatsapp"
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
            } else {
                // multiple files
                val parcelableUris = ArrayList<Parcelable>(uris)
                intent = Intent(Intent.ACTION_SEND_MULTIPLE).apply {
                    type = "*/*"
                    putParcelableArrayListExtra(Intent.EXTRA_STREAM, parcelableUris)
                    putExtra(Intent.EXTRA_TEXT, message)
                    `package` = "com.whatsapp"
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
            }

            // Prefer sending directly to the phone number by using the "jid" extra (WhatsApp internal).
            // This technique is commonly used, but may not be officially documented. We still try it.
            // Format: <phoneNumber>@s.whatsapp.net
            val jid = "$cleanedPhone@s.whatsapp.net"
            intent.putExtra("jid", jid)

            context.startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            // WhatsApp not installed — fallback to web/send with text only (attachments can't be sent)
            fallbackToWhatsAppWeb(context, cleanedPhone, message)
        } catch (e: Exception) {
            e.printStackTrace()
            // final fallback
            fallbackToWhatsAppWeb(context, cleanedPhone, message)
        }
    }

    private fun sendTextOnly(context: Context, phoneNumber: String, message: String) {
        try {
            // Prefer direct app
            val intent = Intent(Intent.ACTION_VIEW).apply {
                val url = "https://api.whatsapp.com/send?phone=$phoneNumber&text=${Uri.encode(message)}"
                data = Uri.parse(url)
                `package` = "com.whatsapp"
            }
            context.startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            // WhatsApp app missing -> open WhatsApp Web in browser
            fallbackToWhatsAppWeb(context, phoneNumber, message)
        }
    }

    private fun fallbackToWhatsAppWeb(context: Context, phoneNumber: String, message: String) {
        val url = "https://api.whatsapp.com/send?phone=$phoneNumber&text=${Uri.encode(message)}"
        val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        context.startActivity(webIntent)
    }
}
