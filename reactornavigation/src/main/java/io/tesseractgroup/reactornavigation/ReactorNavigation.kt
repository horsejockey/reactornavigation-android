package io.tesseractgroup.reactornavigation

import android.content.Context
import io.tesseractgroup.messagerouter.MessageRouter
import io.tesseractgroup.reactor.CommandProcessor
import io.tesseractgroup.reactor.Core
import io.tesseractgroup.reactor.CoreUpdate

/**
 * PrototypeBluetoothLibrary
 * Created by matt on 11/29/17.
 */

class NavigationCommand

abstract class NavigationStateProtocol {

    abstract var rootViewContainer: ViewContainerState
    var appInForeground: Boolean = true

    private fun findSubstateWithTag(tag: ViewContainerTag): ViewContainerState? {
        return rootViewContainer.findSubstateWithTag(tag)
    }

    fun findVisibleView(): ReactorViewState? {
        return rootViewContainer.findVisibleView()
    }

    fun findVisibleContainer(): NavContainerState? {
        return rootViewContainer.findVisibleContainer()
    }
}

object ReactorNavigation {

    fun createNavigationCore(
        state: NavigationStateProtocol,
        commandProcessors: List<CommandProcessor<NavigationStateProtocol, NavigationEvent, NavigationCommand>> = listOf())
        : Core<NavigationStateProtocol, NavigationEvent, NavigationCommand> {
        val processors = commandProcessors + ::commandProcessor
        return Core(state, processors, ::eventHandler)
    }

    private fun eventHandler(state: NavigationStateProtocol, event: NavigationEvent): CoreUpdate<NavigationStateProtocol, NavigationCommand> {

        val containerToUpdate = state.rootViewContainer.findSubstateWithTag(event.containerId)

        when (event) {
            is NavigationEvent.ChangeContainerIndex -> {
                if (containerToUpdate is TabContainerState) {
                    containerToUpdate.selectedIndex = event.selectedIndex
                }
            }
            is NavigationEvent.PresentModally -> {
                containerToUpdate?.modal = event.viewContainer
                containerToUpdate?.modal?.parentTag = event.containerId
            }
            is NavigationEvent.DismissModal -> {
                containerToUpdate?.modal = null
            }
            is NavigationEvent.PushNavView -> {
                if (containerToUpdate is NavContainerState) {
                    containerToUpdate.viewStates = containerToUpdate.viewStates.plus(event.view)
                }
            }
            is NavigationEvent.PopNavView -> {
                if (containerToUpdate is NavContainerState && containerToUpdate.viewStates.count() > 1) {
                    containerToUpdate.viewStates = containerToUpdate.viewStates.dropLast(1)
                }
            }
            is NavigationEvent.UnwindToView -> {
                if (containerToUpdate is NavContainerState) {
                    val unwindToView = event.view
                    if (unwindToView != null) {
                        val index = containerToUpdate.viewStates.indexOf(unwindToView) + 1
                        containerToUpdate.viewStates = containerToUpdate.viewStates.subList(0, index)
                    } else if (containerToUpdate.viewStates.count() > 1) {
                        containerToUpdate.viewStates = listOf(containerToUpdate.viewStates.first())
                    }
                }
            }
            is NavigationEvent.AppContextChanged -> {
                state.appInForeground = event.inForeground
            }
        }

        return CoreUpdate.StateAndCommands(state, listOf(NavigationCommand()))
    }

    val navigationCommandReceived = MessageRouter<NavigationCommand>()

    private fun commandProcessor(@Suppress("UNUSED_PARAMETER") core: Core<NavigationStateProtocol, NavigationEvent, NavigationCommand>, command: NavigationCommand) {
        navigationCommandReceived.send(command)
    }
}

interface ViewStateConvertible {
    fun state(): ReactorViewState
}

interface ReactorViewState {
    fun view(context: Context): ReactorView
    override fun equals(other: Any?): Boolean
}

typealias ViewContainerTag = String

abstract class ViewContainerState {

    abstract val tag: ViewContainerTag
    abstract var modal: ViewContainerState?
    var parentTag: ViewContainerTag? = null

    internal fun findSubstateWithTag(tag: ViewContainerTag): ViewContainerState? {
        if (tag == this.tag) {
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

    internal fun findVisibleView(): ReactorViewState? {
        return findVisibleContainer()?.viewStates?.lastOrNull()
    }

    internal fun findVisibleContainer(): NavContainerState? {
        if (this.modal != null) {
            return this.modal?.findVisibleContainer()
        } else if (this is TabContainerState) {
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
}

data class TabContainerState(override val tag: ViewContainerTag, val tabContainers: List<ViewContainerState>, override var modal: ViewContainerState? = null, var selectedIndex: Int = 0) : ViewContainerState()

data class NavContainerState(override val tag: ViewContainerTag, var viewStates: List<ReactorViewState>, override var modal: ViewContainerState? = null) : ViewContainerState()

sealed class NavigationEvent(val containerId: ViewContainerTag) {

    class AppContextChanged(val inForeground: Boolean) : NavigationEvent("rn_none")

    class ChangeContainerIndex(
        containerId: ViewContainerTag,
        val selectedIndex: Int) : NavigationEvent(containerId)

    class PresentModally(
        overContainerTag: ViewContainerTag,
        val viewContainer: ViewContainerState
    ) : NavigationEvent(overContainerTag)

    class DismissModal(forContainer: ViewContainerTag) : NavigationEvent(forContainer)

    class PushNavView(
        containerId: ViewContainerTag,
        val view: ReactorViewState) : NavigationEvent(containerId)

    class PopNavView(
        containerId: ViewContainerTag) : NavigationEvent(containerId)

    class UnwindToView(
        containerId: ViewContainerTag,
        val view: ReactorViewState?) : NavigationEvent(containerId)
}