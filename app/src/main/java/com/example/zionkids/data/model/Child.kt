package com.example.zionkids.data.model

import androidx.annotation.Keep
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Keep
@Entity(tableName = "children", indices = [Index(value = ["childId"], unique = true)])
data class Child(
    // Basic Info
    // @DocumentId
    @PrimaryKey val childId: String = "",
    val profileImg: String = "",
    val fName: String = "",
    val lName: String = "",
    val oName: String? = null,
    val age: Int = 0,
    val street: String = "",
    val invitedBy: Individual = Individual.UNCLE,
    val invitedByType: String = "",
    val educationPreference: EducationPreference = EducationPreference.NONE,

    // Background Info
    val leftHomeDate: Long? = null,
    val reasonLeftHome: String? = null,
    val leftStreetDate: Long? = null,

    // Education Info
    val lastClass: String? = null,
    val previousSchool: String? = null,
    val reasonLeftSchool: String? = null,

    // Family Resettlement
    val homePreference: Reply = Reply.NO,
    val goHomeDate: Long? = null,
    val region: String? = null,
    val district: String? = null,
    val county: String? = null,
    val sCounty: String? = null,
    val parish: String? = null,
    val village: String? = null,

    // Family Members 1
    val memberFName1: String? = null,
    val memberLName1: String? = null,
    val relationShip1: RelationShip = RelationShip.NONE,
    val telephone1a: String? = null,
    val telephone1b: String? = null,

    // Family Members 2
    val memberFName2: String? = null,
    val memberLName2: String? = null,
    val relationShip2: RelationShip = RelationShip.NONE,
    val telephone2a: String? = null,
    val telephone2b: String? = null,

    // Family Members 3
    val memberFName3: String? = null,
    val memberLName3: String? = null,
    val relationShip3: RelationShip = RelationShip.NONE,
    val telephone3a: String? = null,
    val telephone3b: String? = null,

    // Spiritual Info
    val acceptedJesus: Reply = Reply.NO,
    val acceptedJesusDate: Long? = null,
    val whoPrayed: Individual = Individual.UNCLE,
    val outcome: String? = null,

    // Sync & Status
    val registrationStatus: RegistrationStatus = RegistrationStatus.BASICINFOR,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

 enum class Individual{
     UNCLE,
     CHILD,
     Other
 }
//can a child be sponsore to go for education while at reunited with the family
enum class EducationPreference{
    SCHOOL,
    SKILLING,
    NONE
}

enum class Reply{
    YES,
    NO
}

enum class RelationShip{
    NONE,
    PARENT,
    UNCLE,
    AUNTY,
    OTHER
}

enum class RegistrationStatus{
    BASICINFOR,
    BACKGROUND,
    EDUCATION,
    FAMILY,
    SPIRITUAL,
    COMPLETE
}