package com.wfx.warungpos.data.remote.firebase

import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirebaseRtdbDataSource @Inject constructor(
    private val db: FirebaseDatabase,
) {
    suspend fun write(path: String, value: Any?) {
        db.getReference(path).setValue(value).await()
    }

    suspend fun writeMulti(updates: Map<String, Any?>) {
        db.reference.updateChildren(updates).await()
    }

    suspend fun read(path: String): DataSnapshot =
        db.getReference(path).get().await()

    fun observe(path: String): Flow<DataSnapshot> = callbackFlow {
        val ref = db.getReference(path)
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) { trySend(snapshot) }
            override fun onCancelled(error: DatabaseError) { close(error.toException()) }
        }
        ref.addValueEventListener(listener)
        awaitClose { ref.removeEventListener(listener) }
    }

    fun observeChildren(path: String): Flow<ChildEvent> = callbackFlow {
        val ref = db.getReference(path)
        val listener = object : com.google.firebase.database.ChildEventListener {
            override fun onChildAdded(snapshot: DataSnapshot, prev: String?) {
                trySend(ChildEvent.Added(snapshot))
            }
            override fun onChildChanged(snapshot: DataSnapshot, prev: String?) {
                trySend(ChildEvent.Changed(snapshot))
            }
            override fun onChildRemoved(snapshot: DataSnapshot) {
                trySend(ChildEvent.Removed(snapshot))
            }
            override fun onChildMoved(snapshot: DataSnapshot, prev: String?) = Unit
            override fun onCancelled(error: DatabaseError) { close(error.toException()) }
        }
        ref.addChildEventListener(listener)
        awaitClose { ref.removeEventListener(listener) }
    }

    suspend fun delete(path: String) {
        db.getReference(path).removeValue().await()
    }
}

sealed class ChildEvent {
    abstract val snapshot: DataSnapshot
    data class Added(override val snapshot: DataSnapshot) : ChildEvent()
    data class Changed(override val snapshot: DataSnapshot) : ChildEvent()
    data class Removed(override val snapshot: DataSnapshot) : ChildEvent()
}
