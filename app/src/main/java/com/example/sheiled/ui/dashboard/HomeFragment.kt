package com.example.sheiled.ui.dashboard

import android.app.AlertDialog
import android.content.Context.MODE_PRIVATE
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.*
import android.widget.*
import androidx.fragment.app.Fragment
import com.example.sheiled.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.util.UUID

class HomeFragment : Fragment() {

    private lateinit var contactContainer: LinearLayout
    private lateinit var tvAlertStatus: TextView

    private val db by lazy { FirebaseFirestore.getInstance() }
    private val auth by lazy { FirebaseAuth.getInstance() }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        val view = inflater.inflate(R.layout.layout_home_emergency, container, false)

        contactContainer = view.findViewById(R.id.contactContainer)
        tvAlertStatus = view.findViewById(R.id.tvAlertStatus)
        view.findViewById<Button>(R.id.btnManageContacts).setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.mapContainer, ProfileFragment())
                .addToBackStack(null)
                .commit()
        }
        view.findViewById<Button>(R.id.btnPolice).setOnClickListener { makeCall("112") }
        view.findViewById<Button>(R.id.btnAmbulance).setOnClickListener { makeCall("108") }
        view.findViewById<Button>(R.id.btnWomenHelpline).setOnClickListener { makeCall("181") }

        view.findViewById<Button>(R.id.btnEditAlert).setOnClickListener {
            showAlertManagerDialog()
        }

        loadEmergencyContacts()
        loadAlertMessageStatus()

        return view
    }

    /* ================= EMERGENCY CONTACTS ================= */

    private fun loadEmergencyContacts() {
        val uid = auth.currentUser?.uid ?: return

        db.collection("users").document(uid).get()
            .addOnSuccessListener { doc ->
                val contacts = doc.get("emergency_contacts") as? List<*> ?: emptyList<Any>()
                contactContainer.removeAllViews()

                contacts.filterIsInstance<Map<*, *>>().forEach { contact ->
                    val name = contact["name"] as? String ?: "Contact"
                    val phone = contact["phone"] as? String ?: return@forEach

                    val itemView = layoutInflater.inflate(
                        R.layout.item_emergency_contact,
                        contactContainer,
                        false
                    )

                    itemView.findViewById<TextView>(R.id.tvName).text = name
                    itemView.findViewById<TextView>(R.id.tvPhone).text = phone
                    itemView.findViewById<View>(R.id.btnCall).setOnClickListener {
                        makeCall(phone)
                    }

                    contactContainer.addView(itemView)
                }
            }
    }

    /* ================= ALERT STATUS ================= */

    private fun loadAlertMessageStatus() {
        val uid = auth.currentUser?.uid ?: return

        db.collection("users").document(uid).get()
            .addOnSuccessListener { doc ->
                val alerts = doc.get("alert_messages") as? List<*> ?: emptyList<Any>()

                tvAlertStatus.text =
                    if (alerts.isEmpty())
                        "No alert message set"
                    else
                        "${alerts.size} alert message(s) configured"
            }
    }

    /* ================= ALERT MANAGER ================= */

    private fun showAlertManagerDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_alert_manager, null)
        val listView = dialogView.findViewById<ListView>(R.id.listAlerts)

        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .setCancelable(true)
            .create()

        loadAlertList(listView, dialog)

        dialogView.findViewById<Button>(R.id.btnAddAlert).setOnClickListener {
            dialog.dismiss()
            showAddOrEditAlertDialog(null)
        }

        dialog.show()
    }

    private fun loadAlertList(listView: ListView, dialog: AlertDialog) {
        val uid = auth.currentUser?.uid ?: return

        db.collection("users").document(uid).get()
            .addOnSuccessListener { doc ->
                val alerts = doc.get("alert_messages") as? List<*> ?: emptyList<Any>()

                listView.adapter = object : BaseAdapter() {
                    override fun getCount() = alerts.size
                    override fun getItem(position: Int) = alerts[position]
                    override fun getItemId(position: Int) = position.toLong()

                    override fun getView(
                        position: Int,
                        convertView: View?,
                        parent: ViewGroup
                    ): View {
                        val view = convertView ?: layoutInflater.inflate(
                            R.layout.item_alert_message,
                            parent,
                            false
                        )

                        val alert = alerts[position] as Map<*, *>
                        val message = alert["message"] as? String ?: ""

                        view.findViewById<TextView>(R.id.tvAlertMessage).text = message

                        view.findViewById<View>(R.id.btnEdit).setOnClickListener {
                            dialog.dismiss()
                            showAddOrEditAlertDialog(alert)
                        }

                        view.findViewById<View>(R.id.btnDelete).setOnClickListener {
                            deleteAlert(alert)
                            dialog.dismiss()
                        }

                        return view
                    }
                }
            }
    }

    /* ================= ADD / EDIT ALERT ================= */

    private fun showAddOrEditAlertDialog(existingAlert: Map<*, *>?) {

        val uid = auth.currentUser?.uid ?: return
        val dialogView = layoutInflater.inflate(R.layout.dialog_edit_alert, null)

        val etMessage = dialogView.findViewById<EditText>(R.id.etAlertMessage)
        val listContacts = dialogView.findViewById<ListView>(R.id.listContacts)
        val checkDefault = dialogView.findViewById<CheckBox>(R.id.checkDefault)
        val btnSave = dialogView.findViewById<Button>(R.id.btnSaveAlert)

        val contactPhones = mutableListOf<String>()
        val displayNames = mutableListOf<String>()

        if (existingAlert != null) {
            etMessage.setText(existingAlert["message"] as? String ?: "")
            checkDefault.isChecked = existingAlert["isSelected"] as? Boolean ?: false
        }

        db.collection("users").document(uid).get()
            .addOnSuccessListener { doc ->
                val contacts = doc.get("emergency_contacts") as? List<*> ?: emptyList<Any>()

                contacts.filterIsInstance<Map<*, *>>().forEach { c ->
                    val name = c["name"] as? String ?: "Contact"
                    val phone = c["phone"] as? String ?: return@forEach
                    displayNames.add("$name - $phone")
                    contactPhones.add(phone)
                }

                listContacts.adapter = ArrayAdapter(
                    requireContext(),
                    android.R.layout.simple_list_item_multiple_choice,
                    displayNames
                )

                val selected =
                    existingAlert?.get("contacts") as? List<*> ?: emptyList<Any>()

                selected.forEach {
                    val index = contactPhones.indexOf(it)
                    if (index != -1) listContacts.setItemChecked(index, true)
                }
            }

        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .setCancelable(true)
            .create()

        btnSave.setOnClickListener {

            val message = etMessage.text.toString().trim()
            if (message.isBlank()) return@setOnClickListener

            val selectedPhones = mutableListOf<String>()
            for (i in 0 until listContacts.count) {
                if (listContacts.isItemChecked(i)) {
                    selectedPhones.add(contactPhones[i])
                }
            }

            if (selectedPhones.isEmpty()) return@setOnClickListener

            val alertId = existingAlert?.get("id") as? String ?: UUID.randomUUID().toString()

            saveAlert(uid, alertId, message, selectedPhones, checkDefault.isChecked)
            dialog.dismiss()
        }

        dialog.show()
    }

    /* ================= FIRESTORE HELPERS ================= */

    private fun saveAlert(
        uid: String,
        id: String,
        message: String,
        contacts: List<String>,
        isSelected: Boolean
    ) {
        val userRef = db.collection("users").document(uid)

        userRef.get().addOnSuccessListener { doc ->
            val alerts = doc.get("alert_messages") as? List<*> ?: emptyList<Any>()

            val updated: MutableList<MutableMap<String, Any>> = alerts
                .filterIsInstance<Map<*, *>>()
                .filter { it["id"] != id }
                .map { alert ->
                    val map = mutableMapOf<String, Any>()
                    alert.forEach { (k, v) ->
                        if (k is String && v != null) {
                            map[k] = v
                        }
                    }
                    map["isSelected"] = false
                    map
                }
                .toMutableList()


            val newAlert = mutableMapOf<String, Any>(
                "id" to id,
                "message" to message,
                "contacts" to contacts,
                "isSelected" to isSelected
            )

            updated.add(newAlert)
            userRef.update("alert_messages", updated)

            if (isSelected) cachePanicData(message, contacts)
            loadAlertMessageStatus()
        }
    }

    private fun deleteAlert(alert: Map<*, *>) {
        val uid = auth.currentUser?.uid ?: return

        db.collection("users").document(uid).get()
            .addOnSuccessListener { doc ->
                val alerts = doc.get("alert_messages") as? List<*> ?: emptyList<Any>()
                val updated = alerts.filter { it != alert }

                db.collection("users").document(uid)
                    .update("alert_messages", updated)
                    .addOnSuccessListener { loadAlertMessageStatus() }
            }
    }

    private fun cachePanicData(message: String, contacts: List<String>) {
        requireContext().getSharedPreferences("PANIC_PREFS", MODE_PRIVATE)
            .edit()
            .putString("ALERT_MESSAGE", message)
            .putStringSet("ALERT_CONTACTS", contacts.toSet())
            .apply()
    }

    private fun makeCall(number: String) {
        startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$number")))
    }
}
