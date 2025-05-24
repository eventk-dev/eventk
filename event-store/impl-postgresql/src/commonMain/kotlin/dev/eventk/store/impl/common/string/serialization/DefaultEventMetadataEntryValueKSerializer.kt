package dev.eventk.store.impl.common.string.serialization

import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.JsonContentPolymorphicSerializer
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.serializer

internal object DefaultEventMetadataEntryValueKSerializer : JsonContentPolymorphicSerializer<Any>(Any::class) {
    private val integerRegex = Regex("""-?\d+""")
    private val booleanSerializer = serializer<Boolean>()
    private val doubleSerializer = serializer<Double>()
    private val intSerializer = serializer<Int>()
    private val longSerializer = serializer<Long>()

    override fun selectDeserializer(element: JsonElement): KSerializer<out Comparable<*>> {
        if (element !is JsonPrimitive) throw IllegalStateException("unsupported json element type: ${element::class}")
        if (element.isString) return serializer<String>()

        return when {
            element.content == "true" -> booleanSerializer
            element.content == "false" -> booleanSerializer
            element.content.contains('.') -> doubleSerializer
            else -> {
                val integerMatch = integerRegex.matchEntire(element.content)
                if (integerMatch != null) {
                    val length = integerMatch.value.length - if (integerMatch.value[0] == '-') 1 else 0
                    // assuming any number with more than 9 digits in decimal is a long
                    if (length < 10) intSerializer else longSerializer
                } else {
                    throw IllegalStateException("unsupported json primitive content: ${element.content}")
                }
            }
        }
    }
}
