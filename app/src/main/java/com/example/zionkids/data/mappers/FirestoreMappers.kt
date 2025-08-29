package com.example.zionkids.data.mappers

import com.example.zionkids.data.model.*
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.QuerySnapshot

// ---------- Enum converters with safe defaults (no reified) ----------
private fun String?.toReply(): Reply =
    runCatching { if (this == null) Reply.NO else Reply.valueOf(this) }
        .getOrDefault(Reply.NO)

private fun String?.toIndividual(): Individual =
    runCatching { if (this == null) Individual.UNCLE else Individual.valueOf(this) }
        .getOrDefault(Individual.UNCLE)

private fun String?.toEducationPref(): EducationPreference =
    runCatching { if (this == null) EducationPreference.NONE else EducationPreference.valueOf(this) }
        .getOrDefault(EducationPreference.NONE)

private fun String?.toRelationship(): Relationship =
    runCatching { if (this == null) Relationship.NONE else Relationship.valueOf(this) }
        .getOrDefault(Relationship.NONE)

private fun String?.toRegistrationStatus(): RegistrationStatus =
    runCatching { if (this == null) RegistrationStatus.BASICINFOR else RegistrationStatus.valueOf(this) }
        .getOrDefault(RegistrationStatus.BASICINFOR)

private fun String?.toEventStatus(): EventStatus =
    runCatching { if (this == null) EventStatus.SCHEDULED else EventStatus.valueOf(this) }
        .getOrDefault(EventStatus.SCHEDULED)

// ---------- Safe getters ----------
private fun DocumentSnapshot.str(key: String): String? = this.getString(key)
private fun DocumentSnapshot.lng(key: String): Long?   = this.getLong(key)
private fun DocumentSnapshot.intPos(key: String): Int? = this.getLong(key)?.toInt()

// ---------- One-doc -> Child ----------
fun DocumentSnapshot.toChildOrNull(): Child? {
    val data = this.data ?: return null

    return Child(
        // ids
        childId = str("childId") ?: id,

        // basic
        profileImg = str("profileImg") ?: "",
        fName = str("fName") ?: "",
        lName = str("lName") ?: "",
        oName = str("oName"),
        age = intPos("age") ?: 0,
        dob = lng("dob") ?: 0L,
        dobVerified = (this.getBoolean("dobVerified") ?: false),
        street = str("street") ?: "",

        // invited / edu
        invitedBy = (str("invitedBy")).toIndividual(),
        invitedByType = str("invitedByType") ?: "",
        educationPreference = (str("educationPreference")).toEducationPref(),

        // background
        leftHomeDate = lng("leftHomeDate"),
        reasonLeftHome = str("reasonLeftHome"),
        leftStreetDate = lng("leftStreetDate"),

        // education
        lastClass = str("lastClass"),
        previousSchool = str("previousSchool"),
        reasonLeftSchool = str("reasonLeftSchool"),

        // family resettlement
        homePreference = (str("homePreference")).toReply(),
        goHomeDate = lng("goHomeDate"),
        region = str("region"),
        district = str("district"),
        county = str("county"),
        subCounty = str("subCounty") ?: str("sCounty"), // fallback for old docs
        parish = str("parish"),
        village = str("village"),

        // family members
        memberFName1 = str("memberFName1"),
        memberLName1 = str("memberLName1"),
        relationship1 = (str("relationShip1") ?: str("relationship1")).toRelationship(),
        telephone1a = str("telephone1a"),
        telephone1b = str("telephone1b"),

        memberFName2 = str("memberFName2"),
        memberLName2 = str("memberLName2"),
        relationship2 = (str("relationShip2") ?: str("relationship2")).toRelationship(),
        telephone2a = str("telephone2a"),
        telephone2b = str("telephone2b"),

        memberFName3 = str("memberFName3"),
        memberLName3 = str("memberLName3"),
        relationship3 = (str("relationShip3") ?: str("relationship3")).toRelationship(),
        telephone3a = str("telephone3a"),
        telephone3b = str("telephone3b"),

        // spiritual
        acceptedJesus = (str("acceptedJesus")).toReply(),
        acceptedJesusDate = lng("acceptedJesusDate"),
        whoPrayed = (str("whoPrayed")).toIndividual(),
        outcome = str("outcome"),

        // program flags
        reunitedWithFamily = this.getBoolean("reunitedWithFamily") ?: false,
        sponsoredForEducation = this.getBoolean("sponsoredForEducation") ?: false,
        sponsorId = str("sponsorId"),
        sponsorNotes = str("sponsorNotes"),

        // status
        registrationStatus = (str("registrationStatus")).toRegistrationStatus(),
        graduated = (str("graduated")).toReply(),

        // times (stored as millis in your model)
        createdAt = lng("createdAt") ?: System.currentTimeMillis(),
        updatedAt = lng("updatedAt") ?: System.currentTimeMillis()
    )
}

// ---------- Multi-doc -> List<Child> ----------
fun QuerySnapshot.toChildren(): List<Child> =
    this.documents.mapNotNull { it.toChildOrNull() }

// ---------- Multi-doc -> List<Event> (with safe eventStatus) ----------
fun QuerySnapshot.toEvents(): List<Event> =
    this.documents.mapNotNull { doc ->
        val e = doc.toObject(Event::class.java)?.copy(eventId = doc.id) ?: return@mapNotNull null
        val status = (doc.getString("eventStatus")).toEventStatus()
        e.copy(eventStatus = status)
    }

// ---------- Child -> Firestore maps ----------

/** Build a full Firestore map from a Child (for create or replace). */
fun Child.toFirestoreMapFull(): Map<String, Any> = buildMap {
    // ids
    put("childId", childId)

    // basic
    put("profileImg", profileImg)
    put("fName", fName)
    put("lName", lName)
    oName?.let { put("oName", it) }
    if (age > 0) put("age", age)
    put("dob", dob)
    put("dobVerified", dobVerified)
    put("street", street)

    // enums as strings
    put("invitedBy", invitedBy.name)
    put("invitedByType", invitedByType)
    put("educationPreference", educationPreference.name)

    // background
    leftHomeDate?.let { put("leftHomeDate", it) }
    reasonLeftHome?.let { put("reasonLeftHome", it) }
    leftStreetDate?.let { put("leftStreetDate", it) }

    // education
    lastClass?.let { put("lastClass", it) }
    previousSchool?.let { put("previousSchool", it) }
    reasonLeftSchool?.let { put("reasonLeftSchool", it) }

    // resettlement
    put("homePreference", homePreference.name)
    goHomeDate?.let { put("goHomeDate", it) }
    region?.let { put("region", it) }
    district?.let { put("district", it) }
    county?.let { put("county", it) }
    subCounty?.let { put("subCounty", it) } // use new field name
    parish?.let { put("parish", it) }
    village?.let { put("village", it) }

    // family 1
    memberFName1?.let { put("memberFName1", it) }
    memberLName1?.let { put("memberLName1", it) }
    put("relationship1", relationship1.name)
    telephone1a?.let { put("telephone1a", it) }
    telephone1b?.let { put("telephone1b", it) }

    // family 2
    memberFName2?.let { put("memberFName2", it) }
    memberLName2?.let { put("memberLName2", it) }
    put("relationship2", relationship2.name)
    telephone2a?.let { put("telephone2a", it) }
    telephone2b?.let { put("telephone2b", it) }

    // family 3
    memberFName3?.let { put("memberFName3", it) }
    memberLName3?.let { put("memberLName3", it) }
    put("relationship3", relationship3.name)
    telephone3a?.let { put("telephone3a", it) }
    telephone3b?.let { put("telephone3b", it) }

    // spiritual
    put("acceptedJesus", acceptedJesus.name)
    acceptedJesusDate?.let { put("acceptedJesusDate", it) }
    put("whoPrayed", whoPrayed.name)
    outcome?.let { put("outcome", it) }

    // program flags
    put("reunitedWithFamily", reunitedWithFamily)
    put("sponsoredForEducation", sponsoredForEducation)
    sponsorId?.let { put("sponsorId", it) }
    sponsorNotes?.let { put("sponsorNotes", it) }

    // status
    put("registrationStatus", registrationStatus.name)
    put("graduated", graduated.name)

    // timestamps (millis, matches your model)
    put("createdAt", createdAt)
    put("updatedAt", updatedAt)
}

/** Build a PATCH map (only meaningful fields) for merge updates. */
fun Child.toFirestoreMapPatch(): Map<String, Any> = buildMap {
    put("childId", childId)

    fun putIfNotBlank(key: String, v: String?) { if (!v.isNullOrBlank()) put(key, v) }
    fun putIfNotNull(key: String, v: Any?) { if (v != null) put(key, v) }

    // basic
    putIfNotBlank("profileImg", profileImg)
    putIfNotBlank("fName", fName)
    putIfNotBlank("lName", lName)
    putIfNotNull("oName", oName)
    if (age > 0) put("age", age)
    put("dobVerified", dobVerified)
    putIfNotNull("dob", if (dob != 0L) dob else null)
    putIfNotBlank("street", street)

    // enums
    put("invitedBy", invitedBy.name)
    putIfNotBlank("invitedByType", invitedByType)
    put("educationPreference", educationPreference.name)

    // dates
    putIfNotNull("leftHomeDate", leftHomeDate)
    putIfNotNull("leftStreetDate", leftStreetDate)
    putIfNotNull("goHomeDate", goHomeDate)

    // education
    putIfNotNull("lastClass", lastClass)
    putIfNotNull("previousSchool", previousSchool)
    putIfNotNull("reasonLeftSchool", reasonLeftSchool)

    // location (new field names)
    putIfNotNull("region", region)
    putIfNotNull("district", district)
    putIfNotNull("county", county)
    putIfNotNull("subCounty", subCounty)
    putIfNotNull("parish", parish)
    putIfNotNull("village", village)

    // family (renamed to relationship*)
    putIfNotNull("memberFName1", memberFName1)
    putIfNotNull("memberLName1", memberLName1)
    put("relationship1", relationship1.name)
    putIfNotNull("telephone1a", telephone1a)
    putIfNotNull("telephone1b", telephone1b)

    putIfNotNull("memberFName2", memberFName2)
    putIfNotNull("memberLName2", memberLName2)
    put("relationship2", relationship2.name)
    putIfNotNull("telephone2a", telephone2a)
    putIfNotNull("telephone2b", telephone2b)

    putIfNotNull("memberFName3", memberFName3)
    putIfNotNull("memberLName3", memberLName3)
    put("relationship3", relationship3.name)
    putIfNotNull("telephone3a", telephone3a)
    putIfNotNull("telephone3b", telephone3b)

    // spiritual
    put("acceptedJesus", acceptedJesus.name)
    putIfNotNull("acceptedJesusDate", acceptedJesusDate)
    put("whoPrayed", whoPrayed.name)
    putIfNotNull("outcome", outcome)

    // program flags
    put("reunitedWithFamily", reunitedWithFamily)
    put("sponsoredForEducation", sponsoredForEducation)
    putIfNotNull("sponsorId", sponsorId)
    putIfNotNull("sponsorNotes", sponsorNotes)

    // status
    put("registrationStatus", registrationStatus.name)
    put("graduated", graduated.name)
}
