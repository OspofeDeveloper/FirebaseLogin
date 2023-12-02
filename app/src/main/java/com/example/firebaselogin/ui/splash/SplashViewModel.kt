package com.example.firebaselogin.ui.splash

import androidx.lifecycle.ViewModel
import com.example.firebaselogin.data.AuthService
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class SplashViewModel @Inject constructor(
    private val authService: AuthService
) : ViewModel() {

    private fun isUserLogged(): Boolean {
        return authService.isUserLogged()
    }

    /**
     * Miramos si el usuario ya esta loggado y decidimos a que pantalla navegar en cada caso
     *
     * En este caso solo hay dos pantallas así que podriamos controlar que pantalla es la destino
     * con un simple if/else, pero como hay veces que tenemos muchos destinos diferentes en esos
     * casos es mejor crear una sealed class donde definamos todas los destinos posibles.
     *
     * De esta forma todos los destinos nos los controla el viewmodel y simplemente tenemos que
     * implementar un when en el activity que navegue al destino correspondiente, y como es una
     * clase sellada (sealed class) al hacer el when solo podemos implementar los casos que haya
     * en esta, por lo que nos olvidamos de errores por no implementar casos o implementar casos
     * que no existen.
     */
    fun checkDestination(): SplashDestination {
        val isUserLogger = isUserLogged()
        return if (isUserLogger) {
            SplashDestination.Home
        } else {
            SplashDestination.Login
        }
    }
}

sealed class SplashDestination {
    object Login : SplashDestination()
    object Home : SplashDestination()
}