/*
 * Copyright (C) 2020 The Android Open Source Project
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
package com.android.tools.idea.streaming.emulator.settings

import com.android.tools.idea.IdeInfo
import com.android.tools.idea.flags.StudioFlags
import com.android.tools.idea.streaming.DEFAULT_CAMERA_VELOCITY_CONTROLS
import com.android.tools.idea.streaming.DEFAULT_SNAPSHOT_AUTO_DELETION_POLICY
import com.android.tools.idea.streaming.EmulatorSettings
import com.android.tools.idea.streaming.EmulatorSettings.CameraVelocityControls
import com.android.tools.idea.streaming.EmulatorSettings.SnapshotAutoDeletionPolicy
import com.intellij.openapi.options.Configurable
import com.intellij.openapi.options.ConfigurationException
import com.intellij.openapi.options.SearchableConfigurable
import com.intellij.openapi.ui.ComboBox
import com.intellij.ui.EnumComboBoxModel
import com.intellij.ui.SimpleListCellRenderer
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.dsl.builder.TopGap
import com.intellij.ui.dsl.builder.bindItem
import com.intellij.ui.dsl.builder.bindSelected
import com.intellij.ui.dsl.builder.panel
import com.intellij.ui.layout.selected
import org.jetbrains.android.util.AndroidBundle
import org.jetbrains.annotations.Nls
import javax.swing.JCheckBox

/**
 * Implementation of Settings > Tools > Emulator preference page.
 */
class EmulatorSettingsPage : SearchableConfigurable, Configurable.NoScroll {

  private lateinit var launchInToolWindowCheckBox: JCheckBox
  private lateinit var activateOnAppLaunchCheckBox: JBCheckBox
  private lateinit var activateOnTestLaunchCheckBox: JBCheckBox
  private lateinit var synchronizeClipboardCheckBox: JCheckBox
  private lateinit var showCameraControlPromptsCheckBox: JCheckBox
  private lateinit var cameraVelocityControlComboBox: ComboBox<CameraVelocityControls>
  private val cameraVelocityControlComboBoxModel = EnumComboBoxModel(CameraVelocityControls::class.java)
  private lateinit var snapshotAutoDeletionPolicyComboBox: ComboBox<SnapshotAutoDeletionPolicy>
  private val snapshotAutoDeletionPolicyComboBoxModel = EnumComboBoxModel(SnapshotAutoDeletionPolicy::class.java)

  private val state = EmulatorSettings.getInstance()

  override fun getId() = "emulator.options"

  override fun createComponent() = panel {
    if (StudioFlags.DEVICE_MIRRORING_ADVANCED_TAB_CONTROL.get()) {
      row {
        launchInToolWindowCheckBox =
          checkBox(AndroidBundle.message("android.emulator.settings.launch.tool.window"))
            .comment(AndroidBundle.message("android.emulator.settings.launch.tool.window.tooltip"))
            .component
      }
      row {
        activateOnAppLaunchCheckBox =
          checkBox("Open the Running Devices tool window when launching an app")
            .bindSelected(state::activateOnAppLaunch)
            .component
      }.topGap(TopGap.SMALL)
      row {
        activateOnTestLaunchCheckBox =
          checkBox("Open the Running Devices tool window when launching a test")
            .bindSelected(state::activateOnTestLaunch)
            .component
      }
      row {
        synchronizeClipboardCheckBox =
          checkBox(AndroidBundle.message("android.emulator.settings.clipboard.sharing"))
            .component
      }.topGap(TopGap.SMALL)
      row {
        showCameraControlPromptsCheckBox =
          checkBox(AndroidBundle.message("android.emulator.settings.show.camera.control.prompts"))
            .component
      }.topGap(TopGap.SMALL)
      row {
        panel {
          row("Velocity control keys for virtual scene camera:") {}
          row {
            cameraVelocityControlComboBox =
              comboBox(cameraVelocityControlComboBoxModel,
                       renderer = SimpleListCellRenderer.create(DEFAULT_CAMERA_VELOCITY_CONTROLS.label) { it.label })
                .bindItem(cameraVelocityControlComboBoxModel::getSelectedItem,
                          cameraVelocityControlComboBoxModel::setSelectedItem)
                .component
          }
        }
      }
      row {
        panel {
          row("When encountering snapshots incompatible with the current configuration:") {}
          row {
            snapshotAutoDeletionPolicyComboBox =
              comboBox(snapshotAutoDeletionPolicyComboBoxModel,
                       renderer = SimpleListCellRenderer.create(DEFAULT_SNAPSHOT_AUTO_DELETION_POLICY.displayName) { it.displayName })
                .bindItem(snapshotAutoDeletionPolicyComboBoxModel::getSelectedItem,
                          snapshotAutoDeletionPolicyComboBoxModel::setSelectedItem)
                .component
          }
        }
      }.topGap(TopGap.SMALL)
    }
    else {
      row {
        launchInToolWindowCheckBox =
          checkBox("Launch in the Running Devices tool window")
            .comment("When this setting is enabled, Android Emulator will launch in the Running Devices tool window. " +
                     "Otherwise Android Emulator will launch as a standalone application.")
            .component
      }
      row {
        activateOnAppLaunchCheckBox =
          checkBox("Open the Running Devices tool window when launching an app")
            .bindSelected(state::activateOnAppLaunch)
            .component
      }.topGap(TopGap.SMALL).enabledIf(launchInToolWindowCheckBox.selected)
      row {
        activateOnTestLaunchCheckBox =
          checkBox("Open the Running Devices tool window when launching a test")
            .bindSelected(state::activateOnTestLaunch)
            .component
      }.enabledIf(launchInToolWindowCheckBox.selected)
      row {
        synchronizeClipboardCheckBox =
          checkBox("Enable clipboard sharing")
            .component
      }.topGap(TopGap.SMALL).enabledIf(launchInToolWindowCheckBox.selected)
      row {
        showCameraControlPromptsCheckBox =
          checkBox("Show camera control prompts")
            .component
      }.topGap(TopGap.SMALL).enabledIf(launchInToolWindowCheckBox.selected)
      row {
        panel {
          row("Velocity control keys for virtual scene camera:") {}
          row {
            cameraVelocityControlComboBox =
              comboBox(cameraVelocityControlComboBoxModel,
                       renderer = SimpleListCellRenderer.create(DEFAULT_CAMERA_VELOCITY_CONTROLS.label) { it.label })
                .bindItem(cameraVelocityControlComboBoxModel::getSelectedItem,
                          cameraVelocityControlComboBoxModel::setSelectedItem)
                .component
          }
        }
      }.enabledIf(launchInToolWindowCheckBox.selected)
      row {
        panel {
          row("When encountering snapshots incompatible with the current configuration:") {}
          row {
            snapshotAutoDeletionPolicyComboBox =
              comboBox(snapshotAutoDeletionPolicyComboBoxModel,
                       renderer = SimpleListCellRenderer.create(DEFAULT_SNAPSHOT_AUTO_DELETION_POLICY.displayName) { it.displayName })
                .bindItem(snapshotAutoDeletionPolicyComboBoxModel::getSelectedItem,
                          snapshotAutoDeletionPolicyComboBoxModel::setSelectedItem)
                .component
          }
        }
      }.topGap(TopGap.SMALL).enabledIf(launchInToolWindowCheckBox.selected)
    }
  }

  override fun isModified(): Boolean {
    return launchInToolWindowCheckBox.isSelected != state.launchInToolWindow ||
           activateOnAppLaunchCheckBox.isSelected != state.activateOnAppLaunch ||
           activateOnTestLaunchCheckBox.isSelected != state.activateOnTestLaunch ||
           synchronizeClipboardCheckBox.isSelected != state.synchronizeClipboard ||
           showCameraControlPromptsCheckBox.isSelected != state.showCameraControlPrompts ||
           cameraVelocityControlComboBoxModel.selectedItem != state.cameraVelocityControls ||
           snapshotAutoDeletionPolicyComboBoxModel.selectedItem != state.snapshotAutoDeletionPolicy
  }

  @Throws(ConfigurationException::class)
  override fun apply() {
    state.launchInToolWindow = launchInToolWindowCheckBox.isSelected
    state.activateOnAppLaunch = activateOnAppLaunchCheckBox.isSelected
    state.activateOnTestLaunch = activateOnTestLaunchCheckBox.isSelected
    state.synchronizeClipboard = synchronizeClipboardCheckBox.isSelected
    state.showCameraControlPrompts = showCameraControlPromptsCheckBox.isSelected
    state.cameraVelocityControls = cameraVelocityControlComboBoxModel.selectedItem
    state.snapshotAutoDeletionPolicy = snapshotAutoDeletionPolicyComboBoxModel.selectedItem
  }

  override fun reset() {
    launchInToolWindowCheckBox.isSelected = state.launchInToolWindow
    activateOnAppLaunchCheckBox.isSelected = state.activateOnAppLaunch
    activateOnTestLaunchCheckBox.isSelected = state.activateOnTestLaunch
    synchronizeClipboardCheckBox.isSelected = state.synchronizeClipboard
    showCameraControlPromptsCheckBox.isSelected = state.showCameraControlPrompts
    cameraVelocityControlComboBoxModel.setSelectedItem(state.cameraVelocityControls)
    snapshotAutoDeletionPolicyComboBoxModel.setSelectedItem(state.snapshotAutoDeletionPolicy)
  }

  @Nls
  override fun getDisplayName() = if (IdeInfo.getInstance().isAndroidStudio) "Emulator" else "Android Emulator"
}
