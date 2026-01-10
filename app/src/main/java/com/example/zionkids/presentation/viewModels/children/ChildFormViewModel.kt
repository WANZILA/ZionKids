package com.example.zionkids.presentation.viewModels.children

//import ChildrenSyncWorker
import com.example.zionkids.domain.sync.ChildrenSyncWorker

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.example.core.utils.FormValidatorUtil
import com.example.zionkids.core.Utils.GenerateId
import com.example.zionkids.core.Utils.picker.PickerFeature
import com.example.zionkids.core.Utils.picker.PickerOption
import com.example.zionkids.data.model.*
import com.example.zionkids.domain.repositories.offline.OfflineChildrenRepository
import com.example.zionkids.domain.repositories.online.StreetsRepository
import com.example.zionkids.domain.repositories.online.TechnicalSkillsRepository
//import com.example.zionkids.domain.sync.ChildrenSyncWorker
import com.google.firebase.Timestamp
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val MAX_AGE = 25

@HiltViewModel
class ChildFormViewModel @Inject constructor(
//    private val repo: ChildrenRepository,
    val repo: OfflineChildrenRepository,
    private val techRepo: TechnicalSkillsRepository,
    private val streetRepo: StreetsRepository,
    @ApplicationContext private val appContext: android.content.Context,
//    private val work: WorkManager
) : ViewModel() {

    var ui by mutableStateOf(ChildFormUiState())

    // Build feature instances by passing the repo flows

    val streetPicker = PickerFeature(
        scope = viewModelScope,
        optionsFlow = streetRepo.streetsPickerWatchAll()
    )

    fun onStreetPicked(opt: PickerOption) {
        // update your form state with client id/name/img
//        update { copy(clientId = opt.id, clientName = opt.name, clientImage = opt.imageUrl) }
        ui =  ui.copy(
            street = opt.name
        )
        streetPicker.clearQuery()
    }

    val technicalSkillsPicker = PickerFeature(
        scope = viewModelScope,
        optionsFlow = techRepo.techSkillsPickerWatchAll()
    )

    fun onTechnicalSkillsPicked(opt: PickerOption) {
        // update your form state with client id/name/img
//        update { copy(clientId = opt.id, clientName = opt.name, clientImage = opt.imageUrl) }
        ui.copy(
            technicalSkills = opt.name
        ).also { ui = it }
            technicalSkillsPicker.clearQuery()
    }

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
            RegistrationStatus.FAMILY     -> RegistrationStatus.SPONSORSHIP
            RegistrationStatus.SPONSORSHIP     -> RegistrationStatus.SPIRITUAL
            RegistrationStatus.SPIRITUAL  -> RegistrationStatus.COMPLETE
            RegistrationStatus.COMPLETE   -> RegistrationStatus.SPIRITUAL
        }
        ui = ui.copy(registrationStatus = step)
    }

//    @RequiresApi(Build.VERSION_CODES.O)
@RequiresApi(Build.VERSION_CODES.O)
fun goNext(onAfterSave: (() -> Unit)? = null) = viewModelScope.launch {
        val ok = validateStep(step)
        if (!ok) return@launch
        step = when (step) {
            RegistrationStatus.BASICINFOR -> RegistrationStatus.BACKGROUND
            RegistrationStatus.BACKGROUND -> RegistrationStatus.EDUCATION
            RegistrationStatus.EDUCATION  -> RegistrationStatus.FAMILY
            RegistrationStatus.FAMILY     -> RegistrationStatus.SPONSORSHIP
            RegistrationStatus.SPONSORSHIP -> RegistrationStatus.SPIRITUAL
            RegistrationStatus.SPIRITUAL  -> RegistrationStatus.COMPLETE
            RegistrationStatus.COMPLETE   -> RegistrationStatus.SPIRITUAL

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
    fun onFirstName(v: String) {
        val res = FormValidatorUtil.validateName(v)   // allows letters, digits, _, -, ., ', :
        ui = ui.copy(fName = res.value, fNameError = res.error)
    }

    fun onLastName(v: String) {
        val res = FormValidatorUtil.validateName(v)
        ui = ui.copy(lName = res.value, lNameError = res.error)
    }

    fun onStreet(v: String) {
        // If you added validateStreet() in the util, call that; otherwise reuse validateName:
        val res = FormValidatorUtil.validateName(v)
        ui = ui.copy(street = res.value, streetError = res.error)
    }



    fun onOtherName(v: String) { ui = ui.copy(oName = v) }

    fun onInvitedBy(v: Individual) { ui = ui.copy(invitedBy = v) }
    fun onEduPref(v: EducationPreference) { ui = ui.copy(educationPreference = v) }
    fun onTechSkills(v: String)  { ui = ui.copy(technicalSkills = v) }
    fun onFormerSponsor(v: Relationship)  { ui = ui.copy(formerSponsor = v) }
    fun onFormerSponsorOther(v: String)  { ui = ui.copy(formerSponsorOther = v) }
    fun onTechnicalSkills(v: String) { ui = ui.copy( technicalSkills = v) }
    fun onConfessedBy(v: ConfessedBy)  { ui = ui.copy(confessedBy = v) }
    fun onMinistryName(v: String) { ui = ui.copy( ministryName = v) }

    fun onGender(v: Gender) { ui = ui.copy( gender = v) }
//    fun onClassGroup(v: ClassGroup) { ui = ui.copy(classGroup = v) }

    fun onDobVerified(v: Boolean) { ui = ui.copy(dobVerified = v) }

    fun onSubCounty(v: String?) { ui = ui.copy(subCounty = v ?: "") }
    fun onSponsored(v: Boolean) { ui = ui.copy(sponsoredForEducation = v) }
//    fun onSponsorId(v: String?) { ui = ui.copy(sponsorId = v ?: "") }
    fun onSponsorNotes(v: String?) { ui = ui.copy(sponsorNotes = v ?: "") }

    fun onGraduated(checked: Boolean) {
        ui = ui.copy(graduated = if (checked) Reply.YES else Reply.NO)
    }
    fun generalNotes(v: String?) { ui = ui.copy(generalComments = v ?: "") }

    fun onClassGroup(v: ClassGroup) {
        // If you added validateStreet() in the util, call that; otherwise reuse validateName:
//        val res = FormValidatorUtil.validateName(v)
        ui = ui.copy(classGroup = v)
    }



    private val kampalaTz = java.util.TimeZone.getTimeZone("Africa/Kampala")

    private fun dobFromAge(age: Int): com.google.firebase.Timestamp {
        val cal = java.util.Calendar.getInstance(kampalaTz)
        // keep today's day & month, go back `age` years
        cal.add(java.util.Calendar.YEAR, -age)
        cal.set(java.util.Calendar.HOUR_OF_DAY, 0)
        cal.set(java.util.Calendar.MINUTE, 0)
        cal.set(java.util.Calendar.SECOND, 0)
        cal.set(java.util.Calendar.MILLISECOND, 0)
        return com.google.firebase.Timestamp(cal.time)
    }

    private fun yearsBetweenDobAndToday(dob: com.google.firebase.Timestamp): Int {
        val now = java.util.Calendar.getInstance(kampalaTz)
        val birth = java.util.Calendar.getInstance(kampalaTz).apply { time = dob.toDate() }

        var years = now.get(java.util.Calendar.YEAR) - birth.get(java.util.Calendar.YEAR)
        val birthdayThisYear =
            now.get(java.util.Calendar.MONTH) > birth.get(java.util.Calendar.MONTH) ||
                    (now.get(java.util.Calendar.MONTH) == birth.get(java.util.Calendar.MONTH) &&
                            now.get(java.util.Calendar.DAY_OF_MONTH) >= birth.get(java.util.Calendar.DAY_OF_MONTH))

        if (!birthdayThisYear) years -= 1
        return years.coerceIn(0, MAX_AGE)
    }

    // AGE -> DOB
    fun onAge(text: String) {
        val clean = text.filter { it.isDigit() }.take(2) // 0–99
        var newUi = ui.copy(ageText = clean)

        val age = clean.toIntOrNull()
        if (age != null && age in 0..MAX_AGE) {
            val inferredDob = dobFromAge(age)
            // mark as inferred; user can toggle verified manually if needed
            newUi = newUi.copy(dob = inferredDob, dobVerified = false)
        }
        ui = newUi
    }

    // DOB -> AGE
    fun onDob(dob: com.google.firebase.Timestamp?) {
        var newUi = ui.copy(dob = dob)
        if (dob != null) {
            val age = yearsBetweenDobAndToday(dob)
            newUi = newUi.copy(ageText = age.toString())
        }
        ui = newUi
    }


    @RequiresApi(Build.VERSION_CODES.O)
    fun validateStep(s: RegistrationStatus): Boolean =
        when (s) {
            RegistrationStatus.BASICINFOR -> {
                val fRes      = FormValidatorUtil.validateName(ui.fName)
                val lRes      = FormValidatorUtil.validateName(ui.lName)
                val streetRes = FormValidatorUtil.validateName(ui.street)
                val ageRes    = FormValidatorUtil.validateAgeString(
                    ui.ageText.orEmpty(),
                    minAge = 0,
                    maxAge = MAX_AGE
                )

//                val dobTs = if (ageRes.isValid) {
//                    dobFromAge(ageRes.value.first)   // keep Kampala TZ behavior
//                } else ui.dob

                ui = ui.copy(
                    fName       = fRes.value,       fNameError  = fRes.error,
                    lName       = lRes.value,       lNameError  = lRes.error,
                    street      = streetRes.value,  streetError = streetRes.error,
                    ageText     = ageRes.value.first.toString(),
                    ageError    = ageRes.error,
//                    dob         = dobTs,
                    error       = null
                )

                val ok = fRes.isValid && lRes.isValid && streetRes.isValid && ageRes.isValid
                if (!ok) {
                    ui = ui.copy(error = "Please fix the highlighted fields.")
                    _events.trySend(ChildFormEvent.Error("Missing or invalid fields"))
                }
                ok
            }
            else -> {
                ui = ui.copy(error = null)
                true
            }
        }


    fun ensureNewIdIfNeeded() {
        if (ui.childId.isBlank()) {
            val now = Timestamp.now()
            ui = ui.copy(
                childId = GenerateId.generateId("child"),
                createdAt = now,
                isNew = true
            )
        }
    }

//    /** Partial merge save so we don't wipe other fields. */
//    fun saveProgress() = viewModelScope.launch {
//        ensureNewIdIfNeeded()
//        val now = Timestamp.now()
//        val id = ui.childId
//        val child = buildChild(id = id, now = now, status = step)
//        runCatching { repo.upsert(child, isNew = false) }
//            .onSuccess { ui = ui.copy(childId = id) }
//    }

    /** Final save. Emits Saved(id) on success; do NOT navigate here. */
    @RequiresApi(Build.VERSION_CODES.O)
    fun save() = viewModelScope.launch {
        ui = ui.copy(saving = true, error = null)

        // ensure BASICINFOR fields are valid before saving
        if (!validateStep(RegistrationStatus.BASICINFOR)) {
            ui = ui.copy(saving = false)
            _events.trySend(ChildFormEvent.Error("Missing/invalid basic info"))
            return@launch
        }

        if (ui.isNew && ui.childId.isBlank()) ensureNewIdIfNeeded()
        val now = Timestamp.now()
        val id = ui.childId
        val child = buildChild(id = id, now = now, status = ui.registrationStatus)

        runCatching { repo.upsert(child, isNew = ui.isNew) }
            .onSuccess {
                ui = ui.copy(saving = false, childId = id, isNew = false)
//                CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
//                    ChildrenSyncScheduler.enqueueNow(appContext)
//                }
//                val req = OneTimeWorkRequestBuilder<ChildrenSyncWorker>()
//                    .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
//                    .build()
//
//                work.enqueueUniqueWork(
//                    "children-push-once",
//                    ExistingWorkPolicy.APPEND_OR_REPLACE,
//                    req
//                )
                val req = OneTimeWorkRequestBuilder<ChildrenSyncWorker>()
                    .setConstraints(
                        Constraints.Builder()
                            .setRequiredNetworkType(NetworkType.CONNECTED)
                            .build()
                    )
                    .build()

                WorkManager.getInstance(appContext).enqueueUniqueWork(
//                    "children-push-once",
//                    ExistingWorkPolicy.REPLACE,
                    "children_sync_queue",
                    ExistingWorkPolicy.APPEND,
                    req
                )

//                val req = OneTimeWorkRequestBuilder<ChildrenSyncWorker>()
//                    .setConstraints(
//                        Constraints.Builder()
//                            .setRequiredNetworkType(NetworkType.CONNECTED)
//                            .build()
//                    )
//                    .build()
//
//                work.enqueueUniqueWork(
//                    /* uniqueName = */ "children-push-once",
//                    /* policy     = */ ExistingWorkPolicy.REPLACE, // don’t append into a stuck chain
//                    /* request    = */ req
//                )
                _events.trySend(ChildFormEvent.Saved(id))
            }
            .onFailure {
                ui = ui.copy(saving = false, error = it.message ?: "Failed to save")
                _events.trySend(ChildFormEvent.Error("Failed to save"))
            }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun finish() = save()

    // ---- helpers ----
    private fun buildChild(id: String, now: Timestamp, status: RegistrationStatus): Child =
        Child(
            childId = id,

            // Basic
            profileImg = ui.profileImg,
            fName = ui.fName.trim(),
            lName = ui.lName.trim(),
            oName = ui.oName.trim(),
            gender = ui.gender,
            age = ui.ageText.toIntOrNull() ?: 0,
            dob = ui.dob,
            dobVerified = ui.dobVerified,
            street = ui.street,
            invitedBy = ui.invitedBy,
            invitedByIndividualId = ui.invitedByIndividualId,
            invitedByTypeOther = ui.invitedByTypeOther,
            educationPreference = ui.educationPreference,
            technicalSkills = ui.technicalSkills,

            // Background
            leftHomeDate = ui.leftHomeDate,
            reasonLeftHome = ui.reasonLeftHome,
            leaveStreetDate = ui.leaveStreetDate,

            // Education
            educationLevel = ui.educationLevel,
            lastClass = ui.lastClass,
            previousSchool = ui.previousSchool,
            reasonLeftSchool = ui.reasonLeftSchool,
            formerSponsor = ui.formerSponsor,
            formerSponsorOther = ui.formerSponsorOther,

            // Resettlement
            resettlementPreference = ui.resettlementPreference,
            resettlementPreferenceOther = ui.resettlementPreferenceOther,
            resettled = ui.resettled,
            resettlementDate = ui.resettlementDate,
            region = ui.region,
            district = ui.district,
            county = ui.county,
            subCounty = ui.subCounty,
            parish = ui.parish,
            village = ui.village,

            // Members
            memberFName1 = ui.memberFName1,
            memberLName1 = ui.memberLName1,
            relationship1 = ui.relationship1,
            telephone1a = ui.telephone1a,
            telephone1b = ui.telephone1b,

            memberFName2 = ui.memberFName2,
            memberLName2 = ui.memberLName2,
            relationship2 = ui.relationship2,
            telephone2a = ui.telephone2a,
            telephone2b = ui.telephone2b,

            memberFName3 = ui.memberFName3,
            memberLName3 = ui.memberLName3,
            relationship3 = ui.relationship3,
            telephone3a = ui.telephone3a,
            telephone3b = ui.telephone3b,

            // Spiritual
            acceptedJesus = ui.acceptedJesus,
            confessedBy = ui.confessedBy,
            ministryName = ui.ministryName,
            acceptedJesusDate = ui.acceptedJesusDate,
            whoPrayed = ui.whoPrayed,
            whoPrayedOther = ui.whoPrayedOther,
            whoPrayedId = ui.whoPrayedId,
            classGroup = ui.classGroup,
            outcome = ui.outcome,
            generalComments = ui.generalComments,

            // Sponsorship
            partnershipForEducation = ui.sponsoredForEducation,
            partnerId = ui.sponsorId,
            partnerFName = ui.sponsorFName,
            partnerLName = ui.sponsorLName,
            partnerTelephone1 = ui.sponsorTelephone1,
            partnerTelephone2 = ui.sponsorTelephone2,
            partnerEmail = ui.sponsorEmail,
            partnerNotes = ui.sponsorNotes,

            // Status & audit
            registrationStatus = status,
            graduated = ui.graduated,
            createdAt = ui.createdAt ?: now,
            updatedAt = now
        )

    private fun ChildFormUiState.from(c: Child) = copy(
        childId = c.childId,
        profileImg = c.profileImg,
        fName = c.fName,
        lName = c.lName,
        oName = c.oName,
        gender = c.gender,
        ageText = c.age.toString(),
        dob = c.dob,
        dobVerified = c.dobVerified,
        street = c.street,
        invitedBy = c.invitedBy,
        invitedByIndividualId = c.invitedByIndividualId,
        invitedByTypeOther = c.invitedByTypeOther,
        educationPreference = c.educationPreference,
        technicalSkills = c.technicalSkills,

        leftHomeDate = c.leftHomeDate,
        reasonLeftHome = c.reasonLeftHome,
        leaveStreetDate = c.leaveStreetDate,

        educationLevel = c.educationLevel,
        lastClass = c.lastClass,
        previousSchool = c.previousSchool,
        reasonLeftSchool = c.reasonLeftSchool,
        formerSponsor = c.formerSponsor,
        formerSponsorOther =  c.formerSponsorOther,

        resettlementPreference = c.resettlementPreference,
        resettlementPreferenceOther = c.resettlementPreferenceOther,
        resettled = c.resettled,
        resettlementDate = c.resettlementDate,
        region = c.region,
        district = c.district,
        county = c.county,
        subCounty = c.subCounty,
        parish = c.parish,
        village = c.village,

        memberFName1 = c.memberFName1,
        memberLName1 = c.memberLName1,
        relationship1 = c.relationship1,
        telephone1a = c.telephone1a,
        telephone1b = c.telephone1b,

        memberFName2 = c.memberFName2,
        memberLName2 = c.memberLName2,
        relationship2 = c.relationship2,
        telephone2a = c.telephone2a,
        telephone2b = c.telephone2b,

        memberFName3 = c.memberFName3,
        memberLName3 = c.memberLName3,
        relationship3 = c.relationship3,
        telephone3a = c.telephone3a,
        telephone3b = c.telephone3b,

        acceptedJesus = c.acceptedJesus,
        confessedBy = c.confessedBy,
        ministryName = c.ministryName,
        acceptedJesusDate = c.acceptedJesusDate,
        whoPrayed = c.whoPrayed,
        whoPrayedOther = c.whoPrayedOther,
        whoPrayedId = c.whoPrayedId,
        classGroup = c.classGroup,
        outcome = c.outcome,
        generalComments = c.generalComments,

        sponsoredForEducation = c.partnershipForEducation,
        sponsorId = c.partnerId,
        sponsorFName = c.partnerFName,
        sponsorLName = c.partnerLName,
        sponsorTelephone1 = c.partnerTelephone1,
        sponsorTelephone2 = c.partnerTelephone2,
        sponsorEmail = c.partnerEmail,
        sponsorNotes = c.partnerNotes,

        graduated = c.graduated,
        registrationStatus = c.registrationStatus,
        createdAt = c.createdAt
    )

}

// -------------------- UI STATE --------------------
data class ChildFormUiState(
    val loading: Boolean = false,
    val saving: Boolean = false,
    val error: String? = null,
    val isNew: Boolean = true,

    // per-field errors
    val fNameError: String? = null,
    val lNameError: String? = null,
    val ageError: String? = null,
    val streetError: String? = null,

    // ===== Basic =====
    val childId: String = "",
    val profileImg: String = "",

    val fName: String = "",
    val lName: String = "",
    val oName: String = "",

    val ageText: String = "",
    val gender: Gender = Gender.MALE,

    val dob: Timestamp? = null,
    val dobVerified: Boolean = false,

    val street: String = "",

    val invitedBy: Individual = Individual.UNCLE,
    val invitedByIndividualId: String = "",
    val invitedByTypeOther: String = "",

    val educationPreference: EducationPreference = EducationPreference.NONE,
    val technicalSkills: String = "",

    // ===== Background =====
    val leftHomeDate: Timestamp? = null,
    val reasonLeftHome: String = "",
    val leaveStreetDate: Timestamp? = null,

    // ===== Education =====
    val educationLevel: EducationLevel = EducationLevel.NONE,
    val lastClass: String = "",
    val previousSchool: String = "",
    val reasonLeftSchool: String = "",
    val formerSponsor: Relationship = Relationship.NONE,
    val formerSponsorOther: String = "",

    // ===== Resettlement =====
    val resettlementPreference: ResettlementPreference = ResettlementPreference.DIRECT_HOME,
    val resettlementPreferenceOther: String = "",
    val resettled: Boolean = false,
    val resettlementDate: Timestamp? = null,
    val region: String = "",
    val district: String = "",
    val county: String = "",
    val subCounty: String = "",
    val parish: String = "",
    val village: String = "",

    // ===== Members =====
    val memberFName1: String = "",
    val memberLName1: String = "",
    val relationship1: Relationship = Relationship.NONE,
    val telephone1a: String = "",
    val telephone1b: String = "",

    val memberFName2: String = "",
    val memberLName2: String = "",
    val relationship2: Relationship = Relationship.NONE,
    val telephone2a: String = "",
    val telephone2b: String = "",

    val memberFName3: String = "",
    val memberLName3: String = "",
    val relationship3: Relationship = Relationship.NONE,
    val telephone3a: String = "",
    val telephone3b: String = "",

    // ===== Spiritual =====
    val acceptedJesus: Reply = Reply.NO,
    val confessedBy: ConfessedBy = ConfessedBy.NONE,
    val ministryName: String = " ",
    val acceptedJesusDate: Timestamp? = null,
    val whoPrayed: Individual = Individual.UNCLE,
    val whoPrayedOther: String = "",
    val whoPrayedId: String = "",
    val classGroup: ClassGroup = ClassGroup.SERGEANT,
    val outcome: String = "",
    val generalComments: String = "",

    // ===== Status & program =====
    val registrationStatus: RegistrationStatus = RegistrationStatus.BASICINFOR,
    val graduated: Reply = Reply.NO,

    val sponsoredForEducation: Boolean = false,
    val sponsorId: String = "",
    val sponsorFName: String = "",
    val sponsorLName: String = "",
    val sponsorTelephone1: String = "",
    val sponsorTelephone2: String = "",
    val sponsorEmail: String = "",
    val sponsorNotes: String = "",

    // ===== Audit =====
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
)
