package com.example.firebaselogin.ui.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.firebaselogin.data.AuthService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DetailViewModel @Inject constructor(
    private val authService: AuthService
) : ViewModel() {

    /**
     * En firebase cuando llamamos a logout, este no nos devuelve nada, es decir, nosotros le indicamos
     * a firebase que queremos hacer logout de una cuenta y el ya se encarga de ello en su totalidad,
     * sin devolvernos ningún valor ni nada.
     *
     * Por esa razón en este viewmodel no tenemos la variable isLoading por ejemplo.
     *
     * Por esa razón tambien tenemos que hacer otra cosa. Cada vez que llamemos al logout deberiamos
     * pensar en si tenemos info del usuario guardada, ya sea en base de datos, en alguna variable,
     * en Room o donde sea, hay que borrarla, de tal forma que no hay ningun dato de ese usuario que
     * hemos hecho logout en el dispositivo mobil.
     *
     * Despues de cercionar que toda posible info de ese usuario está eliminada navegamos al login o
     * a la pantalla que sea necesaria
     */
    fun logout(navigateToLogin: () -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            authService.logout()
        }
        navigateToLogin()
    }
}