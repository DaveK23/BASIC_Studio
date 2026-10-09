import com.google.gms.googleservices.GoogleServicesPlugin.MissingGoogleServicesStrategy
import java.io.File
import java.nio.file.Files

plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.kotlin.compose)
  alias(libs.plugins.google.devtools.ksp)
  alias(libs.plugins.secrets)
  alias(libs.plugins.google.services)
}

android {
  namespace = "com.example"
  compileSdk { version = release(36) { minorApiLevel = 1 } }

  defaultConfig {
    applicationId = "com.aistudio.basicstudio.vjzkpq"
    minSdk = 24
    targetSdk = 34
    versionCode = 1
    versionName = "1.0"

    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
  }

  signingConfigs {
    create("release") {
      val keystorePath = System.getenv("KEYSTORE_PATH") ?: "${rootDir}/my-upload-key.jks"
      storeFile = file(keystorePath)
      storePassword = System.getenv("STORE_PASSWORD")
      keyAlias = "upload"
      keyPassword = System.getenv("KEY_PASSWORD")
    }
    create("debugConfig") {
      storeFile = file("${rootDir}/debug.keystore")
      storePassword = "android"
      keyAlias = "androiddebugkey"
      keyPassword = "android"
    }
  }

  buildTypes {
    release {
      isCrunchPngs = false
      isMinifyEnabled = false
      proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
      signingConfig = signingConfigs.getByName("release")
    }
    debug { signingConfig = signingConfigs.getByName("debugConfig") }
  }
  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
  }
  buildFeatures {
    compose = true
    buildConfig = true
  }
  testOptions { unitTests { isIncludeAndroidResources = true } }
  dependenciesInfo {
    includeInApk = false
    includeInBundle = true
  }
  packaging {
    jniLibs {
      useLegacyPackaging = true
    }
  }
}

// Configure the Secrets Gradle Plugin to use .env and .env.example files
// to match the convention used in Web projects.
secrets {
  propertiesFileName = ".env"
  defaultPropertiesFileName = ".env.example"
  ignoreList.add("FIREBASE_APPCHECK_DEBUG_TOKEN")
}

googleServices { missingGoogleServicesStrategy = MissingGoogleServicesStrategy.WARN }

// Some unused dependencies are commented out below instead of being removed.
// This makes it easy to add them back in the future if needed.
dependencies {
  implementation(platform(libs.androidx.compose.bom))
  implementation(platform(libs.firebase.bom))
  // implementation(libs.accompanist.permissions)
  implementation(libs.androidx.activity.compose)
  // implementation(libs.androidx.camera.camera2)
  // implementation(libs.androidx.camera.core)
  // implementation(libs.androidx.camera.lifecycle)
  // implementation(libs.androidx.camera.view)
  implementation(libs.androidx.compose.material.icons.core)
  implementation(libs.androidx.compose.material.icons.extended)
  implementation(libs.androidx.compose.material3)
  implementation(libs.androidx.compose.ui)
  implementation(libs.androidx.compose.ui.graphics)
  implementation(libs.androidx.compose.ui.tooling.preview)
  implementation(libs.androidx.core.ktx)
  // implementation(libs.androidx.datastore.preferences)
  implementation(libs.androidx.lifecycle.runtime.compose)
  implementation(libs.androidx.lifecycle.runtime.ktx)
  implementation(libs.androidx.lifecycle.viewmodel.compose)
  // implementation(libs.androidx.navigation.compose)
  implementation(libs.androidx.room.ktx)
  implementation(libs.androidx.room.runtime)
  // implementation(libs.coil.compose)
  implementation(libs.converter.moshi)
  implementation(libs.firebase.ai)
  // Uncomment to use Firestore:
  // implementation(libs.firebase.firestore)

  // Uncomment ALL FOUR of the following dependencies together to use Firebase Auth and Google
  // Sign-In via Credential Manager:
  // implementation(libs.firebase.auth)
  // implementation(libs.androidx.credentials)
  // implementation(libs.androidx.credentials.play.services)
  // implementation(libs.googleid)
  implementation(libs.firebase.appcheck.recaptcha)
  implementation(libs.firebase.appcheck.debug)
  implementation(libs.kotlinx.coroutines.android)
  implementation(libs.kotlinx.coroutines.core)
  implementation(libs.logging.interceptor)
  implementation(libs.moshi.kotlin)
  implementation(libs.okhttp)
  // implementation(libs.play.services.location)
  implementation(libs.retrofit)
  testImplementation(libs.androidx.compose.ui.test.junit4)
  testImplementation(libs.androidx.core)
  testImplementation(libs.androidx.junit)
  testImplementation(libs.junit)
  testImplementation(libs.kotlinx.coroutines.test)
  testImplementation(libs.robolectric)
  androidTestImplementation(platform(libs.androidx.compose.bom))
  androidTestImplementation(libs.androidx.compose.ui.test.junit4)
  androidTestImplementation(libs.androidx.espresso.core)
  androidTestImplementation(libs.androidx.junit)
  androidTestImplementation(libs.androidx.runner)
  debugImplementation(libs.androidx.compose.ui.test.manifest)
  debugImplementation(libs.androidx.compose.ui.tooling)
  implementation(files("libs/apksigner.jar"))
  "ksp"(libs.androidx.room.compiler)
  "ksp"(libs.moshi.kotlin.codegen)
}

abstract class CopyApkTask : DefaultTask() {
  @get:Internal
  abstract val apkDir: DirectoryProperty

  @get:Internal
  abstract val outputDir: DirectoryProperty

  private fun hasOtherProcessOpenFile(file: File): Boolean {
    val targetCanonicalPath = try {
      file.canonicalPath
    } catch (e: Exception) {
      file.absolutePath
    }

    val procDir = File("/proc")
    if (!procDir.exists() || !procDir.isDirectory) return false

    val currentPid = try {
      ProcessHandle.current().pid()
    } catch (e: Throwable) {
      -1L
    }

    val pids = procDir.listFiles { f -> f.isDirectory && f.name.all { it.isDigit() } } ?: return false
    for (pidDir in pids) {
      val pidStr = pidDir.name
      val pid = pidStr.toLongOrNull() ?: continue
      if (pid == currentPid) continue

      val fdDir = File(pidDir, "fd")
      val fds = fdDir.listFiles() ?: continue
      for (fd in fds) {
        try {
          val linkTarget = Files.readSymbolicLink(fd.toPath()).toFile().canonicalPath
          if (linkTarget == targetCanonicalPath) {
            println("Process $pid has an open handle to $targetCanonicalPath")
            return true
          }
        } catch (e: Exception) {
          // Ignore deleted files, permission errors, or dynamic /proc race conditions
        }
      }
    }
    return false
  }

  @TaskAction
  fun run() {
    val dir = apkDir.get().asFile
    val file = File(dir, "app-debug.apk")
    val destDir = outputDir.get().asFile
    val destFile = File(destDir, file.name)

    // Delay start: Pauses briefly to let file packaging settle
    Thread.sleep(1500)

    // Wait for source file size to stabilize and check /proc for open handles
    var lastSize = -1L
    var stableCount = 0
    for (attempt in 1..100) {
      if (file.exists()) {
        val currentSize = file.length()
        if (currentSize > 0 && currentSize == lastSize) {
          stableCount++
          if (stableCount >= 7) {
            val sourceHasHandles = hasOtherProcessOpenFile(file)
            val destHasHandles = destFile.exists() && hasOtherProcessOpenFile(destFile)
            if (sourceHasHandles || destHasHandles) {
              println("APK size stable at $currentSize, but open file handles found via /proc. Resetting stableCount (attempt $attempt/100)...")
              stableCount = 0
            } else {
              println("APK is fully stabilized at $currentSize bytes with no open file handles.")
              break
            }
          }
        } else {
          stableCount = 0
        }
        lastSize = currentSize
      }
      Thread.sleep(500)
    }

    if (file.exists() && file.length() > 0) {
      file.copyTo(destFile, overwrite = true)
      println("Successfully copied APK to ${destFile.absolutePath} (${destFile.length()} bytes)")
    } else {
      println("Source APK file not found or empty: ${file.absolutePath}")
    }
  }
}

val copyDebugApk = tasks.register<CopyApkTask>("copyDebugApk") {
  apkDir.set(layout.buildDirectory.dir("outputs/apk/debug"))
  outputDir.set(layout.projectDirectory.dir(".."))
}

afterEvaluate {
  tasks.findByName("assembleDebug")?.finalizedBy(copyDebugApk)
}

