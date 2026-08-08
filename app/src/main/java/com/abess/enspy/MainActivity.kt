package com.abess.enspy

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.appbar.MaterialToolbar
import androidx.fragment.app.Fragment
import com.abess.enspy.ui.HomeFragment
import com.abess.enspy.ui.DocumentsFragment
import com.abess.enspy.ui.ForumFragment
import com.abess.enspy.ui.CalendarFragment
import com.abess.enspy.ui.ProfileFragment
import com.abess.enspy.ui.AuthFragment

class MainActivity : AppCompatActivity() {
    lateinit var store: SecureStore
    lateinit var api: ApiClient

    // Pending action stored when we require authentication — executed after successful login/register
    var pendingAction: (() -> Unit)? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        store = SecureStore(this)
        api = ApiClient(store)

        val toolbar = findViewById<MaterialToolbar>(R.id.topAppBar)
        setSupportActionBar(toolbar)

        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNavigation)
        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.navigation_home -> { openFragment(HomeFragment()); true }
                R.id.navigation_library -> { openFragment(DocumentsFragment()); true }
                R.id.navigation_forum -> { openFragment(ForumFragment()); true }
                R.id.navigation_calendar -> { openFragment(CalendarFragment()); true }
                R.id.navigation_profile -> { openFragment(ProfileFragment()); true }
                else -> false
            }
        }

        // default
        if (savedInstanceState == null) openFragment(HomeFragment())
    }

    fun openFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .addToBackStack(null)
            .commit()
    }

    /**
     * If the user is authenticated the [action] will run immediately.
     * Otherwise we open the AuthFragment so the user can login/register first.
     * The action will be executed automatically after successful authentication.
     */
    fun requireAuthentication(action: () -> Unit) {
        val token = store.get("token")
        if (token.isNullOrBlank()) {
            // save action and open auth screen
            pendingAction = action
            openFragment(AuthFragment())
        } else {
            action()
        }
    }

    fun openAuth() {
        openFragment(AuthFragment())
    }

    fun runPendingAction() {
        pendingAction?.invoke()
        pendingAction = null
    }
}
