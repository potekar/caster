package com.example.caster

import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import java.io.File

data class ImgbbUploadResponse(
    val success: Boolean?,
    val data: ImgbbData?
)

data class ImgbbData(
    val url: String?,
    val display_url: String?
)

interface ImgbbApi {
    @Multipart
    @POST("1/upload")
    fun uploadImage(
        @Part("key") key: RequestBody,
        @Part image: MultipartBody.Part
    ): Call<ImgbbUploadResponse>
}

object ImgbbServiceFactory {
    fun create(): ImgbbApi {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        }
        val client = OkHttpClient.Builder()
            .addInterceptor(logging)
            .build()

        val retrofit = Retrofit.Builder()
            .baseUrl("https://api.imgbb.com/")
            .addConverterFactory(GsonConverterFactory.create())
            .client(client)
            .build()
        return retrofit.create(ImgbbApi::class.java)
    }
}

object ImgbbUploader {
    private val api: ImgbbApi by lazy { ImgbbServiceFactory.create() }

    fun upload(file: File, apiKey: String, onSuccess: (String) -> Unit, onError: (Throwable) -> Unit) {
        try {
            val keyBody = apiKey.toRequestBody("text/plain".toMediaTypeOrNull())
            val requestBody = file.asRequestBody("image/*".toMediaTypeOrNull())
            val body = MultipartBody.Part.createFormData("image", file.name, requestBody)
            val call = api.uploadImage(keyBody, body)
            call.enqueue(object : retrofit2.Callback<ImgbbUploadResponse> {
                override fun onResponse(
                    call: Call<ImgbbUploadResponse>,
                    response: retrofit2.Response<ImgbbUploadResponse>
                ) {
                    if (response.isSuccessful) {
                        val link = response.body()?.data?.display_url ?: response.body()?.data?.url
                        if (!link.isNullOrEmpty()) {
                            onSuccess(link)
                        } else {
                            onError(IllegalStateException("IMGBB response missing link"))
                        }
                    } else {
                        onError(IllegalStateException("IMGBB error: ${response.code()}"))
                    }
                }

                override fun onFailure(call: Call<ImgbbUploadResponse>, t: Throwable) {
                    onError(t)
                }
            })
        } catch (e: Exception) {
            onError(e)
        }
    }
}


