package my.noveldokusha.ui

import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import dagger.hilt.android.AndroidEntryPoint
import my.noveldokusha.AppPreferences
import my.noveldokusha.R
import javax.inject.Inject

@AndroidEntryPoint
open class BaseFragment : Fragment() {
    @Inject
    lateinit var appPreferences: AppPreferences

    @Inject
    lateinit var toasty: Toasty

    fun permissionsCondition(vararg permissions: String): Boolean {
        val allGranted = permissions.all { permission ->
            ContextCompat.checkSelfPermission(
                requireActivity(),
                permission
            ) == PackageManager.PERMISSION_GRANTED
        }

        if (allGranted)
            return true
        ActivityCompat.requestPermissions(requireActivity(), permissions, 1)
        return false
    }

    private var activitiesCallbacksCounter: Int = 0
    private val activitiesCallbacks = mutableMapOf<Int, (resultCode: Int, data: Intent?) -> Unit>()

    fun activityRequest(intent: Intent, reply: (resultCode: Int, data: Intent?) -> Unit) {
        val requestCode = activitiesCallbacksCounter++
        activitiesCallbacks[requestCode] = reply
        startActivityForResult(intent, requestCode)
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        activitiesCallbacks.remove(requestCode)?.let { it(resultCode, data) }
    }

    private var permissionsCallbacksCounter: Int = 0
    private val permissionsCallbacks = mutableMapOf<Int, Pair<() -> Unit, (List<String>) -> Unit>>()

    fun permissionRequest(
        vararg permissions: String,
        denied: (deniedPermissions: List<String>) -> Unit = { toasty.show(R.string.permissions_denied) },
        granted: () -> Unit
    ) {
        val hasPermissions = permissions.all { permission ->
            ContextCompat.checkSelfPermission(
                requireContext(),
                permission
            ) == PackageManager.PERMISSION_GRANTED
        }

        if (hasPermissions) granted()
        else {
            val requestCode = permissionsCallbacksCounter++
            permissionsCallbacks[requestCode] = Pair(granted, denied)
            requestPermissions(permissions, requestCode)
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        permissionsCallbacks.remove(requestCode)?.let {
            when {
                grantResults.isEmpty() -> it.second(listOf())
                grantResults.all { result -> result == PackageManager.PERMISSION_GRANTED } -> it.first()
                else -> it.second(permissions.filterIndexed { index, _ -> grantResults[index] == PackageManager.PERMISSION_DENIED })
            }
        }
    }
}