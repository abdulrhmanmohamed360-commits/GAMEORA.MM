package com.gameora.ui.splash

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.gameora.databinding.ActivitySplashBinding
import com.gameora.ui.home.HomeActivity
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * شاشة البداية (Splash): عرض بصري فقط. بعد لحظات بتفتح HomeActivity بنفس
 * الـ extras اللي جت مع الـ Intent (مثلاً لو التطبيق اتفتح من إشعار)،
 * ومفيش أي منطق تاني (تسجيل دخول / شبكة) بيحصل هنا.
 */
class SplashActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySplashBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivitySplashBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.splashLogo.apply {
            alpha = 0f
            scaleX = 0.85f
            scaleY = 0.85f
            animate().alpha(1f).scaleX(1f).scaleY(1f).setDuration(600).start()
        }
        binding.splashTexts.apply {
            alpha = 0f
            animate().alpha(1f).setStartDelay(250).setDuration(600).start()
        }

        lifecycleScope.launch {
            delay(SPLASH_MILLIS)
            startActivity(
                Intent(this@SplashActivity, HomeActivity::class.java).apply {
                    intent?.extras?.let { putExtras(it) }
                }
            )
            finish()
        }
    }

    private companion object {
        const val SPLASH_MILLIS = 1300L
    }
}
