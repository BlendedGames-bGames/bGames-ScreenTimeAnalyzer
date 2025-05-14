package com.example.screentime.network

import com.example.screentime.model.PlayerAttribute
import com.example.screentime.model.UpdateAttributesRequest
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Body
import retrofit2.http.PUT

interface ApiService {
    @GET("player_by_email/{email}")
    suspend fun getPlayerByEmail(@Path("email") email: String): Response<ResponseBody>

    @GET("player/{username}/{password}")
    suspend fun verifyCredentials(
        @Path("username") username: String,
        @Path("password") password: String
    ): Response<ResponseBody>

    @PUT("player_attributes")
    suspend fun updatePlayerAttributes(
        @Body request: UpdateAttributesRequest
    ): Response<ResponseBody>

    @GET("player_all_attributes/{id}")
    suspend fun getPlayerAllAttributes(
        @Path("id") id: Int
    ): Response<List<PlayerAttribute>>

}
