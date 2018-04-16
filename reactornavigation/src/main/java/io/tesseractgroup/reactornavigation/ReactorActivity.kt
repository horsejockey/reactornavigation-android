package io.tesseractgroup.reactornavigation

import android.content.Context
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.Toolbar
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import io.tesseractgroup.reactor.Command
import io.tesseractgroup.reactor.Core
import io.tesseractgroup.reactor.Event
import java.lang.ref.WeakReference

/**
 * PrototypeBluetoothLibrary
 * Created by matt on 11/29/17.
 */
abstract class ReactorActivity(val layoutId: Int, val toolbarId: Int, val reactorContainerId: Int) : AppCompatActivity() {

    abstract var reactorViewModel: ReactorActivityViewModelInterface

    lateinit var toolbar: Toolbar

    override fun onCreate(savedInstanceState: Bundle?) {
        setContentView(layoutId)
        super.onCreate(savedInstanceState)
        toolbar = findViewById(toolbarId)
        setSupportActionBar(toolbar)
        reactorViewModel.setDelegate(this)
    }

    override fun onPause() {
        super.onPause()
        reactorViewModel.fireEvent(NavigationEvent.AppContextChanged(false))
    }

    override fun onResume() {
        super.onResume()
        reactorViewModel.fireEvent(NavigationEvent.AppContextChanged(true))
    }

    /**
     * Responds to state changes by displaying the current visible view.
     */
    open fun updateWithNavState(state: NavigationStateProtocol, command: NavigationCommand) {

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

    /**
     * Transitions to the provided view state if it is not already displayed
     */
    private var transitioningMainView = false

    private fun showView(reactorViewState: ReactorViewState, command: NavigationCommand) {
        if (transitioningMainView) {
            Log.e("NAVIGATION", "Dropping navigation event. In the middle of a transition.")
            return
        }
        val view: View?
        val rootViewGroup = findViewById<ViewGroup>(reactorContainerId)
        if (rootViewGroup.childCount > 0) {
            view = rootViewGroup.getChildAt(0)
        } else {
            Log.e("NAVIGATION", "Dropping navigation event. In the middle of a transition.")
            return
        }

        transitioningMainView = true
        if (view is ViewStateConvertible && view.state() != reactorViewState) {
            toolbar.setOnMenuItemClickListener(null)
            toolbar.title = ""
            toolbar.menu.clear()
            Log.d("REACTOR_NAVIGATION", "Show in main view: ${reactorViewState} replacing view: ${view.state()}")
            hideSoftKeyBoard()
            val viewToRemove = view
            val viewToShow = reactorViewState.view(this)

            rootViewGroup.removeView(viewToRemove)
            rootViewGroup.addView(viewToShow)

            if (viewToRemove is ReactorView) {
                viewToRemove.viewTearDown()
            }
        } else if (!(view is ViewStateConvertible)) {
            Log.d("REACTOR_NAVIGATION", "Replace initial view: ${reactorViewState}")
            rootViewGroup.removeView(view)
            rootViewGroup.addView(reactorViewState.view(this))
        }
        transitioningMainView = false
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
        val state = reactorViewModel.navigationState()
        val selectedContainer = state.rootViewContainer.findVisibleContainer()
        val parentContainerTag = selectedContainer?.parentTag
        if (parentContainerTag != null) {
            reactorViewModel.fireEvent(NavigationEvent.DismissModal(parentContainerTag))
        } else if (selectedContainer != null && selectedContainer.viewStates.count() > 1) {
            reactorViewModel.fireEvent(NavigationEvent.PopNavView(selectedContainer.tag))
        } else {
            finish()
        }
    }
}

interface ReactorActivityViewModelInterface {
    fun setDelegate(delegate: ReactorActivity?)
    fun navigationState(): NavigationStateProtocol
    fun fireEvent(event: Event)
}

class NavigationProcessor<State>(val navStateSelector: ((State) -> NavigationStateProtocol)) {

    private var delegate: WeakReference<ReactorActivity>? = null

    fun setDelegate(delegate: ReactorActivity?) {
        if (delegate != null) {
            this.delegate = WeakReference(delegate)
        } else {
            this.delegate = null
        }
    }

    val processor = fun(core: Core<State>, command: Command) {
        if (command is NavigationCommand) {
            val navState = navStateSelector(core.currentState)
            delegate?.get()?.updateWithNavState(navState, command)
        }
    }
}

class ReactorActivityViewModel<State>(val sharedCore: Core<State>, val navStateSelector: ((State) -> NavigationStateProtocol)) : ReactorActivityViewModelInterface {

    private var delegate: WeakReference<ReactorActivity>? = null

    override fun setDelegate(delegate: ReactorActivity?) {
        if (delegate != null) {
            this.delegate = WeakReference(delegate)
        } else {
            this.delegate = null
        }
    }

    override fun navigationState(): NavigationStateProtocol {
        return navStateSelector(sharedCore.currentState)
    }

    override fun fireEvent(event: Event) {
        sharedCore.fire(event)
    }
}
