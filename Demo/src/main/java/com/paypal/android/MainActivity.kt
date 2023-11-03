package com.paypal.android

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.NavigationUI
import androidx.preference.PreferenceManager
import com.paypal.android.ui.DemoApp
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

//    private lateinit var navHostFragment: NavHostFragment
//    private lateinit var navController: NavController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        addOnNewIntentListener {

        }
        setContent {
            DemoApp()
        }
//        setContentView(R.layout.activity_demo)
//
//        navHostFragment =
//            supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
//        navController = navHostFragment.navController
//
//        val appBarConfiguration = AppBarConfiguration(navController.graph)
//        NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration)
//
//        PreferenceManager.setDefaultValues(this, R.xml.preferences, false)
    }

//    override fun onSupportNavigateUp(): Boolean {
//        return navController.navigateUp() || super.onSupportNavigateUp()
//    }
//
//    override fun onNewIntent(newIntent: Intent?) {
//        super.onNewIntent(newIntent)
//        intent = newIntent
//        Log.e("DEMO ACTIVITY", "ON NEW INTENT")
//    }
//
//    override fun onResume() {
//        super.onResume()
//        Log.e("DEMO ACTIVITY", "ON RESUME")
//    }
}
