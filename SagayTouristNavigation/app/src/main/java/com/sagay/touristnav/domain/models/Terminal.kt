package com.sagay.touristnav.domain.models

data class Terminal(
    val id: String,
    val name: String,
    val lat: Double,
    val lng: Double,
    val colorHex: String,
    val colorName: String,
    val serviceArea: String,
)
