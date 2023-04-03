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
package com.android.tools.idea.layoutinspector.runningdevices

import com.android.annotations.concurrency.UiThread
import com.android.tools.adtui.workbench.WorkBench
import com.android.tools.idea.layoutinspector.LayoutInspector
import com.android.tools.idea.layoutinspector.LayoutInspectorProjectService
import com.android.tools.idea.layoutinspector.dataProviderForLayoutInspector
import com.android.tools.idea.layoutinspector.properties.LayoutInspectorPropertiesPanelDefinition
import com.android.tools.idea.layoutinspector.tree.LayoutInspectorTreePanelDefinition
import com.android.tools.idea.streaming.AbstractDisplayView
import com.intellij.ide.DataManager
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import java.awt.Container
import javax.swing.JComponent

private const val WORKBENCH_NAME = "Layout Inspector"

/**
 * Class grouping components from a Running Devices tab. Used to inject Layout Inspector in the tab.
 * @param disposable Disposable associated with the tab. When the tab is disposed Layout Inspector should be disposed.
 * @param deviceSerialNumber Serial number of the device associated with this tab.
 * @param tabContentPanel The component containing the main content of the tab (the display).
 * @param tabContentPanelContainer The container of [tabContentPanel].
 * @param displayView The [AbstractDisplayView] from running devices. Component on which the device display is rendered.
 */
data class RunningDevicesTabContext(
  val project: Project,
  val disposable: Disposable,
  val deviceSerialNumber: String,
  val tabContentPanel: JComponent,
  val tabContentPanelContainer: Container,
  val displayView: AbstractDisplayView
)

/**
 * Class used to keep track of [RunningDevicesTabContext]s and adding Layout Inspector to them.
 */
interface LayoutInspectorManager {
  companion object {
    @JvmStatic
    fun getInstance(project: Project): LayoutInspectorManager {
      return project.getService(LayoutInspectorManager::class.java)
    }
  }

  fun interface StateListener {
    /**
     * Called each time the state of [LayoutInspectorManager] changes.
     * Which happens each time Layout Inspector is enabled or disabled for a tab.
     */
    fun onStateUpdate(state: Set<RunningDevicesTabContext>)
  }

  fun addStateListener(listener: StateListener)

  /**
   * Injects or removes Layout Inspector in the tab associated to [runningDevicesTabContext].
   */
  fun enableLayoutInspector(runningDevicesTabContext: RunningDevicesTabContext, enable: Boolean)

  /**
   * Returns true if Layout Inspector is enabled for [runningDevicesTabContext], false otherwise.
   */
  fun isEnabled(runningDevicesTabContext: RunningDevicesTabContext): Boolean
}

/**
 * This class is meant to be used on the UI thread, to avoid concurrency issues.
 */
@UiThread
private class LayoutInspectorManagerImpl(private val project: Project) : LayoutInspectorManager {

  /** Keeps track of tabs on which a Workbench was injected */
  private var state = mapOf<String, TabState>()
    set(value) {
      field = value
      updateListeners()
    }

  private val stateListeners = mutableListOf<LayoutInspectorManager.StateListener>()

  override fun addStateListener(listener: LayoutInspectorManager.StateListener) {
    ApplicationManager.getApplication().assertIsDispatchThread()
    updateListeners(listOf(listener))
    stateListeners.add(listener)
  }

  override fun enableLayoutInspector(runningDevicesTabContext: RunningDevicesTabContext, enable: Boolean) {
    ApplicationManager.getApplication().assertIsDispatchThread()
    if (enable) {
      if (state.containsKey(runningDevicesTabContext.deviceSerialNumber)) {
        // do nothing if Layout Inspector is already enabled
        return
      }

      val tabState = addToState(runningDevicesTabContext)
      tabState.enableLayoutInspector()
    }
    else {
      if (!state.containsKey(runningDevicesTabContext.deviceSerialNumber)) {
        // do nothing if Layout Inspector is not enabled
        return
      }

      val tabState = removeFromState(runningDevicesTabContext)
      tabState?.disableLayoutInspector()
    }
  }

  override fun isEnabled(runningDevicesTabContext: RunningDevicesTabContext): Boolean {
    ApplicationManager.getApplication().assertIsDispatchThread()
    return state.containsKey(runningDevicesTabContext.deviceSerialNumber)
  }

  /**
   * Adds [tabContext] to [state].
   * @return the [TabState] associated to [tabContext].
   */
  private fun addToState(tabContext: RunningDevicesTabContext): TabState {
    if (state.containsKey(tabContext.deviceSerialNumber)) {
      return state[tabContext.deviceSerialNumber]!!
    }

    val toAdd = TabState(tabContext)

    Disposer.register(tabContext.disposable) {
      removeFromState(tabContext)
    }

    state = state + mapOf(tabContext.deviceSerialNumber to toAdd)
    return toAdd
  }

  /**
   * Removes [tabContext] from [state].
   * @return the [TabState] associated to [tabContext] that was removed. Or null if nothing was removed.
   */
  // TODO(b/265150325): dispose Layout Inspector when all tabs are removed
  private fun removeFromState(tabContext: RunningDevicesTabContext): TabState? {
    val toRemove = state[tabContext.deviceSerialNumber]
    state = state - tabContext.deviceSerialNumber
    return toRemove
  }

  private fun updateListeners(listenersToUpdate: List<LayoutInspectorManager.StateListener> = stateListeners) {
    listenersToUpdate.forEach { listener -> listener.onStateUpdate(state.map { it.value.tabContext }.toSet()) }
  }

  /**
   * The state of a tab made of [RunningDevicesTabContext] and a [WrapLogic] associated to [tabContext].
   */
  private data class TabState(
    val tabContext: RunningDevicesTabContext,
    val wrapLogic: WrapLogic = WrapLogic(tabContext.tabContentPanel, tabContext.tabContentPanelContainer),
    val displayViewManager: DisplayViewManager = DisplayViewManager(
      tabContext.project.getLayoutInspector().renderModel,
      tabContext.project.getLayoutInspector().renderLogic,
      tabContext.displayView
    )
  ) {
    fun enableLayoutInspector() {
      wrapLogic.wrapComponent { centerPanel ->
        createLayoutInspectorWorkbench(tabContext, centerPanel)
      }
      displayViewManager.startRendering()
    }

    fun disableLayoutInspector() {
      wrapLogic.unwrapComponent()
      displayViewManager.stopRendering()
    }
  }
}

/**
 * Creates a Layout Inspector [WorkBench] with view tree panel and properties panel.
 */
private fun createLayoutInspectorWorkbench(
  tabContext: RunningDevicesTabContext,
  centerPanel: JComponent,
): WorkBench<LayoutInspector> {
  val workbench = WorkBench<LayoutInspector>(tabContext.project, WORKBENCH_NAME, null, tabContext.disposable)
  val layoutInspector = tabContext.project.getLayoutInspector()
  val toolsDefinition = listOf(LayoutInspectorTreePanelDefinition(), LayoutInspectorPropertiesPanelDefinition())
  workbench.init(centerPanel, layoutInspector, toolsDefinition, false)
  DataManager.registerDataProvider(workbench, dataProviderForLayoutInspector(layoutInspector))
  return workbench
}

/** Utility function to get [LayoutInspector] from a [RunningDevicesTabContext] */
private fun Project.getLayoutInspector(): LayoutInspector {
  return LayoutInspectorProjectService
    .getInstance(this)
    .getLayoutInspector()
}