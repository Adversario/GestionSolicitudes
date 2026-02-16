package com.example.gestionsolicitudes.leaks

import android.app.Activity

/**
 * Fuga intencional controlada
 * No debería usarse para guardar Activities ya que provoca leaks
 */
object LeakHolder {
    var leakedActivity: Activity? = null
}