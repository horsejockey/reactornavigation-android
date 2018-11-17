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

abstract class ReactorView(context: Context, private val layoutId: Int) : FrameLayout(context, null, 0), ViewStateConvertible, ViewTreeObserver.OnGlobalLayoutListener {

    private var layoutRequested = false
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

    private fun inflateLayout(){
        LayoutInflater.from(context).inflate(layoutId, this)
        val toolbar = (context as ReactorActivity).toolbar
        viewSetup(toolbar)
        this.rootView.requestLayout()
    }

    override fun onWindowVisibilityChanged(visibility: Int) {
        super.onWindowVisibilityChanged(visibility)
        val isVisible: Boolean
        when {
            visibility == View.GONE -> {
                isVisible = false
            }
            visibility == View.INVISIBLE -> {
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
        if(!layoutRequested){
            layoutRequested = true
            this.viewTreeObserver.addOnGlobalLayoutListener(this)
            inflateLayout()
        }
    }

    override fun onGlobalLayout(){
        this.viewTreeObserver.removeOnGlobalLayoutListener(this)
        if (!viewLayedOut){
            viewLayedOut = true
            viewIsVisible = true
        }
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