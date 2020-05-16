package `in`.projecteka.jataayu.registration.viewmodel

import `in`.projecteka.jataayu.presentation.BaseViewModel
import `in`.projecteka.jataayu.registration.ui.activity.R
import `in`.projecteka.jataayu.util.livedata.SingleLiveEvent
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.view.View
import androidx.databinding.ObservableBoolean
import androidx.databinding.ObservableField
import androidx.databinding.ObservableInt

class PasswordInputViewModel : BaseViewModel(), TextWatcher {

    val accountLockBlockEnable = ObservableField<Int>(View.GONE)
    val accountLockBlockDividerEnable = ObservableField<Int>(View.GONE)
    val passwordInputType = ObservableInt(hiddenPasswordInputType)
    val loginEnabled = ObservableBoolean(false)

    val inputPasswordLbl = ObservableField<String>()
    val inputPasswordVisibilityToggleLbl = ObservableField<Int>(R.string.show)
    val onPasswordVisibilityToggleEvent = SingleLiveEvent<Int>()
    var onClickForgotPasswordEvent = SingleLiveEvent<Void>()



    private val visiblePasswordInputType: Int
        get() = InputType.TYPE_CLASS_TEXT + InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
    private val hiddenPasswordInputType: Int
      get() = InputType.TYPE_CLASS_TEXT + InputType.TYPE_TEXT_VARIATION_PASSWORD

    override fun afterTextChanged(s: Editable?) {}
    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
        loginEnabled.set(inputPasswordLbl.get()?.isNotEmpty() == true)
    }

    fun onForgotPasswordClicked() {
        onClickForgotPasswordEvent.call()
    }

    fun togglePasswordVisible() {
        val newInputType = when (passwordInputType.get()) {
            visiblePasswordInputType -> {
                inputPasswordVisibilityToggleLbl.set(R.string.show)
                hiddenPasswordInputType
            }
            hiddenPasswordInputType -> {
                inputPasswordVisibilityToggleLbl.set(R.string.hide)
                visiblePasswordInputType
            }
            else -> hiddenPasswordInputType
        }
        passwordInputType.set(newInputType)
        onPasswordVisibilityToggleEvent.value = inputPasswordLbl.get()?.length ?: 0
    }

    fun onLoginClicked() {

    }

}
