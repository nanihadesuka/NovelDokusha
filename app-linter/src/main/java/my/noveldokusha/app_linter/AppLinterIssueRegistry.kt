package my.noveldokusha.app_linter

import com.android.tools.lint.client.api.IssueRegistry
import com.android.tools.lint.client.api.Vendor
import com.android.tools.lint.detector.api.CURRENT_API
import com.google.auto.service.AutoService

@Suppress("UnstableApiUsage", "unused")
@AutoService(value = [IssueRegistry::class])
class AppLinterIssueRegistry : IssueRegistry() {
    override val api get() = CURRENT_API
    override val minApi get() = 7

    override val issues get() = listOf(AppLinterDetector.ISSUE)

    // Requires lint API 30.0+; if you're still building for something
    // older, just remove this property.
    override val vendor: Vendor = Vendor(
        vendorName = "App Linter",
        feedbackUrl = "https://com.example.lint.blah.blah",
        contact = "author@com.example.lint"
    )
}