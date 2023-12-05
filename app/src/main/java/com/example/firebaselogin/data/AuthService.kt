package com.example.firebaselogin.data

import android.app.Activity
import android.content.Context
import android.util.Log
import com.example.firebaselogin.R
import com.facebook.AccessToken
import com.facebook.login.LoginManager
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.AuthCredential
import com.google.firebase.auth.FacebookAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.OAuthProvider
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.tasks.await
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class AuthService @Inject constructor(
    private val firebaseAuth: FirebaseAuth,
    @ApplicationContext private val context: Context
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

    fun logout() {
        firebaseAuth.signOut()
        LoginManager.getInstance().logOut()
    }

    /**
     * Para mandar la validación del usuario por SMS se llama a una clase que se llama phoneAuthProvider
     * que es de firebase, y esa clase necesita un objeto "options" bastante complejo.
     *
     * Este objeto tiene:
     *      - newBuilder que recibe nuestro firebaseAuth
     *
     *      - setPhoneNumber que es el numero al cual le enviaremos el SMS
     *
     *      - setTimeout -> Cuanto tiempo tiene que pasar para que le enviemos otro SMS en caso de que
     *        no le llegue o pase alguna cosa, lo normal es 60 segundos
     *
     *      - setAvtivity -> Esto es la mas extraño, ya que es una mala práctica horrible porque me
     *      está pidiendo que le pase una activity desde mi Activity o Fragment hasta la capa de data.
     *      Es muy mala práctica pero es la única forma de hacerlo con Firebase.
     *             - MAS INFO EN -> https://github.com/firebase/firebase-android-sdk/issues/2239
     *
     *      - callback -> Es una lista de funciones que van a ser llamadas dependiendo de la situación,
     *      como por ejemplo: Si el SMS se manda, si hay algun error...
     *      Este lo podriamos hacer añadiendo la implementación de esas funciones dentro de
     *      .setCallbacks, pero siguiendo el principio de Single Responsability no lo haremos aqui,
     *      ya que no es responsabilidad de authService, ya que la unica responsabilidad
     *      de este es comunicarse con Firebase. Por lo tanto moveremos esa lógica al ViewModel.
     *
     * Ya por último enviamos nuesto objeto para que se encargue de toda la verificación y como él
     * se encarga de todo, no hace falta ni devolver ningún valor y, por ende, no recojeremos ningún
     * valor desde result en el viewModel
     *
     */
    fun loginWithPhone(
        phoneNumber: String,
        activity: Activity,
        callBack: PhoneAuthProvider.OnVerificationStateChangedCallbacks
    ) {
        val options = PhoneAuthOptions
            .newBuilder(firebaseAuth)
            .setPhoneNumber(phoneNumber)
            .setTimeout(60L, TimeUnit.SECONDS)
            .setActivity(activity)
            .setCallbacks(callBack)
            .build()

        PhoneAuthProvider.verifyPhoneNumber(options)
    }

    /**
     * Ahora recuperamos las credenciales. Una vez las tenemos, miramos que si se ha introducido el
     * código de verificación correcto y si ha sido correcto, con el método completeRegisterWithPhone
     * crearemos la cuenta a ese usuario.
     *
     * Por esa razón cuando haciamos:
     *      - firebaseAuth.firebaseAuthSettings.setAutoRetrievedSmsCodeForPhoneNumber("+34 123456789", "123456")
     * Estabamos pasando directamente a la función sobreescrita del callback "onVerificationCompleted()"
     * en la cual simplemente navegabamos hacia DetailScreen, por lo tanto nos saltábamos todos los
     * pasos relacionados con la creación de la cuenta
     */
    suspend fun verifyCode(verificationCode: String, phoneCode: String): FirebaseUser? {
        val credentials = PhoneAuthProvider.getCredential(verificationCode, phoneCode)
        return completeRegisterWithCredentials(credentials)

    }

    /**
     * Creamos un GoogleSignInOptions:
     *      - Para el token le pasamos el string default_web_client_id, pero no lo tenemos creado.
     *        Probamos a darle al martillo una vez tengamos puesto todo hasta .build() y puede
     *        ser que se nos cree de forma automática el string en strings.xml.
     *        Si no es el caso, tenemos que ir a nuestro authentication, clicar sobre el proveedor
     *        Google -> Configuración del SDK web -> ID de cliente web -> copiamos ese valor y lo
     *        metemos en nuestro fichero strings.xml
     *
     *        - Devolvemos un Cliente de Google
     */
    fun getGoogleClient(): GoogleSignInClient {
        val gso = GoogleSignInOptions
            .Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(context.getString(R.string.default_web_client_id)).requestEmail()
            .build()

        return GoogleSignIn.getClient(context, gso)
    }

    /**
     * Aqui es donde obtenemos las credenciales de verdad, es decir, este nos devuelve el AuthCredential
     * a partir del idToken.
     *
     * Este nos devuelve un AuthCredential al igual que hacia la función verifyCode, por lo tanto para
     * recuperar el usuario que queremos a partir de estas credenciales podemos reutilizar el método
     * completeRegisterWithCredentials
     */
    suspend fun loginWithGoogle(idToken: String): FirebaseUser? {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        return completeRegisterWithCredentials(credential)
    }


    /**
     * En este caso hacemos lo mismo que en register, usamos suspendCancellableCoroutine pero esta vez
     * hacemos sign In con las credenciales que es lo que le pasamos a nuestro método y devolvemos el
     * usuario que se crea con estas credenciales.
     */
    private suspend fun completeRegisterWithCredentials(
        credential: AuthCredential
    ): FirebaseUser? {
        return suspendCancellableCoroutine { cancellableContinuation ->
            firebaseAuth.signInWithCredential(credential).addOnSuccessListener {
                cancellableContinuation.resume(it.user)
            }
                .addOnFailureListener {
                    Log.i("ospofe", "Error: $it")
                    cancellableContinuation.resumeWithException(it)
                }
        }
    }

    suspend fun completeRegisterWithPhoneVerification(p0: PhoneAuthCredential) =
        completeRegisterWithCredentials(p0)

    suspend fun loginWithFacebook(accessToken: AccessToken): FirebaseUser? {
        val credentials = FacebookAuthProvider.getCredential(accessToken.token)
        return completeRegisterWithCredentials(credentials)
    }

    suspend fun loginWithGithub(activity: Activity): FirebaseUser? {
        val provider = OAuthProvider.newBuilder("github.com").apply {
            scopes = listOf("user:email")
        }.build()

        /**
         * Con el pendingAuthResult vigilamos que no se esté llevando a cabo ningún otro login
         * y que nos pueda dar problemas, por eso añadimos tanto el SuccessListener como el
         * FailureListener.
         *
         * Eso es muy útil cuando hay cosas pendientes pero en el caso de que no haya nada pendiente
         * tenemos que meter una opción mas, indicando que si no es el caso de que haya algo
         * pendiente.
         *
         * En ese caso creamos una función que nos complete el registro pero esta vez con
         * provider, parecido a lo que teniamos con completeRegisterWithPhoneVerification y
         * completeRegisterWithCredentials. En este caso le pasamos:
         *      - actvivity
         *      - provider -> El provider ya montado
         *      - cancellableContinuation -> Le pasamos la cancellableContinuation para que todo se
         *        haga dentro de la misma corrutina y en un único flujo
         */
        return initRegisterWithProvider(activity, provider)
    }

    /**
     * Para conseguir el email necesitamos meter también el calendars en el scope
     */
    suspend fun loginWithMicrosoft(activity: Activity): FirebaseUser? {
        val provider = OAuthProvider.newBuilder("microsoft.com").apply {
            scopes = listOf("mail.read", "calendars.read")
        }.build()

        return initRegisterWithProvider(activity, provider)
    }

    suspend fun loginWithTwitter(activity: Activity): FirebaseUser? {
        val provider = OAuthProvider.newBuilder("twitter.com").build()
        return initRegisterWithProvider(activity, provider)
    }

    suspend fun loginWithYahoo(activity: Activity): FirebaseUser? {
        val provider = OAuthProvider.newBuilder("yahoo.com").build()
        return initRegisterWithProvider(activity, provider)
    }


    private suspend fun initRegisterWithProvider(
        activity: Activity,
        provider: OAuthProvider
    ): FirebaseUser? {
        return suspendCancellableCoroutine { cancellableContinuation ->
            firebaseAuth.pendingAuthResult?.addOnSuccessListener {
                cancellableContinuation.resume(it.user)
            }?.addOnFailureListener {
                cancellableContinuation.resumeWithException(it)
            } ?: completeRegisterWithProvider(activity, provider, cancellableContinuation)
        }
    }

    private fun completeRegisterWithProvider(
        activity: Activity,
        provider: OAuthProvider,
        cancellableContinuation: CancellableContinuation<FirebaseUser?>
    ) {
        firebaseAuth.startActivityForSignInWithProvider(activity, provider).addOnSuccessListener {
            cancellableContinuation.resume(it.user)
        }.addOnFailureListener {
            cancellableContinuation.resumeWithException(it)
        }
    }


    private fun getCurrentUser() = firebaseAuth.currentUser



    /**
     * Para poder automatizar pruebas en el envio de SMS, ya que si ponemos nuestro número de telefono real y
     * enviamos mas de uno 2 o 3 SMS en un periodo corto de tiempo firebase nos va a bloquear,
     * haremos lo siguiente:
     *      - Escribimos toda la instrucción indicando que si le llega el número que usemos para
     *        las pruebas (en este caso +34 123456789), nos va a poner directamente el SMS de
     *        validación, simulando que ya se ha hecho todo el flujo por detrás.
     * De esta forma lo único que hacemos es simular que todo ha ido bien, pero no se envia ningún
     * SMS al telefono ni nos crea ninguna cuenta en firebase, ya que no es mas que una simulación.
     *
     * Por lo tanto esto solamente es util para hacer pruebas o cuando estas en debug, pero no es
     * la forma correcta de hacerlo.
     *
     * Código que lo hace -> firebaseAuth.firebaseAuthSettings.setAutoRetrievedSmsCodeForPhoneNumber("+34 123456789", "123456")
     */
}
