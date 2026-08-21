package com.example.core.services

import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

object GeminiService {
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    private val JSON = "application/json; charset=utf-8".toMediaType()
    
    // Fallback key if BuildConfig is missing or empty (for demonstration)
    // The Secrets Gradle Plugin generates BuildConfig.GEMINI_API_KEY
    private val API_KEY = try {
        val key = BuildConfig::class.java.getField("GEMINI_API_KEY").get(null) as? String
        if (key.isNullOrEmpty() || key == "null") "" else key
    } catch (e: Exception) {
        ""
    }

    suspend fun explainThreat(appName: String, riskScore: Int, flagReason: String): String = withContext(Dispatchers.IO) {
        if (API_KEY.isEmpty()) {
            return@withContext "API Key missing. Please configure GEMINI_API_KEY in your local.properties or .env file to enable AI Threat Analysis."
        }

        val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=\$API_KEY"

        val prompt = "You are a mobile cybersecurity expert. Explain to a non-technical user why the Android app '\$appName' is considered a threat. It was flagged with a risk score of \$riskScore/100 for the following reason: '\$flagReason'. Keep the explanation under 4 sentences and be clear about the potential dangers."

        val jsonBody = JSONObject().apply {
            put("contents", JSONArray().apply {
                put(JSONObject().apply {
                    put("parts", JSONArray().apply {
                        put(JSONObject().apply {
                            put("text", prompt)
                        })
                    })
                })
            })
        }

        val request = Request.Builder()
            .url(url)
            .post(jsonBody.toString().toRequestBody(JSON))
            .build()

        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext "Error analyzing threat: HTTP \${response.code}"
                
                val responseBody = response.body?.string() ?: return@withContext "Empty response from AI"
                val jsonObject = JSONObject(responseBody)
                
                val candidates = jsonObject.optJSONArray("candidates")
                if (candidates != null && candidates.length() > 0) {
                    val content = candidates.getJSONObject(0).optJSONObject("content")
                    val parts = content?.optJSONArray("parts")
                    if (parts != null && parts.length() > 0) {
                        return@withContext parts.getJSONObject(0).optString("text")
                    }
                }
                return@withContext "Failed to parse AI response."
            }
        } catch (e: IOException) {
            return@withContext "Network error: Could not reach AI servers."
        } catch (e: Exception) {
            return@withContext "An unexpected error occurred during AI analysis."
        }
    }
}
