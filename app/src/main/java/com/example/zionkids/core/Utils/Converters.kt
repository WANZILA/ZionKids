package com.example.zionkids.core.Utils

import androidx.room.TypeConverter
import com.example.zionkids.data.model.EducationPreference
import com.example.zionkids.data.model.Individual
import com.example.zionkids.data.model.Person
import com.example.zionkids.data.model.RegistrationStatus
import com.example.zionkids.data.model.RelationShip
import com.example.zionkids.data.model.Reply

class Converters {
    @TypeConverter
    fun fromEducationPreference(value: EducationPreference): String = value.name

    @TypeConverter
    fun toEducationPreference(value: String): EducationPreference =
        EducationPreference.valueOf(value)

    @TypeConverter
    fun fromReply(value: Reply): String = value.name

    @TypeConverter
    fun toReply(value: String): Reply = Reply.valueOf(value)

    @TypeConverter
    fun fromRelationShip(value: RelationShip): String = value.name

    @TypeConverter
    fun toRelationShip(value: String): RelationShip = RelationShip.valueOf(value)

    @TypeConverter
    fun fromPerson(value: Individual): String = value.name

    @TypeConverter
    fun toPerson(value: String): Individual = Individual.valueOf(value)

    @TypeConverter
    fun fromRegistrationStatus(value: RegistrationStatus): String = value.name

    @TypeConverter
    fun toRegistrationStatus(value: String): RegistrationStatus =
        RegistrationStatus.valueOf(value)
}
