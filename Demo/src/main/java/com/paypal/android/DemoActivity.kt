package com.paypal.android

import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.NavigationUI
import androidx.preference.PreferenceManager
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class DemoActivity : AppCompatActivity() {

    private lateinit var navHostFragment: NavHostFragment
    private lateinit var navController: NavController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_demo)

        navHostFragment =
            supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navController = navHostFragment.navController

        val appBarConfiguration = AppBarConfiguration(navController.graph)
        NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration)

        PreferenceManager.setDefaultValues(this, R.xml.preferences, false)

        val authCredentialsMissing =
            BuildConfig.CLIENT_ID.isEmpty() || BuildConfig.CLIENT_SECRET.isEmpty()
        if (authCredentialsMissing) {
            showAuthCredentialsMissingDialog()
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        return navController.navigateUp() || super.onSupportNavigateUp()
    }

    private fun showAuthCredentialsMissingDialog() {
        AlertDialog.Builder(this)
            .setTitle(R.string.auth_credentials_missing_title)
            .setMessage(R.string.auth_credentials_missing_message)
            .setPositiveButton("OK", null)
            .create()
            .show()
    }
}
