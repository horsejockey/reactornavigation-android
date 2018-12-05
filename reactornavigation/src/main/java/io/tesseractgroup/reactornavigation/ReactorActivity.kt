package io.tesseractgroup.reactornavigation

import android.content.Context
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.Toolbar
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import io.tesseractgroup.reactor.Core

/**
 * PrototypeBluetoothLibrary
 * Created by matt on 11/29/17.
 */
abstract class ReactorActivity(
    private val layoutId: Int,
    private val toolbarId: Int,
    private val reactorContainerId: Int) : AppCompatActivity() {

    abstract val navigationCore: Core<NavigationStateProtocol, NavigationEvent, NavigationCommand>
    lateinit var toolbar: Toolbar

    private var activityCreated = false
    private lateinit var rootViewGroup: ViewGroup

    override fun onCreate(savedInstanceState: Bundle?) {
        setContentView(layoutId)
        super.onCreate(savedInstanceState)

        rootViewGroup = findViewById(reactorContainerId)
        toolbar = findViewById(toolbarId)

        setSupportActionBar(toolbar)
        ReactorNavigation.navigationCommandReceived.addMultipleCallbacks(this, ::navigationCommandReceived)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        activityCreated = true
        updateWithNavState(navigationCore.currentState, NavigationCommand())
        return super.onCreateOptionsMenu(menu)
    }

    override fun onPause() {
        super.onPause()
        navigationCore.fire(NavigationEvent.AppContextChanged(false))

    }

    override fun onResume() {
        super.onResume()
        navigationCore.fire(NavigationEvent.AppContextChanged(true))
    }

    override fun onDestroy() {
        if (rootViewGroup.childCount > 0){
            val view = rootViewGroup.getChildAt(0)
            if (view is ReactorView) view.viewTearDown()
        }
        super.onDestroy()
        ReactorNavigation.navigationCommandReceived.remove(this)
    }

    private fun navigationCommandReceived(navigationCommand: NavigationCommand) {
        updateWithNavState(navigationCore.currentState, navigationCommand)
    }

    /**
     * Responds to state changes by displaying the current visible view.
     */
    private fun updateWithNavState(state: NavigationStateProtocol, command: NavigationCommand) {
        runOnUiThread {
            val visibleContainer = state.findVisibleContainer()
            val visibleViewState = state.findVisibleView()

            // Get visible View
            if (visibleViewState != null) {
                showView(visibleViewState, command)
            }
            // Show up button for children views
            if (visibleContainer != null && visibleContainer.viewStates.count() > 1) {
                supportActionBar?.setDisplayHomeAsUpEnabled(true)
            } else {
                supportActionBar?.setDisplayHomeAsUpEnabled(false)
            }
        }
    }

    /**
     * Transitions to the provided view state if it is not already displayed
     */
    private var transitioningMainView = false

    private fun showView(reactorViewState: ReactorViewState, @Suppress("UNUSED_PARAMETER") command: NavigationCommand) {
        if (activityCreated == false) {
            Log.e("NAVIGATION", "Dropping navigation event. Activity not created.")
            return
        }
        if (transitioningMainView) {
            Log.e("NAVIGATION", "In the middle of a transition. Dropping view transition.")
            return
        }
        val view: View?
        if (rootViewGroup.childCount > 0) {
            view = rootViewGroup.getChildAt(0)
        } else {
            Log.e("NAVIGATION", "(Unknown) In the middle of a transition. Dropping view transition.")
            return
        }

        transitioningMainView = true
        if (view is ViewStateConvertible && view.state() != reactorViewState || view !is ViewStateConvertible) {
            toolbar.setOnMenuItemClickListener(null)
            toolbar.title = ""
            toolbar.menu.clear()
            if (view is ViewStateConvertible) {
                Log.d("REACTOR_NAVIGATION", "Show in main view: ${reactorViewState} replacing view: ${view.state()}")
            } else {
                Log.d("REACTOR_NAVIGATION", "Replace initial view: ${reactorViewState}")
            }

            hideSoftKeyBoard()
            val viewToRemove = view
            val viewToShow = getViewForState(reactorViewState)

            rootViewGroup.removeView(viewToRemove)
            rootViewGroup.addView(viewToShow)

            if (viewToRemove is ReactorView) {
                viewToRemove.viewTearDown()
            }
        }
        transitioningMainView = false
    }

    open fun getViewForState(reactorViewState: ReactorViewState): ReactorView {
        return reactorViewState.view(this)
    }

    private fun hideSoftKeyBoard() {
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        val currentFocusElement = currentFocus
        if (imm.isAcceptingText && currentFocusElement != null) { // verify if the soft keyboard is open
            imm.hideSoftInputFromWindow(currentFocusElement.windowToken, 0)
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
        val state = navigationCore.currentState
        val selectedContainer = state.rootViewContainer.findVisibleContainer()
        val parentContainerTag = selectedContainer?.parentTag
        if (selectedContainer != null && selectedContainer.viewStates.count() > 1) {
            navigationCore.fire(NavigationEvent.PopNavView(selectedContainer.tag))
        } else if (parentContainerTag != null && (state.rootViewContainer.tag != parentContainerTag || state.rootViewContainer is NavContainerState)) {
            navigationCore.fire(NavigationEvent.DismissModal(parentContainerTag))
        } else {
            finish()
        }
    }
}

