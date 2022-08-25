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
package com.android.tools.idea.devicemanager.virtualtab;

import com.android.annotations.concurrency.AnyThread;
import com.android.annotations.concurrency.UiThread;
import com.android.annotations.concurrency.WorkerThread;
import com.android.ddmlib.AndroidDebugBridge.IDeviceChangeListener;
import com.android.ddmlib.IDevice;
import com.android.ddmlib.IDevice.DeviceState;
import com.android.tools.idea.avdmanager.AvdManagerConnection;
import com.android.tools.idea.devicemanager.DeviceManagerFutureCallback;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.intellij.util.concurrency.AppExecutorUtil;
import com.intellij.util.concurrency.EdtExecutorService;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import javax.swing.event.EventListenerList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

final class ProcessManager implements IDeviceChangeListener {
  private Object myKeyToStateMap;
  private final @NotNull EventListenerList myListeners;
  private @Nullable ProcessManagerEvent myEvent;
  private final @NotNull Supplier<@NotNull AvdManagerConnection> myGetDefaultAvdManagerConnection;
  private final @NotNull Function<@NotNull ProcessManager, @NotNull FutureCallback<@NotNull Object>> myNewSetKeyToStateMapFutureCallback;

  @UiThread
  ProcessManager() {
    this(AvdManagerConnection::getDefaultAvdManagerConnection, ProcessManager::newSetKeyToStateMapFutureCallback);
  }

  @UiThread
  @VisibleForTesting
  ProcessManager(@NotNull Supplier<@NotNull AvdManagerConnection> getDefaultAvdManagerConnection,
                 @NotNull Function<@NotNull ProcessManager, @NotNull FutureCallback<@NotNull Object>> newSetKeyToStateMapFutureCallback) {
    myListeners = new EventListenerList();
    myGetDefaultAvdManagerConnection = getDefaultAvdManagerConnection;
    myNewSetKeyToStateMapFutureCallback = newSetKeyToStateMapFutureCallback;
  }

  @UiThread
  void addProcessManagerListener(@NotNull ProcessManagerListener listener) {
    myListeners.add(ProcessManagerListener.class, listener);
  }

  @Override
  public void deviceConnected(@NotNull IDevice device) {
  }

  @Override
  public void deviceDisconnected(@NotNull IDevice device) {
  }

  /**
   * Called by the device list monitor and the device client monitor threads
   */
  @WorkerThread
  @Override
  public void deviceChanged(@NotNull IDevice device, int mask) {
    if (!device.isEmulator()) {
      return;
    }

    if ((mask & IDevice.CHANGE_STATE) == 0) {
      return;
    }

    DeviceState state = device.getState();

    if (state == null) {
      return;
    }

    switch (state) {
      case OFFLINE:
      case ONLINE:
        init();
        break;
      default:
        break;
    }
  }

  /**
   * Called by the event dispatch and the device list monitor threads
   */
  @AnyThread
  void init() {
    ListenableFuture<Object> future = Futures.submit(this::collectKeyToStateMap, AppExecutorUtil.getAppExecutorService());
    Futures.addCallback(future, myNewSetKeyToStateMapFutureCallback.apply(this), EdtExecutorService.getInstance());
  }

  /**
   * Called by an application pool thread
   */
  @WorkerThread
  private @NotNull Object collectKeyToStateMap() {
    AvdManagerConnection connection = myGetDefaultAvdManagerConnection.get();

    return connection.getAvds(true).stream()
      .collect(Collectors.toMap(avd -> new VirtualDevicePath(avd.getId()), avd -> State.valueOf(connection.isAvdRunning(avd))));
  }

  @UiThread
  @VisibleForTesting
  static @NotNull FutureCallback<@NotNull Object> newSetKeyToStateMapFutureCallback(@NotNull ProcessManager manager) {
    return new DeviceManagerFutureCallback<>(ProcessManager.class, manager::setKeyToStateMap);
  }

  @UiThread
  private void setKeyToStateMap(@NotNull Object keyToStateMap) {
    myKeyToStateMap = keyToStateMap;
    fireStatesChanged();
  }

  @UiThread
  private void fireStatesChanged() {
    Object[] listeners = myListeners.getListenerList();

    for (int i = listeners.length - 2; i >= 0; i -= 2) {
      if (listeners[i] != ProcessManagerListener.class) {
        continue;
      }

      if (myEvent == null) {
        myEvent = new ProcessManagerEvent(this);
      }

      ((ProcessManagerListener)listeners[i + 1]).statesChanged(myEvent);
    }
  }

  @VisibleForTesting
  enum State {
    STOPPED, LAUNCHED;

    /**
     * Called by an application pool thread
     */
    @WorkerThread
    private static @NotNull State valueOf(boolean online) {
      if (online) {
        return LAUNCHED;
      }

      return STOPPED;
    }
  }

  @VisibleForTesting
  @NotNull Object getKeyToStateMap() {
    return myKeyToStateMap;
  }
}
