// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.android.tools.idea.gradle

import com.intellij.AbstractBundle
import org.jetbrains.annotations.Nls
import org.jetbrains.annotations.NonNls
import org.jetbrains.annotations.PropertyKey

@NonNls
private const val BUNDLE = "messages.AndroidGradlePsdBundle"

internal object AndroidGradlePsdBundle : AbstractBundle(BUNDLE) {

  @Nls
  @JvmStatic
  fun message(@PropertyKey(resourceBundle = BUNDLE) key: String, vararg params: Any) = getMessage(key, *params)
}