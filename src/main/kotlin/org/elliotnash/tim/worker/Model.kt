package org.elliotnash.tim.worker

import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonContentPolymorphicSerializer
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.jsonObject
import org.elliotnash.tim.PageSize
import org.elliotnash.tim.Theme
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

@Serializable(RequestSerializer::class)
interface Request

@Serializable
data class Render(
    @SerialName("Render")
    val render: RenderRequest
) : Request

@Serializable
data class RenderRequest(
    val code: String,
    val options: RenderOptions
)

@Serializable
data class RenderOptions(
    @SerialName("page_size")
    val pageSize: PageSize,
    val theme: Theme,
    val transparent: Boolean
)

@Serializable
class VersionRequest private constructor(
    @SerialName("Version")
    val version: List<Int>
) : Request {
    constructor() : this(listOf())
}

@Serializable(ResponseSerializer::class)
interface Response

@Serializable
data class RenderSuccess(
    @SerialName("RenderSuccess")
    @Serializable(Base64Serializer::class)
    val renderSuccess: ByteArray
) : Response {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as RenderSuccess

        return renderSuccess.contentEquals(other.renderSuccess)
    }

    override fun hashCode(): Int {
        return renderSuccess.contentHashCode()
    }
}

@Serializable
data class RenderError(
    @SerialName("RenderError")
    val renderError: String
) : Response

@Serializable
data class VersionResponse(
    @SerialName("Version")
    val version: Version
) : Response

@Serializable
data class Version(
    val version: String,
    @SerialName("git_hash")
    val gitHash: String,
)

@OptIn(ExperimentalEncodingApi::class)
object Base64Serializer: KSerializer<ByteArray> {
    override val descriptor = PrimitiveSerialDescriptor("Base64ByteArray", PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): ByteArray =
        Base64.decode(decoder.decodeString())

    override fun serialize(encoder: Encoder, value: ByteArray) =
        encoder.encodeString(Base64.encode(value))
}

object ResponseSerializer : JsonContentPolymorphicSerializer<Response>(Response::class) {
    override fun selectDeserializer(element: JsonElement) = when {
        "RenderSuccess" in element.jsonObject -> RenderSuccess.serializer()
        "RenderError" in element.jsonObject -> RenderError.serializer()
        else -> VersionResponse.serializer()
    }
}

object RequestSerializer : JsonContentPolymorphicSerializer<Request>(Request::class) {
    override fun selectDeserializer(element: JsonElement) = when {
        "Render" in element.jsonObject -> Render.serializer()
        else -> VersionRequest.serializer()
    }
}
