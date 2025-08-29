//package com.example.zionkids.presentation.viewModels.children
//
package com.example.zionkids.presentation.viewModels.children

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.zionkids.core.Utils.GenerateId
import com.example.zionkids.data.model.*
import com.example.zionkids.domain.repositories.online.ChildrenRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ChildFormViewModel @Inject constructor(
    private val repo: ChildrenRepository
) : ViewModel() {

    var ui by mutableStateOf(ChildFormUiState())

    // ---- step control ----
    var step by mutableStateOf(RegistrationStatus.BASICINFOR)
        private set

    fun jumpToStep(s: RegistrationStatus) {
        step = s
        ui = ui.copy(registrationStatus = s)
    }

    fun goBack() {
        step = when (step) {
            RegistrationStatus.BASICINFOR -> RegistrationStatus.BASICINFOR
            RegistrationStatus.BACKGROUND -> RegistrationStatus.BASICINFOR
            RegistrationStatus.EDUCATION  -> RegistrationStatus.BACKGROUND
            RegistrationStatus.FAMILY     -> RegistrationStatus.EDUCATION
            RegistrationStatus.SPIRITUAL  -> RegistrationStatus.FAMILY
            RegistrationStatus.COMPLETE   -> RegistrationStatus.SPIRITUAL
        }
        ui = ui.copy(registrationStatus = step)
    }

    fun goNext(onAfterSave: (() -> Unit)? = null) = viewModelScope.launch {
        val ok = validateStep(step)
        if (!ok) return@launch
//        saveProgress()
        step = when (step) {
            RegistrationStatus.BASICINFOR -> RegistrationStatus.BACKGROUND
            RegistrationStatus.BACKGROUND -> RegistrationStatus.EDUCATION
            RegistrationStatus.EDUCATION  -> RegistrationStatus.FAMILY
            RegistrationStatus.FAMILY     -> RegistrationStatus.SPIRITUAL
            RegistrationStatus.SPIRITUAL  -> RegistrationStatus.COMPLETE
            RegistrationStatus.COMPLETE   -> RegistrationStatus.COMPLETE
        }
        ui = ui.copy(registrationStatus = step)
        onAfterSave?.invoke()
    }

    // ---- one-shot events ----
    private val _events = Channel<ChildFormEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    sealed interface ChildFormEvent {
        data class Saved(val id: String) : ChildFormEvent
        data class Error(val msg: String) : ChildFormEvent
    }

    // ---- load existing ----
    fun loadForEdit(childId: String) = viewModelScope.launch {
        ui = ui.copy(loading = true, error = null)
        val existing = repo.getChildFast(childId)
        ui = if (existing != null) {
            ui.from(existing).copy(loading = false)
        } else {
            ui.copy(loading = false, error = "Child not found")
        }
    }

    // ---- common setters used by UI ----
    fun onFirstName(v: String) { ui = ui.copy(fName = v) }
    fun onLastName(v: String)  { ui = ui.copy(lName = v) }
    fun onOtherName(v: String) { ui = ui.copy(oName = v) }
    fun onAge(v: String)       { ui = ui.copy(ageText = v.filter { it.isDigit() }) }
    fun onStreet(v: String)    { ui = ui.copy(street = v) }
    fun onInvitedBy(v: Individual) { ui = ui.copy(invitedBy = v) }
    fun onEduPref(v: EducationPreference) { ui = ui.copy(educationPreference = v) }

    // Optional new setters if you want them:
    fun onDob(millis: Long) { ui = ui.copy(dob = millis) }
    fun onDobVerified(v: Boolean) { ui = ui.copy(dobVerified = v) }
    fun onSubCounty(v: String?) { ui = ui.copy(subCounty = v) }
    fun onReunited(v: Boolean) { ui = ui.copy(reunitedWithFamily = v) }
    fun onSponsored(v: Boolean) { ui = ui.copy(sponsoredForEducation = v) }
    fun onSponsorId(v: String?) { ui = ui.copy(sponsorId = v) }
    fun onSponsorNotes(v: String?) { ui = ui.copy(sponsorNotes = v) }

    // ---- validation per step (lightweight) ----
    private fun validateStep(s: RegistrationStatus): Boolean =
        when (s) {
            RegistrationStatus.BASICINFOR -> {
                val age = ui.ageText.toIntOrNull() ?: -1
                when {
                    ui.fName.isBlank() || ui.lName.isBlank() -> {
                        ui = ui.copy(error = "First and last name are required.")
                        false
                    }
                    ui.ageText.isNotBlank() && age !in 0..25 -> {
                        ui = ui.copy(error = "Age should be 0–25.")
                        false
                    }
                    else -> { ui = ui.copy(error = null); true }
                }
            }
            else -> { ui = ui.copy(error = null); true }
        }

    fun ensureNewIdIfNeeded() {
        if (ui.childId.isBlank()) {
            val now = System.currentTimeMillis()
            ui = ui.copy(
                childId = GenerateId.generateId("child"),
                createdAt = now,
                isNew = true
            )
        }
    }

    /** Partial merge save so we don't wipe other fields. */
    fun saveProgress() = viewModelScope.launch {
        ensureNewIdIfNeeded()
        val now = System.currentTimeMillis()
        val id = ui.childId
        val child = buildChild(id = id, now = now, status = step)
        runCatching { repo.upsert(child, isNew = false) }
            .onSuccess { ui = ui.copy(childId = id) }
    }

    /** Final save. Emits Saved(id) on success; do NOT navigate here. */
    fun save() = viewModelScope.launch {
        ui = ui.copy(saving = true, error = null)

        if (ui.fName.isBlank() || ui.lName.isBlank()) {
            ui = ui.copy(saving = false, error = "First and last name are required.")
            _events.trySend(ChildFormEvent.Error("Missing required fields"))
            return@launch
        }

        if (ui.isNew && ui.childId.isBlank()) ensureNewIdIfNeeded()

        val now = System.currentTimeMillis()
        val id = ui.childId
        val child = buildChild(id = id, now = now, status = ui.registrationStatus)

        runCatching { repo.upsert(child, isNew = ui.isNew) }
            .onSuccess {
                ui = ui.copy(saving = false, childId = id, isNew = false)
                _events.trySend(ChildFormEvent.Saved(id))
            }
            .onFailure {
                ui = ui.copy(saving = false, error = it.message ?: "Failed to save")
                _events.trySend(ChildFormEvent.Error("Failed to save"))
            }
    }

    fun finish() = save()

    // ---- helpers ----
    private fun buildChild(id: String, now: Long, status: RegistrationStatus): Child =
        Child(
            childId = id,

            // Basic
            profileImg = ui.profileImg,
            fName = ui.fName.trim(),
            lName = ui.lName.trim(),
            oName = ui.oName?.trim(),
            age = ui.ageText.toIntOrNull() ?: 0,
            dob = ui.dob,
            dobVerified = ui.dobVerified,
            street = ui.street,
            invitedBy = ui.invitedBy,
            invitedByType = ui.invitedByType,
            educationPreference = ui.educationPreference,

            // Background
            leftHomeDate = ui.leftHomeDate,
            reasonLeftHome = ui.reasonLeftHome,
            leftStreetDate = ui.leftStreetDate,

            // Education
            lastClass = ui.lastClass,
            previousSchool = ui.previousSchool,
            reasonLeftSchool = ui.reasonLeftSchool,

            // Resettlement
            homePreference = ui.homePreference,
            goHomeDate = ui.goHomeDate,
            region = ui.region,
            district = ui.district,
            county = ui.county,
            subCounty = ui.subCounty,              // renamed from sCounty
            parish = ui.parish,
            village = ui.village,

            // Members
            memberFName1 = ui.memberFName1,
            memberLName1 = ui.memberLName1,
            relationship1 = ui.relationship1,     // enum renamed
            telephone1a = ui.telephone1a,
            telephone1b = ui.telephone1b,

            memberFName2 = ui.memberFName2,
            memberLName2 = ui.memberLName2,
            relationship2 = ui.relationship2,     // enum renamed
            telephone2a = ui.telephone2a,
            telephone2b = ui.telephone2b,

            memberFName3 = ui.memberFName3,
            memberLName3 = ui.memberLName3,
            relationship3 = ui.relationship3,     // enum renamed
            telephone3a = ui.telephone3a,
            telephone3b = ui.telephone3b,

            // Spiritual
            acceptedJesus = ui.acceptedJesus,
            acceptedJesusDate = ui.acceptedJesusDate,
            whoPrayed = ui.whoPrayed,
            outcome = ui.outcome,

            // New flags (program state)
            reunitedWithFamily = ui.reunitedWithFamily,
            sponsoredForEducation = ui.sponsoredForEducation,
            sponsorId = ui.sponsorId,
            sponsorNotes = ui.sponsorNotes,

            // Status & audit
            graduated = ui.graduated,
            registrationStatus = status,
            createdAt = if (ui.createdAt == 0L) now else ui.createdAt,
            updatedAt = now
        )

    private fun ChildFormUiState.from(c: Child) = copy(
        childId = c.childId,
        profileImg = c.profileImg,
        fName = c.fName,
        lName = c.lName,
        oName = c.oName,
        ageText = c.age.toString(),
        dob = c.dob,
        dobVerified = c.dobVerified,
        street = c.street,
        invitedBy = c.invitedBy,
        invitedByType = c.invitedByType,
        educationPreference = c.educationPreference,

        leftHomeDate = c.leftHomeDate,
        reasonLeftHome = c.reasonLeftHome,
        leftStreetDate = c.leftStreetDate,

        lastClass = c.lastClass,
        previousSchool = c.previousSchool,
        reasonLeftSchool = c.reasonLeftSchool,

        homePreference = c.homePreference,
        goHomeDate = c.goHomeDate,
        region = c.region,
        district = c.district,
        county = c.county,
        subCounty = c.subCounty,                 // renamed
        parish = c.parish,
        village = c.village,

        memberFName1 = c.memberFName1,
        memberLName1 = c.memberLName1,
        relationship1 = c.relationship1,         // renamed
        telephone1a = c.telephone1a,
        telephone1b = c.telephone1b,

        memberFName2 = c.memberFName2,
        memberLName2 = c.memberLName2,
        relationship2 = c.relationship2,         // renamed
        telephone2a = c.telephone2a,
        telephone2b = c.telephone2b,

        memberFName3 = c.memberFName3,
        memberLName3 = c.memberLName3,
        relationship3 = c.relationship3,         // renamed
        telephone3a = c.telephone3a,
        telephone3b = c.telephone3b,

        acceptedJesus = c.acceptedJesus,
        acceptedJesusDate = c.acceptedJesusDate,
        whoPrayed = c.whoPrayed,
        outcome = c.outcome,

        reunitedWithFamily = c.reunitedWithFamily,
        sponsoredForEducation = c.sponsoredForEducation,
        sponsorId = c.sponsorId,
        sponsorNotes = c.sponsorNotes,

        graduated = c.graduated,
        registrationStatus = c.registrationStatus,
        createdAt = c.createdAt
    )
}

data class ChildFormUiState(
    val loading: Boolean = false,
    val saving: Boolean = false,
    val error: String? = null,
    val isNew: Boolean = true,

    // Basic
    val childId: String = "",
    val profileImg: String = "",
    val fName: String = "",
    val lName: String = "",
    val oName: String? = null,
    val ageText: String = "",
    val dob: Long = 0L,
    val dobVerified: Boolean = false,
    val street: String = "",

    val invitedBy: Individual = Individual.UNCLE,
    val invitedByType: String = "",
    val educationPreference: EducationPreference = EducationPreference.NONE,

    // Status & spiritual
    val homePreference: Reply = Reply.NO,
    val acceptedJesus: Reply = Reply.NO,
    val whoPrayed: Individual = Individual.UNCLE,
    val registrationStatus: RegistrationStatus = RegistrationStatus.BASICINFOR,

    // Audit
    val createdAt: Long = 0L,

    // Background
    val leftHomeDate: Long? = null,
    val reasonLeftHome: String? = null,
    val leftStreetDate: Long? = null,

    // Education
    val lastClass: String? = null,
    val previousSchool: String? = null,
    val reasonLeftSchool: String? = null,

    // Resettlement
    val goHomeDate: Long? = null,
    val region: String? = null,
    val district: String? = null,
    val county: String? = null,
    val subCounty: String? = null,                 // renamed from sCounty
    val parish: String? = null,
    val village: String? = null,

    // Members (enum renamed)
    val memberFName1: String? = null,
    val memberLName1: String? = null,
    val relationship1: Relationship = Relationship.NONE,
    val telephone1a: String? = null,
    val telephone1b: String? = null,

    val memberFName2: String? = null,
    val memberLName2: String? = null,
    val relationship2: Relationship = Relationship.NONE,
    val telephone2a: String? = null,
    val telephone2b: String? = null,

    val memberFName3: String? = null,
    val memberLName3: String? = null,
    val relationship3: Relationship = Relationship.NONE,
    val telephone3a: String? = null,
    val telephone3b: String? = null,

    // Program flags
    val graduated: Reply = Reply.NO,
    val reunitedWithFamily: Boolean = false,
    val sponsoredForEducation: Boolean = false,
    val sponsorId: String? = null,
    val sponsorNotes: String? = null,

    // Spiritual notes
    val acceptedJesusDate: Long? = null,
    val outcome: String? = null
)

//import androidx.compose.runtime.getValue
//import androidx.compose.runtime.mutableStateOf
//import androidx.compose.runtime.setValue
//import androidx.lifecycle.ViewModel
//import androidx.lifecycle.viewModelScope
//import com.example.zionkids.core.Utils.GenerateId
//import com.example.zionkids.data.model.*
//import com.example.zionkids.domain.repositories.online.ChildrenRepository
//import dagger.hilt.android.lifecycle.HiltViewModel
//import kotlinx.coroutines.channels.Channel
//import kotlinx.coroutines.flow.receiveAsFlow
//import kotlinx.coroutines.launch
//import java.util.UUID
//import javax.inject.Inject
//
//@HiltViewModel
//class ChildFormViewModel @Inject constructor(
//    private val repo: ChildrenRepository
//) : ViewModel() {
//
//    var ui by mutableStateOf(ChildFormUiState())
//       // private set // keep state immutable from UI
//
//    // ---- step control ----
//    var step by mutableStateOf(RegistrationStatus.BASICINFOR)
//        private set
//
//    fun jumpToStep(s: RegistrationStatus) {
//        step = s
//        ui = ui.copy(registrationStatus = s)
//    }
//
//    fun goBack() {
//        step = when (step) {
//            RegistrationStatus.BASICINFOR -> RegistrationStatus.BASICINFOR
//            RegistrationStatus.BACKGROUND -> RegistrationStatus.BASICINFOR
//            RegistrationStatus.EDUCATION  -> RegistrationStatus.BACKGROUND
//            RegistrationStatus.FAMILY     -> RegistrationStatus.EDUCATION
//            RegistrationStatus.SPIRITUAL  -> RegistrationStatus.FAMILY
//            RegistrationStatus.COMPLETE   -> RegistrationStatus.SPIRITUAL
//        }
//        ui = ui.copy(registrationStatus = step)
//    }
//    fun goNext(onAfterSave: (() -> Unit)? = null) = viewModelScope.launch {
//        // validate current step lightly
//        val ok = validateStep(step)
//        if (!ok) return@launch
//
//        // save progress on each step (partial merge via repo.upsert)
//        saveProgress()
//
//        step = when (step) {
//            RegistrationStatus.BASICINFOR -> RegistrationStatus.BACKGROUND
//            RegistrationStatus.BACKGROUND -> RegistrationStatus.EDUCATION
//            RegistrationStatus.EDUCATION  -> RegistrationStatus.FAMILY
//            RegistrationStatus.FAMILY     -> RegistrationStatus.SPIRITUAL
//            RegistrationStatus.SPIRITUAL  -> RegistrationStatus.COMPLETE
//            RegistrationStatus.COMPLETE   -> RegistrationStatus.COMPLETE
//        }
//        ui = ui.copy(registrationStatus = step)
//        onAfterSave?.invoke()
//    }
////    fun goNext() = viewModelScope.launch {
////        val ok = validateStep(step)
////        if (!ok) return@launch
////        saveProgress() // merge-friendly partial save
////        step = when (step) {
////            RegistrationStatus.BASICINFOR -> RegistrationStatus.BACKGROUND
////            RegistrationStatus.BACKGROUND -> RegistrationStatus.EDUCATION
////            RegistrationStatus.EDUCATION  -> RegistrationStatus.FAMILY
////            RegistrationStatus.FAMILY     -> RegistrationStatus.SPIRITUAL
////            RegistrationStatus.SPIRITUAL  -> RegistrationStatus.COMPLETE
////            RegistrationStatus.COMPLETE   -> RegistrationStatus.COMPLETE
////        }
////        ui = ui.copy(registrationStatus = step)
////    }
//
//    // ---- events (for one-shot navigation / messages) ----
//    private val _events = Channel<ChildFormEvent>(Channel.BUFFERED)
//    val events = _events.receiveAsFlow()
//
//    sealed interface ChildFormEvent {
//        data class Saved(val id: String) : ChildFormEvent
//        data class Error(val msg: String) : ChildFormEvent
//    }
//
//    // ---- load existing ----
//    fun loadForEdit(childId: String) = viewModelScope.launch {
//        ui = ui.copy(loading = true, error = null)
//        val existing = repo.getChildFast(childId)
//        ui = if (existing != null) {
//            ui.from(existing).copy(loading = false)
//        } else {
//            ui.copy(loading = false, error = "Child not found")
//        }
//    }
//
//    // ---- setters for fields used in UI ----
//    fun onFirstName(v: String) { ui = ui.copy(fName = v) }
//    fun onLastName(v: String)  { ui = ui.copy(lName = v) }
//    fun onOtherName(v: String) { ui = ui.copy(oName = v) }
//    fun onAge(v: String)       { ui = ui.copy(ageText = v.filter { it.isDigit() }) }
//    fun onStreet(v: String)    { ui = ui.copy(street = v) }
//    fun onInvitedBy(v: Individual) { ui = ui.copy(invitedBy = v) }
//    fun onEduPref(v: EducationPreference) { ui = ui.copy(educationPreference = v) }
//
//    // ---- validation per step (lightweight) ----
//    private fun validateStep(s: RegistrationStatus): Boolean =
//        when (s) {
//            RegistrationStatus.BASICINFOR -> {
//                val age = ui.ageText.toIntOrNull() ?: -1
//                when {
//                    ui.fName.isBlank() || ui.lName.isBlank() -> {
//                        ui = ui.copy(error = "First and last name are required.")
//                        false
//                    }
//                    ui.ageText.isNotBlank() && age !in 0..25 -> {
//                        ui = ui.copy(error = "Age should be 0–25.")
//                        false
//                    }
//                    else -> { ui = ui.copy(error = null); true }
//                }
//            }
//            else -> { ui = ui.copy(error = null); true }
//        }
//
////    fun ensureNewIdIfNeeded() {
////        if (ui.childId.isBlank()) {
////            ui = ui.copy(childId = GenerateId.generateId("child"))
////        }
////    }
//fun ensureNewIdIfNeeded() {
//    if (ui.childId.isBlank()) {
//        val now = System.currentTimeMillis()
//        ui = ui.copy(
//            childId = GenerateId.generateId("child"),
//            createdAt = now,
//            isNew = true
//        )
//    }
//}
//
//    /** Partial merge save so we don't wipe other fields. */
////    fun saveProgress() = viewModelScope.launch {
////        val now = System.currentTimeMillis()
////        val id = (ui.childId.ifBlank { UUID.randomUUID().toString() })
////        val child = buildChild(id = id, now = now, status = step)
////        runCatching { repo.upsert(child) }
////            .onSuccess { ui = ui.copy(childId = id) }
////    }
//    fun saveProgress() = viewModelScope.launch {
//        ensureNewIdIfNeeded()
//        val now = System.currentTimeMillis()
//        val id = ui.childId
//        val child = buildChild(id = id, now = now, status = step)
//
//        runCatching {
//            repo.upsert(child, isNew = false)  // patch during steps
//        }.onSuccess {
//            ui = ui.copy(childId = id)
//        }
//    }
//
////    fun saveProgress() = viewModelScope.launch {
////        val now = System.currentTimeMillis()
////        // Ensure we have an id for new forms
////        ensureNewIdIfNeeded()
////        val id = ui.childId
////
////        val child = buildChild(id = id, now = now, status = step)
////
////        runCatching {
////            repo.upsert(child, isNew = ui.createdAt == 0L || ui.createdAt == now || id == ui.childId && ui.createdAt == ui.createdAt)
////            // simpler: repo.upsert(child, isNew = false)  // if you only want to patch during steps
////        }.onSuccess {
////            // Keep the id; leave createdAt as set in buildChild
////            ui = ui.copy(childId = id)
////        }
////    }
//
//
//    /** Final save. Emits Saved(id) on success; do NOT navigate here. */
////    fun save() = viewModelScope.launch {
////        ui = ui.copy(saving = true, error = null)
////
////        val age = ui.ageText.toIntOrNull() ?: 0
////        if (ui.fName.isBlank() || ui.lName.isBlank()) {
////            ui = ui.copy(saving = false, error = "First and last name are required.")
////            _events.trySend(ChildFormEvent.Error("Missing required fields"))
////            return@launch
////        }
////
////        val id = (ui.childId.ifBlank { GenerateId.generateId("child") })
////        val now = System.currentTimeMillis()
////        val child = buildChild(id = id, now = now, status = ui.registrationStatus)
////
////        runCatching { repo.upsert(
////            child,
////            isNew = TODO()
////        ) }
////            .onSuccess {
////                ui = ui.copy(saving = false, childId = id)
////                _events.trySend(ChildFormEvent.Saved(id))
////            }
////            .onFailure {
////                ui = ui.copy(saving = false, error = it.message ?: "Failed to save")
////                _events.trySend(ChildFormEvent.Error("Failed to save"))
////            }
////    }
//    fun save() = viewModelScope.launch {
//        ui = ui.copy(saving = true, error = null)
//
//        if (ui.fName.isBlank() || ui.lName.isBlank()) {
//            ui = ui.copy(saving = false, error = "First and last name are required.")
//            _events.trySend(ChildFormEvent.Error("Missing required fields"))
//            return@launch
//        }
//
//        if (ui.isNew && ui.childId.isBlank()) ensureNewIdIfNeeded()
//
//        val now = System.currentTimeMillis()
//        val id = ui.childId
//        val child = buildChild(id = id, now = now, status = ui.registrationStatus)
//
//        runCatching {
//            repo.upsert(child, isNew = ui.isNew)
//        }.onSuccess {
//            ui = ui.copy(saving = false, childId = id, isNew = false) // flip after first real save
//            _events.trySend(ChildFormEvent.Saved(id))
//        }.onFailure {
//            ui = ui.copy(saving = false, error = it.message ?: "Failed to save")
//            _events.trySend(ChildFormEvent.Error("Failed to save"))
//        }
//    }
//
////    fun save() = viewModelScope.launch {
////        ui = ui.copy(saving = true, error = null)
////
////        if (ui.fName.isBlank() || ui.lName.isBlank()) {
////            ui = ui.copy(saving = false, error = "First and last name are required.")
////            _events.trySend(ChildFormEvent.Error("Missing required fields"))
////            return@launch
////        }
////
////        if (ui.isNew && ui.childId.isBlank()) {
////            // Safety: new record must have an id and createdAt
////            ensureNewIdIfNeeded()
////        }
////
////        val now = System.currentTimeMillis()
////        val id = ui.childId
////        val child = buildChild(id = id, now = now, status = ui.registrationStatus)
////
////        runCatching {
////            repo.upsert(child, isNew = ui.isNew)  // <-- use the flag
////        }.onSuccess {
////            ui = ui.copy(saving = false, childId = id, isNew = false) // <-- flip after first save
////            _events.trySend(ChildFormEvent.Saved(id))
////        }.onFailure {
////            ui = ui.copy(saving = false, error = it.message ?: "Failed to save")
////            _events.trySend(ChildFormEvent.Error("Failed to save"))
////        }
////    }
//
////    fun save() = viewModelScope.launch {
////        ui = ui.copy(saving = true, error = null)
////
////        if (ui.fName.isBlank() || ui.lName.isBlank()) {
////            ui = ui.copy(saving = false, error = "First and last name are required.")
////            _events.trySend(ChildFormEvent.Error("Missing required fields"))
////            return@launch
////        }
////
////        // Ensure id for new record (once)
////        ensureNewIdIfNeeded()
////        val id = ui.childId
////        val isNew = ui.createdAt == 0L || id.isBlank()  // or simply: ui.childId was blank before ensureNewIdIfNeeded()
////       // createdAt = if (ui.childId.isBlank()) now else ui.createdAt
////        val now = System.currentTimeMillis()
////        val child = buildChild(id = id, now = now, status = ui.registrationStatus)
////
////        runCatching {
////            repo.upsert(child, isNew = isNew)
////        }.onSuccess {
////            ui = ui.copy(saving = false, childId = id)
////            _events.trySend(ChildFormEvent.Saved(id))
////        }.onFailure {
////            ui = ui.copy(saving = false, error = it.message ?: "Failed to save")
////            _events.trySend(ChildFormEvent.Error("Failed to save"))
////        }
////    }
//
//    fun finish() = save()
//
//
//
//    // ---- helpers ----
//    private fun buildChild(id: String, now: Long, status: RegistrationStatus): Child =
//        Child(
//            childId = id,
//            profileImg = ui.profileImg,
//            fName = ui.fName.trim(),
//            lName = ui.lName.trim(),
//            oName = ui.oName?.trim(),
//            age = ui.ageText.toIntOrNull() ?: 0,
//            street = ui.street,
//            invitedBy = ui.invitedBy,
//            invitedByType = ui.invitedByType,
//            educationPreference = ui.educationPreference,
//            leftHomeDate = ui.leftHomeDate,
//            reasonLeftHome = ui.reasonLeftHome,
//            leftStreetDate = ui.leftStreetDate,
//            lastClass = ui.lastClass,
//            previousSchool = ui.previousSchool,
//            reasonLeftSchool = ui.reasonLeftSchool,
//            homePreference = ui.homePreference,
//            goHomeDate = ui.goHomeDate,
//            region = ui.region,
//            district = ui.district,
//            county = ui.county,
//            sCounty = ui.sCounty,
//            parish = ui.parish,
//            village = ui.village,
//            memberFName1 = ui.memberFName1,
//            memberLName1 = ui.memberLName1,
//            relationShip1 = ui.relationShip1,
//            telephone1a = ui.telephone1a,
//            telephone1b = ui.telephone1b,
//            memberFName2 = ui.memberFName2,
//            memberLName2 = ui.memberLName2,
//            relationShip2 = ui.relationShip2,
//            telephone2a = ui.telephone2a,
//            telephone2b = ui.telephone2b,
//            memberFName3 = ui.memberFName3,
//            memberLName3 = ui.memberLName3,
//            relationShip3 = ui.relationShip3,
//            telephone3a = ui.telephone3a,
//            telephone3b = ui.telephone3b,
//            acceptedJesus = ui.acceptedJesus,
//            acceptedJesusDate = ui.acceptedJesusDate,
//            whoPrayed = ui.whoPrayed,
//            graduated = ui.graduated,
//            outcome = ui.outcome,
//            registrationStatus = status,
//            createdAt = if (ui.createdAt == 0L) now else ui.createdAt,
//            updatedAt = now
//        )
//
//    private fun ChildFormUiState.from(c: Child) = copy(
//        childId = c.childId,
//        profileImg = c.profileImg,
//        fName = c.fName,
//        lName = c.lName,
//        oName = c.oName,
//        ageText = c.age.toString(),
//        street = c.street,
//        invitedBy = c.invitedBy,
//        invitedByType = c.invitedByType,
//        educationPreference = c.educationPreference,
//        leftHomeDate = c.leftHomeDate,
//        reasonLeftHome = c.reasonLeftHome,
//        leftStreetDate = c.leftStreetDate,
//        lastClass = c.lastClass,
//        previousSchool = c.previousSchool,
//        reasonLeftSchool = c.reasonLeftSchool,
//        homePreference = c.homePreference,
//        goHomeDate = c.goHomeDate,
//        region = c.region,
//        district = c.district,
//        county = c.county,
//        sCounty = c.sCounty,
//        parish = c.parish,
//        village = c.village,
//        memberFName1 = c.memberFName1,
//        memberLName1 = c.memberLName1,
//        relationShip1 = c.relationShip1,
//        telephone1a = c.telephone1a,
//        telephone1b = c.telephone1b,
//        memberFName2 = c.memberFName2,
//        memberLName2 = c.memberLName2,
//        relationShip2 = c.relationShip2,
//        telephone2a = c.telephone2a,
//        telephone2b = c.telephone2b,
//        memberFName3 = c.memberFName3,
//        memberLName3 = c.memberLName3,
//        relationShip3 = c.relationShip3,
//        telephone3a = c.telephone3a,
//        telephone3b = c.telephone3b,
//        acceptedJesus = c.acceptedJesus,
//        acceptedJesusDate = c.acceptedJesusDate,
//        whoPrayed = c.whoPrayed,
//        outcome = c.outcome,
//        graduated = c.graduated,
//        registrationStatus = c.registrationStatus,
//        createdAt = c.createdAt
//    )
//}
//
//data class ChildFormUiState(
//    val loading: Boolean = false,
//    val saving: Boolean = false,
//    val error: String? = null,
//    val isNew: Boolean = true,
//
//    val childId: String = "",
//    val profileImg: String = "",
//    val fName: String = "",
//    val lName: String = "",
//    val oName: String? = null,
//    val ageText: String = "",
//    val street: String = "",
//
//    val invitedBy: Individual = Individual.UNCLE,
//    val invitedByType: String = "",
//    val educationPreference: EducationPreference = EducationPreference.NONE,
//
//    val homePreference: Reply = Reply.NO,
//    val acceptedJesus: Reply = Reply.NO,
//    val whoPrayed: Individual = Individual.UNCLE,
//    val registrationStatus: RegistrationStatus = RegistrationStatus.BASICINFOR,
//
////    val createdAt: Long = System.currentTimeMillis(),
//    val createdAt: Long = 0L,
//
//    val leftHomeDate: Long? = null,
//    val reasonLeftHome: String? = null,
//    val leftStreetDate: Long? = null,
//
//    val lastClass: String? = null,
//    val previousSchool: String? = null,
//    val reasonLeftSchool: String? = null,
//
//    val goHomeDate: Long? = null,
//    val region: String? = null,
//    val district: String? = null,
//    val county: String? = null,
//    val sCounty: String? = null,
//    val parish: String? = null,
//    val village: String? = null,
//
//    val memberFName1: String? = null,
//    val memberLName1: String? = null,
//    val relationShip1: RelationShip = RelationShip.NONE,
//    val telephone1a: String? = null,
//    val telephone1b: String? = null,
//
//    val memberFName2: String? = null,
//    val memberLName2: String? = null,
//    val relationShip2: RelationShip = RelationShip.NONE,
//    val telephone2a: String? = null,
//    val telephone2b: String? = null,
//
//    val memberFName3: String? = null,
//    val memberLName3: String? = null,
//    val relationShip3: RelationShip = RelationShip.NONE,
//    val telephone3a: String? = null,
//    val telephone3b: String? = null,
//    val graduated: Reply = Reply.NO,
//
//    val acceptedJesusDate: Long? = null,
//    val outcome: String? = null
//)
