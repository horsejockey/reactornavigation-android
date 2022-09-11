package io.tesseractgroup.reactornavigation

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import io.tesseractgroup.reactor.reactornavigation.R

class ReactorFragment: Fragment(){

    companion object {

        fun newInstance(reactorView: ReactorView): ReactorFragment {
            val reactorFragment = ReactorFragment()
            reactorFragment.reactorView = reactorView
            return reactorFragment
        }
    }

    lateinit var reactorView: ReactorView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_reactor, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        if (view is ViewGroup && view.childCount == 0) {
            view.addView(reactorView)
        }
    }
}