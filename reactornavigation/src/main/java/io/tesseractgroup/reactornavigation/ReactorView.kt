package io.tesseractgroup.reactornavigation

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.support.v7.widget.Toolbar
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
    private var viewIsVisible = false
        set(value) {
            if (field != value){
                field = value
                if (value){
                    viewDidAppear()
                }else{
                    viewDidDisappear()
                }
            }
        }

    init {
        Log.i("NAVIGATIION", "init in base view")
        LayoutInflater.from(context).inflate(layoutId, this)
        viewTreeObserver.addOnGlobalLayoutListener(this)
    }

    override fun onGlobalLayout() {
        if (!viewLayedOut){
            val mainHandler = Handler(Looper.getMainLooper())
            mainHandler.post {
                Log.i("NAVIGATION", "Layout callback")
                val toolbar = (context as ReactorActivity).toolbar
                viewSetup(toolbar)
                onWindowVisibilityChanged(visibility)
            }
        }
        viewLayedOut = true

        viewTreeObserver.removeOnGlobalLayoutListener(this)
    }

    override fun onWindowVisibilityChanged(visibility: Int) {
        super.onWindowVisibilityChanged(visibility)
        var visibilityStr = "Visibile"
        val isVisible: Boolean
        when {
            visibility == View.GONE -> {
                visibilityStr = "Gone"
                isVisible = false
            }
            visibility == View.INVISIBLE -> {
                visibilityStr = "Hidden"
                isVisible = false
            }
            else -> {
                isVisible = true
            }
        }
        if (viewLayedOut){
            viewIsVisible = isVisible
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        Log.i("NAVIGATION_${this.className()})", "attached to window")
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        Log.i("NAVIGATION_${this.className()})", "detached from window")
    }

    open fun viewDidAppear() {
        Log.i("NAVIGATION_${this.className()})", "View did appear")
    }
    open fun viewDidDisappear() {
        Log.i("NAVIGATION_${this.className()})", "View did disappear")
    }

    abstract fun viewSetup(toolbar: Toolbar)

    open fun viewTearDown(){
        Log.i("NAVIGATION_${this.className()})", "View tear down")
    }
}