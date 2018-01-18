package com.mcarthurlabs.prototypebluetoothlibrary.reactornavigation

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewTreeObserver
import android.widget.FrameLayout

/**
 *  Created by matt on 11/7/17.
 */

fun View.className(): String {
    return "${this::class}".split(".").last()
}

abstract class ReactorView(context: Context, layoutId: Int) : FrameLayout(context, null, 0), ViewStateConvertible, ViewTreeObserver.OnGlobalLayoutListener {

    private var viewLayedOut = false

    init {
        Log.i("NAVIGATIION", "init in base view")
        LayoutInflater.from(context).inflate(layoutId, this)
        viewTreeObserver.addOnGlobalLayoutListener(this)
    }

    override fun onGlobalLayout() {
        if (!viewLayedOut){
            viewSetup()
            val hidden = visibility != View.VISIBLE
            viewVisibilityChanged(hidden)
        }
        Log.i("NAVIGATION", "Layout callback")
        viewLayedOut = true

        viewTreeObserver.removeOnGlobalLayoutListener(this)
    }

    override fun onWindowVisibilityChanged(visibility: Int) {
        super.onWindowVisibilityChanged(visibility)
        var visibilityStr = "Visibile"
        var hidden = false
        when {
            visibility == View.GONE -> {
                visibilityStr = "Gone"
                hidden = true
            }
            visibility == View.INVISIBLE -> {
                visibilityStr = "Hidden"
                hidden = true
            }
            else -> {
                hidden = false
            }
        }
        if (viewLayedOut){
            viewVisibilityChanged(hidden)
        }
        Log.i("NAVIGATION_${this.className()})", "Visibility changed: $visibilityStr")
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        Log.i("NAVIGATION_${this.className()})", "attached to window")
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        Log.i("NAVIGATION_${this.className()})", "detached from window")
    }

    open fun viewVisibilityChanged(hidden: Boolean) {}

    abstract fun viewSetup()
}