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
        Index(value = ["updatedAt"]),
        Index(value = ["backgroundUpdatedAt"]),
        Index(value = ["educationUpdatedAt"]),
        Index(value = ["familyUpdatedAt"]),
        Index(value = ["spiritualUpdatedAt"])
    ]
)
data class Child(
    @PrimaryKey val childId: String = "",

    // ===== Basic Info =====
    val profileImg: String = "",
    val profileImgUpdatedAt: Timestamp? = null,

    val fName: String = "",
    val lName: String = "",
    val oName: String? = null,
    val nameUpdatedAt: Timestamp? = null,

    val age: Int = 0,
    val ageUpdatedAt: Timestamp? = null,

    val dob: Timestamp? = null,
    val dobVerified: Boolean = false,
    val dobUpdatedAt: Timestamp? = null,

    val street: String = "",
    val streetUpdatedAt: Timestamp? = null,

    val invitedBy: Individual = Individual.UNCLE,
    val invitedByType: String = "",
    val invitedByUpdatedAt: Timestamp? = null,

    val educationPreference: EducationPreference = EducationPreference.NONE,
    val educationPreferenceUpdatedAt: Timestamp? = null,

    // ===== Background Info =====
    val leftHomeDate: Timestamp? = null,
    val leftHomeUpdatedAt: Timestamp? = null,

    val reasonLeftHome: String? = null,
    val reasonLeftHomeUpdatedAt: Timestamp? = null,

    val leftStreetDate: Timestamp? = null,
    val leftStreetUpdatedAt: Timestamp? = null,

    val backgroundUpdatedAt: Timestamp? = null,

    // ===== Education Info =====
    val lastClass: String? = null,
    val lastClassUpdatedAt: Timestamp? = null,

    val previousSchool: String? = null,
    val previousSchoolUpdatedAt: Timestamp? = null,

    val reasonLeftSchool: String? = null,
    val reasonLeftSchoolUpdatedAt: Timestamp? = null,

    val educationUpdatedAt: Timestamp? = null,

    // ===== Family Resettlement =====
    val homePreference: Reply = Reply.NO,
    val homePreferenceUpdatedAt: Timestamp? = null,

    val goHomeDate: Timestamp? = null,
    val goHomeUpdatedAt: Timestamp? = null,

    val region: String? = null,
    val district: String? = null,
    val county: String? = null,
    val subCounty: String? = null,
    val parish: String? = null,
    val village: String? = null,
    val locationUpdatedAt: Timestamp? = null,

    // ===== Family Members 1 =====
    val memberFName1: String? = null,
    val memberLName1: String? = null,
    val relationship1: Relationship = Relationship.NONE,
    val telephone1a: String? = null,
    val telephone1b: String? = null,
    val familyContact1UpdatedAt: Timestamp? = null,

    // ===== Family Members 2 =====
    val memberFName2: String? = null,
    val memberLName2: String? = null,
    val relationship2: Relationship = Relationship.NONE,
    val telephone2a: String? = null,
    val telephone2b: String? = null,
    val familyContact2UpdatedAt: Timestamp? = null,

    // ===== Family Members 3 =====
    val memberFName3: String? = null,
    val memberLName3: String? = null,
    val relationship3: Relationship = Relationship.NONE,
    val telephone3a: String? = null,
    val telephone3b: String? = null,
    val familyContact3UpdatedAt: Timestamp? = null,

    val familyUpdatedAt: Timestamp? = null,

    // ===== Spiritual Info =====
    val acceptedJesus: Reply = Reply.NO,
    val acceptedJesusUpdatedAt: Timestamp? = null,

    val acceptedJesusDate: Timestamp? = null,
    val whoPrayed: Individual = Individual.UNCLE,
    val outcome: String? = null,
    val spiritualNotesUpdatedAt: Timestamp? = null,

    val spiritualUpdatedAt: Timestamp? = null,

    // ===== Program statuses =====
    val registrationStatus: RegistrationStatus = RegistrationStatus.BASICINFOR,
    val registrationStatusUpdatedAt: Timestamp? = null,

    val graduated: Reply = Reply.NO,
    val graduatedUpdatedAt: Timestamp? = null,

    // ===== Sponsorship/Family flags =====
    val reunitedWithFamily: Boolean = false,
    val reunitedWithFamilyUpdatedAt: Timestamp? = null,

    val sponsoredForEducation: Boolean = false,
    val sponsorId: String? = null,
    val sponsorNotes: String? = null,
    val sponsorshipUpdatedAt: Timestamp? = null,

    // ===== Audit =====
    val createdAt: Timestamp = Timestamp.now(),
    val updatedAt: Timestamp = Timestamp.now()
)

enum class Individual { UNCLE, CHILD, OTHER }
enum class EducationPreference { SCHOOL, SKILLING, NONE }
enum class Reply { YES, NO }
enum class Relationship { NONE, PARENT, UNCLE, AUNTY, OTHER }
enum class RegistrationStatus { BASICINFOR, BACKGROUND, EDUCATION, FAMILY, SPIRITUAL, COMPLETE }

//
//@Keep
//@Entity(tableName = "children", indices = [Index(value = ["childId"], unique = true)])
//data class Child(
//    // Basic Info
//    // @DocumentId
//    @PrimaryKey val childId: String = "",
//    val profileImg: String = "",
//    val fName: String = "",
//    val lName: String = "",
//    val oName: String? = null,
//    val age: Int = 0,
//    val dob: Long = 0,
//    val dobVerified: Boolean = false,
//    val street: String = "",
//    val invitedBy: Individual = Individual.UNCLE,
//    val invitedByType: String = "",
//    val educationPreference: EducationPreference = EducationPreference.NONE,
//
//    // Background Info
//    val leftHomeDate: Long? = null,
//    val reasonLeftHome: String? = null,
//    val leftStreetDate: Long? = null,
//
//    // Education Info
//    val lastClass: String? = null,
//    val previousSchool: String? = null,
//    val reasonLeftSchool: String? = null,
//
//    // Family Resettlement
//    val homePreference: Reply = Reply.NO,
//    val goHomeDate: Long? = null,
//    val region: String? = null,
//    val district: String? = null,
//    val county: String? = null,
//    val sCounty: String? = null,
//    val parish: String? = null,
//    val village: String? = null,
//
//    // Family Members 1
//    val memberFName1: String? = null,
//    val memberLName1: String? = null,
//    val relationShip1: RelationShip = RelationShip.NONE,
//    val telephone1a: String? = null,
//    val telephone1b: String? = null,
//
//    // Family Members 2
//    val memberFName2: String? = null,
//    val memberLName2: String? = null,
//    val relationShip2: RelationShip = RelationShip.NONE,
//    val telephone2a: String? = null,
//    val telephone2b: String? = null,
//
//    // Family Members 3
//    val memberFName3: String? = null,
//    val memberLName3: String? = null,
//    val relationShip3: RelationShip = RelationShip.NONE,
//    val telephone3a: String? = null,
//    val telephone3b: String? = null,
//
//    // Spiritual Info
//    val acceptedJesus: Reply = Reply.NO,
//    val acceptedJesusDate: Long? = null,
//    val whoPrayed: Individual = Individual.UNCLE,
//    val outcome: String? = null,
//
//    // Sync & Status
//    val registrationStatus: RegistrationStatus = RegistrationStatus.BASICINFOR,
//    val graduated: Reply = Reply.NO,
//    val createdAt: Long = System.currentTimeMillis(),
//    val updatedAt: Long = System.currentTimeMillis()
//)
//
// enum class Individual{
//     UNCLE,
//     CHILD,
//     Other
// }
////can a child be sponsore to go for education while at reunited with the family
//enum class EducationPreference{
//    SCHOOL,
//    SKILLING,
//    NONE
//}
//
//enum class Reply{
//    YES,
//    NO
//}
//
//enum class RelationShip{
//    NONE,
//    PARENT,
//    UNCLE,
//    AUNTY,
//    OTHER
//}
//
//enum class RegistrationStatus{
//    BASICINFOR,
//    BACKGROUND,
//    EDUCATION,
//    FAMILY,
//    SPIRITUAL,
//    COMPLETE
//}