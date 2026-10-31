package com.gameora.ui.common

import android.widget.ImageView
import coil.load
import com.gameora.R

/**
 * Server-driven image loading with Coil. Every game icon, banner, product image, avatar
 * and notification image is loaded from a server URL — never an emoji, never a bundled
 * asset. If the URL is null/empty the placeholder is shown.
 */
object Images {

    fun load(view: ImageView, url: String?) {
        view.load(url) {
            crossfade(true)
            placeholder(R.drawable.bg_placeholder)
            error(R.drawable.bg_placeholder)
        }
    }

    fun avatar(view: ImageView, url: String?) {
        view.load(url) {
            crossfade(true)
            placeholder(R.drawable.bg_avatar_placeholder)
            error(R.drawable.bg_avatar_placeholder)
        }
    }
}
