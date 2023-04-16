package com.rentminder

import android.icu.text.SimpleDateFormat
import android.icu.util.Calendar
import android.util.Log
import android.widget.Toast
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.rentminder.dto.Bill
import com.rentminder.dto.Members
import com.rentminder.dto.Payment

class MainViewModel() : ViewModel() {
    var bills : MutableLiveData<List<Bill>> = MutableLiveData()
    var members : MutableLiveData<List<Members>> = MutableLiveData()
    var member: Members? = null

    private lateinit var firestore : FirebaseFirestore

    init {
        firestore = FirebaseFirestore.getInstance()
        firestore.firestoreSettings = FirebaseFirestoreSettings.Builder().build()
        listenToBills()
    }

    fun listenToBills() {
        member?.let {
            member ->
            firestore.collection("Members").document(member.uid).collection("Payments").addSnapshotListener {
                    snapshot, e ->
                if (e != null) {
                    Log.w("Listen failed", e)
                    return@addSnapshotListener
                }
                snapshot?.let {
                    val allBills = ArrayList<Bill>()
                    val documents = snapshot.documents

                    documents.forEach {
                        val bill = it.toObject(Bill::class.java)
                        bill?.let {
                            allBills.add(bill)
                        }
                    }
                    bills.value = allBills
                }
            }
        }
    }

    fun listenToMembers() {
        firestore.collection("Members").addSnapshotListener { snapshot, e ->
            if (e != null) {
                Log.w("Listen failed", e)
                return@addSnapshotListener
            }
            snapshot?.let {
                val allMembers = ArrayList<Members>()
                val documents = snapshot.documents

                documents.forEach {
                    val member = it.toObject(Members::class.java)
                    member?.let {
                        allMembers.add(member)
                    }
                }
                members.value = allMembers
            }
        }
    }

    fun saveBill(selectedBill: Bill) {
        member?.let {
            member ->
            val document =
                if (selectedBill.billId == null || selectedBill.billId.isEmpty()) {
                    firestore.collection("Members").document(member.uid).collection("Payments").document()
                } else {
                    firestore.collection("Members").document(member.uid).collection("Payments").document(selectedBill.billId)
                }
            selectedBill.billId = document.id
            val handle = document.set(selectedBill)
            handle.addOnSuccessListener { Log.d("Firebase", "Document saved") }
            handle.addOnFailureListener { Log.e("Firebase", "Save failed $it") }
        }
    }

    fun saveMember(){
        member?.let {
            member ->
            val handle = firestore.collection("Members").document(member.uid).set(member)
            handle.addOnSuccessListener { Log.d("Firebase", "Document saved") }
            handle.addOnFailureListener { Log.e("Firebase", "Save failed $it") }
        }
    }

    fun delete(bill: Bill) {
        member?.let{
            member ->
            var billCollection = firestore.collection("Members").document(member.uid).collection("Payments").document(bill.billId)
            billCollection.delete()
                .addOnSuccessListener {
                    // The document was successfully deleted
                    Log.d("TAG", "DocumentSnapshot successfully deleted!")
                }
                .addOnFailureListener { e ->
                    // There was an error deleting the document
                    Log.w("TAG", "Error deleting document", e)
                }
        }
    }
}