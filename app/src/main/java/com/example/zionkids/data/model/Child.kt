package com.example.zionkids.data.model

import androidx.annotation.Keep
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.google.firebase.Timestamp

@Keep
@Entity(
    tableName = "children",
    indices = [
        Index(value = ["graduated"]),
        Index(value = ["registrationStatus"]),
        Index(value = ["updatedAt"])
        // Add these back only if you include the columns:
        // Index(value = ["backgroundUpdatedAt"]),
        // Index(value = ["educationUpdatedAt"]),
        // Index(value = ["familyUpdatedAt"]),
        // Index(value = ["spiritualUpdatedAt"])
    ]
)
data class Child(
    @PrimaryKey val childId: String = "",

    // ===== Basic Info =====
    val profileImg: String = "",

    val fName: String = "",
    val lName: String = "",
    val oName: String = "",

    val age: Int = 0,

    val dob: Timestamp? = null,
    val dobVerified: Boolean = false,
    val gender: Gender = Gender.MALE,

    val street: String = "",

    val invitedBy: Individual = Individual.UNCLE,
    val invitedByIndividualId: String = "",
    val invitedByTypeOther: String = "",

    val educationPreference: EducationPreference = EducationPreference.NONE,

    // ===== Background Info =====
    val leftHomeDate: Timestamp? = null,
    val reasonLeftHome: String = "",
    val leaveStreetDate: Timestamp? = null,

    // ===== Education Info =====
    val lastClass: String = "",
    val previousSchool: String = "",
    val reasonLeftSchool: String = "",

    // ===== Family Resettlement =====
    val resettlementPreference: ResettlementPreference = ResettlementPreference.Home,
    val resettlementPreferenceOther: String = "",
    val resettled: Boolean = false,
    val resettlementDate: Timestamp? = null,

    val region: String = "",
    val district: String = "",
    val county: String = "",
    val subCounty: String = "",
    val parish: String = "",
    val village: String = "",

    // ===== Family Members 1 =====
    val memberFName1: String = "",
    val memberLName1: String = "",
    val relationship1: Relationship = Relationship.NONE,
    val telephone1a: String = "",
    val telephone1b: String = "",

    // ===== Family Members 2 =====
    val memberFName2: String = "",
    val memberLName2: String = "",
    val relationship2: Relationship = Relationship.NONE,
    val telephone2a: String = "",
    val telephone2b: String = "",

    // ===== Family Members 3 =====
    val memberFName3: String = "",
    val memberLName3: String = "",
    val relationship3: Relationship = Relationship.NONE,
    val telephone3a: String = "",
    val telephone3b: String = "",

    // ===== Spiritual Info =====
    val acceptedJesus: Reply = Reply.NO,
    val acceptedJesusDate: Timestamp? = null,
    val whoPrayed: Individual = Individual.UNCLE,
    val whoPrayedOther: String = "",
    val whoPrayedId: String = "",
    val outcome: String = "",
    val generalComments: String = "",
    val classGroup: ClassGroup = ClassGroup.SERGEANT,

    // ===== Program statuses =====
    val registrationStatus: RegistrationStatus = RegistrationStatus.BASICINFOR,
    val graduated: Reply = Reply.NO,

    // ===== Sponsorship/Family flags =====
    val sponsoredForEducation: Boolean = false,
    val sponsorId: String = "",
    val sponsorFName: String = "",
    val sponsorLName: String = "",
    val sponsorTelephone1: String = "",
    val sponsorTelephone2: String = "",
    val sponsorEmail: String = "",
    val sponsorNotes: String = "",

    // ===== Audit =====
    val createdAt: Timestamp = Timestamp.now(),
    val updatedAt: Timestamp = Timestamp.now()
) {
    fun fullName(): String =
        listOf(fName, oName, lName).map { it.trim() }.filter { it.isNotEmpty() }.joinToString(" ")

    fun hasPhone(): Boolean =
        listOf(telephone1a, telephone1b, telephone2a, telephone2b, telephone3a, telephone3b)
            .any { it.trim().isNotEmpty() }

    fun addressLine(): String =
        listOf(village, parish, subCounty, county, district, region)
            .map { it.trim() }.filter { it.isNotEmpty() }.joinToString(", ")

    fun registrationProgress(): Int = when (registrationStatus) {
        RegistrationStatus.BASICINFOR -> 1
        RegistrationStatus.BACKGROUND -> 2
        RegistrationStatus.EDUCATION -> 3
        RegistrationStatus.FAMILY -> 4
        RegistrationStatus.SPONSORSHIP -> 5
        RegistrationStatus.SPIRITUAL -> 6
        RegistrationStatus.COMPLETE -> 7
    }
}

enum class Individual { UNCLE, AUNTY, CHILD, OTHER }
enum class EducationPreference { SCHOOL, SKILLING, NONE }
enum class ResettlementPreference { Home, Phaneroo, Other }
enum class Reply { YES, NO }
enum class Relationship { NONE, PARENT, UNCLE, AUNTY, OTHER }

enum class ClassGroup {
    SERGEANT,
    LIEUTENANT,
    CAPTAIN,
    GENERAL,
}

enum class Gender { MALE, FEMALE }
enum class RegistrationStatus { BASICINFOR, BACKGROUND, EDUCATION, FAMILY,SPONSORSHIP, SPIRITUAL, COMPLETE }
