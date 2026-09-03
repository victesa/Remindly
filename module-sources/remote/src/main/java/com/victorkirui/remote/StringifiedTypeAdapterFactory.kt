package com.victorkirui.remote

import android.util.Log
import com.google.gson.Gson
import com.google.gson.JsonElement
import com.google.gson.JsonParser
import com.google.gson.TypeAdapter
import com.google.gson.TypeAdapterFactory
import com.google.gson.internal.Primitives
import com.google.gson.reflect.TypeToken
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonToken
import com.google.gson.stream.JsonWriter

class StringifiedTypeAdapterFactory : TypeAdapterFactory {
    override fun <T> create(gson: Gson, type: TypeToken<T>): TypeAdapter<T>? {
        val rawType = type.rawType

        if (Primitives.isWrapperType(rawType) ||
            rawType == String::class.java ||
            Number::class.java.isAssignableFrom(rawType) ||
            Boolean::class.java.isAssignableFrom(rawType) ||
            JsonElement::class.java.isAssignableFrom(rawType)
        ) {
            return null
        }

        val delegate = gson.getDelegateAdapter(this, type)

        return object : TypeAdapter<T>() {
            override fun write(out: JsonWriter, value: T?) {
                delegate.write(out, value)
            }

            override fun read(reader: JsonReader): T? {
                if (reader.peek() == JsonToken.NULL) {
                    reader.nextNull()
                    return null
                }

                if (reader.peek() == JsonToken.STRING) {
                    val stringValue = reader.nextString()
                    if (stringValue.isBlank()) return null
                    try {
                        val stringReader = JsonReader(java.io.StringReader(stringValue))
                        stringReader.isLenient = true
                        @Suppress("DEPRECATION")
                        val parsedElement = JsonParser().parse(stringReader)
                        if (parsedElement.isJsonObject || parsedElement.isJsonArray) {
                            return delegate.fromJsonTree(parsedElement)
                        }
                    } catch (e: Exception) {
                        Log.w("StringifiedAdapter", "Failed to parse stringified JSON for ${rawType.simpleName}: $stringValue", e)
                    }
                    return null
                }

                return delegate.read(reader)
            }
        }
    }
}
