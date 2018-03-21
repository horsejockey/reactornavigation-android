package io.tesseractgroup.reactornavigation

import android.annotation.SuppressLint
import android.content.Context
import android.view.View

/**
 * Prototype-MyLifterBluetooth
 * Created by matt on 12/14/17.
 */
data class ReactorDialogAction(
        val layoutId: Int,
        val onClick: (() -> Unit)
)

@SuppressLint("ViewConstructor")
class ReactorDialog constructor(context: Context, val layout: Int, val actions: List<ReactorDialogAction>) : ReactorView(context, layout), ViewStateConvertible {

    override fun state(): ReactorViewState {
        return ReactorDialogState(layout, actions)
    }

    override fun viewSetup(){
        for (action in actions){
            findViewById<View>(action.layoutId).setOnClickListener {
                action.onClick()
            }
        }
    }
}

data class ReactorDialogState(val layout: Int, val actions: List<ReactorDialogAction>) : ReactorViewState {

    override fun view(context: Context): View {
        return ReactorDialog(context, layout, actions)
    }
}

