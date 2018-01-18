package com.mcarthurlabs.prototypebluetoothlibrary.reactornavigation

import android.content.Context
import android.view.View
import io.tesseractgroup.reactor.*

/**
 * PrototypeBluetoothLibrary
 * Created by matt on 11/29/17.
 */

interface NavigationStateProtocol : State {
    var rootViewContainers: List<ViewContainerState>
    var selectedIndex: Int
}

interface ViewStateConvertible {
    fun state(): ReactorViewState
}

interface ReactorViewState {
    fun view(context: Context): View
    override fun equals(other: Any?): Boolean
}

interface ViewContainerTag {
    override fun equals(other: Any?): Boolean
}

class ViewContainerState(val containerTag: ViewContainerTag, var viewStates: List<ReactorViewState>) : State {
    var modal: ViewContainerState? = null
    var overlay: ReactorViewState? = null

    override fun reactTo(event: Event) {
        if (event is NavigationEvent) {
            if (event.containerId == this.containerTag) {
                when (event) {
                    is NavigationEvent.ModalToViewEvent -> modal = event.modal
                    is NavigationEvent.DismissModalEvent -> modal = null
                    is NavigationEvent.PopViewEvent -> {
                        if (viewStates.count() > 1) {
                            viewStates = viewStates.dropLast(1)
                        }
                    }
                    is NavigationEvent.PushViewEvent -> {
                        viewStates = viewStates.plus(event.view)
                    }
                    is NavigationEvent.ShowOverlay -> {
                        if (overlay == null){
                            overlay = event.viewState
                        }
                    }
                    is NavigationEvent.DismissOverlay -> {
                        overlay = null
                    }
                    is NavigationEvent.UnwindEvent -> {
                        val unwindToView = event.view
                        if (unwindToView != null){
                            val index = viewStates.indexOf(unwindToView)
                        }else if (viewStates.count() > 1){
                            viewStates = listOf(viewStates.first())
                        }
                    }
                }
            } else {
                modal?.reactTo(event)
            }
        }
    }

    fun findVisibleViewState(): ReactorViewState? {
        if (modal != null) {
            return modal?.findVisibleViewState()
        } else {
            return viewStates.last()
        }
    }

    fun findVisibleOverlayState(): ReactorViewState? {
        if (modal != null) {
            return modal?.findVisibleOverlayState()
        } else {
            return overlay
        }
    }
}

fun NavigationStateProtocol.findSubstateWithId(id: ViewContainerTag): ViewContainerState? {
    for (containerState in rootViewContainers) {
        if (id == containerState.containerTag) {
            return containerState
        } else if (id == containerState.modal?.containerTag) {
            return containerState.modal
        }
    }

    return null
}


sealed class NavigationEvent(val containerId: ViewContainerTag) : Event {
    class ModalToViewEvent(val modal: ViewContainerState, containerId: ViewContainerTag) : NavigationEvent(containerId)
    class DismissModalEvent(containerId: ViewContainerTag) : NavigationEvent(containerId)
    class ChangeTabEvent(val selectedIndex: Int, containerId: ViewContainerTag) : NavigationEvent(containerId)
    class PushViewEvent(val view: ReactorViewState, containerId: ViewContainerTag) : NavigationEvent(containerId)
    class PopViewEvent(containerId: ViewContainerTag) : NavigationEvent(containerId)
    class UnwindEvent(val view: ReactorViewState?, containerId: ViewContainerTag) : NavigationEvent(containerId)

    class ShowOverlay(containerId: ViewContainerTag, val viewState: ReactorViewState) : NavigationEvent(containerId)
    class DismissOverlay(containerId: ViewContainerTag) : NavigationEvent(containerId)
}
