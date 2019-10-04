package io.tesseractgroup.reactornavigation

import android.content.Context
import android.os.Bundle
import android.transition.Fade
import android.transition.Slide
import android.transition.Transition
import android.util.Log
import android.view.Gravity
import android.view.Menu
import android.view.MenuItem
import android.view.inputmethod.InputMethodManager
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.graphics.drawable.AnimatedStateListDrawableCompat
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.vectordrawable.graphics.drawable.AnimatedVectorDrawableCompat
import io.tesseractgroup.reactor.Core
import kotlinx.coroutines.newSingleThreadContext

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

    // Navigation Icon State Management
    enum class NavigationIconState {
        NONE, CLOSE, BACK;
        fun drawable(context: Context): AnimatedVectorDrawableCompat? {
            return when(this){
                NONE -> null
                CLOSE -> AnimatedVectorDrawableCompat.create(context, R.drawable.pathmorph_back_to_close)
                BACK -> AnimatedVectorDrawableCompat.create(context, R.drawable.pathmorph_close_to_back)
            }
        }
    }
    private var navigationIconSate = NavigationIconState.NONE
        set(value) {
            if (field != value) {
                navigationIconChanged(field, value)
                field = value
            }
        }

    private var activityCreated = false

    override fun onCreate(savedInstanceState: Bundle?) {
        setContentView(layoutId)
        super.onCreate(savedInstanceState)
        val existingFragment = displayedFragment()
        val visibleViewState = navigationCore.currentState.findVisibleView()
        if (existingFragment != null && visibleViewState != null) {
            existingFragment.reactorView = getViewForState(visibleViewState)
        }

        toolbar = findViewById(toolbarId)
        setSupportActionBar(toolbar)
        ReactorNavigation.navigationCommandReceived.addMultipleCallbacks(this, ::navigationCommandReceived)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        activityCreated = true
        updateWithNavState(navigationCore.currentState, NavigationCommand.RootContainerChanged(null))
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
        super.onDestroy()
        ReactorNavigation.navigationCommandReceived.remove(this)
    }

    private fun navigationCommandReceived(navigationCommand: NavigationCommand) {
        if (navigationCommand.navStackChanged) {
            updateWithNavState(navigationCore.currentState, navigationCommand)
        } else if (navigationCommand is NavigationCommand.PresentAlert) {
            runOnUiThread {
                val alertBuilder = AlertDialog.Builder(this)
                alertBuilder.setCancelable(false)
                alertBuilder.setTitle(navigationCommand.title)
                alertBuilder.setMessage(navigationCommand.message)
                for ((index, button) in navigationCommand.buttons.iterator().withIndex()) {
                    if (index == 0) {
                        alertBuilder.setPositiveButton(button.title, button.action)
                    } else if (index == 1) {
                        alertBuilder.setNegativeButton(button.title, button.action)
                    } else {
                        alertBuilder.setNeutralButton(button.title, button.action)
                    }
                }
                alertBuilder.show()
            }
        }
    }

    private fun displayedFragment(): ReactorFragment? {
        val fragment = supportFragmentManager.fragments.firstOrNull()
        return if (fragment is ReactorFragment) fragment
        else null
    }

    /**
     * Responds to state changes by displaying the current visible view.
     */
    private fun updateWithNavState(state: NavigationStateProtocol, command: NavigationCommand) {
        if (command !is VisibleViewChanged) return
        runOnUiThread {
            val visibleContainer = state.findVisibleContainer()
            val visibleViewState = state.findVisibleView()

            // Get visible View
            if (visibleViewState != null && visibleContainer != null) {
                if (activityCreated == false) {
                    Log.e("NAVIGATION", "Dropping navigation event. Activity not created.")
                    return@runOnUiThread
                }
                showView(visibleViewState, visibleContainer.tag, visibleContainer.parentTag, command)

                val rootContainer = state.rootViewContainer
                val rootTag = rootContainer.tag


                val isARootNavContainer = visibleContainer.tag == rootTag || (rootContainer is TabContainerState && visibleContainer.parentTag == rootTag)
                navigationIconSate = when {
                    visibleContainer.viewStates.count() == 1 && !isARootNavContainer && visibleContainer.cancellable -> {
                        NavigationIconState.CLOSE
                    }
                    visibleContainer.viewStates.count() > 1 -> {
                        NavigationIconState.BACK
                    }
                    else -> NavigationIconState.NONE
                }
            }
        }
    }

    // Handle navigation icon changes
    private fun navigationIconChanged(oldValue: NavigationIconState, newValue: NavigationIconState) {
        val drawable = newValue.drawable(this)
        toolbar.navigationIcon = drawable
        drawable?.start()
    }

    /**
     * Transitions to the provided view state if it is not already displayed
     */
    private var transitioningMainView = false
    private var lastView: ReactorView? = null

    private fun showView(reactorViewState: ReactorViewState, containerTag: ViewContainerTag, parentTag: ViewContainerTag?, command: NavigationCommand) {
        if (transitioningMainView) {
            Log.e("NAVIGATION", "In the middle of a transition. Dropping view transition.")
            return
        }
        val view = displayedFragment()?.reactorView

        transitioningMainView = true
        if (view?.viewState != reactorViewState) {
            toolbar.setOnMenuItemClickListener(null)
            toolbar.title = ""
            var isInitialView = false
            if (view != null) {
                Log.d("REACTOR_NAVIGATION", "Show in main view: ${reactorViewState} replacing view: ${view.viewState}")
            } else {
                isInitialView = true
                Log.d("REACTOR_NAVIGATION", "Replace initial view: ${reactorViewState}")
            }

            hideSoftKeyBoard()
            val viewToRemove = view
            val viewToShow = getViewForState(reactorViewState)
            viewToShow.containerTag = containerTag
            viewToShow.parentTag = parentTag
            val fragment = ReactorFragment.newInstance(viewToShow)
            val currentFragment = displayedFragment()

            when (command) {
                is NavigationCommand.TabIndexChanged -> {
                    fragment.enterTransition = ReactorTransitions.enterFade
                    currentFragment?.exitTransition = ReactorTransitions.exitFade
                }
                is NavigationCommand.ModalPresented -> {
                    fragment.enterTransition = ReactorTransitions.bottomSlide
                    currentFragment?.exitTransition = ReactorTransitions.exitFade
                }
                is NavigationCommand.ModalDismissed -> {
                    fragment.enterTransition = ReactorTransitions.enterFade
                    currentFragment?.exitTransition = ReactorTransitions.bottomSlide
                }
                is NavigationCommand.RootContainerChanged -> {
                    fragment.enterTransition = ReactorTransitions.enterFade
                    currentFragment?.exitTransition = ReactorTransitions.exitFade
                }
                NavigationCommand.NavViewPushed -> {
                    fragment.enterTransition = ReactorTransitions.rightSlide
                    currentFragment?.exitTransition = ReactorTransitions.leftSlide
                }
                NavigationCommand.NavViewPopped -> {
                    fragment.enterTransition = ReactorTransitions.leftSlide
                    currentFragment?.exitTransition = ReactorTransitions.rightSlide
                }
                NavigationCommand.NavViewReplaced -> {
                    fragment.enterTransition = ReactorTransitions.enterFade
                    currentFragment?.exitTransition = ReactorTransitions.exitFade
                }
                NavigationCommand.HiddenUpdate,
                NavigationCommand.AppContextChanged,
                is NavigationCommand.PresentAlert -> {
                    // No animation
                }
            }
            val transaction = supportFragmentManager.beginTransaction()
            transaction.replace(reactorContainerId, fragment)
            transaction.commitAllowingStateLoss()
            if (lastView != viewToRemove && !isInitialView){
                Log.e("REACTOR_NAVIGATION", "Last view added doesn't match current view. Consider re-ordering navigation events.")
                lastView?.viewTearDown()
            }
            if (viewToRemove is ReactorView) {
                viewToRemove.viewTearDown()
            }
            lastView = viewToShow
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
     * Handles reactor_back presses
     * All other menu items should be handled in subclass but should call super for reactor_back presses
     */
    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        if (item?.itemId == android.R.id.home) {
            onBackPressed()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onBackPressed() {

        val reactorView = displayedFragment()?.reactorView
        if (reactorView?.onBackPressed() == true) {
            return
        }

        val state = navigationCore.currentState
        val selectedContainer = state.rootViewContainer.findVisibleContainer()
        val parentContainerTag = selectedContainer?.parentTag

        val isRootContainer = parentContainerTag == null || (state.rootViewContainer.tag == parentContainerTag && state.rootViewContainer is TabContainerState)

        if (selectedContainer != null && selectedContainer.viewStates.count() > 1) {
            navigationCore.fire(NavigationEvent.PopNavView(selectedContainer.tag))
        } else if (!isRootContainer && selectedContainer?.cancellable == true) {
            navigationCore.fire(NavigationEvent.DismissModal(parentContainerTag!!))
        } else {
            finish()
        }
    }
}

private object ReactorTransitions {
    val FADE_DEFAULT_TIME = 250L
    val FADE_OVERLAP = 50L

    val SLIDE_DEFAULT_TIME = 500L

    val exitFade: Transition
        get() {
            val exitFade = Fade()
            exitFade.duration = FADE_DEFAULT_TIME
            return exitFade
        }

    val enterFade: Transition
        get() {
            val exitFade = Fade()
            exitFade.startDelay = FADE_DEFAULT_TIME - FADE_OVERLAP
            exitFade.duration = FADE_DEFAULT_TIME
            return exitFade
        }

    val rightSlide: Transition
        get() {
            val enterSlide = Slide(Gravity.RIGHT)
            enterSlide.duration = SLIDE_DEFAULT_TIME
            return enterSlide
        }

    val leftSlide: Transition
        get() {
            val enterSlide = Slide(Gravity.LEFT)
            enterSlide.duration = SLIDE_DEFAULT_TIME
            return enterSlide
        }

    val bottomSlide: Transition
        get() {
            val enterSlide = Slide(Gravity.BOTTOM)
            enterSlide.duration = SLIDE_DEFAULT_TIME
            return enterSlide
        }
}
