package com.binkes.camerakfacedetectorkizito

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform