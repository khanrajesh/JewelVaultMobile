package com.velox.jewelvault.ui.screen.guide

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.velox.jewelvault.BuildConfig
import com.velox.jewelvault.R
import com.velox.jewelvault.data.DataStoreManager
import com.velox.jewelvault.ui.components.InputFieldState
import com.velox.jewelvault.utils.ioLaunch
import com.velox.jewelvault.utils.mainScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Named

data class GuideSection(
    val title: String,
    val steps: List<String>,
    val questions: List<String> = emptyList(),
    val images: List<GuideImage> = emptyList()
)

data class GuideImage(
    val resId: Int,
    val caption: String
)

@HiltViewModel
class GuideViewModel @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val dataStoreManager: DataStoreManager,
    @Named("snackMessage") private val snackBarState: MutableState<String>,
    @Named("currentScreenHeading") private val currentScreenHeadingState: MutableState<String>,
) : ViewModel() {

    val nameState = InputFieldState()
    val mobileState = InputFieldState()
    val emailState = InputFieldState()
    val feedbackSummaryState = InputFieldState()
    val feedbackDetailsState = InputFieldState()
    val feedbackType = mutableStateOf("Improvement idea")
    val submitting = mutableStateOf(false)

    val questionnairePrompts = listOf(
        "Which screen were you on?",
        "What were you trying to do?",
        "Steps just before the issue",
        "Anything else we should know?"
    )
    val questionnaireStates = questionnairePrompts.map { InputFieldState() }

    val guideSections = listOf(
        GuideSection(
            title = "Login, startup & first launch",
            steps = listOf(
                "Login using mobile + PIN, or OTP verification when required.",
                "If biometric login is enabled in Settings, use fingerprint/face unlock on the login screen.",
                "After login, Start Loading runs setup tasks: store sync, user sync, feature/subscription sync and backup check.",
                "Grant required permissions in the startup dialog so import/export, restore and notifications work correctly.",
                "When cloud backup exists, choose whether to restore now (merge mode) or skip and continue.",
                "On first launch with missing store setup, the app routes you to Profile before daily operations."
            ),
            questions = listOf(
                "After login, did startup finish without getting stuck on pending tasks?",
                "If backup exists, did the restore choice appear before entering the main app?"
            )
        ),
        GuideSection(
            title = "Dashboard, rates & alerts",
            steps = listOf(
                "Open the app to land on Dashboard; the live metal rate ticker stays pinned at the top.",
                "Pull down or tap refresh to sync metal rates and all dashboard summary cards.",
                "Use the date range chip (Today/7d/30d/Custom) to recalc sales, invoices and customer stats.",
                "Read Sales Summary to see total amount, invoice count and average bill size for the selected range.",
                "Review Top Selling Items/Sub-categories to restock or promote best sellers quickly.",
                "Check Customer Overview for dues/outstanding counts and tap through to customer lists when needed."
            ),
            questions = listOf(
                "Is the ticker updating automatically or after manual refresh?",
                "Do the sales and customer numbers change when you pick another date range?"
            )
        ),
        GuideSection(
            title = "Profile & store setup",
            steps = listOf(
                "Open Profile and fill Store Name, Proprietor, Mobile, Email, Address and logo image.",
                "Complete compliance fields like Jurisdiction, Registration No, GSTIN and PAN.",
                "Set UPI ID so invoice payment QR/details are generated correctly.",
                "Use Save/Update to persist store data locally and sync it to Firestore.",
                "Use Sync from Cloud if this device should pull latest store details from Firestore.",
                "Use the User Management card from Profile to manage staff access."
            ),
            questions = listOf(
                "Do invoice headers show the latest store identity details?",
                "After cloud sync, do profile fields match the expected store record?"
            )
        ),
        GuideSection(
            title = "User management & roles",
            steps = listOf(
                "Open User Management and add users with Full Name, Mobile, Role and 4-6 digit PIN.",
                "Assign role as Manager, Worker or Salesperson based on responsibilities.",
                "Optionally capture Aadhaar, emergency contact, government ID type/number, DOB and blood group.",
                "Edit users when details change; for edit flow, enter a new PIN only when rotating credentials.",
                "Delete users when they leave, while keeping the admin account protected.",
                "Use Sync Users from Cloud to pull latest user records from Firestore."
            ),
            questions = listOf(
                "Can each user log in with only their assigned role and PIN?",
                "After sync, are local users aligned with Firestore user records?"
            )
        ),
        GuideSection(
            title = "Inventory categories, items & filters",
            steps = listOf(
                "From Inventory, manage categories and sub-categories; deletions require Admin PIN confirmation.",
                "Open a sub-category to add or update items with ID, name, entry type, weights, purity and charges.",
                "Use item actions to print with default label template and review QR preview before printing.",
                "Use Inventory Filter to filter by category, sub-category, entry type, purity, firm/order, dates and weight ranges.",
                "Apply or clear filters quickly, then export filtered inventory to Excel when needed.",
                "Use search/sort in inventory lists to reach target items faster during billing."
            ),
            questions = listOf(
                "Are filtered exports matching the on-screen filtered result set?",
                "Do category/sub-category changes reflect immediately in item add/update forms?"
            )
        ),
        GuideSection(
            title = "Import items (Excel)",
            steps = listOf(
                "Open Import Items and export the sample Excel template before preparing bulk data.",
                "Import an `.xlsx` file and review the compact summary: valid rows, mapping needed and errors.",
                "Apply row-level or bulk mapping for category/sub-category, entry type, purity and MC type.",
                "Use auto-mapping suggestions where available, then review unresolved rows.",
                "Export error/improvement reports for corrections when data quality issues exist.",
                "Use Import Valid Items to commit only valid rows to inventory after final confirmation."
            ),
            questions = listOf(
                "Are all required rows moving from Needs Mapping/Error to Valid before final import?",
                "Does imported stock appear correctly under expected category and sub-category?"
            )
        ),
        GuideSection(
            title = "Scan & Add items",
            steps = listOf(
                "Open Scan & Add to onboard multiple items quickly from QR labels.",
                "Select input mode (Auto/Camera/Hardware) depending on environment and scanner availability.",
                "Scan QR payloads; ID and fields auto-populate into editable rows.",
                "Duplicate scans in a short window are ignored to prevent accidental double entries.",
                "Review per-row errors/warnings and fix category/sub-category mapping before final add.",
                "Tap Add Valid Items to save ready rows, or Clear All to restart the batch."
            ),
            questions = listOf(
                "After scan, does the row status move to READY once required fields are valid?",
                "Are duplicate IDs blocked before insertion into inventory?"
            )
        ),
        GuideSection(
            title = "QR/Barcode lookup screen",
            steps = listOf(
                "Open QR/Barcode scanner screen for fast item lookup by camera or hardware scanner.",
                "Scan either full CSV payload or plain ID; app extracts and uses only the item ID for lookup.",
                "Watch snackbar feedback (`Scanned <id>`) and processing status text after each scan.",
                "Review scanned item details list (category, sub-category, purity, weight and estimated value).",
                "Enable Delete Mode from the top-right menu to remove inventory items with confirmation.",
                "If code is invalid, check label quality and ensure scanner is outputting clean text."
            ),
            questions = listOf(
                "Does every valid label immediately resolve to the expected inventory item?",
                "Is delete mode restricted enough to avoid accidental removals?"
            )
        ),
        GuideSection(
            title = "Sales invoices, drafts & billing",
            steps = listOf(
                "Start Sell Invoice by selecting/adding customer and confirming address + GST/PAN fields.",
                "Add items through search, camera scan or hardware scan; scanned result triggers add-item flow immediately.",
                "Use exchange item section for old-metal adjustments and verify total exchange value impact.",
                "Apply discounts, making charges, taxes and extra charges; totals recalculate live.",
                "Use Draft Invoice flow to stage edits and continue later without losing progress.",
                "Open Sell Preview to finalize payment details, signatures, PDF generation, share and print."
            ),
            questions = listOf(
                "Do scan actions populate the correct item and open item edit/add promptly?",
                "Do final totals in preview/PDF match item list, tax and exchange calculations?"
            )
        ),
        GuideSection(
            title = "Customers, payments & khata",
            steps = listOf(
                "Open Customers to add or update customer profile (name, mobile, address, GST/PAN and notes).",
                "Use customer filters like outstanding/khata to focus collection workflows.",
                "Open Customer Details for totals, outstanding balance, payment summaries and transaction history.",
                "Record outstanding adjustments, regular payments and khata payments from the detail screen.",
                "Use edit/delete actions on transactions carefully for reconciliation corrections.",
                "Navigate to Khata Book Plans for plan template operations and customer assignment."
            ),
            questions = listOf(
                "After payment entries, does outstanding balance recalculate correctly?",
                "Do customer transaction logs stay consistent with invoice and khata activities?"
            )
        ),
        GuideSection(
            title = "Khata book plans",
            steps = listOf(
                "Open Khata Book Plans to create predefined monthly savings/payment plans.",
                "Define plan name, pay months, benefit months and optional description.",
                "Use Khata calculator to simulate totals before applying plan to a customer.",
                "Apply plan by customer mobile and monthly amount from plan detail dialog.",
                "Track active khata customers and progress cards from the same screen.",
                "Edit or delete outdated plans to keep scheme options clean."
            ),
            questions = listOf(
                "Are active khata customers and pending totals updating after each payment cycle?",
                "Do edited plan rules apply correctly for new assignments?"
            )
        ),
        GuideSection(
            title = "Orders, purchases & detail pages",
            steps = listOf(
                "Open Order & Purchase and switch tabs to view order list or purchase list.",
                "Tap a row to open detail page: Order Details or Purchase Order Details.",
                "In Order Detail, verify customer link, item/exchange breakup and invoice mapping fields.",
                "In Purchase Detail, verify bill info, item weights, taxes, extra charges and notes.",
                "Use long-press delete carefully; related child rows are removed with confirmation.",
                "Use these pages for reconciliation before period-end reporting/export."
            ),
            questions = listOf(
                "Do order/purchase detail totals match their parent list values?",
                "Are delete flows removing related rows consistently without orphaned records?"
            )
        ),
        GuideSection(
            title = "Purchase entry flow",
            steps = listOf(
                "Use Purchase screen to record supplier purchases with firm and seller details.",
                "Capture bill number/date, address, GST/PAN and applicable tax percentages.",
                "Add purchase items with category/sub-category, gross/net/fine weights, purity and wastage.",
                "Set fine rate, extra charges and notes for each transaction.",
                "Add exchange metal entries where supplier settlement includes old metal.",
                "Save and verify the entry appears under Order & Purchase > Purchase tab."
            ),
            questions = listOf(
                "After save, is purchase visible with correct bill and item counts?",
                "Are tax/extra-charge values reflected in final purchase totals?"
            )
        ),
        GuideSection(
            title = "Audit workflow",
            steps = listOf(
                "Open Audit and select category and sub-category before scanning.",
                "Choose scan mode (Auto/Camera/Hardware) based on device setup.",
                "Scan labels; app extracts item ID from payload and marks item as scanned.",
                "Already scanned IDs are blocked to avoid duplicate audit counts.",
                "Review scanned item list and total scanned counter during floor verification.",
                "Clear audit list when starting a new physical verification cycle."
            ),
            questions = listOf(
                "Do scanned IDs always resolve inside the selected category/sub-category scope?",
                "Is duplicate-scan protection preventing repeat counts?"
            )
        ),
        GuideSection(
            title = "Settings, permissions & app controls",
            steps = listOf(
                "Use Settings to tune network checks, auto-rate refresh, theme style and HID keyboard behavior.",
                "Set default CGST/SGST/IGST and security options like session timeout, auto-logout and biometric auth.",
                "Open Permission settings and toggle Camera, Storage, Bluetooth, Location and Notification access.",
                "Use Share Logs and Reset App Preferences for support and quick resets.",
                "Use Wipe All Data only when required; PIN and OTP verification protect destructive action.",
                "Use Privacy Policy, Terms and Check for Updates under Legal/About sections."
            ),
            questions = listOf(
                "Are required permissions in Enabled state for scanner, bluetooth and file operations?",
                "Do security settings (timeout/biometric) behave as configured on next login cycle?"
            )
        ),
        GuideSection(
            title = "Sync, restore & file operations",
            steps = listOf(
                "Open Sync & Restore and use Quick Actions: Sync Now, Restore, Export File and Import File.",
                "Set automatic sync frequency (daily/weekly/etc.) to reduce manual backup dependency.",
                "Monitor Sync Status panel for last sync date and progress messages.",
                "For restore, choose source (cloud/local file) and validate local file before applying.",
                "Use MERGE for safer additive restore, or REPLACE only when full overwrite is intentional.",
                "Read risk confirmation text and acknowledge responsibility before destructive restore modes."
            ),
            questions = listOf(
                "Is latest sync timestamp updating after successful sync operations?",
                "Are users choosing MERGE/REPLACE intentionally with clear understanding of impact?"
            )
        ),
        GuideSection(
            title = "Bluetooth devices, printers & scanner precautions",
            steps = listOf(
                "Open Bluetooth Scan/Connect, start scan and connect from Connected/Paired/Discovered sections.",
                "Use CONNECT BY MAC ADDRESS (AA:BB:CC:DD:EE:FF) when normal discovery fails.",
                "After connecting a printer, add it as printer and verify in Manage Printers list.",
                "In Manage Printers, set default printer, check connection, connect/disconnect and run test print protocols.",
                "For barcode scanner, prefer HID mode first, set suffix to Enter (CR/CRLF), and keep focus on the intended input field.",
                "Keep duplicate scan guard active, limit scanner symbologies, and test sleep/wake reconnect behavior before production use."
            ),
            questions = listOf(
                "Does your default printer persist across app restarts and reconnect reliably?",
                "After pairing scanner, does one scan auto-fill and auto-trigger expected action?"
            ),
            images = listOf(
                GuideImage(R.drawable.p1, "Printer P1 reference"),
                GuideImage(R.drawable.p2, "Printer P2 reference"),
                GuideImage(R.drawable.s1, "Scanner S1 reference")
            )
        ),
        GuideSection(
            title = "Subscription & feature flags",
            steps = listOf(
                "Open Subscription Details from Settings to review current plan and active/expired status.",
                "Check plan validity dates and refresh if feature list/subscription appears stale.",
                "Review feature list entries to confirm which modules are enabled for this account.",
                "If a feature is disabled remotely, expect related actions/screens to be hidden or restricted.",
                "During onboarding/startup, feature list and subscription are synced from cloud automatically."
            ),
            questions = listOf(
                "Does subscription screen show the expected plan and validity dates?",
                "After backend flag updates, are enabled/disabled features reflecting in the app?"
            )
        )
    )

    init {
        preloadUser()
        currentScreenHeadingState.value = "Guide & Feedback"
    }

    private fun preloadUser() {
        val user = dataStoreManager.getCurrentLoginUser()
        if (nameState.text.isEmpty()) nameState.text = user.name
        if (mobileState.text.isEmpty()) mobileState.text = user.mobileNo
    }

    fun submitFeedback() {
        if (submitting.value) return

        val name = nameState.text.trim()
        val mobile = mobileState.text.trim()
        val email = emailState.text.trim()
        val summary = feedbackSummaryState.text.trim()
        val details = feedbackDetailsState.text.trim()

        if (name.isBlank() || mobile.isBlank() || summary.isBlank()) {
            snackBarState.value = "Name, mobile number and a short summary are required"
            return
        }

        submitting.value = true

        ioLaunch {
            try {
                val (storeIdFlow, _, storeNameFlow) = dataStoreManager.getSelectedStoreInfo()
                val storeId = storeIdFlow.first()
                val storeName = storeNameFlow.first()

                val questionnaireAnswers = questionnairePrompts
                    .zip(questionnaireStates)
                    .mapNotNull { (prompt, state) ->
                        val answer = state.text.trim()
                        if (answer.isNotEmpty()) prompt to answer else null
                    }
                    .toMap()

                val payload = mutableMapOf<String, Any>(
                    "name" to name,
                    "mobile" to mobile,
                    "summary" to summary,
                    "type" to feedbackType.value,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "appVersion" to BuildConfig.VERSION_NAME
                )

                if (email.isNotEmpty()) payload["email"] = email
                if (details.isNotEmpty()) payload["details"] = details
                if (storeId.isNotEmpty()) payload["storeId"] = storeId
                if (storeName.isNotEmpty()) payload["storeName"] = storeName
                if (questionnaireAnswers.isNotEmpty()) payload["questionnaire"] = questionnaireAnswers

                firestore.collection("app_feedback")
                    .add(payload)
                    .await()

                mainScope {
                    snackBarState.value = "Feedback uploaded to Firebase. Thank you!"
                    feedbackSummaryState.clear()
                    feedbackDetailsState.clear()
                    questionnaireStates.forEach { it.clear() }
                }
            } catch (e: Exception) {
                mainScope {
                    snackBarState.value = "Could not submit feedback: ${e.message}"
                }
            } finally {
                mainScope { submitting.value = false }
            }
        }
    }
}
