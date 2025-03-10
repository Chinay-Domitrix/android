/*
 * Copyright (C) 2018 The Android Open Source Project
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
package com.android.tools.idea.res

import com.android.ide.common.rendering.api.ResourceValue
import com.android.ide.common.rendering.api.ResourceValueImpl
import com.android.ide.common.rendering.api.SampleDataResourceValue
import com.android.resources.ResourceType
import com.android.tools.res.LocalResourceRepository

/**
 * Return all the [SampleDataResourceItem] representing images in all namespaces
 * accessible from this repository
 */
fun LocalResourceRepository.getSampleDataOfType(type: SampleDataResourceItem.ContentType): Sequence<SampleDataResourceItem> {
  val namespaces = this.namespaces.asSequence()

  return namespaces
    .flatMap { this.getResources(it, ResourceType.SAMPLE_DATA).values().asSequence() }
    .filterIsInstance<SampleDataResourceItem>()
    .filter { it.contentType == type }
}

/**
 * Get all Drawable resource available for this [SampleDataResourceItem].
 *
 * If this item is does not have the [SampleDataResourceItem.ContentType.IMAGE], an empty list will be return
 */
fun SampleDataResourceItem.getDrawableResources(): List<ResourceValue> {
  if (this.contentType != SampleDataResourceItem.ContentType.IMAGE) {
    return emptyList()
  }
  val value = resourceValue as SampleDataResourceValue
  return value.valueAsLines.map { line ->
    ResourceValueImpl(referenceToSelf.namespace, ResourceType.DRAWABLE, referenceToSelf.name, line, value.libraryName)
  }
}
