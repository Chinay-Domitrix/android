/*
 * Copyright (C) 2022 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.android.tools.idea.gradle.project.sync

import com.android.builder.model.v2.ModelSyncFile
import com.android.builder.model.v2.dsl.BuildType
import com.android.builder.model.v2.dsl.ClassField
import com.android.builder.model.v2.dsl.DependenciesInfo
import com.android.builder.model.v2.dsl.ProductFlavor
import com.android.builder.model.v2.dsl.SigningConfig
import com.android.builder.model.v2.ide.AaptOptions
import com.android.builder.model.v2.ide.AndroidArtifact
import com.android.builder.model.v2.ide.AndroidGradlePluginProjectFlags
import com.android.builder.model.v2.ide.ApiVersion
import com.android.builder.model.v2.ide.ArtifactDependencies
import com.android.builder.model.v2.ide.BasicArtifact
import com.android.builder.model.v2.ide.BasicVariant
import com.android.builder.model.v2.ide.Edge
import com.android.builder.model.v2.ide.GraphItem
import com.android.builder.model.v2.ide.JavaArtifact
import com.android.builder.model.v2.ide.JavaCompileOptions
import com.android.builder.model.v2.ide.Library
import com.android.builder.model.v2.ide.LibraryType
import com.android.builder.model.v2.ide.LintOptions
import com.android.builder.model.v2.ide.ProjectInfo
import com.android.builder.model.v2.ide.ProjectType
import com.android.builder.model.v2.ide.SourceProvider
import com.android.builder.model.v2.ide.SourceSetContainer
import com.android.builder.model.v2.ide.TestInfo
import com.android.builder.model.v2.ide.TestedTargetVariant
import com.android.builder.model.v2.ide.UnresolvedDependency
import com.android.builder.model.v2.ide.Variant
import com.android.builder.model.v2.ide.VectorDrawablesOptions
import com.android.builder.model.v2.ide.ViewBindingOptions
import com.android.builder.model.v2.models.AndroidDsl
import com.android.builder.model.v2.models.AndroidProject
import com.android.builder.model.v2.models.BasicAndroidProject
import com.android.builder.model.v2.models.Versions
import com.android.builder.model.v2.models.ndk.NativeAbi
import com.android.builder.model.v2.models.ndk.NativeBuildSystem
import com.android.builder.model.v2.models.ndk.NativeModule
import com.android.builder.model.v2.models.ndk.NativeVariant
import com.android.ide.common.gradle.Component
import com.android.ide.common.gradle.Version
import com.android.ide.common.repository.AgpVersion
import com.android.ide.gradle.model.GradlePropertiesModel
import com.android.ide.gradle.model.LegacyAndroidGradlePluginProperties
import com.android.tools.idea.gradle.model.ClasspathType
import com.android.tools.idea.gradle.model.CodeShrinker
import com.android.tools.idea.gradle.model.IdeAaptOptions
import com.android.tools.idea.gradle.model.IdeAndroidProjectType
import com.android.tools.idea.gradle.model.IdeArtifactName
import com.android.tools.idea.gradle.model.IdeDependencies
import com.android.tools.idea.gradle.model.IdeLibrary
import com.android.tools.idea.gradle.model.IdeLintOptions.Companion.SEVERITY_DEFAULT_ENABLED
import com.android.tools.idea.gradle.model.IdeLintOptions.Companion.SEVERITY_ERROR
import com.android.tools.idea.gradle.model.IdeLintOptions.Companion.SEVERITY_FATAL
import com.android.tools.idea.gradle.model.IdeLintOptions.Companion.SEVERITY_IGNORE
import com.android.tools.idea.gradle.model.IdeLintOptions.Companion.SEVERITY_INFORMATIONAL
import com.android.tools.idea.gradle.model.IdeLintOptions.Companion.SEVERITY_WARNING
import com.android.tools.idea.gradle.model.IdeModelSyncFile
import com.android.tools.idea.gradle.model.IdeModuleWellKnownSourceSet
import com.android.tools.idea.gradle.model.IdeModuleWellKnownSourceSet.ANDROID_TEST
import com.android.tools.idea.gradle.model.IdeModuleWellKnownSourceSet.MAIN
import com.android.tools.idea.gradle.model.IdeModuleWellKnownSourceSet.TEST_FIXTURES
import com.android.tools.idea.gradle.model.IdeModuleWellKnownSourceSet.UNIT_TEST
import com.android.tools.idea.gradle.model.IdeSyncIssue
import com.android.tools.idea.gradle.model.IdeTestOptions
import com.android.tools.idea.gradle.model.IdeUnresolvedLibrary
import com.android.tools.idea.gradle.model.LibraryReference
import com.android.tools.idea.gradle.model.impl.IdeAaptOptionsImpl
import com.android.tools.idea.gradle.model.impl.IdeAndroidArtifactCoreImpl
import com.android.tools.idea.gradle.model.impl.IdeAndroidGradlePluginProjectFlagsImpl
import com.android.tools.idea.gradle.model.impl.IdeAndroidLibraryImpl
import com.android.tools.idea.gradle.model.impl.IdeAndroidProjectImpl
import com.android.tools.idea.gradle.model.impl.IdeApiVersionImpl
import com.android.tools.idea.gradle.model.impl.IdeBasicVariantImpl
import com.android.tools.idea.gradle.model.impl.IdeBuildTasksAndOutputInformationImpl
import com.android.tools.idea.gradle.model.impl.IdeBuildTypeContainerImpl
import com.android.tools.idea.gradle.model.impl.IdeBuildTypeImpl
import com.android.tools.idea.gradle.model.impl.IdeClassFieldImpl
import com.android.tools.idea.gradle.model.impl.IdeCustomSourceDirectoryImpl
import com.android.tools.idea.gradle.model.impl.IdeDependenciesCoreDirect
import com.android.tools.idea.gradle.model.impl.IdeDependenciesCoreImpl
import com.android.tools.idea.gradle.model.impl.IdeDependenciesCoreRef
import com.android.tools.idea.gradle.model.impl.IdeDependenciesInfoImpl
import com.android.tools.idea.gradle.model.impl.IdeDependencyCoreImpl
import com.android.tools.idea.gradle.model.impl.IdeExtraSourceProviderImpl
import com.android.tools.idea.gradle.model.impl.IdeJavaArtifactCoreImpl
import com.android.tools.idea.gradle.model.impl.IdeJavaCompileOptionsImpl
import com.android.tools.idea.gradle.model.impl.IdeJavaLibraryImpl
import com.android.tools.idea.gradle.model.impl.IdeLintOptionsImpl
import com.android.tools.idea.gradle.model.impl.IdeModelSyncFileImpl
import com.android.tools.idea.gradle.model.impl.IdeMultiVariantDataImpl
import com.android.tools.idea.gradle.model.impl.IdePreResolvedModuleLibraryImpl
import com.android.tools.idea.gradle.model.impl.IdePrivacySandboxSdkInfoImpl
import com.android.tools.idea.gradle.model.impl.IdeProductFlavorContainerImpl
import com.android.tools.idea.gradle.model.impl.IdeProductFlavorImpl
import com.android.tools.idea.gradle.model.impl.IdeProjectPathImpl
import com.android.tools.idea.gradle.model.impl.IdeSigningConfigImpl
import com.android.tools.idea.gradle.model.impl.IdeSourceProviderContainerImpl
import com.android.tools.idea.gradle.model.impl.IdeSourceProviderImpl
import com.android.tools.idea.gradle.model.impl.IdeSyncIssueImpl
import com.android.tools.idea.gradle.model.impl.IdeTestOptionsImpl
import com.android.tools.idea.gradle.model.impl.IdeTestedTargetVariantImpl
import com.android.tools.idea.gradle.model.impl.IdeUnknownLibraryImpl
import com.android.tools.idea.gradle.model.impl.IdeUnresolvedDependencyImpl
import com.android.tools.idea.gradle.model.impl.IdeUnresolvedKmpAndroidModuleLibraryImpl
import com.android.tools.idea.gradle.model.impl.IdeUnresolvedLibraryTableImpl
import com.android.tools.idea.gradle.model.impl.IdeUnresolvedModuleLibraryImpl
import com.android.tools.idea.gradle.model.impl.IdeVariantBuildInformationImpl
import com.android.tools.idea.gradle.model.impl.IdeVariantCoreImpl
import com.android.tools.idea.gradle.model.impl.IdeVectorDrawablesOptionsImpl
import com.android.tools.idea.gradle.model.impl.IdeViewBindingOptionsImpl
import com.android.tools.idea.gradle.model.impl.ndk.v2.IdeNativeAbiImpl
import com.android.tools.idea.gradle.model.impl.ndk.v2.IdeNativeModuleImpl
import com.android.tools.idea.gradle.model.impl.ndk.v2.IdeNativeVariantImpl
import com.android.tools.idea.gradle.model.impl.throwingIdeDependencies
import com.google.common.collect.ImmutableList
import com.google.common.collect.ImmutableMap
import com.google.common.collect.ImmutableSet
import com.google.common.collect.Lists
import org.gradle.api.attributes.java.TargetJvmEnvironment
import java.io.File

// NOTE: The implementation is structured as a collection of nested functions to ensure no recursive dependencies are possible between
//       models unless explicitly handled by nesting. The same structure expressed as classes allows recursive data structures and thus we
//       cannot validate the structure at compile time.
internal fun modelCacheV2Impl(
  internedModels: InternedModels,
  agpVersion: AgpVersion,
  syncTestMode: SyncTestMode,
  multiVariantAdditionalArtifactSupport: Boolean,
): ModelCache.V2 {
  fun String.deduplicate() = internedModels.intern(this)
  fun List<String>.deduplicateStrings(): List<String> = this.map { it.deduplicate() }
  fun Map<String, String>.deduplicateStrings(): Map<String, String> = map { (k, v) -> k.deduplicate() to v.deduplicate() }.toMap()
  fun Set<String>.deduplicateStrings(): Set<String> = this.map { it.deduplicate() }.toSet()
  fun Collection<String>.deduplicateStrings(): Collection<String> = this.map { it.deduplicate() }

  fun File.deduplicateFile(): File = File(path.deduplicate())
  fun List<File>.deduplicateFiles() = map { it.deduplicateFile() }
  fun Collection<File>.deduplicateFiles() = map { it.deduplicateFile() }

  /**
   * Sources, JavaDocs and Samples are only provided in libraries after AGP version 8.1.0-alpha8.
   * Any attempt to read these values from a version prior to this will result in an exception.
   */
  val isAGPVersion8dot1dot0alpha8orLater = agpVersion.isAtLeast(8, 1, 0, "alpha", 8, false)
  val useAdditionalArtifactsFromLibraries = isAGPVersion8dot1dot0alpha8orLater && multiVariantAdditionalArtifactSupport
  /** If AGP has absolute Gradle build path used in [ProjectInfo.buildId], or it uses the Gradle build name that we need to patch. */
  val supportsAbsoluteGradleBuildPaths = agpVersion.isAtLeast(8, 2, 0, "alpha", 13, false)

  fun sourceProviderFrom(provider: SourceProvider): IdeSourceProviderImpl {
    val folder: File? = provider.manifestFile.parentFile?.deduplicateFile()
    fun File.makeRelativeAndDeduplicate(): String = (if (folder != null) relativeToOrSelf(folder) else this).path.deduplicate()
    fun Collection<File>.makeRelativeAndDeduplicate(): Collection<String> = map { it.makeRelativeAndDeduplicate() }
    return IdeSourceProviderImpl(
      myName = provider.name.deduplicate(),
      myFolder = folder,
      myManifestFile = provider.manifestFile.makeRelativeAndDeduplicate(),
      myJavaDirectories = provider.javaDirectories.makeRelativeAndDeduplicate(),
      myKotlinDirectories = provider.kotlinDirectories.makeRelativeAndDeduplicate(),
      myResourcesDirectories = provider.resourcesDirectories.makeRelativeAndDeduplicate(),
      myAidlDirectories = provider.aidlDirectories?.makeRelativeAndDeduplicate() ?: mutableListOf(),
      myRenderscriptDirectories = provider.renderscriptDirectories?.makeRelativeAndDeduplicate() ?: mutableListOf(),
      myResDirectories = provider.resDirectories?.makeRelativeAndDeduplicate() ?: mutableListOf(),
      myAssetsDirectories = provider.assetsDirectories?.makeRelativeAndDeduplicate() ?: mutableListOf(),
      myJniLibsDirectories = provider.jniLibsDirectories.makeRelativeAndDeduplicate(),
      myShadersDirectories = provider.shadersDirectories?.makeRelativeAndDeduplicate() ?: mutableListOf(),
      myMlModelsDirectories = provider.mlModelsDirectories?.makeRelativeAndDeduplicate() ?: mutableListOf(),
      myCustomSourceDirectories = provider.customDirectories?.map {
        IdeCustomSourceDirectoryImpl(it.sourceTypeName, folder, it.directory.makeRelativeAndDeduplicate())
      } ?: emptyList(),
      myBaselineProfileDirectories = if (agpVersion.isAtLeast(8, 0, 0, "beta", 1, false))
        provider.baselineProfileDirectories?.makeRelativeAndDeduplicate() ?: mutableListOf()
      else
        mutableListOf()
    )
  }

  fun classFieldFrom(classField: ClassField): IdeClassFieldImpl {
    return IdeClassFieldImpl(
      name = classField.name.deduplicate(),
      type = classField.type.deduplicate(),
      value = classField.value.deduplicate(),
    )
  }

  fun vectorDrawablesOptionsFrom(options: VectorDrawablesOptions): IdeVectorDrawablesOptionsImpl {
    return IdeVectorDrawablesOptionsImpl(useSupportLibrary = options.useSupportLibrary)
  }

  fun apiVersionFrom(version: ApiVersion): IdeApiVersionImpl {
    val codename = version.codename?.deduplicate()
    val apiString = codename ?: version.apiLevel.toString().deduplicate()
    return IdeApiVersionImpl(
      apiLevel = version.apiLevel,
      codename = codename,
      apiString = apiString
    )
  }

  fun signingConfigFrom(config: SigningConfig): IdeSigningConfigImpl {
    return IdeSigningConfigImpl(
      name = config.name.deduplicate(),
      storeFile = config.storeFile?.deduplicateFile(),
      storePassword = config.storePassword?.deduplicate(),
      keyAlias = config.keyAlias?.deduplicate()
    )
  }

  fun productFlavorFrom(flavor: ProductFlavor): IdeProductFlavorImpl {
    return IdeProductFlavorImpl(
      name = flavor.name.deduplicate(),
      resValues = flavor.resValues?.mapValues { classFieldFrom(it.value) } ?: mapOf(),
      proguardFiles = ImmutableList.copyOf(flavor.proguardFiles.deduplicateFiles()),
      consumerProguardFiles = ImmutableList.copyOf(flavor.consumerProguardFiles.deduplicateFiles()),
      // AGP may return internal Groovy GString implementation as a value in
      // manifestPlaceholders
      // map. It cannot be serialized
      // with IDEA's external system serialization. We convert values to String to
      // make them
      // usable as they are converted to String by
      // the manifest merger anyway.
      manifestPlaceholders = ImmutableMap.copyOf(flavor.manifestPlaceholders.entries.associate { it.key to it.value.toString() }),
      applicationIdSuffix = flavor.applicationIdSuffix?.deduplicate(),
      versionNameSuffix = flavor.versionNameSuffix?.deduplicate(),
      multiDexEnabled = flavor.multiDexEnabled,
      testInstrumentationRunnerArguments = ImmutableMap.copyOf(flavor.testInstrumentationRunnerArguments.deduplicateStrings()),
      resourceConfigurations = ImmutableList.copyOf(flavor.resourceConfigurations.deduplicateStrings()),
      vectorDrawables = vectorDrawablesOptionsFrom(flavor.vectorDrawables),
      dimension = flavor.dimension?.deduplicate(),
      applicationId = flavor.applicationId?.deduplicate(),
      versionCode = flavor.versionCode,
      versionName = flavor.versionName?.deduplicate(),
      minSdkVersion = flavor.minSdkVersion?.let { it: ApiVersion -> apiVersionFrom(it) },
      targetSdkVersion = flavor.targetSdkVersion?.let { it: ApiVersion -> apiVersionFrom(it) },
      maxSdkVersion = flavor.maxSdkVersion,
      testApplicationId = flavor.testApplicationId?.deduplicate(),
      testInstrumentationRunner = flavor.testInstrumentationRunner?.deduplicate(),
      testFunctionalTest = flavor.testFunctionalTest,
      testHandleProfiling = flavor.testHandleProfiling
    )
  }

  fun mergeProductFlavorsFrom(
    defaultConfig: IdeProductFlavorImpl,
    productFlavors: List<IdeProductFlavorImpl>,
    projectType: IdeAndroidProjectType
  ): IdeProductFlavorImpl {

    var applicationId = defaultConfig.applicationId
    var applicationIdSuffix = defaultConfig.applicationIdSuffix
    val consumerProguardFiles = mutableListOf<File>()
    consumerProguardFiles.addAll(defaultConfig.consumerProguardFiles)
    var versionNameSuffix = defaultConfig.versionNameSuffix
    var versionCode = defaultConfig.versionCode
    var versionName = defaultConfig.versionName
    var testApplicationId = defaultConfig.testApplicationId
    var testInstrumentationRunner = defaultConfig.testInstrumentationRunner
    val testInstrumentationRunnerArguments = mutableMapOf<String, String>()
    testInstrumentationRunnerArguments.putAll(defaultConfig.testInstrumentationRunnerArguments)
    var testHandleProfiling = defaultConfig.testHandleProfiling
    var testFunctionalTest = defaultConfig.testFunctionalTest
    val resourceConfigurations = mutableListOf<String>()
    resourceConfigurations.addAll(defaultConfig.resourceConfigurations)
    val manifestPlaceholder = mutableMapOf<String, String>()
    manifestPlaceholder.putAll(defaultConfig.manifestPlaceholders)
    val proguardFiles = mutableListOf<File>()
    proguardFiles.addAll(defaultConfig.proguardFiles)
    val resValues = mutableMapOf<String, IdeClassFieldImpl>()
    resValues.putAll(defaultConfig.resValues)
    var multiDexEnabled = defaultConfig.multiDexEnabled
    var vectorDrawables = defaultConfig.vectorDrawables

    for (flavor in Lists.reverse(productFlavors)) {
      versionCode = flavor.versionCode ?: versionCode
      versionName = flavor.versionName ?: versionName
      testApplicationId = flavor.testApplicationId ?: testApplicationId
      testInstrumentationRunner = flavor.testInstrumentationRunner ?: testInstrumentationRunner
      testInstrumentationRunnerArguments.putAll(flavor.testInstrumentationRunnerArguments)
      testHandleProfiling = flavor.testHandleProfiling ?: testHandleProfiling
      testFunctionalTest = flavor.testFunctionalTest ?: testFunctionalTest
      resourceConfigurations.addAll(flavor.resourceConfigurations)
      manifestPlaceholder.putAll(flavor.manifestPlaceholders)
      resValues.putAll(flavor.resValues)
      multiDexEnabled = flavor.multiDexEnabled ?: multiDexEnabled
      if (flavor.vectorDrawables?.useSupportLibrary != null) {
        vectorDrawables = IdeVectorDrawablesOptionsImpl(flavor.vectorDrawables!!.useSupportLibrary)
      }
    }
    for (flavor in productFlavors) {
      if (flavor.proguardFiles.isNotEmpty() && flavor.consumerProguardFiles.isNotEmpty()) {
        proguardFiles.addAll(flavor.proguardFiles)
        consumerProguardFiles.addAll(flavor.consumerProguardFiles)
      }
      applicationId = flavor.applicationId ?: applicationId
      applicationIdSuffix = applicationIdSuffix?.plus(if (flavor.applicationIdSuffix != null) ".${flavor.applicationIdSuffix}" else "")
      versionNameSuffix = versionNameSuffix.orEmpty() + flavor.versionNameSuffix.orEmpty()
    }
    return IdeProductFlavorImpl(
      name = "",
      resValues = resValues,
      proguardFiles = proguardFiles,
      consumerProguardFiles = consumerProguardFiles,
      manifestPlaceholders = manifestPlaceholder.entries.associate { it.key to it.value },
      applicationIdSuffix = applicationIdSuffix,
      versionNameSuffix = versionNameSuffix,
      multiDexEnabled = multiDexEnabled,
      testInstrumentationRunnerArguments = ImmutableMap.copyOf(testInstrumentationRunnerArguments),
      resourceConfigurations = ImmutableList.copyOf(resourceConfigurations),
      vectorDrawables = vectorDrawables,
      dimension = "",
      applicationId =
      if (projectType == IdeAndroidProjectType.PROJECT_TYPE_TEST) {
        testApplicationId
      } else {
        applicationId?.plus(if (applicationIdSuffix != null) ".${applicationIdSuffix}" else "")
      },
      versionCode = versionCode,
      versionName = versionName,
      minSdkVersion = null,
      targetSdkVersion = null,
      maxSdkVersion = null,
      testApplicationId = testApplicationId,
      testInstrumentationRunner = testInstrumentationRunner,
      testFunctionalTest = testFunctionalTest,
      testHandleProfiling = testHandleProfiling
    )
  }

  fun sourceProviderContainerFrom(container: SourceProvider): IdeExtraSourceProviderImpl {
    return IdeExtraSourceProviderImpl(
      // As we no longer have ArtifactMetaData, we use hardcoded values for androidTests, unitTests and testFixtures artifacts.

      artifactName = if (container.name.startsWith("androidTest")) {
        "_android_test_"
      } else if (container.name.startsWith("testFixtures")) {
        "_test_fixtures_"
      } else "_unit_test_",
      sourceProvider = sourceProviderFrom(container)
    )
  }

  fun productFlavorContainerFrom(
    productFlavor: ProductFlavor,
    container: SourceSetContainer?
  ): IdeProductFlavorContainerImpl {
    return IdeProductFlavorContainerImpl(
      productFlavor = productFlavorFrom(productFlavor),
      sourceProvider = container?.sourceProvider?.let { it: SourceProvider -> sourceProviderFrom(it) },
      extraSourceProviders = listOfNotNull(
        container?.androidTestSourceProvider?.let { it: SourceProvider -> sourceProviderContainerFrom(it) },
        container?.unitTestSourceProvider?.let { it: SourceProvider -> sourceProviderContainerFrom(it) },
        container?.testFixturesSourceProvider?.let { it: SourceProvider -> sourceProviderContainerFrom(it) }
      )
    )
  }

  fun sourceProviderContainerFrom(
    container: SourceSetContainer?
  ): IdeSourceProviderContainerImpl {
    return IdeSourceProviderContainerImpl(
      sourceProvider = container?.sourceProvider?.let { it: SourceProvider -> sourceProviderFrom(it) },
      extraSourceProviders = listOfNotNull(
        container?.androidTestSourceProvider?.let { it: SourceProvider -> sourceProviderContainerFrom(it) },
        container?.unitTestSourceProvider?.let { it: SourceProvider -> sourceProviderContainerFrom(it) },
        container?.testFixturesSourceProvider?.let { it: SourceProvider -> sourceProviderContainerFrom(it) }
      )
    )
  }

  fun buildTypeFrom(buildType: BuildType): IdeBuildTypeImpl {
    return IdeBuildTypeImpl(
      name = buildType.name.deduplicate(),
      resValues = buildType.resValues?.mapValues { classFieldFrom(it.value) } ?: mapOf(),
      proguardFiles = ImmutableList.copyOf(buildType.proguardFiles.deduplicateFiles()),
      consumerProguardFiles = ImmutableList.copyOf(buildType.consumerProguardFiles.deduplicateFiles()),
      // AGP may return internal Groovy GString implementation as a value in
      // manifestPlaceholders
      // map. It cannot be serialized
      // with IDEA's external system serialization. We convert values to String to
      // make them
      // usable as they are converted to String by
      // the manifest merger anyway.
      manifestPlaceholders = ImmutableMap.copyOf(buildType.manifestPlaceholders.entries.associate { it.key to it.value.toString() }
                                                   .deduplicateStrings()),
      applicationIdSuffix = buildType.applicationIdSuffix?.deduplicate(),
      versionNameSuffix = buildType.versionNameSuffix?.deduplicate(),
      multiDexEnabled = buildType.multiDexEnabled,
      isDebuggable = buildType.isDebuggable,
      isJniDebuggable = buildType.isJniDebuggable,
      isRenderscriptDebuggable = buildType.isRenderscriptDebuggable,
      renderscriptOptimLevel = buildType.renderscriptOptimLevel,
      isMinifyEnabled = buildType.isMinifyEnabled,
      isZipAlignEnabled = buildType.isZipAlignEnabled
    )
  }

  fun buildTypeContainerFrom(buildType: BuildType, container: SourceSetContainer?): IdeBuildTypeContainerImpl {
    return IdeBuildTypeContainerImpl(
      buildType = buildTypeFrom(buildType),
      sourceProvider = container?.sourceProvider?.let { sourceProviderFrom(it) },
      extraSourceProviders = listOfNotNull(
        container?.androidTestSourceProvider?.let { sourceProviderContainerFrom(it) },
        container?.unitTestSourceProvider?.let { sourceProviderContainerFrom(it) },
        container?.testFixturesSourceProvider?.let { sourceProviderContainerFrom(it) }
      )
    )
  }

  fun Library.getComponent() = libraryInfo?.let {
    when (it.group) {
      "__local_aars__", "__wrapped_aars__", "__local_asars__", "artifacts" -> null
      else -> Component(it.group, it.name, Version.parse(it.version))
    }
  }

  /**
   * @param androidLibrary Instance returned by android plugin.
   * path to build directory for all modules.
   * @return Instance of [IdeLibrary] based on dependency type.
   */
  fun androidLibraryFrom(androidLibrary: Library): LibraryReference {
    val libraryInfo = androidLibrary.libraryInfo ?: error("libraryInfo missing for ${androidLibrary.key}")

    val androidLibraryData = androidLibrary.androidLibraryData ?: error("androidLibraryData missing for ${androidLibrary.key}")

    val artifactAddress = "${libraryInfo.group}:${libraryInfo.name}:${libraryInfo.version}@aar"
    val core = IdeAndroidLibraryImpl.create(
      artifactAddress = artifactAddress,
      component = androidLibrary.getComponent(),
      name = "",
      folder = androidLibraryData.resFolder.parentFile.deduplicateFile(),
      artifact = androidLibrary.artifact ?: File(""),
      lintJar = androidLibrary.lintJar?.path,
      srcJar = if (useAdditionalArtifactsFromLibraries) androidLibrary.srcJar?.path else null,
      docJar = if (useAdditionalArtifactsFromLibraries) androidLibrary.docJar?.path else null,
      samplesJar = if (useAdditionalArtifactsFromLibraries) androidLibrary.samplesJar?.path else null,
      manifest = androidLibraryData.manifest.path ?: "",
      compileJarFiles = androidLibraryData.compileJarFiles.map { it.path },
      runtimeJarFiles = androidLibraryData.runtimeJarFiles.map { it.path },
      resFolder = androidLibraryData.resFolder.path ?: "",
      resStaticLibrary = androidLibraryData.resStaticLibrary,
      assetsFolder = androidLibraryData.assetsFolder.path ?: "",
      jniFolder = androidLibraryData.jniFolder.path ?: "",
      aidlFolder = androidLibraryData.aidlFolder.path ?: "",
      renderscriptFolder = androidLibraryData.renderscriptFolder.path ?: "",
      proguardRules = androidLibraryData.proguardRules.path ?: "",
      externalAnnotations = androidLibraryData.externalAnnotations.path ?: "",
      publicResources = androidLibraryData.publicResources.path ?: "",
      symbolFile = androidLibraryData.symbolFile.path,
      deduplicate = { internedModels.intern(this) }
    )
    return internedModels.getOrCreate(core)
  }

  /**
   * @param javaLibrary Instance of type [LibraryType.JAVA_LIBRARY] returned by android plugin.
   * @return Instance of [Library] based on dependency type.
   */
  fun javaLibraryFrom(javaLibrary: Library): LibraryReference {
    val libraryInfo = javaLibrary.libraryInfo ?: error("libraryInfo missing for ${javaLibrary.key}")
    val artifactAddress = "${libraryInfo.group}:${libraryInfo.name}:${libraryInfo.version}@jar"
    val unnamed = IdeJavaLibraryImpl(
      artifactAddress = artifactAddress,
      component = javaLibrary.getComponent(),
      name = "",
      artifact = javaLibrary.artifact!!,
      srcJar = if (useAdditionalArtifactsFromLibraries) javaLibrary.srcJar else null,
      docJar = if (useAdditionalArtifactsFromLibraries) javaLibrary.docJar else null,
      samplesJar = if (useAdditionalArtifactsFromLibraries) javaLibrary.samplesJar else null,
    )
    return internedModels.getOrCreate(unnamed)
  }

  fun javaLibraryFromJarFile(jarFile: File): LibraryReference {
    return internedModels.getOrCreate(IdeJavaLibraryImpl("${ModelCache.LOCAL_JARS}:" + jarFile.path + ":unspecified", null, "", jarFile, null, null, null))
  }

  fun moduleLibraryFrom(
    projectPath: String,
    buildId: BuildId,
    lintJar: File?,
    artifact: ArtifactRef
  ): LibraryReference {

    fun kmpAndroidUnresolved(): LibraryReference {
      return internedModels.getOrCreate(
        IdeUnresolvedKmpAndroidModuleLibraryImpl(
          buildId = buildId.asString,
          projectPath = projectPath,
          lintJar = lintJar?.path?.let(::File),
        )
      )
    }

    fun resolved(artifact: AndroidArtifactRef): LibraryReference {
      return internedModels.getOrCreate(
        IdePreResolvedModuleLibraryImpl(
          buildId = buildId.asString,
          projectPath = projectPath,
          variant = artifact.variantName,
          lintJar = lintJar?.path?.let(::File),
          sourceSet = if (artifact.isTestFixture) TEST_FIXTURES else MAIN
        )
      )
    }

    fun unresolved(artifact: File): LibraryReference {
      return internedModels.getOrCreate(
        IdeUnresolvedModuleLibraryImpl(
          buildId = buildId.asString,
          projectPath = projectPath,
          variant = null,
          lintJar = lintJar?.path?.let(::File),
          artifact = artifact
        )
      )
    }

    return when (artifact) {
      is AndroidArtifactRef -> resolved(artifact)
      is KmpAndroidArtifactRef -> kmpAndroidUnresolved()
      is NonAndroidAndroidArtifactRef -> unresolved(artifact.artifactFile)
    }
  }

  fun createFromDependencies(
    classpathId: ClasspathIdentifier,
    dependencies: DependencyGraphCompat?,
    libraries: Map<String, Library>,
    bootClasspath: Collection<String>,
    androidProjectPathResolver: AndroidProjectPathResolver,
    buildPathMap: Map<String, BuildId>,
  ): ModelResult<IdeModelWithPostProcessor<IdeDependenciesCoreImpl>> = ModelResult.create {
    // Map from unique artifact address to level2 library instance. The library instances are
    // supposed to be shared by all artifacts. When creating IdeLevel2Dependencies, check if current library is available in this map,
    // if it's available, don't create new one, simple add reference to it. If it's not available, create new instance and save
    // to this map, so it can be reused the next time when the same library is added.
    val librariesById = mutableMapOf<String, LibraryReference>()
    fun buildPathToBuildId(projectInfo: ProjectInfo): BuildId {
      val buildTreePath = projectInfo.buildTreePath
      val buildId = if (supportsAbsoluteGradleBuildPaths) {
        buildPathMap[projectInfo.buildTreePath]
      } else {
        // If there is no full support for Gradle build paths, we are looking only to match the last segment.
        // E.g. ":nested" matches ":includedBuild:nested".
        buildPathMap.entries.firstOrNull { it.key.endsWith(buildTreePath) }?.value
      }
      return buildId ?: error("Unknown build name: '${projectInfo.displayName}'. Known names ${buildPathMap}")
    }

    data class LibraryWithDependencies(val library: Library, val dependencies: List<String>)

    fun createModuleDependency(
      seenDependencies: MutableMap<String, List<String>>,
      projectInfo: ProjectInfo,
      artifactAddress: String,
      lintJar: File?,
      artifact: ArtifactRef,
      dependencies: List<String>
    ) {
      if (!seenDependencies.contains(artifactAddress)) {
        seenDependencies[artifactAddress] = dependencies
        librariesById.computeIfAbsent(artifactAddress) {
          val buildId = buildPathToBuildId(projectInfo)
          moduleLibraryFrom(
            projectPath = projectInfo.projectPath,
            buildId = buildId,
            lintJar = lintJar,
            artifact = artifact
          )
        }
      }
    }

    fun AndroidModule.resolveVariantName(
      projectInfo: ProjectInfo
    ): String {
      return this.androidVariantResolver.resolveVariant(
        buildType = projectInfo.buildType,
        productFlavors = { dimension ->
          projectInfo.productFlavors[dimension]
            ?: run {
              // See: b/242856048 and b/242289523: Flavors in `FAKE_DIMENSION` are not reported here.
              if (dimension == FAKE_DIMENSION) {
                val suffix = projectInfo.buildType.orEmpty()
                projectInfo
                  .attributes["com.android.build.gradle.internal.attributes.VariantAttr"]
                  ?.takeIf { it.endsWith(suffix, ignoreCase = true) }
                  ?.let { it.substring(0, it.length - suffix.length) }
              } else null
            }
            ?: error(
              "$dimension attribute not found in a dependency model " +
                "of ${classpathId.projectPath} (${classpathId.buildId}) " +
                "on  ${projectInfo.displayName}"
            )
        }
      )
        ?: error(
          "Cannot find a variant matching build type '${projectInfo.buildType}' " +
            "and product flavors '${projectInfo.productFlavors}' " +
            "in ${projectInfo.displayName} " +
            "referred from ${classpathId.projectPath} (${classpathId.buildId})"
        )
    }

    fun populateProjectDependencies(libraries: List<LibraryWithDependencies>, seenDependencies: MutableMap<String, List<String>>) {
      /**
       * Determines whether a dependency target is an Android component.
       *
       * It was initially assumed that the Android Gradle plugin should only populate `identifier.artifact` property
       * for non-Android components. However, it happened that it is also not null in the case of dependencies of
       * `androidTest` (or similarly of `TEST_ONLY` projects) artifact on `main` artifacts.
       *
       * Also, unfortunately, the artifact file returned in this case is not known to the IDE.
       *
       * Temporarily, detect Android components by presence of a build type or a product flavor.
       */
      fun ProjectInfo.isAndroidComponent(): Boolean = buildType != null || productFlavors.isNotEmpty()

      fun ProjectInfo.isKmpAndroidComponent(): Boolean =
        attributes["org.jetbrains.kotlin.platform.type"] == "jvm" &&
        attributes[TargetJvmEnvironment.TARGET_JVM_ENVIRONMENT_ATTRIBUTE.name] == TargetJvmEnvironment.ANDROID

      for (identifier in libraries) {
        val projectInfo = identifier.library.projectInfo!!
        // TODO(b/203750717): Model this explicitly in the tooling model.
        val artifact =
          if (projectInfo.isKmpAndroidComponent()) {
            KmpAndroidArtifactRef
          } else if (projectInfo.isAndroidComponent()) {
            val androidModule: AndroidModule =
              androidProjectPathResolver.resolve(buildPathToBuildId(projectInfo), projectInfo.projectPath)
                ?: error("Cannot find an Android module: ${projectInfo.displayName}")
            val variantName = androidModule.resolveVariantName(projectInfo)
            AndroidArtifactRef(variantName, projectInfo.isTestFixtures)
          } else {
            val artifact = identifier.library.artifact ?: let {
              recordException {
                error(
                  "Unresolved module dependency ${projectInfo.displayName} in " +
                  "${classpathId.projectPath} (${classpathId.buildId}). Neither the source set nor the artifact property was populated" +
                  " by the Android Gradle plugin."
                )
              }
              null
            } ?: continue
            NonAndroidAndroidArtifactRef(artifact)
          }
        createModuleDependency(
          seenDependencies = seenDependencies,
          projectInfo = projectInfo,
          artifactAddress = identifier.library.key,
          lintJar = identifier.library.lintJar,
          artifact = artifact,
          dependencies = identifier.dependencies
        )
      }
    }

    fun populateJavaLibraries(
      javaLibraries: Collection<LibraryWithDependencies>,
      seenDependencies: MutableMap<String, List<String>>
    ) {
      for (javaLibrary in javaLibraries) {
        val address = javaLibrary.library.key
        if (!seenDependencies.contains(address)) {
          seenDependencies[address] = javaLibrary.dependencies
          librariesById.computeIfAbsent(address) {
            javaLibraryFrom(javaLibrary.library)
          }
        }
      }
    }

    fun populateOptionalSdkLibrariesLibraries(
      bootClasspath: Collection<String>,
      seenDependencies: MutableMap<String, List<String>>
    ) {
      getUsefulBootClasspathLibraries(bootClasspath).forEach { jarFile ->
        if (!seenDependencies.contains(jarFile.path)) { // Any unique key identifying the library  is suitable.
          seenDependencies[jarFile.path] = listOf()
          librariesById.computeIfAbsent(jarFile.path) {
            javaLibraryFromJarFile(jarFile)
          }
        }
      }
    }

    class LibrariesByType(
      val androidLibraries: List<LibraryWithDependencies>,
      val javaLibraries: List<LibraryWithDependencies>,
      val projectLibraries: List<LibraryWithDependencies>,
      val unknownLibraries: List<LibraryWithDependencies>
    )

    fun getTypedLibraries(
      dependencies: List<LibraryWithDependencies>?
    ): LibrariesByType {
      val androidLibraries: MutableList<LibraryWithDependencies> = mutableListOf()
      val javaLibraries: MutableList<LibraryWithDependencies> = mutableListOf()
      val projectLibraries: MutableList<LibraryWithDependencies> = mutableListOf()
      val unknownLibraries: MutableList<LibraryWithDependencies> = mutableListOf()

      dependencies?.forEach { dep ->
        when (dep.library.type) {
          LibraryType.ANDROID_LIBRARY -> androidLibraries
          LibraryType.PROJECT -> projectLibraries
          LibraryType.JAVA_LIBRARY -> javaLibraries
          LibraryType.RELOCATED -> unknownLibraries
          LibraryType.NO_ARTIFACT_FILE -> unknownLibraries
        }.add(dep)
      }

      return LibrariesByType(
        androidLibraries = androidLibraries,
        javaLibraries = javaLibraries,
        projectLibraries = projectLibraries,
        unknownLibraries = unknownLibraries
      )
    }

    /*
    Flattens a direct acyclic graph of dependencies into a list that includes each node only once and is the result of traversal in the
    depth-first pre-order order.
   */
    fun List<GraphItem>.toFlatLibraryList(): List<LibraryWithDependencies> {
      val result = mutableListOf<LibraryWithDependencies>()
      // We process items in the order that the recursive depth-first pre-order traversal would achieve. This is for compatibility
      // with v1 models and will change soon when we start exposing graphs to the IDE.
      val seenGraphItemLibraryKeys = HashSet<String>()
      val queue = ArrayDeque(this@toFlatLibraryList.asReversed())
      while (queue.isNotEmpty()) {
        val item = queue.removeLast()
        if (seenGraphItemLibraryKeys.add(item.key)) {
          queue.addAll(item.dependencies.asReversed().asSequence().filter { !seenGraphItemLibraryKeys.contains(it.key) })
          val library = libraries[item.key]
          if (library != null) {
            result.add(LibraryWithDependencies(library, item.dependencies.map {
              it.key
            }))
          }
        }
      }
      return result
    }

    fun List<Edge>.toFlatLibraryList(): List<LibraryWithDependencies> {
      val result = LinkedHashMap<String, MutableList<String>>()
      forEach {
        result.computeIfAbsent(it.from) { mutableListOf() }.apply {
          // Self-dependencies are used to denote just the existence of a node.
          if (it.from != it.to) {
            add(it.to)
          }
        }
      }

      return result.map {
        LibraryWithDependencies(libraries[it.key]!!, it.value)
      }
    }

    fun populateAndroidLibraries(
      androidLibraries: Collection<LibraryWithDependencies>,
      seenDependencies: MutableMap<String, List<String>>
    ) {
      androidLibraries.forEach { androidLibrary ->
        val address = androidLibrary.library.key
        if (!seenDependencies.contains(address)) {
          seenDependencies[address] = androidLibrary.dependencies
          librariesById.computeIfAbsent(address) {
            androidLibraryFrom(androidLibrary.library)
          }
        }
      }
    }

    fun populateUnknownDependencies(
      libraries: List<LibraryWithDependencies>,
      seenDependencies: MutableMap<String, List<String>>
    ) {
      libraries.forEach { unknownLibrary ->
        val address = unknownLibrary.library.key
        if (!seenDependencies.contains(address)) {
          seenDependencies[address] = unknownLibrary.dependencies
          librariesById.computeIfAbsent(address) {
            internedModels.getOrCreate(IdeUnknownLibraryImpl(unknownLibrary.library.key))
          }
        }
      }
    }

    fun createIdeDependencies(
      artifactAddressesAndDependencies: Map<String, List<String>>
    ): IdeDependenciesCoreImpl {
      val dependencyList = mutableListOf<IdeDependencyCoreImpl>()
      val indexed = mutableMapOf<String, Int>()

      /* Collect the list of indexes for Project dependencies in order that they be potentially referred to by other modules */
      val projectIdToIndex: MutableMap<ClasspathIdentifier, Int> = HashMap()

      artifactAddressesAndDependencies.onEachIndexed { index, entry ->
        indexed[entry.key] = index
      }

      artifactAddressesAndDependencies.forEach { (key, deps) ->
        val libraryReference = librariesById[key]!!

        val library = internedModels.lookup(libraryReference)
        if (library is IdePreResolvedModuleLibraryImpl) {
          val id = ClasspathIdentifier(BuildId(File(library.buildId)), library.projectPath, library.sourceSet, classpathId.classpathType)
          projectIdToIndex[id] = indexed[key]!!
        }

        dependencyList.add(IdeDependencyCoreImpl(libraryReference, deps.mapNotNull { indexed[it] }))
      }

      val ideDependenciesCore = IdeDependenciesCoreDirect(dependencyList)
      /* If we are computing the runtime classpath then save references to any module dependencies within the graph */
      if (classpathId.classpathType == ClasspathType.RUNTIME) {
        projectIdToIndex.forEach { (id, index) ->
          // Don't override stored classpath references, preferring the first classpath that we come across instead of the last.
          internedModels.addProjectReferenceToArtifactClasspath(
            id,
            ideDependenciesCore to index)
        }
      }

      return ideDependenciesCore
    }

    fun createIdeDependenciesInstance(): IdeDependenciesCoreImpl {
      val seenDependencies = mutableMapOf<String, List<String>>()
      val dependencyList = when (dependencies) {
        is DependencyGraphCompat.AdjacencyList -> dependencies.edges.toFlatLibraryList()
        is DependencyGraphCompat.GraphItemList -> dependencies.graphItems.toFlatLibraryList()
        null -> null
      }
      val typedLibraries = getTypedLibraries(dependencyList)

      populateAndroidLibraries(typedLibraries.androidLibraries, seenDependencies)
      populateJavaLibraries(typedLibraries.javaLibraries, seenDependencies)
      populateOptionalSdkLibrariesLibraries(bootClasspath, seenDependencies)
      populateProjectDependencies(typedLibraries.projectLibraries, seenDependencies)
      populateUnknownDependencies(typedLibraries.unknownLibraries, seenDependencies)
      return createIdeDependencies(seenDependencies)
    }

    fun createDependencyRef(): IdeDependenciesCoreRef? {
      val classpathToIndex = internedModels.getProjectReferenceToArtifactClasspath(classpathId) ?: return null
      return IdeDependenciesCoreRef(classpathToIndex.first, classpathToIndex.second)
    }

    val dependenciesCore = createIdeDependenciesInstance()

    IdeModelWithPostProcessor(
      dependenciesCore,
      postProcessor = {
        if (dependencies == null) createDependencyRef() ?: dependenciesCore
        else dependenciesCore
      }
    )
  }

  /**
   * Create [IdeDependencies] from [ArtifactDependencies].
   */
  fun dependenciesFrom(
    classpathId: ClasspathIdentifier,
    dependencies: DependencyGraphCompat?,
    libraries: Map<String, Library>,
    bootClasspath: Collection<String>,
    androidProjectPathResolver: AndroidProjectPathResolver,
    buildPathMap: Map<String, BuildId>,
  ): ModelResult<IdeModelWithPostProcessor<IdeDependenciesCoreImpl>> {
    return createFromDependencies(
      classpathId,
      dependencies,
      libraries,
      bootClasspath,
      androidProjectPathResolver,
      buildPathMap,
    )
  }

  fun List<UnresolvedDependency>.unresolvedDependenciesFrom(): List<IdeUnresolvedDependencyImpl> {
    return map { IdeUnresolvedDependencyImpl(it.name, it.cause) }
  }

  fun convertV2Execution(execution: TestInfo.Execution?): IdeTestOptions.Execution? {
    return if (execution == null) null
    else when (execution) {
      TestInfo.Execution.HOST -> IdeTestOptions.Execution.HOST
      TestInfo.Execution.ANDROID_TEST_ORCHESTRATOR -> IdeTestOptions.Execution.ANDROID_TEST_ORCHESTRATOR
      TestInfo.Execution.ANDROIDX_TEST_ORCHESTRATOR -> IdeTestOptions.Execution.ANDROIDX_TEST_ORCHESTRATOR
      else -> throw IllegalStateException("Unknown execution option: $execution")
    }
  }

  fun testOptionsFrom(testOptions: TestInfo): IdeTestOptionsImpl {
    return IdeTestOptionsImpl(
      animationsDisabled = testOptions.animationsDisabled,
      execution = convertV2Execution(testOptions.execution),
      instrumentedTestTaskName = testOptions.instrumentedTestTaskName
    )
  }

  fun convertCodeShrinker(codeShrinker: com.android.builder.model.v2.ide.CodeShrinker?): CodeShrinker? {
    return if (codeShrinker == null) null
    else when (codeShrinker) {
      com.android.builder.model.v2.ide.CodeShrinker.PROGUARD -> CodeShrinker.PROGUARD
      com.android.builder.model.v2.ide.CodeShrinker.R8 -> CodeShrinker.R8
      else -> throw IllegalStateException("Unknown code shrinker option: $codeShrinker")
    }
  }

  /**
   * Converts a [ModelSyncFile] from the Gradle Sync Model to the internal Ide Model.
   */
  fun modelSyncFileFrom(modelSyncFile: ModelSyncFile): IdeModelSyncFileImpl {
    return IdeModelSyncFileImpl(
      // TODO(b/205713031): Parse syncType and handle unknown values.
      modelSyncType = IdeModelSyncFile.IdeModelSyncType.BASIC,
      taskName = modelSyncFile.taskName.deduplicate(),
      syncFile = modelSyncFile.syncFile.deduplicateFile()
    )
  }

  fun buildTasksOutputInformationFrom(artifact: AndroidArtifact): IdeBuildTasksAndOutputInformationImpl {
    return IdeBuildTasksAndOutputInformationImpl(
      assembleTaskName = artifact.assembleTaskName,
      assembleTaskOutputListingFile = artifact.assembleTaskOutputListingFile?.path?.takeUnless { it.isEmpty() }?.deduplicate(),
      bundleTaskName = artifact.bundleInfo?.bundleTaskName,
      bundleTaskOutputListingFile = artifact.bundleInfo?.bundleTaskOutputListingFile?.path?.takeUnless { it.isEmpty() }?.deduplicate(),
      apkFromBundleTaskName = artifact.bundleInfo?.apkFromBundleTaskName,
      apkFromBundleTaskOutputListingFile = artifact.bundleInfo?.apkFromBundleTaskOutputListingFile?.path?.takeUnless { it.isEmpty() }
        ?.deduplicate()
    )
  }

  /*
   AGP from 8.0.0-alpha02 provides desugar method files per artifact (so they can diverge between main and test).
   For older AGPs from 7.3.0-alpha06 which provide desugar method files in the variant that apply to main and test,
   fall back to that value.
  */
  fun getDesugaredMethodsList(artifact: AndroidArtifact, fallback: Collection<File>): Collection<File> {
    return if (agpVersion.isAtLeast(8, 0, 0, "alpha", 2, false))
      artifact.desugaredMethodsFiles
    else
      fallback
  }

  fun androidArtifactFrom(
    name: IdeArtifactName,
    basicArtifact: BasicArtifact,
    mainVariantName: String,
    legacyAndroidGradlePluginProperties: LegacyAndroidGradlePluginProperties?,
    fallbackDesugaredMethodsFiles: Collection<File>,
    artifact: AndroidArtifact
  ): IdeAndroidArtifactCoreImpl {
    val testInfo = artifact.testInfo

    val applicationId: String? = getApplicationIdFromArtifact(agpVersion, artifact, name, legacyAndroidGradlePluginProperties, mainVariantName)

    return IdeAndroidArtifactCoreImpl(
      name = name,
      compileTaskName = artifact.compileTaskName,
      assembleTaskName = artifact.assembleTaskName,
      classesFolder = artifact.classesFolders,
      ideSetupTaskNames = artifact.ideSetupTaskNames.toList(),
      generatedSourceFolders = artifact.generatedSourceFolders.deduplicateFiles().distinct(),
      variantSourceProvider = basicArtifact.variantSourceProvider?.let { sourceProviderFrom(it) },
      multiFlavorSourceProvider = basicArtifact.multiFlavorSourceProvider?.let { sourceProviderFrom(it) },
      compileClasspathCore = throwingIdeDependencies(),
      runtimeClasspathCore = throwingIdeDependencies(),
      unresolvedDependencies = emptyList(),
      applicationId = applicationId,
      generatedResourceFolders = artifact.generatedResourceFolders.deduplicateFiles().distinct(),
      signingConfigName = artifact.signingConfigName?.deduplicate(),
      abiFilters = ImmutableSet.copyOf(artifact.abiFilters.orEmpty()),
      isSigned = artifact.isSigned,
      additionalRuntimeApks = testInfo?.additionalRuntimeApks?.deduplicateFiles() ?: emptyList(),
      testOptions = artifact.testInfo?.let { testOptionsFrom(it) },
      buildInformation = buildTasksOutputInformationFrom(artifact),
      codeShrinker = convertCodeShrinker(artifact.codeShrinker),
      isTestArtifact = name == IdeArtifactName.ANDROID_TEST,
      modelSyncFiles = artifact.modelSyncFiles.map { modelSyncFileFrom(it) },
      privacySandboxSdkInfo = if (agpVersion.isAtLeast(7, 4, 0, "alpha", 10, false))
        artifact.privacySandboxSdkInfo?.let { IdePrivacySandboxSdkInfoImpl(it.task, it.outputListingFile) }
      else
        null,
      desugaredMethodsFiles = getDesugaredMethodsList(artifact, fallbackDesugaredMethodsFiles),
      generatedClassPaths = if (agpVersion.isAtLeast(8, 2, 0, "alpha", 7, false))
        artifact.generatedClassPaths else emptyMap()
    )
  }

  fun androidArtifactFrom(
    ownerBuildId: BuildId,
    ownerProjectPath: String,
    artifact: IdeAndroidArtifactCoreImpl,
    artifactDependencies: ArtifactDependenciesCompat,
    libraries: Map<String, Library>,
    bootClasspath: Collection<String>,
    androidProjectPathResolver: AndroidProjectPathResolver,
    buildPathMap: Map<String, BuildId>,
    artifactName: IdeModuleWellKnownSourceSet,
  ): ModelResult<IdeModelWithPostProcessor<IdeAndroidArtifactCoreImpl>> {
    return ModelResult.create {
      val compileClasspathCore = dependenciesFrom(
        ClasspathIdentifier(ownerBuildId, ownerProjectPath, artifactName, ClasspathType.COMPILE),
        artifactDependencies.compileDependencies,
        libraries,
        bootClasspath,
        androidProjectPathResolver,
        buildPathMap,
      ).recordAndGet()

      val runtimeClasspathCore = dependenciesFrom(
        ClasspathIdentifier(ownerBuildId, ownerProjectPath, artifactName, ClasspathType.RUNTIME),
        artifactDependencies.runtimeDependencies,
        libraries,
        bootClasspath,
        androidProjectPathResolver,
        buildPathMap,
      ).recordAndGet()

      IdeModelWithPostProcessor(
        // We need to update the classpaths as these are used before the postProcessor is called in order to get the
        // next set of Gradle projects to fetch models for.
        // Any classpath references will not be resolved until the postProcessor has been run
        artifact.copy(
          compileClasspathCore = compileClasspathCore?.model ?: IdeDependenciesCoreDirect(emptyList()),
          runtimeClasspathCore = throwingIdeDependencies(),
          unresolvedDependencies = artifactDependencies.unresolvedDependencies.unresolvedDependenciesFrom(),
        ),
        postProcessor = {
          artifact.copy(
            compileClasspathCore = compileClasspathCore?.postProcess() ?: IdeDependenciesCoreDirect(emptyList()),
            runtimeClasspathCore = runtimeClasspathCore?.postProcess() ?: IdeDependenciesCoreDirect(emptyList()),
            unresolvedDependencies = artifactDependencies.unresolvedDependencies.unresolvedDependenciesFrom(),
          )
        }
      )
    }
  }

  fun javaArtifactFrom(
    name: IdeArtifactName,
    basicArtifact: BasicArtifact,
    artifact: JavaArtifact
  ): IdeJavaArtifactCoreImpl {
    return IdeJavaArtifactCoreImpl(
      name = name,
      compileTaskName = artifact.compileTaskName,
      assembleTaskName = artifact.assembleTaskName,
      classesFolder = artifact.classesFolders,
      ideSetupTaskNames = artifact.ideSetupTaskNames.deduplicateStrings().toList(),
      generatedSourceFolders = artifact.generatedSourceFolders.deduplicateFiles().distinct(),
      variantSourceProvider = basicArtifact.variantSourceProvider?.let { sourceProviderFrom(it) },
      multiFlavorSourceProvider = basicArtifact.multiFlavorSourceProvider?.let { sourceProviderFrom(it) },
      compileClasspathCore = throwingIdeDependencies(),
      runtimeClasspathCore = throwingIdeDependencies(),
      unresolvedDependencies = emptyList(),
      mockablePlatformJar = artifact.mockablePlatformJar,
      isTestArtifact = name == IdeArtifactName.UNIT_TEST,
      generatedClassPaths = if (agpVersion.isAtLeast(8, 2, 0, "alpha", 7, false))
        artifact.generatedClassPaths else emptyMap()
    )
  }

  fun javaArtifactFrom(
    buildId: BuildId,
    projectPath: String,
    artifact: IdeJavaArtifactCoreImpl,
    variantDependencies: ArtifactDependenciesCompat,
    libraries: Map<String, Library>,
    bootClasspath: Collection<String>,
    androidProjectPathResolver: AndroidProjectPathResolver,
    buildPathMap: Map<String, BuildId>,
    artifactName: IdeModuleWellKnownSourceSet,
  ): ModelResult<IdeModelWithPostProcessor<IdeJavaArtifactCoreImpl>> {
    return ModelResult.create {
      val compileClasspathCore = dependenciesFrom(
        ClasspathIdentifier(buildId, projectPath, artifactName, ClasspathType.COMPILE),
        variantDependencies.compileDependencies,
        libraries,
        bootClasspath,
        androidProjectPathResolver,
        buildPathMap,
      ).recordAndGet()

      val runtimeClasspathCore = dependenciesFrom(
        ClasspathIdentifier(buildId, projectPath, artifactName, ClasspathType.RUNTIME),
        variantDependencies.runtimeDependencies,
        libraries,
        bootClasspath,
        androidProjectPathResolver,
        buildPathMap,
      ).recordAndGet()

      IdeModelWithPostProcessor(
        // We can't just passthrough the artifact as we need to update the classpaths.
        // These are used before the postProcessor is called in order to get the next set of Gradle projects to fetch models for.
        // However any classpath references will not be resolved until the postProcessor has been run
        artifact.copy(
          compileClasspathCore = compileClasspathCore?.model ?: IdeDependenciesCoreDirect(emptyList()),
          runtimeClasspathCore = throwingIdeDependencies(),
          unresolvedDependencies = variantDependencies.unresolvedDependencies.unresolvedDependenciesFrom(),
        ),
        postProcessor = {
          artifact.copy(
            compileClasspathCore = compileClasspathCore?.postProcess() ?: IdeDependenciesCoreDirect(emptyList()),
            runtimeClasspathCore = runtimeClasspathCore?.postProcess() ?: IdeDependenciesCoreDirect(emptyList()),
            unresolvedDependencies = variantDependencies.unresolvedDependencies.unresolvedDependenciesFrom(),
          )
        }
      )
    }
  }

  fun ideTestedTargetVariantFrom(testedTargetVariant: TestedTargetVariant): IdeTestedTargetVariantImpl = IdeTestedTargetVariantImpl(
    targetProjectPath = testedTargetVariant.targetProjectPath.deduplicate(),
    targetVariant = testedTargetVariant.targetVariant.deduplicate()
  )

  fun getTestedTargetVariants(variant: Variant): List<IdeTestedTargetVariantImpl> {
    if (variant.testedTargetVariant == null) return emptyList()
    return listOf(ideTestedTargetVariantFrom(variant.testedTargetVariant!!))
  }

  fun variantFrom(
    androidProject: IdeAndroidProjectImpl,
    basicVariant: BasicVariant,
    variant: Variant,
    legacyAndroidGradlePluginProperties: LegacyAndroidGradlePluginProperties?
  ): ModelResult<IdeVariantCoreImpl> {
    // Currently, all plugins going through the model cache v2 building will be of multi-variant type
    val multiVariantData = androidProject.multiVariantData!!
    // To get merged flavors for V2, we merge flavors from default config and all the flavors.
    val mergedFlavor = mergeProductFlavorsFrom(
      multiVariantData.defaultConfig,
      multiVariantData.productFlavors.map { it.productFlavor }.filter { basicVariant.productFlavors.contains(it.name) }.toList(),
      androidProject.projectType
    )

    val buildType = multiVariantData.buildTypes.find { it.buildType.name == basicVariant.buildType }?.buildType

    fun <T> merge(f: IdeProductFlavorImpl.() -> T, b: IdeBuildTypeImpl.() -> T, combine: (T?, T?) -> T): T {
      return combine(mergedFlavor.f(), buildType?.b())
    }

    fun <T> combineMaps(u: Map<String, T>?, v: Map<String, T>?): Map<String, T> = u.orEmpty() + v.orEmpty()
    fun <T> combineSets(u: Collection<T>?, v: Collection<T>?): Collection<T> = (u?.toSet().orEmpty() + v.orEmpty()).toList()

    val versionNameSuffix =
      if (mergedFlavor.versionNameSuffix == null && buildType?.versionNameSuffix == null) null
      else mergedFlavor.versionNameSuffix.orEmpty() + buildType?.versionNameSuffix.orEmpty()
    val variantName = variant.name.deduplicate()
    val fallbackDesugaredMethodsFiles = if (agpVersion.isAtLeast(7, 3, 0, "alpha", 6, false)) variant.desugaredMethods else emptyList()

    return ModelResult.create {
      IdeVariantCoreImpl(
        name = variantName,
        displayName = variant.displayName.deduplicate(),
        mainArtifact = androidArtifactFrom(
          IdeArtifactName.MAIN, basicVariant.mainArtifact, variantName, legacyAndroidGradlePluginProperties,
          fallbackDesugaredMethodsFiles, variant.mainArtifact
        ),
        // If AndroidArtifact isn't null, then same goes for the ArtifactDependencies.
        unitTestArtifact = variant.unitTestArtifact?.let { it: JavaArtifact ->
          javaArtifactFrom(IdeArtifactName.UNIT_TEST, basicVariant.unitTestArtifact!!, it)
        },
        androidTestArtifact = variant.androidTestArtifact?.let { it: AndroidArtifact ->
          androidArtifactFrom(
            IdeArtifactName.ANDROID_TEST, basicVariant.androidTestArtifact!!, variantName, legacyAndroidGradlePluginProperties,
            fallbackDesugaredMethodsFiles, it
          )
        },
        testFixturesArtifact = variant.testFixturesArtifact?.let { it: AndroidArtifact ->
          androidArtifactFrom(
            IdeArtifactName.TEST_FIXTURES, basicVariant.testFixturesArtifact!!, variantName, legacyAndroidGradlePluginProperties,
            fallbackDesugaredMethodsFiles, it
          )
        },
        buildType = basicVariant.buildType?.deduplicate() ?: "",
        productFlavors = ImmutableList.copyOf(basicVariant.productFlavors.deduplicateStrings()),
        minSdkVersion = apiVersionFrom(variant.mainArtifact.minSdkVersion),
        targetSdkVersion = variant.mainArtifact.targetSdkVersionOverride?.let { it: ApiVersion -> apiVersionFrom(it) },
        maxSdkVersion = variant.mainArtifact.maxSdkVersion,
        versionCode = mergedFlavor.versionCode,
        versionNameWithSuffix = mergedFlavor.versionName?.let { it + versionNameSuffix.orEmpty() }?.deduplicate(),
        versionNameSuffix = versionNameSuffix?.deduplicate(),
        instantAppCompatible = variant.isInstantAppCompatible,
        vectorDrawablesUseSupportLibrary = mergedFlavor.vectorDrawables?.useSupportLibrary ?: false,
        resourceConfigurations = mergedFlavor.resourceConfigurations.deduplicateStrings(),
        testInstrumentationRunner = mergedFlavor.testInstrumentationRunner?.deduplicate(),
        testInstrumentationRunnerArguments = mergedFlavor.testInstrumentationRunnerArguments.deduplicateStrings(),
        testedTargetVariants = getTestedTargetVariants(variant),

        resValues = merge({ resValues }, { resValues }, ::combineMaps),
        proguardFiles = merge({ proguardFiles }, { proguardFiles }, ::combineSets),
        consumerProguardFiles = merge({ consumerProguardFiles }, { consumerProguardFiles }, ::combineSets),
        manifestPlaceholders = merge({ manifestPlaceholders }, { manifestPlaceholders }, ::combineMaps),
        deprecatedPreMergedApplicationId = null,
        deprecatedPreMergedTestApplicationId = null,
        desugaredMethodsFiles = fallbackDesugaredMethodsFiles,
      )
    }
  }

  fun variantFrom(
    ownerBuildId: BuildId,
    ownerProjectPath: String,
    variant: IdeVariantCoreImpl,
    variantDependencies: VariantDependenciesCompat,
    bootClasspath: Collection<String>,
    androidProjectPathResolver: AndroidProjectPathResolver,
    buildPathMap: Map<String, BuildId>
  ): ModelResult<IdeVariantWithPostProcessor> {
    return ModelResult.create {
      val mainArtifact =
        variant.mainArtifact.let {
          androidArtifactFrom(
            ownerBuildId = ownerBuildId,
            ownerProjectPath = ownerProjectPath,
            artifact = it,
            artifactDependencies = variantDependencies.mainArtifact,
            libraries = variantDependencies.libraries,
            bootClasspath = bootClasspath,
            androidProjectPathResolver = androidProjectPathResolver,
            buildPathMap = buildPathMap,
            artifactName = MAIN,
          ).recordAndGet()
        }

      val unitTestArtifact =
        variant.unitTestArtifact?.let {
          javaArtifactFrom(
            buildId = ownerBuildId,
            projectPath = ownerProjectPath,
            artifact = it,
            variantDependencies = variantDependencies.unitTestArtifact!!,
            libraries = variantDependencies.libraries,
            bootClasspath = bootClasspath,
            androidProjectPathResolver = androidProjectPathResolver,
            buildPathMap = buildPathMap,
            artifactName = UNIT_TEST,
          ).recordAndGet()
        }

      val androidTestArtifact =
        variant.androidTestArtifact?.let {
          androidArtifactFrom(
            ownerBuildId = ownerBuildId,
            ownerProjectPath = ownerProjectPath,
            artifact = it,
            artifactDependencies = variantDependencies.androidTestArtifact!!,
            libraries = variantDependencies.libraries,
            bootClasspath = bootClasspath,
            androidProjectPathResolver = androidProjectPathResolver,
            buildPathMap = buildPathMap,
            artifactName = ANDROID_TEST,
          ).recordAndGet()
        }

      val testFixturesArtifact =
        variant.testFixturesArtifact?.let {
          androidArtifactFrom(
            ownerBuildId = ownerBuildId,
            ownerProjectPath = ownerProjectPath,
            artifact = it,
            artifactDependencies = variantDependencies.testFixturesArtifact!!,
            libraries = variantDependencies.libraries,
            bootClasspath = bootClasspath,
            androidProjectPathResolver = androidProjectPathResolver,
            buildPathMap = buildPathMap,
            TEST_FIXTURES,
          ).recordAndGet()
        }

      IdeVariantWithPostProcessor(
        variant.copy(
          mainArtifact = mainArtifact?.model ?: error("Failed to fetch models of the main artifact of $ownerProjectPath ($ownerBuildId)"),
          androidTestArtifact = androidTestArtifact?.model,
          unitTestArtifact = unitTestArtifact?.model,
          testFixturesArtifact = testFixturesArtifact?.model
        ),
        postProcessor = fun(): IdeVariantCoreImpl {
          return variant.copy(
            mainArtifact = mainArtifact.postProcess(),
            androidTestArtifact = androidTestArtifact?.postProcess(),
            unitTestArtifact = unitTestArtifact?.postProcess(),
            testFixturesArtifact = testFixturesArtifact?.postProcess()
          )
        }
      )
    }
  }

  fun nativeAbiFrom(nativeAbi: NativeAbi): IdeNativeAbiImpl {
    return IdeNativeAbiImpl(
      name = nativeAbi.name.deduplicate(),
      sourceFlagsFile = nativeAbi.sourceFlagsFile.deduplicateFile(),
      symbolFolderIndexFile = nativeAbi.symbolFolderIndexFile.deduplicateFile(),
      buildFileIndexFile = nativeAbi.buildFileIndexFile.deduplicateFile(),
      additionalProjectFilesIndexFile = nativeAbi.additionalProjectFilesIndexFile.deduplicateFile()
    )
  }

  fun nativeVariantFrom(nativeVariant: NativeVariant): IdeNativeVariantImpl {
    return IdeNativeVariantImpl(
      name = nativeVariant.name.deduplicate(),
      abis = nativeVariant.abis.map { nativeAbiFrom(it) },
    )
  }

  fun nativeModuleFrom(nativeModule: NativeModule): IdeNativeModuleImpl {
    return IdeNativeModuleImpl(
      name = nativeModule.name.deduplicate(),
      variants = nativeModule.variants.map { nativeVariantFrom(it) },
      nativeBuildSystem = when (nativeModule.nativeBuildSystem) {
        NativeBuildSystem.NDK_BUILD -> com.android.tools.idea.gradle.model.ndk.v2.NativeBuildSystem.NDK_BUILD
        NativeBuildSystem.CMAKE -> com.android.tools.idea.gradle.model.ndk.v2.NativeBuildSystem.CMAKE
        NativeBuildSystem.NINJA -> com.android.tools.idea.gradle.model.ndk.v2.NativeBuildSystem.NINJA
        // No forward compatibility. Old Studio cannot open projects with newer AGP.
        else -> error("Unknown native build system: ${nativeModule.nativeBuildSystem}")
      },
      ndkVersion = nativeModule.ndkVersion,
      defaultNdkVersion = nativeModule.defaultNdkVersion,
      externalNativeBuildFile = nativeModule.externalNativeBuildFile
    )
  }

  fun severityOverridesFrom(lintOptions: LintOptions): Map<String, Int>? {
    val severityOverrides = mutableMapOf<String, Int>()
    lintOptions.fatal.forEach { severityOverrides[it] = SEVERITY_FATAL }
    lintOptions.error.forEach { severityOverrides[it] = SEVERITY_ERROR }
    lintOptions.warning.forEach { severityOverrides[it] = SEVERITY_WARNING }
    lintOptions.informational.forEach { severityOverrides[it] = SEVERITY_INFORMATIONAL }
    lintOptions.disable.forEach { severityOverrides[it] = SEVERITY_IGNORE }
    lintOptions.enable.forEach { severityOverrides[it] = SEVERITY_DEFAULT_ENABLED }
    return severityOverrides.ifEmpty { null }
  }

  fun lintOptionsFrom(options: LintOptions): IdeLintOptionsImpl = IdeLintOptionsImpl(
    baselineFile = options.baseline,
    lintConfig = options.lintConfig,
    severityOverrides = severityOverridesFrom(options),
    isCheckTestSources = options.checkTestSources,
    isCheckDependencies = options.checkDependencies,
    disable = options.disable.deduplicateStrings(),
    enable = options.enable.deduplicateStrings(),
    check = options.checkOnly.deduplicateStrings(),
    isAbortOnError = options.abortOnError,
    isAbsolutePaths = options.absolutePaths,
    isNoLines = options.noLines,
    isQuiet = options.quiet,
    isCheckAllWarnings = options.checkAllWarnings,
    isIgnoreWarnings = options.ignoreWarnings,
    isWarningsAsErrors = options.warningsAsErrors,
    isIgnoreTestSources = options.ignoreTestSources,
    isIgnoreTestFixturesSources = options.ignoreTestFixturesSources,
    isCheckGeneratedSources = options.checkGeneratedSources,
    isExplainIssues = options.explainIssues,
    isShowAll = options.showAll,
    textReport = options.textReport,
    textOutput = options.textOutput,
    htmlReport = options.htmlReport,
    htmlOutput = options.htmlOutput,
    xmlReport = options.xmlReport,
    xmlOutput = options.xmlOutput,
    isCheckReleaseBuilds = options.checkReleaseBuilds
  )

  fun javaCompileOptionsFrom(options: JavaCompileOptions): IdeJavaCompileOptionsImpl {
    return IdeJavaCompileOptionsImpl(
      encoding = options.encoding,
      sourceCompatibility = options.sourceCompatibility,
      targetCompatibility = options.targetCompatibility,
      isCoreLibraryDesugaringEnabled = options.isCoreLibraryDesugaringEnabled
    )
  }

  fun convertNamespacing(namespacing: AaptOptions.Namespacing): IdeAaptOptions.Namespacing {
    return when (namespacing) {
      AaptOptions.Namespacing.DISABLED -> IdeAaptOptions.Namespacing.DISABLED
      AaptOptions.Namespacing.REQUIRED -> IdeAaptOptions.Namespacing.REQUIRED
      else -> throw IllegalStateException("Unknown namespacing option: $namespacing")
    }
  }

  fun aaptOptionsFrom(original: AaptOptions): IdeAaptOptionsImpl {
    return IdeAaptOptionsImpl(namespacing = convertNamespacing(original.namespacing))
  }

  fun ideVariantBuildInformationFrom(variant: Variant): IdeVariantBuildInformationImpl {
    return IdeVariantBuildInformationImpl(
      variantName = variant.name,
      buildInformation = buildTasksOutputInformationFrom(variant.mainArtifact)
    )
  }

  fun createVariantBuildInformation(
    project: AndroidProject
  ): Collection<IdeVariantBuildInformationImpl> {
    return project.variants.map(::ideVariantBuildInformationFrom)
  }

  fun viewBindingOptionsFrom(model: ViewBindingOptions): IdeViewBindingOptionsImpl {
    return IdeViewBindingOptionsImpl(enabled = model.isEnabled)
  }

  fun dependenciesInfoFrom(model: DependenciesInfo) = IdeDependenciesInfoImpl(
    includeInApk = model.includeInApk,
    includeInBundle = model.includeInBundle
  )

  fun androidGradlePluginProjectFlagsFrom(
    flags: AndroidGradlePluginProjectFlags,
    gradlePropertiesModel: GradlePropertiesModel
  ): IdeAndroidGradlePluginProjectFlagsImpl =
    IdeAndroidGradlePluginProjectFlagsImpl(
      applicationRClassConstantIds =
      AndroidGradlePluginProjectFlags.BooleanFlag.APPLICATION_R_CLASS_CONSTANT_IDS.getValue(flags),
      testRClassConstantIds = AndroidGradlePluginProjectFlags.BooleanFlag.TEST_R_CLASS_CONSTANT_IDS.getValue(flags),
      transitiveRClasses = AndroidGradlePluginProjectFlags.BooleanFlag.TRANSITIVE_R_CLASS.getValue(flags),
      usesCompose = AndroidGradlePluginProjectFlags.BooleanFlag.JETPACK_COMPOSE.getValue(flags),
      mlModelBindingEnabled = AndroidGradlePluginProjectFlags.BooleanFlag.ML_MODEL_BINDING.getValue(flags),
      unifiedTestPlatformEnabled = AndroidGradlePluginProjectFlags.BooleanFlag.UNIFIED_TEST_PLATFORM.getValue(flags),
      // If the property is not found in AGPProjectFlags (e.g., when opening older AGPs), get it from GradlePropertiesModel
      useAndroidX = AndroidGradlePluginProjectFlags.BooleanFlag.USE_ANDROID_X.getValue(flags, gradlePropertiesModel.useAndroidX),
      enableVcsInfo = AndroidGradlePluginProjectFlags.BooleanFlag.ENABLE_VCS_INFO.getValue(flags)
    )

  fun copyProjectType(projectType: ProjectType): IdeAndroidProjectType = when (projectType) {
    // TODO(b/187504821): is the number of supported project type in V2 reduced ? this is a restricted list compared to V1.

    ProjectType.APPLICATION -> IdeAndroidProjectType.PROJECT_TYPE_APP
    ProjectType.LIBRARY -> IdeAndroidProjectType.PROJECT_TYPE_LIBRARY
    ProjectType.TEST -> IdeAndroidProjectType.PROJECT_TYPE_TEST
    ProjectType.DYNAMIC_FEATURE -> IdeAndroidProjectType.PROJECT_TYPE_DYNAMIC_FEATURE
    else -> error("Unknown Android project type: $projectType")
  }

  fun androidProjectFrom(
    rootBuildId: BuildId,
    buildId: BuildId,
    basicProject: BasicAndroidProject,
    project: AndroidProject,
    modelsVersions: Versions,
    androidDsl: AndroidDsl,
    legacyAndroidGradlePluginProperties: LegacyAndroidGradlePluginProperties?,
    gradlePropertiesModel: GradlePropertiesModel,
    defaultVariantName: String?
  ): ModelResult<IdeAndroidProjectImpl> {
    val defaultConfigCopy: IdeProductFlavorImpl = productFlavorFrom(androidDsl.defaultConfig)
    val defaultConfigSourcesCopy: IdeSourceProviderContainerImpl = sourceProviderContainerFrom(basicProject.mainSourceSet)
    val buildTypesCopy: Collection<IdeBuildTypeContainerImpl> = zip(
      androidDsl.buildTypes,
      basicProject.buildTypeSourceSets,
      { it.name },
      { it.sourceProvider.name },
      ::buildTypeContainerFrom
    )
    val productFlavorCopy: Collection<IdeProductFlavorContainerImpl> = zip(
      androidDsl.productFlavors,
      basicProject.productFlavorSourceSets,
      { it.name },
      { it.sourceProvider.name },
      ::productFlavorContainerFrom
    )
    val basicVariantsCopy: Collection<IdeBasicVariantImpl> = project.variants.map {
      IdeBasicVariantImpl(
        name = it.name,
        applicationId = getApplicationIdFromArtifact(agpVersion, it.mainArtifact, IdeArtifactName.MAIN, legacyAndroidGradlePluginProperties, it.name),
        testApplicationId = it.androidTestArtifact?.let { androidTestArtifact ->
          getApplicationIdFromArtifact(agpVersion, androidTestArtifact, IdeArtifactName.ANDROID_TEST, legacyAndroidGradlePluginProperties, it.name)
        }
      )
    }
    val flavorDimensionCopy: Collection<String> = androidDsl.flavorDimensions.deduplicateStrings()
    val bootClasspathCopy: Collection<String> = ImmutableList.copyOf(basicProject.bootClasspath.map { it.absolutePath })
    val signingConfigsCopy: Collection<IdeSigningConfigImpl> = androidDsl.signingConfigs.map { signingConfigFrom(it) }
    val lintOptionsCopy: IdeLintOptionsImpl = lintOptionsFrom(androidDsl.lintOptions)
    val javaCompileOptionsCopy = javaCompileOptionsFrom(project.javaCompileOptions)
    val aaptOptionsCopy = aaptOptionsFrom(androidDsl.aaptOptions)
    val dynamicFeaturesCopy: Collection<String> = project.dynamicFeatures?.deduplicateStrings() ?: listOf()
    val variantBuildInformation = createVariantBuildInformation(project)
    val viewBindingOptionsCopy: IdeViewBindingOptionsImpl? = project.viewBindingOptions?.let { viewBindingOptionsFrom(it) }
    val dependenciesInfoCopy: IdeDependenciesInfoImpl? = androidDsl.dependenciesInfo?.let { dependenciesInfoFrom(it) }
    val buildToolsVersionCopy = androidDsl.buildToolsVersion
    val groupId = androidDsl.groupId
    val lintChecksJarsCopy: List<File> = project.lintChecksJars.deduplicateFiles()
    val isBaseSplit = basicProject.projectType == ProjectType.APPLICATION
    val agpFlags: IdeAndroidGradlePluginProjectFlagsImpl = androidGradlePluginProjectFlagsFrom(project.flags, gradlePropertiesModel)
    val desugarLibConfig = if (agpVersion.isAtLeast(8, 1, 0, "alpha", 5, false))
      project.desugarLibConfig
    else
      listOf()

    return ModelResult.create {
      if (syncTestMode == SyncTestMode.TEST_EXCEPTION_HANDLING) error("**internal error for tests**")
      IdeAndroidProjectImpl(
        agpVersion = modelsVersions.agp,
        projectPath = IdeProjectPathImpl(
          rootBuildId = rootBuildId.asFile,
          buildId = buildId.asFile,
          projectPath = basicProject.path,
        ),
        defaultSourceProvider = defaultConfigSourcesCopy,
        multiVariantData = IdeMultiVariantDataImpl(
          defaultConfig = defaultConfigCopy,
          buildTypes = buildTypesCopy,
          productFlavors = productFlavorCopy,
        ),
        basicVariants = basicVariantsCopy,
        flavorDimensions = flavorDimensionCopy,
        compileTarget = androidDsl.compileTarget,
        bootClasspath = bootClasspathCopy,
        signingConfigs = signingConfigsCopy,
        lintOptions = lintOptionsCopy,
        lintChecksJars = lintChecksJarsCopy,
        javaCompileOptions = javaCompileOptionsCopy,
        aaptOptions = aaptOptionsCopy,
        buildFolder = basicProject.buildFolder,
        dynamicFeatures = dynamicFeaturesCopy,
        baseFeature = null,
        variantsBuildInformation = variantBuildInformation,
        viewBindingOptions = viewBindingOptionsCopy,
        dependenciesInfo = dependenciesInfoCopy,
        buildToolsVersion = buildToolsVersionCopy,
        resourcePrefix = project.resourcePrefix,
        groupId = groupId,
        namespace = project.namespace,
        testNamespace = project.androidTestNamespace,
        projectType = copyProjectType(basicProject.projectType),
        isBaseSplit = isBaseSplit,
        agpFlags = agpFlags,
        isKaptEnabled = false,
        desugarLibraryConfigFiles = desugarLibConfig,
        defaultVariantName = defaultVariantName
      )
    }
  }

  return object : ModelCache.V2 {
    override val libraryLookup: (LibraryReference) -> IdeUnresolvedLibrary = internedModels::lookup
    override fun createLibraryTable(): IdeUnresolvedLibraryTableImpl = internedModels.createLibraryTable()

    override fun variantFrom(
      androidProject: IdeAndroidProjectImpl,
      basicVariant: BasicVariant,
      variant: Variant,
      legacyAndroidGradlePluginProperties: LegacyAndroidGradlePluginProperties?
    ): ModelResult<IdeVariantCoreImpl> =
      variantFrom(androidProject, basicVariant, variant, legacyAndroidGradlePluginProperties)

    override fun variantFrom(
      ownerBuildId: BuildId,
      ownerProjectPath: String,
      variant: IdeVariantCoreImpl,
      variantDependencies: VariantDependenciesCompat,
      bootClasspath: Collection<String>,
      androidProjectPathResolver: AndroidProjectPathResolver,
      buildPathMap: Map<String, BuildId>
    ): ModelResult<IdeVariantWithPostProcessor> =
        variantFrom(
          ownerBuildId,
          ownerProjectPath,
          variant,
          variantDependencies,
          bootClasspath,
          androidProjectPathResolver,
          buildPathMap
        )


    override fun androidProjectFrom(
      rootBuildId: BuildId,
      buildId: BuildId,
      basicProject: BasicAndroidProject,
      project: AndroidProject,
      androidVersion: Versions,
      androidDsl: AndroidDsl,
      legacyAndroidGradlePluginProperties: LegacyAndroidGradlePluginProperties?,
      gradlePropertiesModel: GradlePropertiesModel,
      defaultVariantName: String?
    ): ModelResult<IdeAndroidProjectImpl> =
      androidProjectFrom(
        rootBuildId,
        buildId,
        basicProject,
        project,
        androidVersion,
        androidDsl,
        legacyAndroidGradlePluginProperties,
        gradlePropertiesModel,
        defaultVariantName
      )

    override fun nativeModuleFrom(nativeModule: NativeModule): IdeNativeModuleImpl = nativeModuleFrom(nativeModule)
  }
}

private fun getApplicationIdFromArtifact(
  agpVersion: AgpVersion,
  artifact: AndroidArtifact,
  name: IdeArtifactName,
  legacyAndroidGradlePluginProperties: LegacyAndroidGradlePluginProperties?,
  mainVariantName: String
) = if (agpVersion.agpModelIncludesApplicationId) {
  artifact.applicationId
} else {
  when (name) {
    IdeArtifactName.MAIN -> legacyAndroidGradlePluginProperties?.componentToApplicationIdMap?.get(mainVariantName)
    IdeArtifactName.ANDROID_TEST -> legacyAndroidGradlePluginProperties?.componentToApplicationIdMap?.get(mainVariantName + "AndroidTest")
    IdeArtifactName.UNIT_TEST, IdeArtifactName.TEST_FIXTURES -> null
  }
}

private inline fun <K, V, W, R> zip(
  original1: Collection<V>,
  original2: Collection<W>,
  key1: (V) -> K,
  key2: (W) -> K,
  mapper: (V, W?) -> R
): List<R> {
  val original2Keyed = original2.associateBy { key2(it) }
  return original1.map { mapper(it, original2Keyed[key1(it)]) }
}

internal fun Collection<com.android.builder.model.v2.ide.SyncIssue>.toV2SyncIssueData(): List<IdeSyncIssue> {
  return map { syncIssue ->
    IdeSyncIssueImpl(
      message = syncIssue.message,
      data = syncIssue.data,
      multiLineMessage = safeGet(syncIssue::multiLineMessage, null)?.filterNotNull()?.toList(),
      severity = syncIssue.severity,
      type = syncIssue.type
    )
  }
}

private sealed class ArtifactRef
private object KmpAndroidArtifactRef : ArtifactRef()
private data class AndroidArtifactRef(val variantName: String, val isTestFixture: Boolean) : ArtifactRef()
private data class NonAndroidAndroidArtifactRef(val artifactFile: File) : ArtifactRef()

/**
 * AGP 8.2+ will return the absolute Gradle build path (which supports nested composite builds).
 * AGP older than 8.2 will return a build name as buildId (which does not contain leading ":").
 *
 * Please use this property instead of [ProjectInfo.buildId]
 */
val ProjectInfo.buildTreePath
  get() = if (buildId.startsWith(":")) buildId else ":$buildId"

val ProjectInfo.displayName
  get() = "$projectPath ($buildTreePath)"