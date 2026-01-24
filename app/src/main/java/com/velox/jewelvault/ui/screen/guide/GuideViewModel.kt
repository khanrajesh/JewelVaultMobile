package com.velox.jewelvault.ui.screen.guide

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.velox.jewelvault.BuildConfig
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
    val questions: List<String> = emptyList()
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
            title = "Inventory, scan & labels",
            steps = listOf(
                "Go to Inventory, choose a category then sub-category to load items with purity, weight and price.",
                "Use the search bar or filters to narrow by purity, tags, weight band or making charges.",
                "Tap the camera/scan icon to open the QR/Barcode scanner and point at a label to open that item instantly.",
                "Open an item to edit details, attach images and generate or re-print labels to the paired Bluetooth printer.",
                "Use Bulk Import from the menu to upload CSV/Excel files and refresh stock in one go.",
                "During audits, use Audit mode and scan items to mark what is physically verified."
            ),
            questions = listOf(
                "Can you find items quickly by scanning labels instead of searching?",
                "Do printed labels show the correct template, price and metal purity?"
            )
        ),
        GuideSection(
            title = "Customers & khata",
            steps = listOf(
                "Open Customers and tap Add to capture name, mobile, GSTIN, tags and notes.",
                "Track dues in the Khata view; filter by pending or cleared to focus on collections.",
                "Open a customer to see invoice timeline, outstanding balance and recent payments.",
                "Record a payment or adjustment to update the outstanding figure immediately."
            ),
            questions = listOf(
                "Are dues and payments reflecting correctly after every entry?",
                "Did you link the customer to recent invoices for faster lookups?"
            )
        ),
        GuideSection(
            title = "Sales invoices & billing",
            steps = listOf(
                "Start from Sell Invoice; select or add a customer and confirm store details and UPI from Settings.",
                "Add items by scanning their QR/Barcode with the camera or picking from inventory; add exchange items if needed.",
                "Enter quantity, making charges, discounts and other charges; totals recalc live as you type.",
                "Capture customer and owner signatures before completing; preview the PDF to verify amounts and taxes.",
                "Choose payment method (Cash/Card/UPI) and enter paid amount to mark partial or full payment.",
                "Share or print the generated invoice PDF directly from the preview screen."
            ),
            questions = listOf(
                "Are GST/CGST/SGST and rounding values correct on the preview and PDF?",
                "Does scanning an item code populate its weight, purity and rate automatically?"
            )
        ),
        GuideSection(
            title = "Orders & purchase",
            steps = listOf(
                "Open Ledger and switch to Orders to track customer orders, or Purchases for supplier bills.",
                "Create a Purchase entry with supplier name, invoice number/date, and add items with weights and rates.",
                "Attach supplier bills or notes so reconciliation is clear later.",
                "When goods arrive, mark items as received to update stock and close the purchase.",
                "Use filters to view pending, partial and completed purchases during reconciliation."
            ),
            questions = listOf(
                "Do purchase totals match stock increments after marking items received?",
                "Is the supplier invoice number captured and searchable later?"
            )
        ),
        GuideSection(
            title = "Settings, sync & devices",
            steps = listOf(
                "Set up store profile (name, GSTIN, PAN, registration, address, logo) so invoices use the correct header.",
                "Configure taxes, invoice numbering and UPI ID under Billing/Payments before creating invoices.",
                "Pair Bluetooth printers or scanners in Bluetooth Devices (app tested with Seznix printers) and run a test print to save the default device.",
                "Use Sync & Restore to push data to Firebase Storage and verify the latest sync timestamp.",
                "Restore from cloud sync on a new device after signing in with the same account."
            ),
            questions = listOf(
                "Have you scheduled regular Firebase syncs and checked the last sync time?",
                "Does the selected default printer stay paired across app restarts?"
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
