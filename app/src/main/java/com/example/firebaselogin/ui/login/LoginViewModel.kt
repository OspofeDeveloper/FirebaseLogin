package com.example.firebaselogin.ui.login

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.firebaselogin.data.AuthService
import com.facebook.AccessToken
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.firebase.FirebaseException
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val authService: AuthService
) : ViewModel() {

    private var _isLoading = MutableStateFlow<Boolean>(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    lateinit var verificationCode: String


    fun login(user: String, password: String, navigateToDetail: () -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true

            val result = withContext(Dispatchers.IO) {
                authService.login(user, password)
            }

            if (result != null) {
                navigateToDetail()
            } else {
                //error
            }

            _isLoading.value = false
        }

    }

    /**
     * A diferencia de lo que hicimos con el primer logIn con email, aqui no hace falta que gestionemos
     * nada del logout o de controlar si el usuario ya está con la sesión inciada ni nada, ya que con
     * lo que implementamos en ese caso ya nos funciona con este tipo de login también
     */
    fun loginWithPhone(
        phoneNumber: String,
        activity: Activity,
        onVerificationComplete: () -> Unit,
        onVerificationFailed: (String) -> Unit,
        onCodeSent: () -> Unit
    ) {
        viewModelScope.launch {
            _isLoading.value = true

            /** Implementamos las verificaciones del envio de SMS y se las mandamos a AuthService */
            val callBack = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {

                /**
                 * Este no se llama cuando la función haya ido bien y se mande el SMS, sino que se
                 * manda si por algún casual consigue detectar que este numero es válido y no hace
                 * falta ni mandarle un SMS, que puede ser porque sea un número que ya lo ha hecho
                 * varias veces, porque sea un emulador...
                 *
                 * Si verifica que es un mobil de fiar va a venir aquí directamente y nos vamos a saltar
                 * el último paso, por lo tanto aquí tendremos que hacer lo que hagamos en ese
                 * último paso, que seria navegar a Detail.
                 *
                 * En resumen esto es en el caso que no haga falta hacer todo el rollo completo de
                 * la verificación porque el móbil ya es fiable. En ese caso simplemente se llama a
                 * este método.
                 *
                 * Como se ha mencionado aqui teniamos solamente el navegar a Detail, pero como este
                 * método se llama cuando supuestamente ya se ha verificado el código y se salta todos
                 * los pasos intermedios de enviar un código de verificación y verificar que el código
                 * que introduce el usuario es el correcto, si solamente navegamos a Detail no estamos
                 * creando ninguna cuenta al usuario que se está loggando.
                 * Esto quiere decir que en vez de solo navegar a Detail primero tenemos que crear la
                 * cuenta y registrarlo a través de llamar a la funcion completeRegisterWithPhone()
                 * de nuestro authService, pero esta vez pasandole p0 que son unas credenciales que
                 * nos proporciona de forma directa firebase
                 */
                override fun onVerificationCompleted(p0: PhoneAuthCredential) {
                    viewModelScope.launch {
                        val result = withContext(Dispatchers.IO) {

                            /**
                             * Para mantener la función completeRegisterWithCredentials privada dentro
                             * de authService, creamos una función pública cuya función sea simplemente
                             * llamar a dicha función privada para mantener el codigo mas limpio
                             */
                            authService.completeRegisterWithPhoneVerification(p0)
                        }

                        if (result != null) {
                            onVerificationComplete()
                        }
                    }

                }

                /** Se llama cuando hay algun error como que no llega el SMS */
                override fun onVerificationFailed(p0: FirebaseException) {
                    _isLoading.value = false

                    onVerificationFailed(p0.message.orEmpty())
                }

                /** Se llama cuando desde firebase se haya mandado el SMS al móbil, de tal forma
                 *  que podemos controlarlo para hacer que el usuario del móbil pueda meter el
                 *  código, por ejemplo.
                 *
                 *  Como para la verificación (verifyCode()) necesitaremos el código de verificación
                 *  p0, guardamos ese valor en una variable local verificationCode. En este caso como
                 *  este onCodeSent está dentro de una corrutina no podemos acceder a esa variable
                 *  simplemente haciendo this.verificationCode, en caso de querer llamarla dentro
                 *  de una corutina haremos this@LoginViewModel.verificationCode = p0
                 */
                override fun onCodeSent(p0: String, p1: PhoneAuthProvider.ForceResendingToken) {
                    this@LoginViewModel.verificationCode = p0
                    _isLoading.value = false
                    onCodeSent()
                }

            }

            withContext(Dispatchers.IO) {
                authService.loginWithPhone(phoneNumber, activity, callBack)
            }

            _isLoading.value = false
        }
    }


    /**
     * Para la función verifyCode necesitamos 2 cosas:
     *      - verificationCode -> Es el código p0 que nos devuelve la función onCodeSent del
     *                            callback para que verifiquemos
     *      - phoneCode -> Código que introduce el usuario para verificar
     */
    fun verifyCode(phoneCode: String, onSuccessVerification: () -> Unit) {
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                authService.verifyCode(verificationCode, phoneCode)
            }

            if (result != null) {
                onSuccessVerification()
            }
        }
    }

    /**
     * Authservice nos devuelve un GoogleSignInClient y a través de una función lambda le enviamos
     * ese gsc a nuestra Activity
     */
    fun onGoogleLoginSelected(googleLauncherLogin: (GoogleSignInClient) -> Unit) {
        val gsc = authService.getGoogleClient()
        googleLauncherLogin(gsc)
    }

    /**
     * Este método va a ser el que va a hacer el login y el registro real a firebase. Va ser un
     * poco como el método verifyCode(), aqui le vamos a pasar solamente el idToken, ya que todo
     * el registro con Google ya lo hemos hecho antes en el View con el launcher y ahora solamente
     * nos queda unirlo y sincronizarlo con firebase.
     *
     * Ahora solo le decimos a firebase si con ese token puede inciar sesión, y firebase ya se
     * encarga de to_do, que para eso usamos firebase
     */
    fun loginWithGoogle(idToken: String, navigateToDetail: () -> Unit) {
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                authService.loginWithGoogle(idToken)
            }

            if (result != null) {
                navigateToDetail()
            }
        }
    }

    fun loginWithFacebook(accessToken: AccessToken, navigateToDetail: () -> Unit) {
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                authService.loginWithFacebook(accessToken)
            }

            if (result != null) {
                navigateToDetail()
            }
        }
    }

    fun onGithubLoginSelected(activity: Activity, navigateToDetail: () -> Unit) {
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                authService.loginWithGithub(activity)
            }

            if (result != null) {
                navigateToDetail()
            }
        }
    }

    fun onMicrosoftLoginSelected(activity: Activity, navigateToDetail: () -> Unit) {
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                authService.loginWithMicrosoft(activity)
            }

            if (result != null) {
                navigateToDetail()
            }
        }
    }

    fun onTwitterLoginSelected(activity: Activity, navigateToDetail: () -> Unit) {
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                authService.loginWithTwitter(activity)
            }

            if (result != null) {
                navigateToDetail()
            }
        }
    }

}