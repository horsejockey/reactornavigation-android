package io.tesseractgroup.reactornavigation

import android.content.Context
import android.view.View
import io.tesseractgroup.reactor.Event
import io.tesseractgroup.reactor.State

/**
 * PrototypeBluetoothLibrary
 * Created by matt on 11/29/17.
 */

abstract class NavigationStateProtocol : State {

    abstract var rootViewContainer: ViewContainerState
    abstract var overlay: ReactorViewState?

    fun findSubstateWithTag(tag: ViewContainerTag): ViewContainerState? {
        return rootViewContainer.findSubstateWithTag(tag)
    }
    fun findVisibleView() : ReactorViewState? {
        return rootViewContainer.findVisibleView()
    }

    fun findVisibleContainer(): NavContainerState?{
        return rootViewContainer.findVisibleContainer()
    }

    override fun reactTo(event: Event) {
        rootViewContainer.reactTo(event)
        if (event is NavigationEvent.ShowOverlay){
            overlay = event.viewState
        }else if(event is NavigationEvent.DismissOverlay){
            overlay = null
        }
    }
}

interface ViewStateConvertible {
    fun state(): ReactorViewState
}

interface ReactorViewState {
    fun view(context: Context): View
    override fun equals(other: Any?): Boolean
}

typealias ViewContainerTag = String

abstract class ViewContainerState {
    abstract val containerTag: ViewContainerTag
    abstract var modal: ViewContainerState?
    var parentContainerTag: ViewContainerTag? = null
        internal set

    internal fun findSubstateWithTag(tag: ViewContainerTag): ViewContainerState? {
        if (tag == containerTag) {
            return this
        } else if (modal?.findSubstateWithTag(tag) != null) {
            return modal?.findSubstateWithTag(tag)
        } else if (this is TabContainerState) {
            for (tabContainer in this.tabContainers) {
                if (tabContainer.findSubstateWithTag(tag) != null) {
                    return tabContainer.findSubstateWithTag(tag)
                }
            }
        }
        return null
    }

    internal fun findVisibleView() : ReactorViewState? {
        return findVisibleContainer()?.viewStates?.lastOrNull()
    }

    internal fun findVisibleContainer(): NavContainerState? {
        if (this.modal != null) {
            return this.modal?.findVisibleContainer()
        }else if (this is TabContainerState) {
            val visibleTab = this.tabContainers[selectedIndex]
            val visibleSubState = visibleTab.findVisibleContainer()
            if (visibleSubState != null) {
                return visibleSubState
            } else if (visibleTab is NavContainerState) {
                return visibleTab
            }
        } else if (this is NavContainerState) {
            return this
        }
        return null
    }

    abstract fun reactTo(event: Event)
}


data class TabContainerState(override val containerTag: ViewContainerTag, var tabContainers: List<ViewContainerState>) : ViewContainerState() {

    override var modal: ViewContainerState? = null
    var selectedIndex: Int = 0

    override fun reactTo(event: Event) {
        if (event is NavigationEvent) {
            if (event.containerId == this.containerTag) {
                when (event) {
                    is NavigationEvent.PresentModally -> {
                        modal = event.viewContainer
                        modal?.parentContainerTag = this.containerTag
                    }
                    is NavigationEvent.DismissModal -> modal = null
                    is NavigationEvent.ChangeTabContainerIndex -> selectedIndex = event.selectedIndex
                }
            } else {
                for (viewContainer in tabContainers){
                    viewContainer.reactTo(event)
                }
                modal?.reactTo(event)
            }
        }
    }
}

data class NavContainerState(override val containerTag: ViewContainerTag, var viewStates: List<ReactorViewState>) : ViewContainerState() {

    override var modal: ViewContainerState? = null

    override fun reactTo(event: Event) {
        if (event is NavigationEvent) {
            if (event.containerId == this.containerTag) {
                when (event) {
                    is NavigationEvent.PresentModally -> {
                        modal = event.viewContainer
                        modal?.parentContainerTag = this.containerTag
                    }
                    is NavigationEvent.DismissModal -> modal = null
                    is NavigationEvent.PopNavView -> {
                        if (viewStates.count() > 1) {
                            viewStates = viewStates.dropLast(1)
                        }
                    }
                    is NavigationEvent.PushNavView -> {
                        viewStates = viewStates.plus(event.view)
                    }
                    is NavigationEvent.UnwindToView -> {
                        val unwindToView = event.view
                        if (unwindToView != null) {
                            val index = viewStates.indexOf(unwindToView)
                        } else if (viewStates.count() > 1) {
                            viewStates = listOf(viewStates.first())
                        }
                    }
                }
            } else {
                modal?.reactTo(event)
            }
        }
    }
}

private val overLayTag: ViewContainerTag = "Reactor Overlay"

sealed class NavigationEvent(val containerId: ViewContainerTag) : Event {

    class PresentModally(val viewContainer: ViewContainerState, overContainerTag: ViewContainerTag) : NavigationEvent(overContainerTag)
    class DismissModal(forContainer: ViewContainerTag) : NavigationEvent(forContainer)

    class ChangeTabContainerIndex(containerId: ViewContainerTag, val selectedIndex: Int) : NavigationEvent(containerId)

    class PushNavView(val view: ReactorViewState, containerId: ViewContainerTag) : NavigationEvent(containerId)
    class PopNavView(containerId: ViewContainerTag) : NavigationEvent(containerId)
    class UnwindToView(val view: ReactorViewState?, containerId: ViewContainerTag) : NavigationEvent(containerId)

    class ShowOverlay(val viewState: ReactorViewState) : NavigationEvent(overLayTag)
    class DismissOverlay : NavigationEvent(overLayTag)
}
