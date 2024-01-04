package com.mybrickhub.brickpicker.utility

import android.annotation.SuppressLint
import android.util.Base64
import android.util.Log
import com.mybrickhub.brickpicker.MyApplication
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException
import java.io.UnsupportedEncodingException
import java.net.URLEncoder
import java.security.SecureRandom
import java.text.SimpleDateFormat
import java.util.Date
import java.util.TimeZone
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

object Api {

    private fun generateNonce(): String {
        val nonceLength = 32
        val characters = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz"
        val random = SecureRandom()
        return (1..nonceLength)
            .map { characters[random.nextInt(characters.length)] }
            .joinToString("")
    }

    private fun encode(value: String): String {
        return try {
            URLEncoder.encode(value, "UTF-8")
        } catch (e: UnsupportedEncodingException) {
            throw RuntimeException("Error encoding value: $value", e)
        }
    }

    fun apiCall(apiMethod: String, apiUrl: String, apiBody: JSONObject?, callback: ApiCallback) {

        val sharedPreferences = MyApplication.sharedPreferences

        // Api daily reset
        val lastResetDate = sharedPreferences.getLong(Settings.KEY_API_COUNT_LAST_RESET, 0)
        if (!isSameDay(lastResetDate, System.currentTimeMillis())) {
            sharedPreferences.edit()
                .putInt(Settings.KEY_API_COUNTS, 0)
                .putLong(Settings.KEY_API_COUNT_LAST_RESET, System.currentTimeMillis())
                .putBoolean(Settings.KEY_API_LIMIT_WARNING_NOTICED, false)
                .apply()
        }

        // Api counter
        var loadedInt = sharedPreferences.getInt(Settings.KEY_API_COUNTS, -1)
        if (loadedInt >= 0) {
            loadedInt++
            sharedPreferences.edit().putInt(Settings.KEY_API_COUNTS, loadedInt).apply()
        }

        val oauthParameters = mapOf(
            "oauth_signature_method" to "HMAC-SHA1",
            "oauth_timestamp" to System.currentTimeMillis() / 1000L,
            "oauth_nonce" to generateNonce(),
            "oauth_version" to "1.0"
        )

        val oauthConsumerKey = sharedPreferences.getString(Settings.KEY_API_CONSUMER_KEY, "").toString()
        val oauthConsumerSecret = sharedPreferences.getString(Settings.KEY_API_CONSUMER_SECRET, "").toString()
        val oauthTokenValue = sharedPreferences.getString(Settings.KEY_API_TOKEN, "").toString()
        val oauthTokenSecret = sharedPreferences.getString(Settings.KEY_API_SECRET, "").toString()

        val client = OkHttpClient()

        val oauthNonce = oauthParameters["oauth_nonce"]
        val oauthTimestamp = oauthParameters["oauth_timestamp"]
        val oauthSignature = generateOAuthSignature(
            method = apiMethod,
            apiUrl = apiUrl,
            oauthParameters = oauthParameters,
            oauthConsumerKey = oauthConsumerKey,
            oauthConsumerSecret = oauthConsumerSecret,
            oauthTokenValue = oauthTokenValue,
            oauthTokenSecret = oauthTokenSecret
        )

        val requestBuilder = Request.Builder()
            .url(apiUrl)
            .addHeader("Authorization", "OAuth oauth_consumer_key=\"${oauthConsumerKey}\",oauth_token=\"${oauthTokenValue}\",oauth_signature_method=\"HMAC-SHA1\",oauth_timestamp=\"${oauthTimestamp}\",oauth_nonce=\"${oauthNonce}\",oauth_version=\"1.0\",oauth_signature=\"${oauthSignature}\"")

        if (apiMethod.equals("GET", ignoreCase = false)) {
            requestBuilder
                .get()
            }

        when (apiMethod) {
            "GET" -> requestBuilder.get()
            "POST" -> {
                val mediaType = "application/json"
                if (apiBody != null) {
                    val body = apiBody.toString()
                    val requestBody = body.toByteArray(Charsets.US_ASCII).toRequestBody(mediaType.toMediaTypeOrNull())
                    requestBuilder
                        .post(requestBody)
                        .addHeader("Content-Type", mediaType)
                } else {
                    requestBuilder
                        .post("".toRequestBody(null))
                        .addHeader("Content-Type", mediaType)
                }
            }
            "PUT" -> {
                val mediaType = "application/json"
                if (apiBody != null) {
                    val body = apiBody.toString()
                    val requestBody = body.toByteArray(Charsets.US_ASCII).toRequestBody(mediaType.toMediaTypeOrNull())
                    requestBuilder
                        .put(requestBody)
                        .addHeader("Content-Type", mediaType)
                } else {
                    requestBuilder
                        .put("".toRequestBody(null))
                        .addHeader("Content-Type", mediaType)
                }
            }
        }

        val request = requestBuilder.build()

        client.newCall(request).enqueue(object : Callback {
            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    val responseBody = response.body?.string()
                    if (responseBody != null) {
                        //Log.d("MeineKlasse", responseBody)
                        try {
                            val jsonResponse = JSONObject(responseBody)
                            val metaJSONArray = jsonResponse.getJSONObject("meta")

                            when (val statusCode = metaJSONArray.getInt("code")) {
                                in 200..299 -> {
                                    val data = jsonResponse.opt("data")
                                    if (data != null) {
                                        val decodedData = decodeEntities(data.toString())
                                        callback.onSuccess(decodedData)
                                    } else {
                                        callback.onSuccess(null)
                                    }
                                }
                                else -> {
                                    // Rufe die onFailure-Methode des Callbacks auf
                                    val errorMessage = metaJSONArray.getString("message")
                                    val errorDescription = metaJSONArray.getString("description")
                                    callback.onFailure("$statusCode: $errorMessage $errorDescription")
                                }
                            }
                        } catch (e: JSONException) {
                            e.printStackTrace()
                            // Handle JSON parsing error
                            // Rufe die onFailure-Methode des Callbacks auf
                            callback.onFailure("JSON parsing error")
                        }

                    }
                } else {
                    Log.d("MeineKlasse", "Fehler")
                    // Rufe die onFailure-Methode des Callbacks auf
                    callback.onFailure("API error: ${response.code}")
                }
            }

            override fun onFailure(call: Call, e: IOException) {
                // Handle failure
                e.printStackTrace()
                Log.d("MeineKlasse", "Fehler2")
                // Rufe die onFailure-Methode des Callbacks auf
                callback.onFailure("Network error")
            }
        })
    }

    private fun generateOAuthSignature(
        method: String,
        apiUrl: String,
        oauthParameters: Map<String, Any>,
        oauthConsumerKey: String,
        oauthConsumerSecret: String,
        oauthTokenValue: String,
        oauthTokenSecret: String
    ): String {
        val encodedParams = oauthParameters
            .plus("oauth_consumer_key" to oauthConsumerKey)
            .plus("oauth_token" to oauthTokenValue)
            .toSortedMap()
            .map { "${it.key}=${it.value}" }
            .joinToString("&")

        val query: String?
        val baseUrl: String?
        var queryBefore = StringBuilder()
        var queryAfter = StringBuilder()

        if (apiUrl.contains("?")) {
            query = apiUrl.split("?")[1] + "&"
            baseUrl = apiUrl.split("?")[0]

            if (query.contains("&")) {
                val queryArray = query.split("&")
                for (queryParam in queryArray) {
                    if (queryParam < "oauth_signature") {
                        queryBefore.append("&").append(queryParam)
                    } else {
                        queryAfter.append("&").append(queryParam)
                    }
                }
            }

            queryBefore = StringBuilder(queryBefore.removePrefix("&"))
            // Sortiere die Parameter innerhalb von queryBefore und queryAfter
            queryBefore = StringBuilder(queryBefore.split("&").sorted().joinToString("&") + "&")
            queryAfter = StringBuilder(queryAfter.split("&").sorted().joinToString("&"))
            queryBefore = StringBuilder(queryBefore.removePrefix("&"))

        } else {
            queryBefore = StringBuilder()
            queryAfter = StringBuilder()
            baseUrl = apiUrl
        }

        val signatureBaseString =
            "${method}&${encode(baseUrl)}&${encode(queryBefore.toString())}${encode(encodedParams)}${
                encode(
                    queryAfter.toString()
                )
            }"
        val signingKey = "${oauthConsumerSecret}&${oauthTokenSecret}"

        // Initialisiere die HMAC-SHA1-Signatur
        val hmac = Mac.getInstance("HmacSHA1")
        val keySpec = SecretKeySpec(signingKey.toByteArray(), "HmacSHA1")
        hmac.init(keySpec)

        // Berechne die Signatur
        val signatureBytes = hmac.doFinal(signatureBaseString.toByteArray())
        return encode(Base64.encodeToString(signatureBytes, Base64.NO_WRAP)) //signature
    }

    fun decodeEntities(encodedString: String): String {
        val translateRe = Regex("&(nbsp|amp|quot|lt|gt);")
        val translate = mapOf(
            "nbsp" to " ",
            "amp" to "&",
            "quot" to "\"",
            "lt" to "<",
            "gt" to ">"
        )

        val result = translateRe.replace(encodedString) { matchResult ->
            val entity = matchResult.groupValues[1]
            translate[entity] ?: ""
        }

        return Regex("&#(\\d+);").replace(result) { matchResult ->
            val numStr = matchResult.groupValues[1]
            val num = numStr.toIntOrNull() ?: 0
            num.toChar().toString()
        }
    }

    @SuppressLint("SimpleDateFormat")
    private fun isSameDay(time1: Long, time2: Long): Boolean {
        val sdf = SimpleDateFormat("yyyy-MM-dd")
        sdf.timeZone = TimeZone.getTimeZone("UTC")
        val date1 = sdf.format(Date(time1))
        val date2 = sdf.format(Date(time2))
        return date1 == date2
    }

    interface ApiCallback {
        fun onSuccess(data: Any?)
        fun onFailure(error: String)
    }
}