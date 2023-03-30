/*
 * Copyright (C) 2023 The Android Open Source Project
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
package com.android.tools.idea.dagger.concepts

/** Shared definitions of annotation names used when building the Dagger index. */
object DaggerAnnotations {
  internal const val BINDS = "dagger.Binds"
  internal const val BINDS_INSTANCE = "dagger.BindsInstance"
  internal const val BINDS_OPTIONAL_OF = "dagger.BindsOptionalOf"
  internal const val COMPONENT = "dagger.Component"
  internal const val COMPONENT_BUILDER = "dagger.Component.Builder"
  internal const val COMPONENT_FACTORY = "dagger.Component.Factory"
  internal const val INJECT = "javax.inject.Inject"
  internal const val MODULE = "dagger.Module"
  internal const val PROVIDES = "dagger.Provides"
  internal const val SUBCOMPONENT = "dagger.Subcomponent"
}
