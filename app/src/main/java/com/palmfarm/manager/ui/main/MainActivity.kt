package com.palmfarm.manager.ui.main

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.palmfarm.manager.R
import com.palmfarm.manager.databinding.ActivityMainBinding

/**
 * Main activity hosting bottom navigation and fragments
 */
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupNavigation()
    }

    private fun setupNavigation() {
        // Get NavHostFragment
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.navHostFragment) as NavHostFragment
        val navController = navHostFragment.navController

        // Setup BottomNavigationView with NavController
        binding.bottomNavigation.setupWithNavController(navController)

        // Handle bottom navigation reselection (scroll to top)
        binding.bottomNavigation.setOnItemReselectedListener { item ->
            // When user taps the same tab, scroll to top or perform other actions
            when (item.itemId) {
                R.id.navigation_dashboard -> {
                    // Scroll dashboard to top if needed
                }
                R.id.navigation_tasks -> {
                    // Scroll tasks to top if needed
                }
                R.id.navigation_production -> {
                    // Scroll production to top if needed
                }
                R.id.navigation_finances -> {
                    // Scroll finances to top if needed
                }
                R.id.navigation_settings -> {
                    // Scroll settings to top if needed
                }
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.navHostFragment) as NavHostFragment
        val navController = navHostFragment.navController
        return navController.navigateUp() || super.onSupportNavigateUp()
    }
}
