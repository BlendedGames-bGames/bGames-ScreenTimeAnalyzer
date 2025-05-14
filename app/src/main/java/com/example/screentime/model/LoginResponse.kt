package com.example.screentime.model

data class LoginResponse(
    val message: String?
)

data class User(
    val id_players: Int,
    val email: String,
    val name: String
)
