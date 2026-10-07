package com.smsrelay.app

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.security.MessageDigest
import java.util.UUID

internal data class ShareAccount(val id: String = UUID.randomUUID().toString(), val name: String, val email: String)

/** Local preferences and imported messages are isolated for each signed-in account. */
internal class WorkspaceStore(context: Context, email: String) {
    private val accountKey = MessageDigest.getInstance("SHA-256")
        .digest(email.lowercase().toByteArray()).joinToString("") { "%02x".format(it) }
    private val preferences = context.getSharedPreferences("workspace_$accountKey", Context.MODE_PRIVATE)

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

    fun accounts(): List<ShareAccount> = readArray("accounts") { item ->
        ShareAccount(item.getString("id"), item.getString("name"), item.getString("email"))
    }

    fun saveAccounts(accounts: List<ShareAccount>) {
        val array = JSONArray()
        accounts.forEach { array.put(JSONObject().put("id", it.id).put("name", it.name).put("email", it.email)) }
        preferences.edit().putString("accounts", array.toString()).apply()
    }

    fun messages(): List<SmsItem> = readArray("messages") { item ->
        SmsItem(item.getString("sender"), item.getString("category"), item.getString("receivedAt"), item.getString("body"))
    }

    fun saveMessages(messages: List<SmsItem>) {
        val array = JSONArray()
        messages.forEach {
            array.put(JSONObject().put("sender", it.sender).put("category", it.category)
                .put("receivedAt", it.receivedAt).put("body", it.body))
        }
        preferences.edit().putString("messages", array.toString()).apply()
    }

    private fun <T> readArray(key: String, read: (JSONObject) -> T): List<T> = try {
        val array = JSONArray(preferences.getString(key, "[]"))
        List(array.length()) { read(array.getJSONObject(it)) }
    } catch (_: org.json.JSONException) {
        emptyList()
    }
}
