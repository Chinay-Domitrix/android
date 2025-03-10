/*
 * Copyright (C) 2016 The Android Open Source Project
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
package com.android.tools.idea.layoutlib;

import java.lang.ref.Reference;
import java.lang.ref.SoftReference;
import java.text.MessageFormat;
import java.util.Locale;
import java.util.ResourceBundle;
import org.jetbrains.annotations.Nls;
import java.util.function.Supplier;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.PropertyKey;

/**
 * Messages bundle.
 */
public final class LayoutlibBundle {
  @NonNls
  private static final String BUNDLE_NAME = "messages.LayoutlibBundle";
  private static Reference<ResourceBundle> ourBundle;

  private static ResourceBundle getBundle() {
    ResourceBundle bundle = com.intellij.reference.SoftReference.dereference(ourBundle);
    if (bundle == null) {
      bundle = ResourceBundle.getBundle(BUNDLE_NAME);
      ourBundle = new SoftReference<>(bundle);
    }
    return bundle;
  }

  private LayoutlibBundle() {
  }

  @NotNull
  public static @Nls String message(@NotNull @PropertyKey(resourceBundle = BUNDLE_NAME) String key, @NotNull Object... params) {
    return readFromBundleAndFormat(getBundle(), key, params);
  }

  public static Supplier<String> messagePointer(@NotNull @PropertyKey(resourceBundle = BUNDLE_NAME) String key, @NotNull Object... params) {
    return () -> message(key, params);
  }

  private static String readFromBundleAndFormat(@NotNull ResourceBundle bundle, @NotNull String key, @NotNull Object... params) {
    String rawValue = bundle.getString(key);
    Locale locale = bundle.getLocale();
    MessageFormat format = new MessageFormat(rawValue, locale);
    return format.format(params);
  }
}
