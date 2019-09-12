package io.tesseractgroup.reactornavigation

import android.content.Context
import android.content.DialogInterface
import io.tesseractgroup.messagerouter.MessageRouter
import io.tesseractgroup.purestatemachine.StateUpdate
import io.tesseractgroup.reactor.CommandProcessor
import io.tesseractgroup.reactor.Core

/**
 * PrototypeBluetoothLibrary
 * Created by matt on 11/29/17.
 */
internal interface VisibleViewChanged

data class AlertButton(
        val title: String,
        val action: ((DialogInterface, Int) -> Unit)? = null
)

sealed class NavigationCommand(val navStackChanged: Boolean) {
    // View Container Changes
    data class TabIndexChanged(val lastVisibleViewContainer: NavContainerState) : NavigationCommand(true), VisibleViewChanged

    data class ModalPresented(val lastVisibleViewContainer: NavContainerState) : NavigationCommand(true), VisibleViewChanged
    data class ModalDismissed(val lastVisibleViewContainer: NavContainerState) : NavigationCommand(true), VisibleViewChanged
    object RootContainerChanged : NavigationCommand(true), VisibleViewChanged
    // View State Changes
    object NavViewPushed : NavigationCommand(true), VisibleViewChanged

    object NavViewPopped : NavigationCommand(true), VisibleViewChanged
    object NavViewReplaced : NavigationCommand(true), VisibleViewChanged

    // Change that didn't affect the visible View State
    object HiddenUpdate : NavigationCommand(true)

    object AppContextChanged : NavigationCommand(false)
    data class PresentAlert(
            val title: String? = null,
            val message: String? = null,
            val buttons: List<AlertButton>
    ) : NavigationCommand(false)
}

abstract class NavigationStateProtocol {

    abstract var rootViewContainer: ViewContainerState
        internal set
    var appInForeground: Boolean = true
        internal set

    internal fun findSubstateWithTag(tag: ViewContainerTag): ViewContainerState? {
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

    private fun eventHandler(state: NavigationStateProtocol, event: NavigationEvent): StateUpdate<NavigationStateProtocol, NavigationCommand> {

        val noContainer = "NO_CONTAINER"
        val oldVisibleViewContainer = state.findVisibleContainer()

        val containerId = when (event) {
            is NavigationEvent.AppContextChanged -> noContainer
            is NavigationEvent.ChangeContainerIndex -> event.containerId
            is NavigationEvent.PresentModally -> event.containerId ?: oldVisibleViewContainer?.tag
            is NavigationEvent.DismissModal -> event.containerId
                    ?: oldVisibleViewContainer?.parentTag
            is NavigationEvent.PushNavView -> event.containerId ?: oldVisibleViewContainer?.tag
            is NavigationEvent.ReplaceNavView -> event.containerId ?: oldVisibleViewContainer?.tag
            is NavigationEvent.ReplaceNavViewStack -> event.containerId
            is NavigationEvent.PopNavView -> event.containerId ?: oldVisibleViewContainer?.tag
            is NavigationEvent.UnwindToView -> event.containerId
            is NavigationEvent.ReplaceRootContainer -> noContainer
        }

        if (containerId == null) return StateUpdate.NoUpdate()

        val containerToUpdate = state.rootViewContainer.findSubstateWithTag(containerId)
        var command: NavigationCommand = NavigationCommand.HiddenUpdate
        val updatingCurrentContainer = oldVisibleViewContainer == containerToUpdate

        when (event) {
            is NavigationEvent.ChangeContainerIndex -> {
                if (containerToUpdate is TabContainerState) {
                    containerToUpdate.selectedIndex = event.selectedIndex
                    if (state.findVisibleContainer() != oldVisibleViewContainer && oldVisibleViewContainer != null) {
                        command = NavigationCommand.TabIndexChanged(oldVisibleViewContainer)
                    }
                }
            }
            is NavigationEvent.PresentModally -> {
                containerToUpdate?.modal = event.viewContainer
                containerToUpdate?.modal?.parentTag = containerId
                if (state.findVisibleContainer() != oldVisibleViewContainer && oldVisibleViewContainer != null) {
                    command = NavigationCommand.ModalPresented(oldVisibleViewContainer)
                }
            }
            is NavigationEvent.DismissModal -> {
                containerToUpdate?.modal = null
                if (state.findVisibleContainer() != oldVisibleViewContainer && oldVisibleViewContainer != null) {
                    command = NavigationCommand.ModalDismissed(oldVisibleViewContainer)
                }
            }
            is NavigationEvent.PushNavView -> {
                if (containerToUpdate is NavContainerState) {
                    containerToUpdate.viewStates = containerToUpdate.viewStates.plus(event.view)
                    if (updatingCurrentContainer) {
                        command = NavigationCommand.NavViewPushed
                    }
                }
            }
            is NavigationEvent.PopNavView -> {
                if (containerToUpdate is NavContainerState && containerToUpdate.viewStates.count() > 1) {
                    containerToUpdate.viewStates = containerToUpdate.viewStates.dropLast(1)
                    if (updatingCurrentContainer) {
                        command = NavigationCommand.NavViewPopped
                    }
                }
            }
            is NavigationEvent.UnwindToView -> {
                if (containerToUpdate is NavContainerState) {
                    val unwindToView = event.view
                    val lastView = containerToUpdate.viewStates.last()
                    if (unwindToView != null) {
                        val index = containerToUpdate.viewStates.indexOf(unwindToView) + 1
                        containerToUpdate.viewStates = containerToUpdate.viewStates.subList(0, index)
                    } else if (containerToUpdate.viewStates.count() > 1) {
                        containerToUpdate.viewStates = listOf(containerToUpdate.viewStates.first())
                    }
                    if (updatingCurrentContainer && lastView != state.findVisibleView()) {
                        command = NavigationCommand.NavViewPopped
                    }
                }
            }
            is NavigationEvent.ReplaceNavView -> {
                if (containerToUpdate is NavContainerState) {
                    containerToUpdate.viewStates = containerToUpdate.viewStates.dropLast(1)
                    containerToUpdate.viewStates = containerToUpdate.viewStates.plus(event.view)
                    if (updatingCurrentContainer) {
                        command = NavigationCommand.NavViewReplaced
                    }
                }
            }
            is NavigationEvent.ReplaceNavViewStack -> {
                if (containerToUpdate is NavContainerState) {
                    containerToUpdate.viewStates = event.views
                    if (updatingCurrentContainer) {
                        command = NavigationCommand.NavViewPopped
                    }
                }
            }
            is NavigationEvent.AppContextChanged -> {
                state.appInForeground = event.inForeground
                command = NavigationCommand.AppContextChanged
            }
            is NavigationEvent.ReplaceRootContainer -> {
                state.rootViewContainer = event.container
                command = NavigationCommand.RootContainerChanged
            }
        }

        return StateUpdate.StateAndCommands(state, listOf(command))
    }

    val navigationCommandReceived = MessageRouter<NavigationCommand>()

    private fun commandProcessor(@Suppress("UNUSED_PARAMETER") core: Core<NavigationStateProtocol, NavigationEvent, NavigationCommand>, command: NavigationCommand) {
        navigationCommandReceived.send(command)
    }
}

interface ReactorViewState {
    fun view(context: Context): ReactorView
}

typealias ViewContainerTag = String

abstract class ViewContainerState {

    abstract var tag: ViewContainerTag
        internal set
    abstract var modal: NavContainerState?
        internal set
    var parentTag: ViewContainerTag? = null
        internal set

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
            } else {
                return visibleTab
            }
        } else if (this is NavContainerState) {
            return this
        }
        return null
    }
}

class TabContainerState(override var tag: ViewContainerTag, val tabContainers: List<NavContainerState>, override var modal: NavContainerState? = null, selectedIndex: Int = 0) : ViewContainerState() {

    var selectedIndex: Int
        internal set

    init {
        this.selectedIndex = selectedIndex
        for (container in tabContainers) {
            container.parentTag = tag
        }
        modal?.parentTag = tag
    }
}

class NavContainerState(override var tag: ViewContainerTag, viewStates: List<ReactorViewState>, override var modal: NavContainerState? = null, val cancellable: Boolean = true) : ViewContainerState() {

    var viewStates: List<ReactorViewState>
        internal set

    init {
        this.viewStates = viewStates
        this.modal = modal
        modal?.parentTag = tag
    }
}

sealed class NavigationEvent(open val containerId: ViewContainerTag?) {

    class AppContextChanged(val inForeground: Boolean) : NavigationEvent(null)

    class ChangeContainerIndex(
            val selectedIndex: Int,
            override val containerId: ViewContainerTag
    ) : NavigationEvent(containerId)

    class PresentModally(
            val viewContainer: NavContainerState,
            overContainerTag: ViewContainerTag? = null
    ) : NavigationEvent(overContainerTag)

    class DismissModal(forContainer: ViewContainerTag? = null) : NavigationEvent(forContainer)

    class PushNavView(
            val view: ReactorViewState,
            containerId: ViewContainerTag? = null
    ) : NavigationEvent(containerId)

    class ReplaceNavView(
            val view: ReactorViewState,
            containerId: ViewContainerTag? = null
    ) : NavigationEvent(containerId)

    class ReplaceNavViewStack(
            val views: List<ReactorViewState>,
            override val containerId: ViewContainerTag
    ) : NavigationEvent(containerId)

    class PopNavView(
            containerId: ViewContainerTag? = null
    ) : NavigationEvent(containerId)

    class UnwindToView(
            val view: ReactorViewState?,
            override val containerId: ViewContainerTag
    ) : NavigationEvent(containerId)

    class ReplaceRootContainer(
            val container: ViewContainerState
    ) : NavigationEvent(null)
}