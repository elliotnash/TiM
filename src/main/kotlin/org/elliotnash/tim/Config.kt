package org.elliotnash.tim

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

const val DEFAULT_POOL_SIZE = 5
const val DEFAULT_PREFIX = "!"

@Serializable
data class Config(
    @SerialName("bb_url")
    val url: String,
    @SerialName("bb_password")
    val password: String,
    @SerialName("pool_size")
    val poolSize: Int = DEFAULT_POOL_SIZE,
    val prefix: String = DEFAULT_PREFIX
)
