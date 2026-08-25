package com.binkes.camerakfacedetectorkizito.face

import kotlinx.cinterop.ExperimentalForeignApi
import platform.CoreMedia.CMSampleBufferRef


@OptIn(ExperimentalForeignApi::class)
actual class ImageFrame(val sampleBuffer: CMSampleBufferRef)
