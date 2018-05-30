package ${packageName};

import android.content.Context
import android.support.v7.widget.Toolbar
import io.tesseractgroup.reactornavigation.ReactorView
import io.tesseractgroup.reactornavigation.ReactorViewState
import io.tesseractgroup.reactornavigation.ViewStateConvertible


data class ${className}ViewState(val description: String = "${className}") : ReactorViewState {

    override fun view(context: Context): ${className}View {
        return ${className}View(context)
    }
}

class ${className}View(context: Context) : ReactorView(context, R.layout.view_${className?lower_case}), ViewStateConvertible {

    override fun state(): ReactorViewState {
        return ${className}ViewState()
    }

    override fun viewSetup(toolbar: Toolbar) {

    }

    override fun viewDidAppear() {
        super.viewDidAppear()
    }

    override fun viewDidDisappear() {
        super.viewDidDisappear()
    }

}
 