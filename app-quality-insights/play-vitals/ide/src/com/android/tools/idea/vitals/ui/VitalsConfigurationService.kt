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
package com.android.tools.idea.vitals.ui

import com.android.tools.idea.concurrency.AndroidCoroutineScope
import com.android.tools.idea.concurrency.AndroidDispatchers
import com.android.tools.idea.gradle.project.model.GradleAndroidModel
import com.android.tools.idea.insights.AppInsightsConfigurationManager
import com.android.tools.idea.insights.AppInsightsModel
import com.android.tools.idea.insights.AppInsightsProjectLevelControllerImpl
import com.android.tools.idea.insights.ConnectionMode
import com.android.tools.idea.insights.LoadingState
import com.android.tools.idea.insights.OfflineStatusManagerImpl
import com.android.tools.idea.insights.VITALS_KEY
import com.android.tools.idea.insights.analytics.AppInsightsTracker
import com.android.tools.idea.insights.analytics.AppInsightsTrackerImpl
import com.android.tools.idea.insights.client.AppConnection
import com.android.tools.idea.insights.client.AppInsightsCache
import com.android.tools.idea.insights.client.AppInsightsCacheImpl
import com.android.tools.idea.insights.client.AppInsightsClient
import com.android.tools.idea.insights.events.ExplicitRefresh
import com.android.tools.idea.insights.events.actions.AppInsightsActionQueueImpl
import com.android.tools.idea.insights.getHolderModules
import com.android.tools.idea.insights.isAndroidApp
import com.android.tools.idea.insights.ui.AppInsightsToolWindowFactory
import com.android.tools.idea.vitals.client.VitalsClient
import com.android.tools.idea.vitals.createVitalsFilters
import com.android.tools.idea.vitals.datamodel.VitalsConnection
import com.google.gct.login.LoginState
import com.intellij.codeInsight.daemon.DaemonCodeAnalyzer
import com.intellij.openapi.Disposable
import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.MessageType
import com.intellij.openapi.util.Disposer
import java.time.Clock
import java.util.concurrent.ConcurrentLinkedQueue
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.jetbrains.annotations.VisibleForTesting

@Service(Service.Level.PROJECT)
class VitalsConfigurationService(project: Project) : Disposable {
  private val cache = AppInsightsCacheImpl()

  val manager: AppInsightsConfigurationManager =
    VitalsConfigurationManager(project, cache, VitalsClient(this, cache)).also {
      Disposer.register(this, it)
    }

  override fun dispose() = Unit
}

class VitalsConfigurationManager(
  override val project: Project,
  @VisibleForTesting val cache: AppInsightsCache,
  private val client: AppInsightsClient,
  loginState: Flow<Boolean> = LoginState.loggedIn
) : AppInsightsConfigurationManager, Disposable {

  private val logger = Logger.getInstance(VitalsConfigurationManager::class.java)
  private val scope = AndroidCoroutineScope(this)
  private val refreshConfigurationFlow =
    MutableSharedFlow<Unit>(replay = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)

  private val queryConnectionsFlow =
    flow {
        refreshConfigurationFlow
          .combine(loginState) { _, loggedIn -> loggedIn }
          .collect { loggedIn ->
            if (loggedIn) {
              val connections = client.listConnections()
              if (connections is LoadingState.Ready) {
                Logger.getInstance(VitalsConfigurationManager::class.java)
                  .info("Accessible Android Vitals connections: ${connections.value}")
              }
              emit(connections)
            } else {
              emit(LoadingState.Unauthorized(null))
            }
          }
      }
      .shareIn(scope, SharingStarted.Eagerly, replay = 1)

  override val offlineStatusManager = OfflineStatusManagerImpl()

  private val controller =
    AppInsightsProjectLevelControllerImpl(
      key = VITALS_KEY,
      AndroidCoroutineScope(this, AndroidDispatchers.uiThread),
      AndroidDispatchers.workerThread,
      client,
      queryConnectionsFlow.mapConnectionsToVariantConnectionsIfReady(),
      offlineStatusManager,
      onIssuesChanged = { DaemonCodeAnalyzer.getInstance(project).restart() },
      tracker = AppInsightsTrackerImpl(project, AppInsightsTracker.ProductType.PLAY_VITALS),
      clock = Clock.systemDefaultZone(),
      project = project,
      queue = AppInsightsActionQueueImpl(ConcurrentLinkedQueue()),
      onErrorAction = { msg, hyperlinkListener ->
        AppInsightsToolWindowFactory.showBalloon(project, MessageType.ERROR, msg, hyperlinkListener)
      },
      defaultFilters = createVitalsFilters(),
      cache = cache
    )

  override val configuration =
    queryConnectionsFlow
      .mapAvailableAppsToModel()
      .stateIn(scope, SharingStarted.Eagerly, AppInsightsModel.Uninitialized)

  init {
    scope.launch {
      controller.eventFlow.filter { it is ExplicitRefresh }.collect { refreshConfiguration() }
    }
  }

  override fun refreshConfiguration() {
    refreshConfigurationFlow.tryEmit(Unit)
  }

  override fun dispose() = Unit

  private fun Flow<LoadingState.Done<List<AppConnection>>>
    .mapConnectionsToVariantConnectionsIfReady() = mapNotNull { result ->
    when (result) {
      is LoadingState.Ready -> {
        offlineStatusManager.enterMode(ConnectionMode.ONLINE)
        val modules = project.getHolderModules().filter { it.isAndroidApp }
        val appIds =
          modules
            .flatMap { module -> GradleAndroidModel.get(module)?.allApplicationIds ?: emptyList() }
            .toSet()
        result.value
          .map { app -> VitalsConnection(app.appId, app.displayName, app.appId in appIds) }
          .also { cache.populateConnections(it) }
      }
      is LoadingState.NetworkFailure -> {
        logger.warn("Encountered error in getting Vitals connections: ${result.message}")
        offlineStatusManager.enterMode(ConnectionMode.OFFLINE)
        cache.getRecentConnections()
      }
      else -> null
    }
  }

  private fun Flow<LoadingState.Done<List<AppConnection>>>.mapAvailableAppsToModel() = map {
    when (it) {
      is LoadingState.Ready,
      is LoadingState.NetworkFailure -> {
        AppInsightsModel.Authenticated(controller)
      }
      // TODO(b/274775776): disambiguate between different failures. Ex: authentication, grpc, etc.
      is LoadingState.Failure -> {
        AppInsightsModel.Unauthenticated
      }
    }
  }
}
