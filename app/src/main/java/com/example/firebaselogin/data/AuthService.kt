package com.example.firebaselogin.data

import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class AuthService @Inject constructor(
    private val firebaseAuth: FirebaseAuth
) {

    /**
     * Lo que devuelve esta función es un Task, que viene a ser como un deferred en corrutinas, es
     * decir, como si fuese una corrutina inacabada.
     * Por esa razón y para que no nos de problemas vamos a añadir un await para que espere a que
     * se acabe por completo antes de devolverla, por lo que en vez de devolvernos esa Task, lo que
     * nos devolverá es un AuthService directamente.
     *
     * Como lo que nosotros queremos es el usuario que tiene ese email y contraseña, tendremos que
     * devolver eso, y por lo tanto ya finalmente lo que estaremos devolviendo es un FirebaseUser?,
     * el cual puede ser nulo en el caso de que el email y contraseña no sean válidos.
     *
     * Otra forma de validar que el usuario es correcto seria substituir esa instruccion por:
     *  - return firebaseAuth.signInWithEmailAndPassword(email, password).await().isSuccessful
     * De esa forma nos devuelve un boolean indicando si es correcto o no, como en RealTime Database
     * pero como en nuestro caso queremos quedarnos con el id del usuario tenemos que hacerlo de la
     * otra forma.
     *
     * A parte de eso también tenemos listeners, por ejemplo:
     *  - return firebaseAuth.signInWithEmailAndPassword(email, password).await().addOnCancelledListener
     * Esto sirva para saber si ha cancelado la cuenta. Hay muchos mas del estilo
     */
    suspend fun login(email: String, password: String): FirebaseUser? {
        return firebaseAuth.signInWithEmailAndPassword(email, password).await().user
    }

}
