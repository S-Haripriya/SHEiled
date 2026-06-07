package com.example.sheiled.ui.dashboard

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatDelegate
import androidx.fragment.app.Fragment
import com.example.sheiled.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions

class DriverProfileFragment : Fragment() {

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    // Editable
    private lateinit var tvName: TextView
    private lateinit var tvPhone: TextView
    private lateinit var tvEmail: TextView
    private lateinit var tvBaseFare: TextView
    private lateinit var tvArea: TextView

    // Read-only
    private lateinit var tvLicenseNumber: TextView
    private lateinit var tvLicenseExpiry: TextView
    private lateinit var tvVehicleNumber: TextView
    private lateinit var tvVehicleType: TextView

    private lateinit var tvStatus: TextView

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        val v = inflater.inflate(R.layout.driver_profile, container, false)

        // Find views
        tvName = v.findViewById(R.id.tvName)
        tvPhone = v.findViewById(R.id.tvPhone)
        tvEmail = v.findViewById(R.id.tvEmail)
        tvBaseFare = v.findViewById(R.id.tvBaseFare)
        tvArea = v.findViewById(R.id.tvArea)

        tvLicenseNumber = v.findViewById(R.id.tvLicenseNumber)
        tvLicenseExpiry = v.findViewById(R.id.tvLicenseExpiry)
        tvVehicleNumber = v.findViewById(R.id.tvVehicleNumber)
        tvVehicleType = v.findViewById(R.id.tvVehicleType)
        tvStatus = v.findViewById(R.id.tvStatus)

        loadDriverProfile()

        // Editable fields
        v.findViewById<LinearLayout>(R.id.rowName)
            .setOnClickListener { editTextField("name", "Name") }

        v.findViewById<LinearLayout>(R.id.rowPhone)
            .setOnClickListener { editTextField("phone", "Phone Number") }

        v.findViewById<LinearLayout>(R.id.rowEmail)
            .setOnClickListener { editTextField("email", "Email") }

        v.findViewById<LinearLayout>(R.id.rowArea)
            .setOnClickListener { editTextField("area", "Area of Service") }

        v.findViewById<LinearLayout>(R.id.rowBaseFare)
            .setOnClickListener { editNumberField("baseFare", "Base Fare (₹)") }

        // Settings


        v.findViewById<View>(R.id.btnPermissions)
            .setOnClickListener { openAppSettings() }

        v.findViewById<View>(R.id.btnLogout)
            .setOnClickListener { confirmLogout() }

        return v
    }

    /* ================= LOAD PROFILE ================= */

    private fun loadDriverProfile() {
        val uid = auth.currentUser?.uid ?: return

        db.collection("drivers")
            .document(uid)
            .get()
            .addOnSuccessListener { doc ->
                if (!isAdded || !doc.exists()) return@addOnSuccessListener

                tvName.text = doc.getString("name") ?: "-"
                tvPhone.text = doc.getString("phone") ?: "-"
                tvEmail.text = doc.getString("email") ?: "-"

                val baseFare = doc.getDouble("baseFare") ?: 0.0
                tvBaseFare.text =
                    if (baseFare > 0) "₹$baseFare" else "Not set"

                tvArea.text = doc.getString("area") ?: "Not set"

                tvLicenseNumber.text =
                    doc.getString("licenseNumber") ?: "Not available"

                tvLicenseExpiry.text =
                    doc.getString("licenseExpiryDate") ?: "Not available"

                tvVehicleNumber.text =
                    doc.getString("vehicleNumber") ?: "Not available"

                tvVehicleType.text =
                    doc.getString("vehicleType") ?: "Not available"

                val completed =
                    (doc.getDouble("baseFare") ?: 0.0) > 0 &&
                            !doc.getString("area").isNullOrBlank()

                tvStatus.text =
                    if (completed) "PROFILE COMPLETED"
                    else "PROFILE INCOMPLETE"
            }
    }

    /* ================= EDIT TEXT ================= */

    private fun editTextField(field: String, label: String) {
        val input = EditText(requireContext())
        input.setText(getCurrentValue(field))

        AlertDialog.Builder(requireContext())
            .setTitle("Edit $label")
            .setView(input)
            .setPositiveButton("Save") { _, _ ->
                val value = input.text.toString().trim()
                if (value.isBlank()) {
                    toast("$label cannot be empty")
                    return@setPositiveButton
                }
                updateField(field, value)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    /* ================= EDIT NUMBER ================= */

    private fun editNumberField(field: String, label: String) {
        val input = EditText(requireContext())
        input.inputType =
            InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
        input.setText(getCurrentValue(field).replace("₹", ""))

        AlertDialog.Builder(requireContext())
            .setTitle("Edit $label")
            .setView(input)
            .setPositiveButton("Save") { _, _ ->
                val value = input.text.toString().toDoubleOrNull()
                if (value == null || value <= 0) {
                    toast("$label must be greater than 0")
                    return@setPositiveButton
                }
                updateField(field, value)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    /* ================= UPDATE FIRESTORE ================= */

    private fun updateField(field: String, value: Any) {
        val uid = auth.currentUser?.uid ?: return

        val data = mapOf(field to value)

        db.collection("drivers")
            .document(uid)
            .set(data, SetOptions.merge())
            .addOnSuccessListener {
                validateProfileCompletion()
                loadDriverProfile()
                toast("Updated successfully")
            }
            .addOnFailureListener {
                toast("Firestore error: ${it.message}")
            }
    }

    private fun validateProfileCompletion() {
        val uid = auth.currentUser?.uid ?: return

        db.collection("drivers")
            .document(uid)
            .get()
            .addOnSuccessListener { doc ->
                val fare = doc.getDouble("baseFare") ?: 0.0
                val area = doc.getString("area") ?: ""

                db.collection("drivers")
                    .document(uid)
                    .set(
                        mapOf("profileCompleted" to (fare > 0 && area.isNotBlank())),
                        SetOptions.merge()
                    )
            }
    }

    /* ================= HELPERS ================= */

    private fun getCurrentValue(field: String): String =
        when (field) {
            "name" -> tvName.text.toString()
            "phone" -> tvPhone.text.toString()
            "email" -> tvEmail.text.toString()
            "area" -> tvArea.text.toString()
            "baseFare" -> tvBaseFare.text.toString()
            else -> ""
        }

    private fun toast(msg: String) =
        Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()

    private fun openAppSettings() {
        startActivity(
            Intent(
                Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                Uri.parse("package:${requireContext().packageName}")
            )
        )
    }

    private fun confirmLogout() {
        AlertDialog.Builder(requireContext())
            .setTitle("Logout")
            .setMessage("Are you sure?")
            .setPositiveButton("Yes") { _, _ ->
                auth.signOut()
                startActivity(
                    Intent(
                        requireContext(),
                        com.example.sheiled.ui.auth.AuthChoiceActivity::class.java
                    )
                )
                requireActivity().finish()
            }
            .setNegativeButton("No", null)
            .show()
    }
}
