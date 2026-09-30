package com.example.data.remote

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

@JsonClass(generateAdapter = true)
data class NwsAlertsResponse(
    val features: List<NwsAlertFeature>?
)

@JsonClass(generateAdapter = true)
data class NwsAlertFeature(
    val id: String?,
    val properties: NwsAlertProperties?,
    val geometry: NwsGeometry?
)

@JsonClass(generateAdapter = true)
data class NwsAlertProperties(
    val id: String?,
    val areaDesc: String?,
    val event: String?,
    val headline: String?,
    val description: String?,
    val instruction: String?,
    val severity: String?,
    val urgency: String?,
    val certainty: String?,
    val effective: String?,
    val expires: String?
)

@JsonClass(generateAdapter = true)
data class NwsGeometry(
    val type: String?,
    val coordinates: Any?
)

interface WeatherGovApi {
    @GET("alerts/active")
    suspend fun getActiveAlerts(
        @Query("point") point: String? = null,
        @Query("status") status: String = "actual",
        @Query("message_type") messageType: String = "alert",
        @Query("limit") limit: Int = 20
    ): NwsAlertsResponse

    companion object {
        private const val BASE_URL = "https://api.weather.gov/"

        fun create(): WeatherGovApi {
            val userAgentInterceptor = Interceptor { chain ->
                val request = chain.request().newBuilder()
                    .header("User-Agent", "StormRadar-Android/1.0 (weather@aistudio.internal)")
                    .header("Accept", "application/geo+json")
                    .build()
                chain.proceed(request)
            }

            val client = OkHttpClient.Builder()
                .addInterceptor(userAgentInterceptor)
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(10, TimeUnit.SECONDS)
                .build()

            val moshi = Moshi.Builder()
                .add(KotlinJsonAdapterFactory())
                .build()

            return Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(client)
                .addConverterFactory(MoshiConverterFactory.create(moshi))
                .build()
                .create(WeatherGovApi::class.java)
        }
    }
}
