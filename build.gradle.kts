// Top-level build file where you can add configuration options common to all sub-projects/modules.
run {
  val existingHandler = Thread.getDefaultUncaughtExceptionHandler()
  Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
    if (thread.name.startsWith("AWT-EventQueue") &&
        throwable is NullPointerException &&
        throwable.stackTrace.any { frame ->
          val cn = frame.className
          cn.contains("BinaryFileTypeDecompilers") || cn.contains("FileDocumentManager")
        }
    ) {
      // Suppress harmless KSP Analysis API background event posted after KSP teardown
      return@setDefaultUncaughtExceptionHandler
    }
    existingHandler?.uncaughtException(thread, throwable)
  }
}

plugins {
  alias(libs.plugins.android.application) apply false
  alias(libs.plugins.kotlin.compose) apply false
  alias(libs.plugins.google.devtools.ksp) apply false
  alias(libs.plugins.roborazzi) apply false
  alias(libs.plugins.secrets) apply false
  alias(libs.plugins.google.services) apply false
}

