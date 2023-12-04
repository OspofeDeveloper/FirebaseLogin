package com.example.firebaselogin.ui.detail

import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.example.firebaselogin.databinding.ActivityDetailBinding
import com.example.firebaselogin.ui.login.LoginActivity
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class DetailActivity : AppCompatActivity() {

    private val detailViewModel: DetailViewModel by viewModels()

    private lateinit var binding: ActivityDetailBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)
        initUI()
    }

    private fun initUI() {
        initListeners()
    }

    private fun initListeners() {
        binding.btnLogout.setOnClickListener {
            detailViewModel.logout {
                navigateToLogin()
            }
        }
    }

    /**
     * En este caso como queremos navegar a la pantalla de login que es la pantalla inicial de la
     * aplicación tenemos que borrar toda la pila de navegación.
     */
    private fun navigateToLogin() {
        startActivity(Intent(this, LoginActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK
            }
        )
    }
}