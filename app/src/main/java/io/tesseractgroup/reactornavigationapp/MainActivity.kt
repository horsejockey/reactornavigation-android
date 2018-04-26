package io.tesseractgroup.reactornavigationapp

import android.os.Bundle
import io.tesseractgroup.messagerouter.MessageRouter
import io.tesseractgroup.reactor.Command
import io.tesseractgroup.reactor.Core
import io.tesseractgroup.reactor.Event
import io.tesseractgroup.reactornavigation.*

object App {
    private val handler = fun(state: AppState, event: Event): Pair<AppState, Command> {
        when(event){
            is AppEvent -> {
                when(event){
                    is AppEvent.UpdateName -> {
                        val updatedState = state.copy(name = event.name)
                        return Pair(updatedState, AppCommand.NoOp())
                    }
                }
            }
            is NavigationEvent -> {
                val updates = Navigation.handler(state.navigationState, event)
                val updatedState = state.copy(navigationState = updates.first)
                return Pair(updatedState, updates.second)
            }
        }
        return Pair(state, AppCommand.NoOp())
    }


    val core = Core<AppState>(AppState("Hello World", NavigationState()), listOf(ReactorNavigation::handler), handler)
}

sealed class AppEvent: Event {
    data class UpdateName(val name: String?): AppEvent()
}

sealed class AppCommand: Command {
    class NoOp: AppCommand()
}

data class AppState(val name: String?, val navigationState: NavigationStateProtocol)

const val NAV_CONT = "Main Container"

class NavigationState : NavigationStateProtocol(){

    override var rootViewContainer: ViewContainerState = NavContainerState(NAV_CONT, listOf(NameViewState()))
}

object ReactorNavigation {

    val navigationCommandReceived = MessageRouter<NavigationCommand>()

    fun handler(core: Core<AppState>, command: Command) {
        if (command is NavigationCommand) {
            navigationCommandReceived.send(command)
        }
    }
}


class MainActivity : ReactorActivity(R.layout.activity_main, R.id.my_toolbar, R.id.container_reactor) {


    override var reactorViewModel: ReactorActivityViewModelInterface = ReactorActivityViewModel(App.core, { state: AppState -> state.navigationState }, ReactorNavigation.navigationCommandReceived)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
    }
}
