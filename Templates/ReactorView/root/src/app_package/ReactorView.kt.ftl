package ${packageName};

import android.content.Context
import android.support.v7.widget.Toolbar
import io.tesseractgroup.reactornavigation.ReactorView
import io.tesseractgroup.reactornavigation.ReactorViewState

class ${className}ViewState : ReactorViewState {

    override fun view(context: Context): ${className}View {
        return ${className}View(context, this)
    }
}

@SuppressLint("ViewConstructor")
class ${className}View(context: Context, override val viewState: ${className}ViewState) : ReactorView(context, R.layout.view_${className?lower_case}, viewState) {


    override fun viewSetup(toolbar: Toolbar) {
    }

    override fun viewDidAppear() {
        super.viewDidAppear()
    }

    override fun viewDidDisappear() {
        super.viewDidDisappear()
    }

    override fun viewTearDown() {
        super.viewTearDown()
    }

}
 