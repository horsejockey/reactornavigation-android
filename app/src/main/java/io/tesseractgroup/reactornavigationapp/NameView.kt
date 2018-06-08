package io.tesseractgroup.reactornavigationapp;

import android.content.Context
import android.support.v7.widget.Toolbar
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.widget.ImageView
import io.tesseractgroup.reactornavigation.*
import kotlinx.android.synthetic.main.view_name.view.*

data class NameViewState(val description: String = "Name") : ReactorViewState {

    override fun view(context: Context): NameView {
        return NameView(context)
    }
}

class NameView(context: Context) : ReactorView(context, R.layout.view_name), ViewStateConvertible {

    override fun viewSetup(toolbar: Toolbar) {

//        setupToolbar(toolbar)

        Log.i("NAVIGATION_${this.className()})", "View setup")

//        editText_name.setText("Hello world.")

        editText_name.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                val text = s.toString()
                App.core.fire(AppEvent.UpdateName(text))
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

        })

        button.setOnClickListener {
            App.core.fire(NavigationEvent.PushNavView(NAV_CONT, NameViewState(editText_name.text.toString())))
        }

    }

    override fun state(): ReactorViewState {
        return NameViewState()
    }

    override fun viewDidAppear() {
        super.viewDidAppear()
        setupToolbar((context as MainActivity).toolbar)
    }

    override fun viewDidDisappear() {
        super.viewDidDisappear()
    }

    private fun setupToolbar(toolbar: Toolbar) {
        (context as MainActivity).menuInflater.inflate(R.menu.settings, toolbar.menu)
//        toolbar.inflateMenu(R.menu.settings)
//        toolbar.findViewById<ImageView>(R.id.logo).visibility = View.VISIBLE
//        toolbar.setOnMenuItemClickListener(onMenuItemClickListener)
    }
}
