package io.tesseractgroup.reactornavigationapp;

import android.content.Context
import android.support.v7.widget.Toolbar
import android.text.Editable
import android.text.TextWatcher
import io.tesseractgroup.reactornavigation.ReactorView
import io.tesseractgroup.reactornavigation.ReactorViewState
import io.tesseractgroup.reactornavigation.ViewStateConvertible
import kotlinx.android.synthetic.main.view_name.view.*

data class NameViewState(val description: String = "Name") : ReactorViewState {

    override fun view(context: Context): NameView {
        return NameView(context)
    }
}

class NameView(context: Context) : ReactorView(context, R.layout.view_name), ViewStateConvertible {

    override fun viewSetup(toolbar: Toolbar) {

        editText_name.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                val text = s.toString()
                App.core.fire(AppEvent.UpdateName(text))
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

        })

    }

    override fun state(): ReactorViewState {
        return NameViewState()
    }

    override fun viewDidAppear() {
        super.viewDidAppear()
    }

    override fun viewDidDisappear() {
        super.viewDidDisappear()
    }
}
