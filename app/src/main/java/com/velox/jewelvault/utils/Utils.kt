package com.velox.jewelvault.utils

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import kotlin.reflect.KClass
import kotlin.reflect.full.memberProperties

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