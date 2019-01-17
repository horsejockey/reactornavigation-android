package io.tesseractgroup.reactornavigationapp;

import android.annotation.SuppressLint
import android.content.Context
import android.support.v7.widget.Toolbar
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import io.tesseractgroup.reactornavigation.*
import kotlinx.android.synthetic.main.view_name.view.*

class NameViewState : ReactorViewState {

    var editTextName: String = ""

    override fun view(context: Context): NameView {
        return NameView(context, name, this)
    }
}

@SuppressLint("ViewConstructor")
class NameView(context: Context, val name: String, override val viewState: NameViewState) : ReactorView(context, R.layout.view_name, viewState) {

    override fun viewSetup(toolbar: Toolbar) {

//        setupToolbar(toolbar)

        Log.i("NAVIGATION_${this.className()})", "View setup")

        textView_savedState.setText(name)

        editText_name.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                val text = s.toString()
                viewState.editTextName = text
                App.core.fire(AppEvent.UpdateName(text))
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

        })

        editText_name.setText(viewState.editTextName)

        button.setOnClickListener {
            App.navigationCore.fire(NavigationEvent.PushNavView(NAV_CONT, NameViewState(editText_name.text.toString())))
        }

        setupToolbar(toolbar)
    }

    override fun viewDidAppear() {
        super.viewDidAppear()
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
