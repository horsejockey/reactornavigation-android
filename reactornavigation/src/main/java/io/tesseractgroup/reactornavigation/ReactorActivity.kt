package com.mcarthurlabs.prototypebluetoothlibrary.reactornavigation

import android.content.Context
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import io.tesseractgroup.reactor.*
import java.lang.ref.WeakReference


/**
 * PrototypeBluetoothLibrary
 * Created by matt on 11/29/17.
 */
abstract class ReactorActivity(args: Bundle? = null, val reactorContainerId: Int, val reactorModalId: Int) : AppCompatActivity() {

    abstract var reactorViewModel: ReactorActivityViewModelInterface

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        reactorViewModel.setDelegate(this)

    }

    override fun onDestroy() {
        super.onDestroy()
        reactorViewModel.stopListening()
    }

    override fun onStart() {
        super.onStart()
        reactorViewModel.startListening()
    }

    override fun onStop() {
        super.onStop()
        reactorViewModel.stopListening()
    }

    /**
     * Responds to state changes by displaying the current visible view.
     */
    fun updateWith(state: NavigationStateProtocol) {

        val rootContainers = state.rootViewContainers
        val selectedIndex = state.selectedIndex
        val selectedContainer = rootContainers[selectedIndex]

        // Get visible View
        val visibleView = selectedContainer.findVisibleViewState()
        if (visibleView != null) {
            showView(visibleView)
        }
        val overlay = selectedContainer.findVisibleOverlayState()
        showOverlayView(overlay)
        // Show up button for children views
        if (selectedContainer.viewStates.count() > 1) {
            supportActionBar?.setDisplayHomeAsUpEnabled(true)
        } else {
            supportActionBar?.setDisplayHomeAsUpEnabled(false)
        }
    }

    /**
     * Transitions to the provided view state if it is not already displayed
     */
    private fun showView(reactorViewState: ReactorViewState) {
        val view: View?
        val rootViewGroup = findViewById<ViewGroup>(reactorContainerId)
        if (rootViewGroup.childCount > 0) {
            view = rootViewGroup.getChildAt(0)
        }else{
            view = null
        }

        if (view == null){
            // in the middle of a transition
            return
        }

        if (view is ViewStateConvertible && view.state() != reactorViewState) {
            Log.d("REACTOR_NAVIGATION", "Show in main view: ${reactorViewState} replacing view: ${view.state()}")
            hideSoftKeyBoard()
            rootViewGroup.removeView(view)
            rootViewGroup.addView(reactorViewState.view(this))
        }else if ( !(view is ViewStateConvertible)){
            Log.d("REACTOR_NAVIGATION", "Replace initial view: ${reactorViewState}")
            rootViewGroup.removeView(view)
            rootViewGroup.addView(reactorViewState.view(this))
        }
    }

    private fun hideSoftKeyBoard() {
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager

        if (imm.isAcceptingText()) { // verify if the soft keyboard is open
            imm.hideSoftInputFromWindow(currentFocus!!.windowToken, 0)
        }
    }

    /**
     * Transitions to the provided view state if it is not already displayed
     */
    private fun showOverlayView(reactorViewState: ReactorViewState?) {
        val view: View?
        val rootViewGroup = findViewById<ViewGroup>(reactorModalId)
        if (reactorViewState == null && rootViewGroup.childCount > 0) {
            // Dismiss the current overlay
            hideSoftKeyBoard()
            rootViewGroup.removeAllViews()
            rootViewGroup.visibility = View.GONE
        }else if (reactorViewState != null && rootViewGroup.childCount == 0){
            Log.d("REACTOR_NAVIGATION", "Show in overlay view: ${reactorViewState}")
            hideSoftKeyBoard()
            rootViewGroup.addView(reactorViewState.view(this))
            rootViewGroup.visibility = View.VISIBLE
        }
    }

    /**
     * Handles back presses
     * All other menu items should be handled in subclass but should call super for back presses
     */
    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        if (item?.itemId == android.R.id.home) {
            onBackPressed()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onBackPressed() {
        val state = reactorViewModel.navigationState()
        val rootContainers = state.rootViewContainers
        val selectedIndex = state.selectedIndex
        val selectedContainer = rootContainers[selectedIndex]
        if (selectedContainer.modal != null) {
            reactorViewModel.fireEvent(NavigationEvent.DismissModalEvent(selectedContainer.containerTag))
        } else if (selectedContainer.viewStates.count() > 1) {
            reactorViewModel.fireEvent(NavigationEvent.PopViewEvent(selectedContainer.containerTag))
        } else {
            finish()
        }
    }
}


interface ReactorActivityViewModelInterface {
    fun setDelegate(delegate: ReactorActivity?)

    fun fireEvent(event: Event)

    fun startListening()

    fun stopListening()

    fun navigationState(): NavigationStateProtocol

}

class ReactorActivityViewModel<StateType : State>(val sharedCore: Core<StateType>, val navStateSelector: ((StateType) -> NavigationStateProtocol)) : ReactorActivityViewModelInterface, Subscriber<StateType> {

    private var delegate: WeakReference<ReactorActivity>? = null

    override fun setDelegate(delegate: ReactorActivity?) {
        if (delegate != null) {
            this.delegate = WeakReference(delegate)
            sharedCore.add(this)
        } else {
            sharedCore.remove(this)
            this.delegate = null
        }
    }

    override fun navigationState(): NavigationStateProtocol {
        return navStateSelector(sharedCore.state)
    }

    override fun updateWith(state: StateType) {

        val navState = navStateSelector(state)
        delegate?.get()?.updateWith(navState)
    }

    override fun fireEvent(event: Event) {
        sharedCore.fire(event)
    }

    override fun startListening() {
        sharedCore.add(this)
    }

    override fun stopListening() {
        sharedCore.remove(this)
    }
}
