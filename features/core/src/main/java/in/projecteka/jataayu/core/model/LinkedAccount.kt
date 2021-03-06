package `in`.projecteka.jataayu.core.model

import `in`.projecteka.jataayu.core.BR
import `in`.projecteka.jataayu.core.R
import `in`.projecteka.jataayu.presentation.callback.IDataBindingModel
import `in`.projecteka.jataayu.presentation.callback.IGroupDataBindingModel
import androidx.annotation.IdRes

data class LinkedAccount(
    val providerName: String?,
    val patientReferenceId: String?,
    val patientName: String?,
    override val childrenViewModels: List<IDataBindingModel>?,
    @IdRes override val childrenResourceId: Int,
    override var isExpanded: Boolean
) : IGroupDataBindingModel {

    override fun layoutResId(): Int {
        return R.layout.account_item
    }

    override fun dataBindingVariable(): Int {
        return BR.linkedAccount
    }
}