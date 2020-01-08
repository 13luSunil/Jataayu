package `in`.org.projecteka.jataayu.provider.model

import `in`.org.projecteka.featuresprovider.R
import `in`.org.projecteka.jataayu.core.BR
import `in`.org.projecteka.jataayu.core.model.ProviderIdentifier
import `in`.org.projecteka.jataayu.core.model.ProviderInfo
import org.junit.Assert.assertEquals
import org.junit.Test

class ProviderInfoTest {
    val providerInfo: ProviderInfo by lazy {
        ProviderInfo(
            "Pune",
            ProviderIdentifier("ABC123", "Sahyadri Hospital"),
            "9876543210",
            "Mustispeciality"
        )
    }

    @Test
    fun shouldReturnCorrectLayoutResId() {
        assertEquals(R.layout.provider_search_result_item, providerInfo.layoutResId())
    }

    @Test
    fun shouldReturnCorrectDataBindingVariable() {
        assertEquals(BR.providerInfo, providerInfo.dataBindingVariable())
    }

    @Test
    fun shouldReturnCorrectNameCityPair() {
        assertEquals("Sahyadri Hospital, Pune", providerInfo.nameCityPair())
    }
}