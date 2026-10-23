package com.plcoding.cardiganapp

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform