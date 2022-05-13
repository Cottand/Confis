package eu.dcotta.confis.plugin.toolwindow

import com.intellij.lang.Language
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.editor.Document
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileTypes.FileType
import com.intellij.openapi.fileTypes.FileTypeManager
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.testFramework.LightVirtualFile
import com.intellij.xdebugger.XExpression
import com.intellij.xdebugger.XSourcePosition
import com.intellij.xdebugger.evaluation.EvaluationMode
import com.intellij.xdebugger.evaluation.EvaluationMode.CODE_FRAGMENT
import com.intellij.xdebugger.evaluation.XDebuggerEditorsProvider
import com.intellij.xdebugger.impl.breakpoints.XExpressionImpl
import com.intellij.xdebugger.impl.ui.XDebuggerExpressionEditor
import org.jetbrains.kotlin.idea.KotlinFileType
import org.jetbrains.kotlin.idea.KotlinLanguage

val kotlinLang by lazy { Language.findLanguageByID(KotlinLanguage.INSTANCE.id) }
val kotlinFileType by lazy { FileTypeManager.getInstance().getStdFileType(KotlinFileType.INSTANCE.name) }

class CircumstanceEditor(
    project: Project,
) : XDebuggerExpressionEditor(
    project,
    DebugProvider(),
    null,
    null,
    XExpressionImpl("", kotlinLang, "", CODE_FRAGMENT),
    true,
    true,
    true,
) {
    val app = ApplicationManager.getApplication()

    override fun setContext(context: PsiElement?) {
        // ensure we are executing async on UI thread
        app.invokeLater {
            super.setContext(context)
        }
    }

    class DebugProvider : XDebuggerEditorsProvider() {
        override fun getFileType(): FileType {
            return kotlinFileType
        }

        override fun createDocument(
            project: Project,
            expression: XExpression,
            sourcePosition: XSourcePosition?,
            mode: EvaluationMode
        ): Document {
            val file = LightVirtualFile(
                "confisDebugProviderLVF",
                expression.language ?: kotlinLang ?: error("Expected Kotlin Language Available"),
                expression.expression,
            )

            return ReadAction.compute<Document, Exception> {
                FileDocumentManager.getInstance().getDocument(file)
                    ?: error("Expected document from temp Kotlin file")
            }
        }

        override fun getSupportedLanguages(
            project: Project,
            sourcePosition: XSourcePosition?
        ): MutableCollection<Language> = mutableListOf(kotlinLang!!)
    }
}
