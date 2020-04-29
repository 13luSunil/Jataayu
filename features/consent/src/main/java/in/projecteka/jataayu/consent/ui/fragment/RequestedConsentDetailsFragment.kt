package `in`.projecteka.jataayu.consent.ui.fragment

import `in`.projecteka.jataayu.consent.R
import `in`.projecteka.jataayu.consent.databinding.RequestedConsentDetailsFragmentBinding
import `in`.projecteka.jataayu.consent.ui.activity.ConsentDetailsActivity
import `in`.projecteka.jataayu.consent.ui.activity.CreatePinActivity
import `in`.projecteka.jataayu.consent.ui.activity.PinVerificationActivity
import `in`.projecteka.jataayu.consent.viewmodel.RequestedConsentDetailsViewModel
import `in`.projecteka.jataayu.core.model.*
import `in`.projecteka.jataayu.core.model.approveconsent.ConsentArtifactResponse
import `in`.projecteka.jataayu.core.model.approveconsent.HiTypeAndLinks
import `in`.projecteka.jataayu.network.utils.*
import `in`.projecteka.jataayu.presentation.callback.IDataBindingModel
import `in`.projecteka.jataayu.presentation.callback.ItemClickCallback
import `in`.projecteka.jataayu.presentation.showAlertDialog
import `in`.projecteka.jataayu.presentation.ui.fragment.BaseFragment
import `in`.projecteka.jataayu.provider.ui.handler.ConsentDetailsClickHandler
import `in`.projecteka.jataayu.util.extension.setTitle
import `in`.projecteka.jataayu.util.ui.DateTimeUtils
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.ViewDataBinding
import androidx.lifecycle.Observer
import com.google.android.material.chip.Chip
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.koin.androidx.viewmodel.ext.android.sharedViewModel


class RequestedConsentDetailsFragment : BaseFragment(), ItemClickCallback,
    ConsentDetailsClickHandler {

    private lateinit var binding: RequestedConsentDetailsFragmentBinding

    private val viewModel: RequestedConsentDetailsViewModel by sharedViewModel()

    private lateinit var consent: Consent

    private var hiTypeObjects = ArrayList<HiType>()

    private var linkedAccounts: List<Links>? = null

    private val eventBusInstance: EventBus = EventBus.getDefault()

    override fun onItemClick(
        iDataBindingModel: IDataBindingModel,
        itemViewBinding: ViewDataBinding
    ) {
    }

    companion object {
        fun newInstance() = RequestedConsentDetailsFragment()
        const val KEY_CONSENT_EVENT_TYPE = "consent_event_type"
        const val KEY_EVENT_GRANT = "grant"
        const val KEY_EVENT_DENY = "deny"
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initObservers()
        binding.clickHandler = this
    }

    private fun initObservers() {
        if (!eventBusInstance.isRegistered(this))
            eventBusInstance.register(this)

        if (viewModel.linkedAccountsResponse.value == null) {
            viewModel.linkedAccountsResponse.observe(this, Observer<PayloadResource<LinkedAccountsResponse>> {
                when (it) {
                    is Loading -> showProgressBar(it.isLoading)
                    is Success -> {
                        linkedAccounts = it.data?.linkedPatient?.links
                        linkedAccounts?.forEach { link ->
                            link.careContexts.forEach { careContext ->
                                careContext.contextChecked = true
                            }
                        }
                    }
                }
            })
            viewModel.getLinkedAccounts()
        }
        viewModel.consentArtifactResponse.observe(this, Observer<PayloadResource<ConsentArtifactResponse>> {
            when (it) {
                is Loading -> {
                    showProgressBar(it.isLoading)
                }
                is Success -> {
                    if (it.data?.consents?.isNotEmpty() == true) {
                        val intent = Intent()
                        intent.putExtra(KEY_CONSENT_EVENT_TYPE, KEY_EVENT_GRANT)
                        activity?.setResult(Activity.RESULT_OK, intent)
                        activity?.finish()
                    }
                }
            }
        })
        viewModel.consentDenyResponse.observe(this, Observer<PayloadResource<Void>> {
            when (it) {
                is Loading -> showProgressBar(it.isLoading, getString(R.string.denying_consent))
                is Success -> {
                    activity?.let {
                        val intent = Intent()
                        intent.putExtra(KEY_CONSENT_EVENT_TYPE, KEY_EVENT_DENY)
                        activity?.setResult(Activity.RESULT_OK, intent)
                        activity?.finish()
                    }
                }
                is PartialFailure -> {
                    context?.showAlertDialog(
                        getString(R.string.failure), it.error?.message,
                        getString(android.R.string.ok)
                    )
                }
                is Failure -> {
                    context?.showAlertDialog(
                        getString(R.string.failure), it.error?.message,
                        getString(android.R.string.ok)
                    )
                }
            }
        })
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = RequestedConsentDetailsFragmentBinding.inflate(inflater)
        return binding.root
    }

    private fun renderUi() {

        with(binding) {
            this.consent = this@RequestedConsentDetailsFragment.consent
            requestExpired = isExpiredOrGrantedOrDenied()
            isGrantedConsent = false
            cgRequestInfoTypes.removeAllViews()
        }

        if (hiTypeObjects.isEmpty()) createHiTypesFromConsent()

        hiTypeObjects.forEach { hiType ->
            if (hiType.isChecked) {
                binding.cgRequestInfoTypes.addView(newChip(hiType.type))
            }
        }
    }

    override fun onEditClick(view: View) {
        linkedAccounts?.let {
            eventBusInstance.postSticky(HiTypeAndLinks(hiTypeObjects, it))
            (activity as ConsentDetailsActivity).editConsentDetails()
        }
    }

    override fun onDenyConsent(view: View) {
        MaterialAlertDialogBuilder(context)
            .setTitle(R.string.title_deny_consent)
            .setPositiveButton(R.string.deny) { _, _ -> viewModel.denyConsent(consent.id) }
            .setNegativeButton(android.R.string.cancel, null)
            .setMessage(R.string.msg_deny_consent)
            .show()
    }

    override fun onGrantConsent(view: View) {
        verifyAction()
    }

    private fun verifyAction() {
        startAuthenticator()
    }

    private fun startAuthenticator() {
        validateUser()
    }

    private fun createHiTypesFromConsent() {
        consent.hiTypes.forEach {
            hiTypeObjects.add(HiType(it, true))
        }
    }

    private fun newChip(description: String): Chip? =
        Chip(context, null, R.style.Chip_NonEditable).apply {
            text = description
        }

    override fun onVisible() {
        super.onVisible()
        setTitle(
            when (consent.status) {
                RequestStatus.DENIED -> R.string.denied_consent
                RequestStatus.GRANTED -> R.string.granted_consent
                RequestStatus.REQUESTED -> if (DateTimeUtils.isDateExpired(consent.permission.dataEraseAt))
                    R.string.expired_consent
                else R.string.new_request
            }
        )
        getNameOf(consent.hiu) {
            renderUi()
        }
    }

    private fun isExpiredOrGrantedOrDenied(): Boolean {
        return (DateTimeUtils.isDateExpired(consent.permission.dataEraseAt) || (consent.status == RequestStatus.DENIED))
    }

    @Subscribe(sticky = true)
    public fun onConsentReceived(consent: Consent) {
        this.consent = consent
    }

    private fun grantConsent() {
        linkedAccounts?.let {
            if (it.isNotEmpty()) {
                if (!eventBusInstance.isRegistered(this))
                    eventBusInstance.register(this)
                showProgressBar(true)
                viewModel.grantConsent(
                    consent.id,
                    viewModel.getConsentArtifact(it, hiTypeObjects, consent.permission))
            }
        }
    }

    private fun validateUser() {
        if (viewModel.preferenceRepository.pinCreated) {
            val intent = Intent(context, PinVerificationActivity::class.java)
            startActivityForResult(intent, 301)
        } else {
            val intent = Intent(context, CreatePinActivity::class.java)
            startActivityForResult(intent, 201)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK){
            if (requestCode == 201) {
                viewModel.preferenceRepository.pinCreated = true
                grantConsent()
            }  else if (requestCode == 301){
                grantConsent()
            }
        }
    }

    private fun getNameOf(hiu: HipHiuIdentifiable, completion: () -> Unit) {
        val hipHiuNameResponse = viewModel.fetchHipHiuNamesOf(listOf(hiu))
        hipHiuNameResponse.observe(this, Observer {
            if(it.status) {
                consent.hiu.name = it.nameMap[consent.hiu.getId()] ?: ""
            }
            completion()
        })
    }
}
