package com.mybrickhub.brickpicker

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.mybrickhub.brickpicker.databinding.ActivityMainBinding
import com.mybrickhub.brickpicker.ui.home.HomeViewModel
import com.google.android.material.navigation.NavigationView
import com.mybrickhub.brickpicker.utility.Settings
import com.mybrickhub.brickpicker.utility.Theme

class MainActivity : AppCompatActivity() {

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityMainBinding
    private lateinit var homeViewModel: HomeViewModel

    private lateinit var sharedPreferences: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        sharedPreferences = this.getSharedPreferences("Settings", Context.MODE_PRIVATE)
        Theme().setAppTheme(sharedPreferences.getString(Settings.KEY_THEME, "System default").toString())

        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.appBarMain.toolbar)

        val drawerLayout: DrawerLayout = binding.drawerLayout
        val navView: NavigationView = binding.navView
        val navController = findNavController(R.id.nav_host_fragment_content_main)
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.nav_home, R.id.nav_setup, R.id.nav_settings
            ), drawerLayout
        )
        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)

        homeViewModel = ViewModelProvider(this)[HomeViewModel::class.java]

    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment_content_main)
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }

    override fun onDestroy() {
        super.onDestroy()
        homeViewModel.clearOrders()
    }

    fun openSupportPage(@Suppress("UNUSED_PARAMETER") item: MenuItem) {
        val uri = Uri.parse(getString(R.string.support_the_developer_uri))
        val intent = Intent(Intent.ACTION_VIEW, uri)
        startActivity(intent)
    }



}