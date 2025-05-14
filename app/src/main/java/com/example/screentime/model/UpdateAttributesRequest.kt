package com.example.screentime.model

data class UpdateAttributesRequest(
    val id_player: Int,
    val id_attributes: List<Int>,
    val new_data: List<Int>
)