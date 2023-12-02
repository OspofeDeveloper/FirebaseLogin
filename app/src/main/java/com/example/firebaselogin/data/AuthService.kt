package com.example.firebaselogin.data

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

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
     * Esto sirva para saber si ha cancelado la cuenta. Hay muchos mas del estilo y son muy útiles para,
     * por ejemplo, poner logs en cada uno de ellos y controlar que pasa en todo momento
     */
    suspend fun login(email: String, password: String): FirebaseUser? {
        return firebaseAuth.signInWithEmailAndPassword(email, password).await().user
    }

    /**
     * En este caso las funcionalidades que hemos comentado en login son iguales en este caso.
     *
     * Lo que tambien vamos a ver es otra funcionalidad que nos proporciona en este caso
     * suspendCancellableCoroutine, esta es una corrutina que nos permite gestionar en este caso
     * nuestra lógica a través del listener. En este caso esta función no permite que nuestra
     * corrutina termine hasta que se haga nuestro addOnSuccessListener y en caso de que el
     * registro se haga bien guardamos el usuario que queremos devolver y continuamos la corrutina
     * ya con ese valor obtenido.
     *
     * También es muy potente porque podemos usar varios listeners de la misma forma para que en
     * cada caso dependiendo de que haya pasado y por lo tanto dentro de que listener vayamos podemos
     * implementar una lógica u otra.
     *
     * Como en este caso si falla mandamos un excepción, en el código del viewmodel que lanzamos la
     * corrutina podemos rodearlo de un bloque try/catch, lo que nos permite utilizar los mensajes
     * de error que nos proporciona firebase y hacer una buena gestión de errores
     *
     * Esto en el caso de querer manejar nuestra lógica con los listeners es muy potente. Sino, lo
     * hacemos con el await().user
     */
    suspend fun register(email: String, password: String): FirebaseUser? {
        return suspendCancellableCoroutine { cancellableContinuation ->
            firebaseAuth.createUserWithEmailAndPassword(email, password)
                .addOnSuccessListener {
                    val user = it.user
                    cancellableContinuation.resume(user)
                }
                .addOnFailureListener {
                    cancellableContinuation.resumeWithException(it)
                }
        }


    }


    /**
     * Implementamos esta función para ser llamada desde el Splash Screen y que nos haga un control
     * de si la sesión ya estaba iniciada y por lo tanto podemos ir directamente a la pantalla de
     * detalle o si no tenemos la sesión inciada y tenemos que empezar en la pantalla de inicio de
     * sesión.
     *
     * Usando firebaseAuth.currentUser este nos devuelve un firebaseUser, por lo que lo que podemos
     * hacer es retornar firebaseUser != null de tal forma que:
     *      - Si este se cumple (es true) -> el usuario ya está loggado
     *      - Si este no se cumple (es false) -> el usuario no está loggado
     *
     * Pero como a veces necesitamos recuperar también el usuario lo que hacemos es crear una clase
     * privada getCurrentUser que lo recupera, de tal forma que cualquier otro método de esta clase
     * que lo necesite puede acceder a él a partir de esta función
     */
    fun isUserLogged(): Boolean {
        return getCurrentUser() != null
    }

    private fun getCurrentUser() = firebaseAuth.currentUser
}
