package com.example.firebaselogin.ui.login

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.firebaselogin.data.AuthService
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
                 * la verificación porque el móbil ya es fiable. En ese caso simplemente navegamos
                 * a la siguiente pantalla.
                 */
                override fun onVerificationCompleted(p0: PhoneAuthCredential) {
                    //navigateToDetail()
                    onVerificationComplete()
                }

                /** Se llama cuando hay algun error como que no llega el SMS */
                override fun onVerificationFailed(p0: FirebaseException) {
                    _isLoading.value = false

                    onVerificationFailed(p0.message.orEmpty())
                }

                /** Se llama cuando desde firebase se haya mandado el SMS al móbil, de tal forma
                 *  que podemos controlarlo para hacer que el usuario del móbil pueda meter el
                 *  código, por ejemplo
                 */
                override fun onCodeSent(p0: String, p1: PhoneAuthProvider.ForceResendingToken) {
                    _isLoading.value = false

                    onCodeSent
                }

            }

            withContext(Dispatchers.IO) {
                authService.loginWithPhone(phoneNumber, activity, callBack)
            }

            _isLoading.value = false
        }
    }

}