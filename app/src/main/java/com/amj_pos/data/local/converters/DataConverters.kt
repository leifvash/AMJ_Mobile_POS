package com.amj_pos.data.local.converters

import androidx.room.TypeConverter
import com.amj_pos.data.local.entities.PaymentMethod
import com.amj_pos.data.local.entities.UtangType

class DataConverters {
    @TypeConverter
    fun fromPaymentMethod(value: PaymentMethod): String = value.name

    @TypeConverter
    fun toPaymentMethod(value: String): PaymentMethod = PaymentMethod.valueOf(value)

    @TypeConverter
    fun fromUtangType(value: UtangType): String = value.name

    @TypeConverter
    fun toUtangType(value: String): UtangType = UtangType.valueOf(value)
}
