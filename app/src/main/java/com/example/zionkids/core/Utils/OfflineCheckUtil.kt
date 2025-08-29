// com/example/zionkids/core/Utils/Network.kt (file name can be anything)
package com.example.zionkids.core.Utils

import com.google.firebase.firestore.FirebaseFirestoreException

fun Throwable.isOfflineError(): Boolean {
    return this is FirebaseFirestoreException &&
            this.code == FirebaseFirestoreException.Code.UNAVAILABLE
}
