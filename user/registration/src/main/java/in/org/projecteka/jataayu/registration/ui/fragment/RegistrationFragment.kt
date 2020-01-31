package `in`.org.projecteka.jataayu.registration.ui.fragment

import `in`.org.projecteka.jataayu.core.databinding.PatientAccountResultItemBinding
import `in`.org.projecteka.jataayu.core.model.*
import `in`.org.projecteka.jataayu.presentation.adapter.GenericRecyclerViewAdapter
import `in`.org.projecteka.jataayu.presentation.callback.DateTimeSelectionCallback
import `in`.org.projecteka.jataayu.presentation.callback.IDataBindingModel
import `in`.org.projecteka.jataayu.presentation.callback.ItemClickCallback
import `in`.org.projecteka.jataayu.presentation.callback.ProgressDialogCallback
import `in`.org.projecteka.jataayu.presentation.decorator.DividerItemDecorator
import `in`.org.projecteka.jataayu.presentation.ui.fragment.BaseFragment
import `in`.org.projecteka.jataayu.presentation.ui.fragment.DatePickerDialog
import `in`.org.projecteka.jataayu.presentation.ui.fragment.TimePickerDialog
import `in`.org.projecteka.jataayu.registration.R
import `in`.org.projecteka.jataayu.util.extension.setTitle
import `in`.org.projecteka.jataayu.util.extension.showLongToast
import `in`.org.projecteka.jataayu.util.extension.toUtc
import `in`.org.projecteka.jataayu.util.ui.DateTimeUtils
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import androidx.annotation.IdRes
import androidx.core.content.ContextCompat
import androidx.databinding.ViewDataBinding
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.chip.Chip
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.koin.androidx.viewmodel.ext.android.sharedViewModel
import java.util.*
import kotlin.collections.ArrayList

class RegistrationFragment : BaseFragment() {

    private lateinit var binding: Regi
    lateinit var listItems: List<IDataBindingModel>

    private val eventBusInstance: EventBus = EventBus.getDefault()
    lateinit var consent: Consent
    private lateinit var modifiedConsent: Consent
    private var hiTypes = ArrayList<HiType>()
    private val viewModel: ConfirmConsentViewModel by sharedViewModel()

    private val linkedAccountsObserver = Observer<LinkedAccountsResponse> {
        renderLinkedAccounts(it.linkedPatient.links)
    }

    companion object {
        fun newInstance() = RegistrationFragment()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentConsentDetailsEditBinding.inflate(inflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        consent = eventBusInstance.getStickyEvent(Consent::class.java)
        hiTypes = eventBusInstance.getStickyEvent(ArrayList<HiType>()::class.java)
        modifiedConsent = consent.clone()
        binding.consent = modifiedConsent
        binding.clickHandler = this
        binding.pickerClickHandler = this

        renderUi()
    }

    private fun renderUi() {
        for (i in 0 until hiTypes.size) {
            binding.cgRequestInfoTypes.addView(newChip(hiTypes[i].type, hiTypes[i].isChecked))
        }

        binding.cgRequestInfoTypes.setOnCheckedChangeListener { group, checkedId ->
            val chip = chipGroup.findViewById<Chip>(checkedId)
            if (chip != null) {
                showLongToast(chip.text.toString())
            }
        }

        val consent = eventBusInstance.getStickyEvent(Consent::class.java)
        viewModel.linkedAccountsResponse.observe(this, linkedAccountsObserver)
        if (viewModel.linkedAccountsResponse.value == null) {
            showProgressBar(true)
            viewModel.getLinkedAccounts(consent.id, this)
        }
    }

    override fun onItemClick(iDataBindingModel: IDataBindingModel, itemViewBinding: ViewDataBinding) {
        if (itemViewBinding is PatientAccountResultItemBinding) { //Check if it header or item
            val checkbox = itemViewBinding.cbCareContext
            checkbox.toggle()
            if (!checkbox.isChecked) cb_link_all_providers.isChecked = false
        }
    }

    private fun renderLinkedAccounts(linkedAccounts: List<Links?>) {
        listItems = viewModel.getItems(linkedAccounts)

        rvLinkedAccounts.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = GenericRecyclerViewAdapter(this@RegistrationFragment, listItems)
            val dividerItemDecorator = DividerItemDecorator(ContextCompat.getDrawable(context!!, R.color.transparent)!!)
            addItemDecoration(dividerItemDecorator)
        }
    }

    override fun onTimeSelected(timePair: Pair<Int, Int>) {
        val calendar = Calendar.getInstance()
        calendar.time = DateTimeUtils.getDate(modifiedConsent.permission.dataExpiryAt)!!
        calendar.set(Calendar.HOUR_OF_DAY, timePair.first)
        calendar.set(Calendar.MINUTE, timePair.second)
        modifiedConsent.permission.dataExpiryAt = calendar.time.toUtc()
        binding.consent = modifiedConsent
    }

    override fun onDateSelected(@IdRes datePickerId: Int, date: Date) {
        when (datePickerId) {
            R.id.tv_requests_info_from -> modifiedConsent.updateFromDate(date.toUtc())
            R.id.tv_requests_info_to -> modifiedConsent.updateToDate(date.toUtc())
            R.id.tv_expiry_date -> modifiedConsent.permission.dataExpiryAt = date.toUtc()
        }
        binding.consent = modifiedConsent
    }

    override fun onTimePickerClick(view: View) {
        TimePickerDialog(modifiedConsent.getConsentExpiryTime(), this).show(
            fragmentManager!!,
            modifiedConsent.getConsentExpiryTime()
        )
    }

    override fun onDatePickerClick(view: View) {
        when (view.id) {
            R.id.tv_requests_info_from -> {
                val from = DateTimeUtils.getDate(modifiedConsent.permission.dateRange.from)?.time!!
                val datePickerDialog =
                    DatePickerDialog(R.id.tv_requests_info_from, from, DatePickerDialog.UNDEFINED_DATE, System.currentTimeMillis(), this)
                datePickerDialog.show(fragmentManager!!, modifiedConsent.permission.dateRange.from)
            }
            R.id.tv_requests_info_to -> {
                val to = DateTimeUtils.getDate(modifiedConsent.permission.dateRange.to)?.time!!
                val from = DateTimeUtils.getDate(modifiedConsent.permission.dateRange.from)?.time!!
                DatePickerDialog(R.id.tv_requests_info_to, to, from, System.currentTimeMillis(), this).show(
                    fragmentManager!!,
                    modifiedConsent.permission.dateRange.to
                )
            }
            R.id.tv_expiry_date -> {
                DatePickerDialog(
                    R.id.tv_expiry_date, DateTimeUtils.getDate(modifiedConsent.permission.dataExpiryAt)?.time!!,
                    System.currentTimeMillis(), DatePickerDialog.UNDEFINED_DATE, this
                ).show(
                    fragmentManager!!,
                    modifiedConsent.permission.dataExpiryAt
                )
            }
        }
    }

    private fun newChip(description: String, isChecked: Boolean): Chip? {
        val chip = Chip(context)
        chip.text = description
        chip.isChecked = isChecked
        return chip
    }

    @Subscribe
    public fun onConsentReceived(consent: Consent) {
    }

    @Subscribe
    public fun onHiTypesReceived(hiTypes: ArrayList<HiType>) {
    }

    override fun onStart() {
        super.onStart()
        if (!eventBusInstance.isRegistered(this))
            eventBusInstance.register(this)
    }

    override fun onStop() {
        super.onStop()
        eventBusInstance.unregister(this)
    }

    override fun onVisible() {
        super.onVisible()
        setTitle(R.string.edit_request)
    }

    override fun onBackPressedCallback() {
        super.onBackPressedCallback()
        binding.consent = consent
    }

    override fun toggleProvidersSelection(view: View) {
        val checked = (view as CheckBox).isChecked
        listItems.forEach { if (it is CareContext) it.contextChecked = checked }

        rvLinkedAccounts.adapter?.notifyDataSetChanged()
    }

    override fun onSaveClick(view: View) {
        hiTypes.forEach { it.isChecked = (chipGroup.getChildAt(hiTypes.indexOf(it)) as Chip).isChecked }

        eventBusInstance.postSticky(modifiedConsent)
        eventBusInstance.post(hiTypes)
        activity?.onBackPressed()
    }

    override fun onSuccess(any: Any?) {
        showProgressBar(false)
    }

    override fun onFailure(any: Any?) {
        showProgressBar(false)
    }

    private fun showProgressBar(shouldShow: Boolean) {
        binding.progressBarVisibility = shouldShow
    }
}