package com.smsrelay.app

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject
import java.security.MessageDigest
import java.util.UUID

internal data class ShareAccount(val id: String = UUID.randomUUID().toString(), val name: String, val email: String)

internal class WorkspaceStore(context: Context, email: String) {
    private val accountKey = MessageDigest.getInstance("SHA-256")
        .digest(email.lowercase().toByteArray()).joinToString("") { "%02x".format(it) }
    private val preferences = context.getSharedPreferences("workspace_$accountKey", Context.MODE_PRIVATE)
    var petVoiceName: String
        get() = preferences.getString("pet_voice", "").orEmpty()
        set(value) { preferences.edit().putString("pet_voice", value).apply() }
    var petSpeechRate: Float
        get() = preferences.getFloat("pet_rate", 1f)
        set(value) { preferences.edit().putFloat("pet_rate", value).apply() }
    var petPitch: Float
        get() = preferences.getFloat("pet_pitch", 1f)
        set(value) { preferences.edit().putFloat("pet_pitch", value).apply() }
    var petSpecies: String
        get() = preferences.getString("pet_species", "Cat").orEmpty().takeIf { it in setOf("Cat", "Dog", "Elephant", "Rabbit") } ?: "Cat"
        set(value) { preferences.edit().putString("pet_species", value).apply() }
    var petColour: Int
        get() = preferences.getInt("pet_colour", 0xFFF7C18C.toInt())
        set(value) { preferences.edit().putInt("pet_colour", value).apply() }
    var petName: String
        get() = preferences.getString("pet_name", "Milo").orEmpty()
        set(value) { preferences.edit().putString("pet_name", value).apply() }
    var petEnabled: Boolean
        get() = preferences.getBoolean("pet_enabled", false)
        set(value) { preferences.edit().putBoolean("pet_enabled", value).apply() }
    var sharingEnabled: Boolean
        get() = preferences.getBoolean("sharing_enabled", true)
        set(value) { preferences.edit().putBoolean("sharing_enabled", value).apply() }
    var includeFinancial: Boolean
        get() = preferences.getBoolean("include_financial", false)
        set(value) { preferences.edit().putBoolean("include_financial", value).apply() }
    var allowedSenders: String
        get() = preferences.getString("allowed_senders", "").orEmpty()
        set(value) { preferences.edit().putString("allowed_senders", value).apply() }
    var profileName: String
        get() = preferences.getString("profile_name", "").orEmpty()
        set(value) { preferences.edit().putString("profile_name", value).apply() }
    var aboutMe: String
        get() = preferences.getString("about_me", "").orEmpty()
        set(value) { preferences.edit().putString("about_me", value).apply() }
    var selectedSources: Set<String>
        get() = preferences.getStringSet("selected_sources", emptySet()).orEmpty().toSet()
        set(value) { preferences.edit().putStringSet("selected_sources", value.toSet()).apply() }
    var spokenAlerts: Boolean
        get() = preferences.getBoolean("spoken_alerts", false)
        set(value) { preferences.edit().putBoolean("spoken_alerts", value).apply() }

    var assistantAlerts: Boolean
        get() = preferences.getBoolean("assistant_alerts", false)
        set(value) { preferences.edit().putBoolean("assistant_alerts", value).apply() }
    var collectionEnabled: Boolean
        get() = preferences.getBoolean("collection_enabled", true)
        set(value) { preferences.edit().putBoolean("collection_enabled", value).apply() }

    fun listen(listener: SharedPreferences.OnSharedPreferenceChangeListener) = preferences.registerOnSharedPreferenceChangeListener(listener)
    fun stopListening(listener: SharedPreferences.OnSharedPreferenceChangeListener) = preferences.unregisterOnSharedPreferenceChangeListener(listener)
    fun accounts(): List<ShareAccount> = readArray("accounts") { item ->
        ShareAccount(item.getString("id"), item.getString("name"), item.getString("email"))
    }
    fun saveAccounts(accounts: List<ShareAccount>) {
        val array = JSONArray()
        accounts.forEach { array.put(JSONObject().put("id", it.id).put("name", it.name).put("email", it.email)) }
        preferences.edit().putString("accounts", array.toString()).apply()
    }
    fun messages(): List<SmsItem> = readArray("messages") { item ->
        SmsItem(item.getString("sender"), item.getString("category"), item.getString("receivedAt"), item.getString("body"),
            id = item.optString("id", "legacy_${item.toString().hashCode()}"),
            source = item.optString("source", "Imported"), sourcePackage = item.optString("sourcePackage"),
            capturedAt = item.optLong("capturedAt", 0L))
    }
    private fun saveMessages(messages: List<SmsItem>) {
        val array = JSONArray()
        messages.forEach {
            array.put(JSONObject().put("sender", it.sender).put("category", it.category)
                .put("receivedAt", it.receivedAt).put("body", it.body).put("id", it.id)
                .put("source", it.source).put("sourcePackage", it.sourcePackage).put("capturedAt", it.capturedAt))
        }
        preferences.edit().putString("messages", array.toString()).apply()
    }
    fun appendMessages(incoming: List<SmsItem>): List<SmsItem> = synchronized(messageLock) {
        val current = messages()
        val existingIds = current.map { it.id }.toSet()
        val added = incoming.distinctBy { it.id }.filter { it.id !in existingIds }
        if (added.isNotEmpty()) saveMessages(mergeCollectedMessages(current, added))
        added
    }
    fun clearMessages() = synchronized(messageLock) { saveMessages(emptyList()) }
    private fun <T> readArray(key: String, read: (JSONObject) -> T): List<T> = try {
        val array = JSONArray(preferences.getString(key, "[]"))
        List(array.length()) { read(array.getJSONObject(it)) }
    } catch (_: org.json.JSONException) { emptyList() }
    companion object { private val messageLock = Any() }
}

internal fun mergeCollectedMessages(current: List<SmsItem>, incoming: List<SmsItem>): List<SmsItem> =
    (current + incoming).distinctBy { it.id }.sortedByDescending { it.capturedAt }
