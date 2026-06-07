package com.example.sheiled.ui.auth

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.example.sheiled.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.cloudinary.android.MediaManager
import com.cloudinary.android.callback.UploadCallback
import com.cloudinary.android.callback.ErrorInfo



class DriverRegisterActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    private var licenseUri: Uri? = null

    // UI refs (needed inside callbacks)
    private lateinit var etName: EditText
    private lateinit var etPhone: EditText
    private lateinit var etEmail: EditText
    private lateinit var etPassword: EditText
    private lateinit var etVehicle: EditText
    private lateinit var etLicenseNumber: EditText
    private lateinit var etLicenseExpiry: EditText
    private lateinit var rgVehicle: RadioGroup
    private lateinit var btnRegister: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_driver_register)

        // ✅ Cloudinary init (ONLY cloud_name needed)
        val config = mapOf(
            "cloud_name" to "ddjeztnsv"   // <-- your cloud name
        )
        MediaManager.init(this, config)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        // UI binding
        etName = findViewById(R.id.etName)
        etPhone = findViewById(R.id.etPhone)
        etEmail = findViewById(R.id.etEmail)
        etPassword = findViewById(R.id.etPassword)
        etVehicle = findViewById(R.id.etVehicleNumber)
        etLicenseNumber = findViewById(R.id.etLicenseNumber)
        etLicenseExpiry = findViewById(R.id.etLicenseExpiry)
        rgVehicle = findViewById(R.id.rgVehicleType)

        val btnUpload = findViewById<Button>(R.id.btnUploadLicense)
        btnRegister = findViewById(R.id.btnRegister)

        btnUpload.setOnClickListener {
            licensePicker.launch("image/*")
        }

        btnRegister.setOnClickListener {
            validateAndRegisterDriver()
        }
    }

    // ---------------- VALIDATION ----------------
    private fun validateAndRegisterDriver() {

        if (
            etName.text.isNullOrBlank() ||
            etEmail.text.isNullOrBlank() ||
            etPassword.text.isNullOrBlank() ||
            etVehicle.text.isNullOrBlank() ||
            etLicenseNumber.text.isNullOrBlank() ||
            etLicenseExpiry.text.isNullOrBlank()
        ) {
            toast("Please fill all fields")
            return
        }

        if (etPhone.text.length < 10) {
            toast("Enter a valid phone number")
            return
        }

        if (rgVehicle.checkedRadioButtonId == -1) {
            toast("Please select vehicle type")
            return
        }

        if (licenseUri == null) {
            toast("Please upload license image")
            return
        }

        btnRegister.isEnabled = false

        // 🔐 Create auth user
        auth.createUserWithEmailAndPassword(
            etEmail.text.toString().trim(),
            etPassword.text.toString().trim()
        ).addOnSuccessListener {
            uploadLicenseToCloudinary()
        }.addOnFailureListener { e ->
            btnRegister.isEnabled = true
            toast("Authentication failed: ${e.message}")
        }
    }

    // ---------------- STEP 7: CLOUDINARY UPLOAD ----------------
    private fun uploadLicenseToCloudinary() {

        MediaManager.get().upload(licenseUri!!)
            .unsigned("driver_license_upload")

            .callback(object : UploadCallback {

                override fun onStart(requestId: String) {
                    toast("Uploading license...")
                }

                override fun onProgress(
                    requestId: String,
                    bytes: Long,
                    totalBytes: Long
                ) {
                    // optional
                }

                override fun onSuccess(
                    requestId: String,
                    resultData: Map<*, *>
                ) {
                    val imageUrl = resultData["secure_url"] as String
                    saveDriverDataToFirestore(imageUrl)
                }

                override fun onError(
                    requestId: String,
                    error: ErrorInfo
                ) {
                    auth.currentUser?.delete()
                    btnRegister.isEnabled = true
                    toast("Upload failed: ${error.description}")
                }

                override fun onReschedule(
                    requestId: String,
                    error: ErrorInfo
                ) {
                    toast("Upload rescheduled: ${error.description}")
                }
            })
            .dispatch()
    }

        // ---------------- STEP 8: SAVE TO FIRESTORE ----------------
    private fun saveDriverDataToFirestore(imageUrl: String) {

        val uid = auth.currentUser!!.uid

        val vehicleType =
            if (rgVehicle.checkedRadioButtonId == R.id.rbOwn) "Own" else "Rented"

        val driverData = hashMapOf(
            "role" to "driver",
            "status" to "pending_verification",
            "verified" to false,
             "availability" to "pending",
            "name" to etName.text.toString().trim(),
            "phone" to etPhone.text.toString().trim(),
            "email" to etEmail.text.toString().trim(),

            "licenseNumber" to etLicenseNumber.text.toString().trim(),
            "licenseExpiryDate" to etLicenseExpiry.text.toString().trim(),
            "licenseImageUrl" to imageUrl,

            "vehicleNumber" to etVehicle.text.toString().trim(),
            "vehicleType" to vehicleType,

            "createdAt" to System.currentTimeMillis()
        )

        db.collection("drivers")
            .document(uid)
            .set(driverData)
            .addOnSuccessListener {

                toast("Registration submitted. Await admin approval.")

                auth.signOut()
                startActivity(
                    Intent(this, LoginActivity::class.java)
                )
                finish()
            }
            .addOnFailureListener { e ->
                auth.currentUser?.delete()
                btnRegister.isEnabled = true
                toast("Failed to save data: ${e.message}")
            }
    }

    // ---------------- IMAGE PICKER ----------------
    private val licensePicker =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            if (uri != null) {
                licenseUri = uri
                toast("License image selected")
            }
        }

    private fun toast(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_LONG).show()
    }
}
