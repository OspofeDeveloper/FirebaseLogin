package com.example.firebaselogin.ui.login

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.core.widget.doOnTextChanged
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.firebaselogin.databinding.ActivityLoginBinding
import com.example.firebaselogin.databinding.DialogPhoneLoginBinding
import com.example.firebaselogin.ui.detail.DetailActivity
import com.example.firebaselogin.ui.signup.SignUpActivity
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.common.api.ApiException
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class LoginActivity : AppCompatActivity() {

    private val loginViewModel: LoginViewModel by viewModels()
    private lateinit var binding: ActivityLoginBinding

    /** Mirar exactamente como funciona esto
     *
     *  Lo que yo entiendo es que el result nos dice si de momento el tema de hacer el login con google
     *  ha ido correcto, y en el caso de que asi sea crearemos esa variable task, la cual va a recuperar
     *  los datos del usuario que se está queriendo loggar.
     *  Como puede ser que estos datos no se nos puedan enviar por cualquier razón, la api de google
     *  nos mandaria una excepción, por lo tanto abrimos un bloque try/catch donde:
     *      - Si nos devuelve los datos a task, mapeamos ese resultado al modelo "ApiException" que nos
     *        proporciona Google y indicamos que no va a ser nunca null. Desde account que es la
     *        variable donde almacenamos toda esa info, podemos obtener cualquier información sobre
     *        el usuario que se intenta loggar, como:
     *              - Foto perfil google -> account.phoyoUrl
     *              - Nombre del usuario -> account.givenName
     *              ...
     *
     *        Nosotros el que realmente solo queremos es el idToken, que es el que vamos a necesitar
     *        para ligarlo y engancharlo a firebase es el idToken, el cual también indicamos que no
     *        puede ser nulo ya que en caso de serlo lo mandamos a catch para así tener un mejor control
     *        de errores (En caso de que algo falle, me da igual que, indicamos que algo ha ido mal)
     */
    private val googleLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result: ActivityResult ->
            if (result.resultCode == Activity.RESULT_OK) {
                val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
                try {
                    val account = task.getResult(ApiException::class.java)!!
                    loginViewModel.loginWithGoogle(account.idToken!!) {
                        navigateToDetail()
                    }
                } catch (e: ApiException) {
                    Toast.makeText(this, "Ha ocurrido un error: ${e.message}", Toast.LENGTH_SHORT)
                        .show()
                }
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)
        initUI()
    }

    private fun initUI() {
        initListeners()
        initUIState()
    }

    private fun initUIState() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                loginViewModel.isLoading.collect {
                    binding.pbLoading.isVisible = it
                }
            }
        }
    }

    private fun initListeners() {
        binding.btnLogin.setOnClickListener {
            loginViewModel.login(
                user = binding.tieUser.text.toString(),
                password = binding.tiePassword.text.toString()
            ) { navigateToDetail() }
        }

        binding.tvRegister.setOnClickListener {
            navigateToSignUp()
        }

        binding.btnLoginPhone.setOnClickListener {
            showPhoneLogin()
        }

        /**
         * Una vez tenemos nuestro GoogleSignInClient, desde la activity tenemos que crear el launcher.
         * El launcher es el encargado de lanzar la pantalla y lanzar toda la gestión, es decir, cuando
         * hacemos login con Google, sale una pantalla con información sobre tus correos y demás.
         *
         * To_do eso se hace a través de un launcher, pero un launcher necesita las credenciales, es
         * decir, el GoogleSignInClient para saber que mostrar y como configurarlo.
         *
         * Entonces creamos arriba de la activity un private val googleLauncher
         */
        binding.btnLoginGoogle.setOnClickListener {
            loginViewModel.onGoogleLoginSelected { gsc ->
                googleLauncher.launch(gsc.signInIntent)
            }
        }
    }

    private fun showPhoneLogin() {
        val phoneBinding = DialogPhoneLoginBinding.inflate(layoutInflater)
        val alertDialog = AlertDialog.Builder(this).apply { setView(phoneBinding.root) }.create()

        phoneBinding.btnPhone.setOnClickListener {
            loginViewModel.loginWithPhone(phoneBinding.tiePhone.text.toString(), this,
                onCodeSent = {
                    /** Una vez se envia el código de verificación no puede modificar el num de telefono */
                    phoneBinding.tiePhone.isEnabled = false
                    phoneBinding.btnPhone.isEnabled = false

                    phoneBinding.pinView.isVisible = true

                    /**
                     * Con esto lo que hacemos es que cuando se mande el código de forma automática
                     * el cursor del usuario se ponga encima de donde tenemos que escribir el código
                     * de verificación, de tal forma que el usuario no tiene que clicar encima de
                     * esa celda para escribirlo.
                     */
                    phoneBinding.pinView.requestFocus()
                    val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
                    imm.showSoftInput(phoneBinding.pinView, InputMethodManager.SHOW_IMPLICIT)
                },
                onVerificationComplete = { navigateToDetail() },
                onVerificationFailed = { showToast("An error occurred: $it") }
            )
        }

        /**
         * Una vez introducimos el codigo de verificación completo del SMS llamamos a la función
         * verifyCode
         */
        phoneBinding.pinView.doOnTextChanged { text, _, _, _ ->
            if (text?.length == 6) {
                loginViewModel.verifyCode(text.toString()) { navigateToDetail() }
            }
        }

        alertDialog.show()
    }

    private fun showToast(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
    }

    private fun navigateToSignUp() {
        startActivity(Intent(this, SignUpActivity::class.java))
    }

    private fun navigateToDetail() {
        startActivity(Intent(this, DetailActivity::class.java))
    }
}