package com.example.zionkids.data.mappers

import com.example.zionkids.data.model.*
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.QuerySnapshot
import com.google.firebase.Timestamp
import kotlin.String

// ---------- Enum converters with safe defaults (no reified) ----------
//private fun String?.toReply(): Reply =
//    runCatching { if (this == null) Reply.NO else Reply.valueOf(this) }
//        .getOrDefault(Reply.NO)

//private fun String?.toIndividual(): Individual =
//    runCatching { if (this == null) Individual.UNCLE else Individual.valueOf(this) }
//        .getOrDefault(Individual.UNCLE)

//private fun String?.toEducationPref(): EducationPreference =
//    runCatching { if (this == null) EducationPreference.NONE else EducationPreference.valueOf(this) }
//        .getOrDefault(EducationPreference.NONE)

//private fun String?.toRelationship(): Relationship =
//    runCatching { if (this == null) Relationship.NONE else Relationship.valueOf(this) }
//        .getOrDefault(Relationship.NONE)

//private fun String?.toRegistrationStatus(): RegistrationStatus =
//    runCatching { if (this == null) RegistrationStatus.BASICINFOR else RegistrationStatus.valueOf(this) }
//        .getOrDefault(RegistrationStatus.BASICINFOR)

private fun String?.toEventStatus(): EventStatus =
    runCatching { if (this == null) EventStatus.SCHEDULED else EventStatus.valueOf(this) }
        .getOrDefault(EventStatus.SCHEDULED)

private fun String?.toClassGroup(): ClassGroup =
    runCatching { if (this == null) ClassGroup.SERGEANT else ClassGroup.valueOf(this) }
        .getOrDefault(ClassGroup.SERGEANT)

private fun String?.toGender(): Gender =
    runCatching { if (this == null) Gender.MALE else Gender.valueOf(this) }
        .getOrDefault(Gender.MALE)


// ---------- Safe getters ----------
//private fun DocumentSnapshot.str(key: String): String? = this.getString(key)
//private fun DocumentSnapshot.lng(key: String): Long?   = this.getLong(key)
//private fun DocumentSnapshot.intPos(key: String): Int? = this.getLong(key)?.toInt()


/* ======================= SAFE GETTERS ======================= */
private fun DocumentSnapshot.str(key: String): String? = getString(key)
private fun DocumentSnapshot.intPos(key: String): Int? = getLong(key)?.toInt()
private fun DocumentSnapshot.ts(key: String): Timestamp? = getTimestamp(key)

/* ======================= ENUM PARSERS (safe defaults) ======================= */
private fun String?.toReply(): Reply =
    runCatching { if (this == null) Reply.NO else Reply.valueOf(this) }.getOrDefault(Reply.NO)

private fun String?.toIndividual(): Individual =
    runCatching { if (this == null) Individual.UNCLE else Individual.valueOf(this) }.getOrDefault(Individual.UNCLE)

private fun String?.toEducationPref(): EducationPreference =
    runCatching { if (this == null) EducationPreference.NONE else EducationPreference.valueOf(this) }
        .getOrDefault(EducationPreference.NONE)

private fun String?.toRelationship(): Relationship =
    runCatching { if (this == null) Relationship.NONE else Relationship.valueOf(this) }
        .getOrDefault(Relationship.NONE)

private fun String?.toRegistrationStatus(): RegistrationStatus =
    runCatching { if (this == null) RegistrationStatus.BASICINFOR else RegistrationStatus.valueOf(this) }
        .getOrDefault(RegistrationStatus.BASICINFOR)

fun QuerySnapshot.toEvents(): List<Event> =
    this.documents.mapNotNull { doc ->
        val e = doc.toObject(Event::class.java)?.copy(eventId = doc.id) ?: return@mapNotNull null
        val status = (doc.getString("eventStatus")).toEventStatus()
        e.copy(eventStatus = status)
    }



/* ======================= DOC -> CHILD ======================= */
fun DocumentSnapshot.toChildOrNull(): Child? {
    if (data == null) return null

    return Child(
        // ===== IDs =====
        childId = str("childId") ?: id,

        // ===== Basic Info =====
        profileImg = str("profileImg") ?: "",

        fName = str("fName") ?: "",
        lName = str("lName") ?: "",
        oName = str("oName") ?: "",
        gender = (str("educationPreference")).toGender(),

        age = intPos("age") ?: 0,

        dob = ts("dob"),
        dobVerified = getBoolean("dobVerified") ?: false,

        street = str("street") ?: "",

        invitedBy = (str("invitedBy")).toIndividual(),
        invitedByIndividualId = str("invitedByIndividualId") ?: "",
        invitedByTypeOther = str("invitedByTypeOther") ?: "",

        educationPreference = (str("educationPreference")).toEducationPref(),

        // ===== Background Info =====
        leftHomeDate = ts("leftHomeDate"),
        reasonLeftHome = str("reasonLeftHome") ?: "",
        leaveStreetDate = ts("leaveStreetDate") ?: ts("leftStreetDate"),

        // ===== Education Info =====
        lastClass = str("lastClass") ?: "",
        previousSchool = str("previousSchool") ?: "",
        reasonLeftSchool = str("reasonLeftSchool") ?: "",

        // ===== Family Resettlement =====
        resettlementPreference = run {
            val v = str("resettlementPreference")
            try { if (v != null) ResettlementPreference.valueOf(v) else ResettlementPreference.Home }
            catch (_: IllegalArgumentException) { ResettlementPreference.Home }
        },
        resettlementPreferenceOther = str("resettlementPreferenceOther") ?: "",
        resettled = getBoolean("resettled") ?: false,
        resettlementDate = ts("resettlementDate"),

        region = str("region") ?: "",
        district = str("district") ?: "",
        county = str("county") ?: "",
        subCounty = str("subCounty") ?: (str("sCounty") ?: ""),
        parish = str("parish") ?: "",
        village = str("village") ?: "",

        // ===== Family Members 1 =====
        memberFName1 = str("memberFName1") ?: "",
        memberLName1 = str("memberLName1") ?: "",
        relationship1 = (str("relationShip1") ?: str("relationship1")).toRelationship(),
        telephone1a = str("telephone1a") ?: "",
        telephone1b = str("telephone1b") ?: "",

        // ===== Family Members 2 =====
        memberFName2 = str("memberFName2") ?: "",
        memberLName2 = str("memberLName2") ?: "",
        relationship2 = (str("relationShip2") ?: str("relationship2")).toRelationship(),
        telephone2a = str("telephone2a") ?: "",
        telephone2b = str("telephone2b") ?: "",

        // ===== Family Members 3 =====
        memberFName3 = str("memberFName3") ?: "",
        memberLName3 = str("memberLName3") ?: "",
        relationship3 = (str("relationShip3") ?: str("relationship3")).toRelationship(),
        telephone3a = str("telephone3a") ?: "",
        telephone3b = str("telephone3b") ?: "",

        // ===== Spiritual Info =====
        acceptedJesus = (str("acceptedJesus")).toReply(),
        acceptedJesusDate = ts("acceptedJesusDate"),
        whoPrayed = (str("whoPrayed")).toIndividual(),
        whoPrayedOther = str("whoPrayedOther") ?: "",
        whoPrayedId = str("whoPrayedId") ?: "",
        classGroup = (str("classGroup")).toClassGroup(),
        outcome = str("outcome") ?: "",
        generalComments = str("generalComments") ?: "",

        // ===== Program statuses =====
        registrationStatus = (str("registrationStatus")).toRegistrationStatus(),
        graduated = (str("graduated")).toReply(),

        // ===== Sponsorship / Flags =====
        sponsoredForEducation = getBoolean("sponsoredForEducation") ?: false,
        sponsorId = str("sponsorId") ?: "",
        sponsorFName = str("sponsorFName") ?: "",
        sponsorLName = str("sponsorLName") ?: "",
        sponsorTelephone1 = str("sponsorTelephone1") ?: "",
        sponsorTelephone2 = str("sponsorTelephone2") ?: "",
        sponsorEmail = str("sponsorEmail") ?: "",
        sponsorNotes = str("sponsorNotes") ?: "",

        // ===== Audit =====
        createdAt = ts("createdAt") ?: Timestamp.now(),
        updatedAt = ts("updatedAt") ?: Timestamp.now()
    )
}

/* ======================= SNAPSHOT -> LIST ======================= */
fun QuerySnapshot.toChildren(): List<Child> = documents.mapNotNull { it.toChildOrNull() }

/* ======================= CHILD -> FIRESTORE (FULL) ======================= */
/** Full map for create/replace. Writes all fields; nullable fields are skipped. */
fun Child.toFirestoreMapFull(): Map<String, Any> = buildMap {
    // ===== IDs =====
    put("childId", childId)

    // ===== Basic Info =====
    put("profileImg", profileImg)

    put("fName", fName)
    put("lName", lName)
    put("oName", oName)
    put("gender", gender)

    if (age > 0) put("age", age)

    dob?.let { put("dob", it) }
    put("dobVerified", dobVerified)

    put("street", street)

    put("invitedBy", invitedBy.name)
    put("invitedByIndividualId", invitedByIndividualId)
    put("invitedByTypeOther", invitedByTypeOther)

    put("educationPreference", educationPreference.name)

    // ===== Background Info =====
    leftHomeDate?.let { put("leftHomeDate", it) }
    reasonLeftHome?.let { put("reasonLeftHome", it) }
    leaveStreetDate?.let { put("leaveStreetDate", it) }

    // ===== Education Info =====
    put("lastClass", lastClass)
    put("previousSchool", previousSchool)
    put("reasonLeftSchool", reasonLeftSchool)

    // ===== Family Resettlement =====
    put("resettlementPreference", resettlementPreference.name)
    put("resettlementPreferenceOther", resettlementPreferenceOther)
    put("resettled", resettled)
    resettlementDate?.let { put("resettlementDate", it) }

    put("region", region)
    put("district", district)
    put("county", county)
    put("subCounty", subCounty)
    put("parish", parish)
    put("village", village)

    // ===== Family Members 1 =====
    put("memberFName1", memberFName1)
    put("memberLName1", memberLName1)
    put("relationship1", relationship1.name)
    put("telephone1a", telephone1a)
    put("telephone1b", telephone1b)

    // ===== Family Members 2 =====
    put("memberFName2", memberFName2)
    put("memberLName2", memberLName2)
    put("relationship2", relationship2.name)
    put("telephone2a", telephone2a)
    put("telephone2b", telephone2b)

    // ===== Family Members 3 =====
    put("memberFName3", memberFName3)
    put("memberLName3", memberLName3)
    put("relationship3", relationship3.name)
    put("telephone3a", telephone3a)
    put("telephone3b", telephone3b)

    // ===== Spiritual Info =====
    put("acceptedJesus", acceptedJesus.name)
    acceptedJesusDate?.let { put("acceptedJesusDate", it) }
    put("whoPrayed", whoPrayed.name)
    put("whoPrayedOther", whoPrayedOther)
    put("whoPrayedId", whoPrayedId)
    put("outcome", outcome)
    put("classGroup", classGroup.name)
    put("generalComments", generalComments)

    // ===== Program statuses =====
    put("registrationStatus", registrationStatus.name)
    put("graduated", graduated.name)

    // ===== Sponsorship / Flags =====
    put("sponsoredForEducation", sponsoredForEducation)
    put("sponsorId", sponsorId)
    put("sponsorFName", sponsorFName)
    put("sponsorLName", sponsorLName)
    put("sponsorTelephone1", sponsorTelephone1)
    put("sponsorTelephone2", sponsorTelephone2)
    put("sponsorEmail", sponsorEmail)
    put("sponsorNotes", sponsorNotes)

    // ===== Audit =====
    put("createdAt", createdAt)
    put("updatedAt", updatedAt)
}


/** Build a PATCH map (only non-null / non-blank fields) for merge updates. */
fun Child.toFirestoreMapPatch(): Map<String, Any> = buildMap {
    fun putIfNotBlank(key: String, v: String?) { if (!v.isNullOrBlank()) put(key, v) }
    fun putIfNotNull(key: String, v: Any?) { if (v != null) put(key, v) }

    // ===== Basic Info =====
    putIfNotBlank("profileImg", profileImg)
    putIfNotBlank("fName", fName)
    putIfNotBlank("lName", lName)
    putIfNotBlank("oName", oName)
    putIfNotBlank("gender", gender.name)

    if (age > 0) put("age", age)

    putIfNotNull("dob", dob)
    put("dobVerified", dobVerified)

    putIfNotBlank("street", street)

    put("invitedBy", invitedBy.name)
    putIfNotBlank("invitedByIndividualId", invitedByIndividualId)
    putIfNotBlank("invitedByTypeOther", invitedByTypeOther)

    put("educationPreference", educationPreference.name)

    // ===== Background =====
    putIfNotNull("leftHomeDate", leftHomeDate)
    putIfNotBlank("reasonLeftHome", reasonLeftHome)
    putIfNotNull("leaveStreetDate", leaveStreetDate)

    // ===== Education =====
    putIfNotBlank("lastClass", lastClass)
    putIfNotBlank("previousSchool", previousSchool)
    putIfNotBlank("reasonLeftSchool", reasonLeftSchool)

    // ===== Family Resettlement =====
    put("resettlementPreference", resettlementPreference.name)
    putIfNotBlank("resettlementPreferenceOther", resettlementPreferenceOther)
    put("resettled", resettled)
    putIfNotNull("resettlementDate", resettlementDate)

    putIfNotBlank("region", region)
    putIfNotBlank("district", district)
    putIfNotBlank("county", county)
    putIfNotBlank("subCounty", subCounty)
    putIfNotBlank("parish", parish)
    putIfNotBlank("village", village)

    // ===== Family Members 1 =====
    putIfNotBlank("memberFName1", memberFName1)
    putIfNotBlank("memberLName1", memberLName1)
    put("relationship1", relationship1.name)
    putIfNotBlank("telephone1a", telephone1a)
    putIfNotBlank("telephone1b", telephone1b)

    // ===== Family Members 2 =====
    putIfNotBlank("memberFName2", memberFName2)
    putIfNotBlank("memberLName2", memberLName2)
    put("relationship2", relationship2.name)
    putIfNotBlank("telephone2a", telephone2a)
    putIfNotBlank("telephone2b", telephone2b)

    // ===== Family Members 3 =====
    putIfNotBlank("memberFName3", memberFName3)
    putIfNotBlank("memberLName3", memberLName3)
    put("relationship3", relationship3.name)
    putIfNotBlank("telephone3a", telephone3a)
    putIfNotBlank("telephone3b", telephone3b)

    // ===== Spiritual =====
    put("acceptedJesus", acceptedJesus.name)
    putIfNotNull("acceptedJesusDate", acceptedJesusDate)
    put("whoPrayed", whoPrayed.name)
    putIfNotBlank("whoPrayedOther", whoPrayedOther)
    putIfNotBlank("whoPrayedId", whoPrayedId)
    putIfNotBlank("outcome", outcome)
    putIfNotBlank("generalComments", generalComments)

    // ===== Status =====
    put("registrationStatus", registrationStatus.name)
    put("graduated", graduated.name)

    // ===== Sponsorship =====
    put("sponsoredForEducation", sponsoredForEducation)
    putIfNotBlank("sponsorId", sponsorId)
    putIfNotBlank("sponsorFName", sponsorFName)
    putIfNotBlank("sponsorLName", sponsorLName)
    putIfNotBlank("sponsorTelephone1", sponsorTelephone1)
    putIfNotBlank("sponsorTelephone2", sponsorTelephone2)
    putIfNotBlank("sponsorEmail", sponsorEmail)
    putIfNotBlank("sponsorNotes", sponsorNotes)

    // ===== Audit =====
    put("createdAt", createdAt)
    put("updatedAt", Timestamp.now())
}



///* ======================= DOC -> user ======================= */
//fun DocumentSnapshot.toUserOrNull(): UserProfile? {
//    if (data == null) return null
//
//    return UserProfile(
//         // ===== IDs =====
//        uid = str("uid") ?: id,
//
//     email = str("email") ?: "",
//     displayName= str("displayName") ?: "",
//     roles = (str("roles")).toRole(),
//     disabled = getBoolean("disabled") ?: false,
//
//
//    )
//}
//
///* ======================= SNAPSHOT -> LISt ======================= */
//
//fun QuerySnapshot.toUsers(): List<UserProfile> = documents.mapNotNull { it.toUserOrNull() }
//
//private fun String?.toRole(): List<Role> =
//    runCatching {
//        if (this == null){ Role.ADMIN  }
//        else if (this == null){ Role.LEAD  }
//        else if (this == null){ Role.VOLUNTEER  }
//        else if (this == null){ Role.SPONSOR  }
//        else Role.valueOf(this)
//    }
//        .getOrDefault(Role.VIEWER)


fun DocumentSnapshot.toUserOrNull(): UserProfile? {
    if (data == null) return null

    val rolesRaw = get("roles") // could be String, List<String>, List<Role>, or null
    return UserProfile(
        // ===== IDs =====
        uid = str("uid") ?: id,

        // ===== BASIC =====
        email = str("email") ?: "",
        displayName = str("displayName") ?: "",

        // ===== ROLES =====
//        userRoles = rolesRaw,
//         userRole = (str("assignedRoles")).toAssignedRole(),
        userRole = (str("userRole") ?: str("assignedRole") ?: str("assignedRoles") ?: str("role")).toAssignedRole(),

        // ===== FLAGS =====
        disabled = getBoolean("disabled") ?: false
    )
}

/* ======================= SNAPSHOT -> LIST ======================= */
fun QuerySnapshot.toUsers(): List<UserProfile> =
    documents.mapNotNull { it.toUserOrNull() }

/* ======================= ROLE PARSING ======================= */

//private fun Any?.toRoleList(): List<AssignedRole> = when (this) {
//    null -> listOf(AssignedRole.VOLUNTEER)
//
//    // roles: "ADMIN"
//    is String -> listOfNotNull(this.toRoleOrNull())
//
//    // roles: ["ADMIN","VOLUNTEER"] or [Role.ADMIN, Role.VOLUNTEER]
//    is Collection<*> -> this.mapNotNull { item ->
//        when (item) {
//            is String -> item.toRoleOrNull()
//            is AssignedRole -> item
//            else -> null
//        }
//    }.ifEmpty { listOf(AssignedRole.VOLUNTEER) }
//
//    // Unknown type
//    else -> listOf(AssignedRole.VOLUNTEER)
//}

private fun String.toRoleOrNull(): AssignedRole? = runCatching {
    // Normalize common variants; keep your current enum spelling "LEAD"
    when (val norm = trim().uppercase()) {
        "VIEWER" -> AssignedRole.VOLUNTEER   // alias if some data writes "LEADER"
        else -> AssignedRole.valueOf(norm)
    }
}.getOrNull()
private fun String?.toAssignedRole(): AssignedRole =
    runCatching { if (this == null) AssignedRole.VOLUNTEER else AssignedRole.valueOf(this) }
        .getOrDefault(AssignedRole.VOLUNTEER)

//private fun String?.toAssignedRole(): AssignedRole =
//    when (this?.trim()?.uppercase()) {
//        null, "", "VOLUNTEER" -> AssignedRole.VOLUNTEER
//        "LEADER"      -> AssignedRole.LEADER   // alias supported
//        "ADMIN"               -> AssignedRole.ADMIN
//        "VIEWER"              -> AssignedRole.VIEWER
//        "SPONSOR"             -> AssignedRole.SPONSOR
//        else                  -> AssignedRole.VOLUNTEER // unknowns default
//    }

