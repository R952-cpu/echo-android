

package com.bitchat.android.util

import android.content.Context
import android.content.SharedPreferences

object StaffAuth {
    private const val PREFS_NAME = "staff_prefs"
    private const val KEY_IS_STAFF = "is_staff"
    private const val STAFF_CODE = "ECHObeta" // même code que sur iOS

    private fun prefs(context: Context): SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun isStaff(context: Context): Boolean =
        prefs(context).getBoolean(KEY_IS_STAFF, false)

    /** Retourne true si le code est correct et active le statut staff. */
    fun activate(context: Context, code: String): Boolean {
        return if (code == STAFF_CODE) {
            prefs(context).edit().putBoolean(KEY_IS_STAFF, true).apply()
            true
        } else {
            false
        }
    }

    /** Révoque le statut staff. */
    fun deactivate(context: Context) {
        prefs(context).edit().putBoolean(KEY_IS_STAFF, false).apply()
    }
}
