package com.invozone.mapboxnavigation.storage

import android.content.Context
import android.content.SharedPreferences
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.invozone.mapboxnavigation.model.MainPlace
import java.lang.reflect.Type


enum class KeyPref {
    FCM_TOKEN,IS_FIRST_TIME_APP_LOAD
}

enum class KeyObjPref {
    USER_DATA,CURR_LOCATION,TOKEN_DATA
}

enum class KeyListPref {
    USER_LIST,
    RECENT_LIST
}

private fun Context.getSecurePref(prefix:String="security_master_key_"): SharedPreferences {
    val builder = KeyGenParameterSpec.Builder(
        "${prefix}_${this.packageName}",
        KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
    )
        .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
        .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
        .setKeySize(256)


    val keyGenParameterSpec = builder.build()

    val mainKeyAlias = MasterKeys.getOrCreate(keyGenParameterSpec)
    val sharedPrefsFile = this.packageName
    return EncryptedSharedPreferences.create(
        sharedPrefsFile,
        mainKeyAlias,
        this,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )
}

fun Context.setPref(keyPref: KeyPref, value: String?) {
    getSecurePref().edit().apply {
        putString(keyPref.name, value)
    }.also {
        it.apply()
    }
}

fun Context.getPref(keyPref: KeyPref, defValue: String?): String? {
    return getSecurePref().getString(keyPref.name, defValue)
}

fun Context.setPref(keyPref: KeyPref, value: Boolean) {
    getSecurePref().edit().apply {
        putBoolean(keyPref.name, value)
    }.also {
        it.apply()
    }
}

fun Context.getPref(keyPref: KeyPref, defValue: Boolean): Boolean {
    return getSecurePref().getBoolean(keyPref.name, defValue)
}

fun <T> Context.getPref(keyObjPref: KeyObjPref, classOfT: Class<T>): T? {
    return try {
        val jsonString = getSecurePref().getString(keyObjPref.name, "")
        Gson().fromJson(jsonString, classOfT)
    } catch (e: Exception) {
        null
    }
}

fun Context.setPref(keyObjPref: KeyObjPref, anyObj: Any) {
    getSecurePref().edit().apply {
        putString(keyObjPref.name, Gson().toJson(anyObj))
    }.also {
        it.apply()
    }
}

fun Context.setPref(keyListPref: KeyListPref, list: List<Any>) {
    getSecurePref().edit().apply {
        putString(keyListPref.name, Gson().toJson(list))
    }.also {
        it.apply()
    }
}

fun <T> Context.getPref(keyListPref: KeyListPref): List<T> {
    return try {
        val jsonString = getSecurePref().getString(keyListPref.name, "")
        val type: Type = object : TypeToken<List<T?>?>() {}.type
        Gson().fromJson(jsonString, type)
    } catch (e: Exception) {
        listOf()
    }
}

fun Context.setPref(keyListPref: KeyListPref, list: ArrayList<MainPlace>) {
    getSecurePref().edit().apply {
        putString(keyListPref.name, Gson().toJson(list))
    }.also {
        it.apply()
    }
}

fun Context.getPref(keyListPref: KeyListPref): ArrayList<MainPlace> {
    return try {
        val jsonString = getSecurePref().getString(keyListPref.name, "")
        val type: Type = object : TypeToken<ArrayList<MainPlace>?>() {}.type
        Gson().fromJson(jsonString, type)
    } catch (e: Exception) {
        arrayListOf<MainPlace>()
    }
}

fun Context.clearAllPref() {
    getSecurePref().edit().clear().apply()
}



