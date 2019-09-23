package io.tesseractgroup.reactornavigation

import android.content.Context
import android.util.Log
import android.view.*
import android.widget.FrameLayout
import androidx.appcompat.widget.Toolbar

/**
 *  Created by matt on 11/7/17.
 */

fun View.className(): String {
    return "${this::class}".split(".").last()
}

abstract class ReactorView(context: Context, private val layoutId: Int, open val viewState: ReactorViewState) : FrameLayout(context, null, 0), ViewTreeObserver.OnGlobalLayoutListener {

    lateinit var containerTag: ViewContainerTag
        internal set
    var parentTag: ViewContainerTag? = null
        internal set
    private var layoutRequested = false
    private var viewLayedOut = false
    internal var viewIsVisible = false
        set(value) {
            if (field != value) {
                field = value
                if (value) {
                    viewDidAppear()
                } else {
                    viewDidDisappear()
                }
            }
        }

    private fun inflateLayout() {
        LayoutInflater.from(context).inflate(layoutId, this)
        val toolbar = (context as ReactorActivity).toolbar
        toolbar.menu.clear()
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
        if (viewLayedOut) {
            viewIsVisible = isVisible
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        if (!layoutRequested) {
            layoutRequested = true
            this.viewTreeObserver.addOnGlobalLayoutListener(this)
            inflateLayout()
        }
    }

    override fun onGlobalLayout() {
        this.viewTreeObserver.removeOnGlobalLayoutListener(this)
        if (!viewLayedOut) {
            viewLayedOut = true
            viewIsVisible = true
        }
    }

    open fun onBackPressed(): Boolean {
        return false
    }

    open fun homeUpIsEnabled(): Boolean? {
        return null
    }

    open fun viewDidAppear() {
        Log.i("NAVIGATION_${this.className()})", "View did appear")
    }

    open fun viewDidDisappear() {
        Log.i("NAVIGATION_${this.className()})", "View did disappear")
    }

    abstract fun viewSetup(toolbar: Toolbar)

    open fun viewTearDown() {
        Log.i("NAVIGATION_${this.className()})", "View tear down")
    }
}