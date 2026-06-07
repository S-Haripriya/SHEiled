package com.example.sheiled.ui.dashboard

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.database.Cursor
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.provider.ContactsContract
import android.provider.MediaStore
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.example.sheiled.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import java.io.File
import java.io.FileOutputStream

class ProfileFragment : Fragment() {

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    private lateinit var imgAvatar: ImageView
    private lateinit var contactContainer: LinearLayout

    /* ---------- LAUNCHERS ---------- */

    private val imagePicker =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            if (uri != null && isAdded) saveImageToInternalStorage(uri)
        }

    // Handles the selection of a contact from the phone book
    private val contactPickerLauncher =
        registerForActivityResult(ActivityResultContracts.PickContact()) { uri ->
            uri?.let { processSelectedContact(it) }
        }

    // Handles requesting the READ_CONTACTS permission
    private val contactPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                contactPickerLauncher.launch(null)
            } else {
                Toast.makeText(requireContext(), "Permission denied to read contacts", Toast.LENGTH_SHORT).show()
            }
        }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        val v = inflater.inflate(R.layout.layout_profile, container, false)

        val tvName = v.findViewById<TextView>(R.id.tvName)
        val tvPhone = v.findViewById<TextView>(R.id.tvPhone)
        val tvEmail = v.findViewById<TextView>(R.id.tvEmail)
        contactContainer = v.findViewById(R.id.contactContainer)
        imgAvatar = v.findViewById(R.id.imgAvatar)

        loadProfile(tvName, tvPhone, tvEmail)
        loadEmergencyContacts(contactContainer)
        displaySavedImage()

        imgAvatar.setOnClickListener { imagePicker.launch("image/*") }
        v.findViewById<View>(R.id.btnDeletePhoto)?.setOnClickListener { deleteProfileImage() }

        v.findViewById<LinearLayout>(R.id.rowName)?.setOnClickListener { editMandatoryField("name", "Name") }
        v.findViewById<LinearLayout>(R.id.rowPhone)?.setOnClickListener { editMandatoryField("phone", "Phone Number") }
        v.findViewById<LinearLayout>(R.id.rowEmail)?.setOnClickListener { editMandatoryField("email", "Email") }
        v.findViewById<LinearLayout>(R.id.btnPermissions)?.setOnClickListener { openAppSettings() }
        v.findViewById<LinearLayout>(R.id.btnLogout)?.setOnClickListener { confirmLogout() }
        v.findViewById<TextView>(R.id.tvAddContact)?.setOnClickListener { showAddContactOptions() }

        return v
    }

    /* ---------------- EMERGENCY CONTACTS LOGIC ---------------- */

    private fun showAddContactOptions() {
        val options = arrayOf("Manual Entry", "Import from Phone Contacts")
        AlertDialog.Builder(requireContext())
            .setTitle("Add Contact")
            .setItems(options) { _, which ->
                if (which == 0) showManualAddDialog() else checkContactPermission()
            }.show()
    }

    private fun checkContactPermission() {
        when {
            ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.READ_CONTACTS)
                    == PackageManager.PERMISSION_GRANTED -> {
                contactPickerLauncher.launch(null)
            }
            else -> {
                contactPermissionLauncher.launch(Manifest.permission.READ_CONTACTS)
            }
        }
    }

    private fun processSelectedContact(contactUri: Uri) {
        val cr = requireActivity().contentResolver
        val cursor = cr.query(contactUri, null, null, null, null)

        if (cursor != null && cursor.moveToFirst()) {
            val id = cursor.getString(cursor.getColumnIndexOrThrow(ContactsContract.Contacts._ID))
            val name = cursor.getString(cursor.getColumnIndexOrThrow(ContactsContract.Contacts.DISPLAY_NAME))

            // Query Phone Numbers for this Contact ID
            val pCursor = cr.query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI, null,
                ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = ?",
                arrayOf(id), null
            )

            var phone = ""
            if (pCursor != null && pCursor.moveToFirst()) {
                phone = pCursor.getString(pCursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.NUMBER))
                pCursor.close()
            }
            cursor.close()

            // Cleanup phone number (remove spaces/dashes) and validate
            val cleanPhone = phone.replace(Regex("[^0-9]"), "").takeLast(10)

            if (cleanPhone.length == 10) {
                saveContactToFirebase(name, cleanPhone)
            } else {
                Toast.makeText(requireContext(), "Selected contact has no valid 10-digit number", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showManualAddDialog() {
        val layout = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(40, 20, 40, 0)
        }
        val nameInput = EditText(requireContext()).apply { hint = "Name" }
        val phoneInput = EditText(requireContext()).apply { hint = "Phone (10 digits)" }
        layout.addView(nameInput)
        layout.addView(phoneInput)

        AlertDialog.Builder(requireContext())
            .setTitle("Manual Entry")
            .setView(layout)
            .setPositiveButton("Save") { _, _ ->
                val name = nameInput.text.toString().trim()
                val phone = phoneInput.text.toString().trim()
                if (name.isNotEmpty() && phone.length == 10) {
                    saveContactToFirebase(name, phone)
                } else {
                    Toast.makeText(requireContext(), "Invalid Input", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun saveContactToFirebase(name: String, phone: String) {
        val contact = mapOf("name" to name, "phone" to phone)
        auth.uid?.let { uid ->
            db.collection("users").document(uid)
                .update("emergency_contacts", FieldValue.arrayUnion(contact))
                .addOnSuccessListener {
                    Toast.makeText(requireContext(), "Contact Added", Toast.LENGTH_SHORT).show()
                    loadEmergencyContacts(contactContainer)
                }
        }
    }

    /* ---------------- DATA LOADING & STORAGE ---------------- */

    private fun loadEmergencyContacts(container: LinearLayout) {
        val uid = auth.uid ?: return
        db.collection("users").document(uid).addSnapshotListener { doc, _ ->
            if (!isAdded || doc == null) return@addSnapshotListener
            container.removeAllViews()
            val list = doc.get("emergency_contacts") as? List<Map<String, String>> ?: emptyList()
            list.forEach { contact ->
                val row = TextView(requireContext()).apply {
                    text = "${contact["name"]} - ${contact["phone"]}"
                    textSize = 15f
                    setPadding(12, 24, 12, 24)
                    setTextColor(resources.getColor(android.R.color.black, null))
                    setOnClickListener { showEditDeleteContactDialog(contact, list.size) }
                }
                container.addView(row)
            }
        }
    }

    private fun saveImageToInternalStorage(uri: Uri) {
        val bitmap = MediaStore.Images.Media.getBitmap(requireActivity().contentResolver, uri)
        val file = File(requireContext().filesDir, "profile_image.jpg")
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
        }
        displaySavedImage()
    }

    private fun displaySavedImage() {
        val file = File(requireContext().filesDir, "profile_image.jpg")
        if (file.exists()) {
            val bitmap = BitmapFactory.decodeFile(file.absolutePath)
            imgAvatar.setImageBitmap(bitmap)
        }
    }

    private fun deleteProfileImage() {
        val file = File(requireContext().filesDir, "profile_image.jpg")
        if (file.exists()) file.delete()
        imgAvatar.setImageResource(R.drawable.ic_profile)
    }

    private fun loadProfile(name: TextView, phone: TextView, email: TextView) {
        val uid = auth.currentUser?.uid ?: return
        db.collection("users").document(uid).get().addOnSuccessListener {
            if (!isAdded) return@addOnSuccessListener
            name.text = it.getString("name") ?: "N/A"
            phone.text = it.getString("phone") ?: "N/A"
            email.text = it.getString("email") ?: "N/A"
        }
    }

    private fun editMandatoryField(field: String, label: String) {
        val input = EditText(requireContext()).apply {
            hint = "Enter $label"
            if (field == "email") inputType = android.text.InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
        }

        AlertDialog.Builder(requireContext())
            .setTitle("Edit $label")
            .setView(input)
            .setPositiveButton("Save") { _, _ ->
                val value = input.text.toString().trim()

                if (value.isEmpty()) {
                    Toast.makeText(requireContext(), "$label cannot be empty", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                if (field == "email" && !android.util.Patterns.EMAIL_ADDRESS.matcher(value).matches()) {
                    Toast.makeText(requireContext(), "Invalid Email Format", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                auth.uid?.let {
                    db.collection("users").document(it).update(field, value)
                }

                loadProfile(
                    requireView().findViewById(R.id.tvName),
                    requireView().findViewById(R.id.tvPhone),
                    requireView().findViewById(R.id.tvEmail)
                )
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    private fun showEditDeleteContactDialog(contact: Map<String, String>, count: Int) {
        val options = arrayOf("Edit", "Delete")
        AlertDialog.Builder(requireContext())
            .setItems(options) { _, which ->
                if (which == 0) showEditContactDialog(contact)
                else if (count > 2) deleteContact(contact)
                else Toast.makeText(requireContext(), "Minimum 2 contacts required", Toast.LENGTH_SHORT).show()
            }.show()
    }

    private fun showEditContactDialog(contact: Map<String, String>) {
        val layout = LinearLayout(requireContext()).apply { orientation = LinearLayout.VERTICAL }
        val name = EditText(requireContext()).apply { setText(contact["name"]) }
        val phone = EditText(requireContext()).apply { setText(contact["phone"]) }
        layout.addView(name); layout.addView(phone)

        AlertDialog.Builder(requireContext())
            .setTitle("Edit Contact").setView(layout)
            .setPositiveButton("Update") { _, _ ->
                val updated = mapOf("name" to name.text.toString(), "phone" to phone.text.toString())
                auth.uid?.let { uid ->
                    db.collection("users").document(uid).update("emergency_contacts", FieldValue.arrayRemove(contact))
                        .addOnSuccessListener { db.collection("users").document(uid).update("emergency_contacts", FieldValue.arrayUnion(updated)) }
                }
            }.setNegativeButton("Cancel", null).show()
    }

    private fun deleteContact(contact: Map<String, String>) {
        auth.uid?.let { db.collection("users").document(it).update("emergency_contacts", FieldValue.arrayRemove(contact)) }
    }

    private fun confirmLogout() {
        AlertDialog.Builder(requireContext())
            .setTitle("Logout")
            .setMessage("Are you sure you want to logout?")
            .setPositiveButton("Yes") { _, _ ->

                // Clear local session
                requireContext()
                    .getSharedPreferences("SHEILD_PREFS", android.content.Context.MODE_PRIVATE)
                    .edit()
                    .clear()
                    .apply()

                // Clear Firebase session
                FirebaseAuth.getInstance().signOut()

                // Go to AuthChoiceActivity
                val intent = Intent(
                    requireContext(),
                    com.example.sheiled.ui.auth.AuthChoiceActivity::class.java
                )
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)

            }
            .setNegativeButton("No", null)
            .show()
    }

    private fun openAppSettings() {
        startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${requireContext().packageName}")))
    }
}