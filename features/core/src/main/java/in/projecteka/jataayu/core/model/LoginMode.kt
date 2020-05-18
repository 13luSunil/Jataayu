package `in`.projecteka.jataayu.core.model
import com.google.gson.annotations.SerializedName



data class LoginType(
    @SerializedName("loginMode")
    val loginMode: LoginMode
)



enum class LoginMode(val loginMode: String) {
    @SerializedName("OTP")
    OTP("OTP"),
    @SerializedName("CREDENTIAL")
    PASSWORD("CREDENTIAL")
}