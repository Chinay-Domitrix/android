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
package com.android.tools.idea.layoutinspector.runningdevices.actions

import com.android.tools.adtui.actions.createTestActionEvent
import com.android.tools.idea.appinspection.inspector.api.process.ProcessDescriptor
import com.android.tools.idea.concurrency.AndroidCoroutineScope
import com.android.tools.idea.layoutinspector.MODERN_DEVICE
import com.android.tools.idea.layoutinspector.createProcess
import com.android.tools.idea.layoutinspector.model.NotificationModel
import com.android.tools.idea.layoutinspector.pipeline.AbstractInspectorClient
import com.android.tools.idea.layoutinspector.pipeline.DisconnectedClient
import com.android.tools.idea.layoutinspector.pipeline.InspectorClient
import com.android.tools.idea.layoutinspector.pipeline.TreeLoader
import com.android.tools.idea.layoutinspector.properties.PropertiesProvider
import com.google.common.truth.Truth.assertThat
import com.google.wireless.android.sdk.stats.DynamicLayoutInspectorAttachToProcess
import com.intellij.openapi.Disposable
import com.intellij.openapi.project.Project
import com.intellij.testFramework.DisposableRule
import com.intellij.testFramework.ProjectRule
import org.junit.Rule
import org.junit.Test
import java.nio.file.Path

class ToggleDeepInspectActionTest {
  @get:Rule val projectRule = ProjectRule()
  @get:Rule val disposableRule = DisposableRule()

  @Test
  fun testActionClick() {
    var isSelected = false
    val toggleDeepInspectAction =
      ToggleDeepInspectAction({ isSelected }, { isSelected = !isSelected }, { DisconnectedClient })

    toggleDeepInspectAction.actionPerformed(createTestActionEvent(toggleDeepInspectAction))
    assertThat(isSelected).isTrue()

    toggleDeepInspectAction.actionPerformed(createTestActionEvent(toggleDeepInspectAction))
    assertThat(isSelected).isFalse()
  }

  @Test
  fun testTitleAndDescription() {
    val toggleDeepInspectAction = ToggleDeepInspectAction({ false }, {}, { DisconnectedClient })

    val event = createTestActionEvent(toggleDeepInspectAction)
    toggleDeepInspectAction.update(event)
    assertThat(event.presentation.text).isEqualTo("")
    assertThat(event.presentation.description)
      .isEqualTo("Select a component in the device to view inspection information.")
  }

  @Test
  fun testDeepInspectActionIsDisabledWhenClientIsNotConnected() {
    val inspectorClient =
      FakeInspectorClient(
        projectRule.project,
        MODERN_DEVICE.createProcess(),
        disposableRule.disposable
      )
    val toggleDeepInspectAction = ToggleDeepInspectAction({ false }, {}, { inspectorClient })

    val event = createTestActionEvent(toggleDeepInspectAction)
    toggleDeepInspectAction.update(event)

    assertThat(event.presentation.isEnabled).isFalse()

    inspectorClient.state = InspectorClient.State.CONNECTED
    toggleDeepInspectAction.update(event)

    assertThat(event.presentation.isEnabled).isTrue()
  }
}

private open class FakeInspectorClient(
  project: Project,
  process: ProcessDescriptor,
  parentDisposable: Disposable
) :
  AbstractInspectorClient(
    DynamicLayoutInspectorAttachToProcess.ClientType.APP_INSPECTION_CLIENT,
    project,
    NotificationModel(project),
    process,
    isInstantlyAutoConnected = true,
    DisconnectedClient.stats,
    AndroidCoroutineScope(parentDisposable),
    parentDisposable
  ) {
  override suspend fun startFetching() = throw NotImplementedError()
  override suspend fun stopFetching() = throw NotImplementedError()
  override fun refresh() = throw NotImplementedError()
  override fun saveSnapshot(path: Path) = throw NotImplementedError()

  override suspend fun doConnect() {}
  override suspend fun doDisconnect() {}

  override val capabilities
    get() = throw NotImplementedError()
  override val treeLoader: TreeLoader
    get() = throw NotImplementedError()
  override val isCapturing: Boolean
    get() = false
  override val provider: PropertiesProvider
    get() = throw NotImplementedError()
}
