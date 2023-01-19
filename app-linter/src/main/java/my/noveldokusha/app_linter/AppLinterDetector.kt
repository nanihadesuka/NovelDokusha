package my.noveldokusha.app_linter

import com.android.tools.lint.client.api.UElementHandler
import com.android.tools.lint.detector.api.*
import org.jetbrains.kotlin.psi.psiUtil.isExtensionDeclaration
import org.jetbrains.kotlin.psi.psiUtil.isTopLevelKtOrJavaMember
import org.jetbrains.uast.*
import java.util.*

@Suppress("UnstableApiUsage")
class AppLinterDetector : Detector(), SourceCodeScanner {
    override fun getApplicableUastTypes(): List<Class<out UElement>> {
        return listOf(UMethod::class.java)
    }

    override fun getApplicableFiles(): EnumSet<Scope> {
        return EnumSet.of(Scope.ALL_JAVA_FILES)
    }

    val packageScope = "my.noveldokusha.mapper".split(".")

    override fun createUastHandler(context: JavaContext) = object : UElementHandler() {

        val isPackageOfInterest: Boolean =
            packageScope == context.uastFile?.packageName?.split(".")?.take(packageScope.size)

        override fun visitMethod(node: UMethod) {
            if (!isPackageOfInterest) return
            val psi = node.sourcePsi ?: return

            val isTopLevelFunction by lazy { psi.isTopLevelKtOrJavaMember() }
            val isExtensionFunction by lazy { psi.isExtensionDeclaration() }
            val isPublic by lazy { context.evaluator.isPublic(node) }
            val startsWithMapTo by lazy { node.name.startsWith("mapTo") }

            val valid = !isTopLevelFunction ||
                    !isExtensionFunction ||
                    startsWithMapTo ||
                    !isPublic

            if (valid)
                return

            context.report(
                ISSUE,
                node,
                context.getLocation(node),
                """
                Mapper extension function *${node.name}* should start with prefix *mapTo*
            """.trimIndent()
            )
        }
    }

    companion object {
        @JvmField
        val ISSUE: Issue = Issue.create(
            id = "AppSampleId",
            briefDescription = "Lint Mentions",
            explanation = """
                    This check highlights string literals in code which mentions the word `lint`. \
                    Blah blah blah.

                    Another paragraph here.
                    """,
            category = Category.CORRECTNESS,
            priority = 7,
            severity = Severity.ERROR,
            implementation = Implementation(
                AppLinterDetector::class.java,
                Scope.JAVA_FILE_SCOPE
            )
        )
    }
}