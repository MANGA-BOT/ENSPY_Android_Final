package com.abess.enspy.ui

import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.abess.enspy.ApiClient
import com.abess.enspy.MainActivity
import com.abess.enspy.R
import com.abess.enspy.SecureStore
import com.google.android.material.button.MaterialButton
import org.json.JSONObject

class AuthFragment : Fragment(R.layout.fragment_auth) {

    // sample filieres -- update with real values from API if available
    private val filieres = listOf(
        1 to "INFO",
        2 to "MSP",
        3 to "GC",
        4 to "GMI",
        5 to "AUTRE"
    )

    private val levels = listOf("1", "2")

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val email = view.findViewById<AutoCompleteTextView>(R.id.input_email)
        val password = view.findViewById<AutoCompleteTextView>(R.id.input_password)
        val filiere = view.findViewById<AutoCompleteTextView>(R.id.input_filiere)
        val level = view.findViewById<AutoCompleteTextView>(R.id.input_level)
        val btn = view.findViewById<MaterialButton>(R.id.btn_action)
        val toggle = view.findViewById<TextView>(R.id.toggle_mode)
        val activity = requireActivity() as MainActivity
        val api: ApiClient = activity.api
        val store: SecureStore = activity.store

        // adapters
        filiere.setAdapter(ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, filieres.map { it.second }))
        level.setAdapter(ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, levels))

        var registerMode = false
        fun updateUiForMode() {
            if (registerMode) {
                (view.findViewById<TextView>(R.id.auth_title)).text = "Créer un compte"
                btn.text = "S'enregistrer"
                toggle.text = "J'ai déjà un compte"
            } else {
                (view.findViewById<TextView>(R.id.auth_title)).text = "Connexion"
                btn.text = "Se connecter"
                toggle.text = "Pas encore de compte ? Créer un compte"
            }
        }

        updateUiForMode()

        toggle.setOnClickListener {
            registerMode = !registerMode
            updateUiForMode()
        }

        btn.setOnClickListener {
            val em = email.text.toString().trim()
            val pw = password.text.toString()
            if (em.isEmpty() || pw.isEmpty()) return@setOnClickListener

            btn.isEnabled = false

            if (!registerMode) {
                // login
                val body = JSONObject().apply { put("email", em); put("password", pw) }
                api.post("/api/auth/login", body) { status, raw ->
                    activity.runOnUiThread {
                        btn.isEnabled = true
                        val resp = runCatching { JSONObject(raw) }.getOrNull() ?: JSONObject()
                        val token = resp.optString("token")
                        if (status in 200..299 && token.isNotBlank()) {
                            store.put("token", token)
                            val user = resp.optJSONObject("user") ?: resp.optJSONObject("student")
                            if (user != null) store.put("student", user.toString())
                            // return to home
                            activity.openFragment(HomeFragment())
                        } else {
                            // show error
                        }
                    }
                }
            } else {
                // register - need filiere id and level id
                val filiereName = filiere.text.toString()
                val filiereId = filieres.find { it.second == filiereName }?.first ?: 5
                val levelId = level.text.toString().toIntOrNull() ?: 1
                val body = JSONObject().apply {
                    put("email", em)
                    put("password", pw)
                    put("filiereId", filiereId)
                    put("levelId", levelId)
                    put("name", em.substringBefore("@"))
                }
                api.post("/api/auth/register", body) { status, raw ->
                    activity.runOnUiThread {
                        btn.isEnabled = true
                        val resp = runCatching { JSONObject(raw) }.getOrNull() ?: JSONObject()
                        val token = resp.optString("token")
                        if (status in 200..299 && token.isNotBlank()) {
                            store.put("token", token)
                            val user = resp.optJSONObject("user") ?: resp.optJSONObject("student")
                            if (user != null) store.put("student", user.toString())
                            activity.openFragment(HomeFragment())
                        } else {
                            // show error
                        }
                    }
                }
            }
        }
    }
}
