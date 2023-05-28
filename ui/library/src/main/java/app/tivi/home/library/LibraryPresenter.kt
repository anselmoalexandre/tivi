/*
 * Copyright 2022 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package app.tivi.home.library

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.paging.PagingConfig
import androidx.paging.compose.collectAsLazyPagingItems
import app.tivi.api.UiMessageManager
import app.tivi.common.compose.rememberCoroutineScope
import app.tivi.data.models.SortOption
import app.tivi.data.traktauth.TraktAuthState
import app.tivi.domain.executeSync
import app.tivi.domain.interactors.GetTraktAuthState
import app.tivi.domain.interactors.UpdateLibraryShows
import app.tivi.domain.observers.ObservePagedLibraryShows
import app.tivi.domain.observers.ObserveTraktAuthState
import app.tivi.domain.observers.ObserveUserDetails
import app.tivi.screens.AccountScreen
import app.tivi.screens.LibraryScreen
import app.tivi.screens.ShowDetailsScreen
import app.tivi.settings.TiviPreferences
import app.tivi.util.Logger
import app.tivi.util.ObservableLoadingCounter
import app.tivi.util.collectStatus
import com.slack.circuit.runtime.CircuitContext
import com.slack.circuit.runtime.Navigator
import com.slack.circuit.runtime.Screen
import com.slack.circuit.runtime.presenter.Presenter
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch
import me.tatarka.inject.annotations.Assisted
import me.tatarka.inject.annotations.Inject

@Inject
class LibraryUiPresenterFactory(
    private val presenterFactory: (Navigator) -> LibraryPresenter,
) : Presenter.Factory {
    override fun create(
        screen: Screen,
        navigator: Navigator,
        context: CircuitContext,
    ): Presenter<*>? = when (screen) {
        is LibraryScreen -> presenterFactory(navigator)
        else -> null
    }
}

@Inject
class LibraryPresenter(
    @Assisted private val navigator: Navigator,
    private val updateLibraryShows: UpdateLibraryShows,
    private val observePagedLibraryShows: ObservePagedLibraryShows,
    private val observeTraktAuthState: ObserveTraktAuthState,
    private val observeUserDetails: ObserveUserDetails,
    private val getTraktAuthState: GetTraktAuthState,
    private val preferences: TiviPreferences,
    private val logger: Logger,
) : Presenter<LibraryUiState> {

    @Composable
    override fun present(): LibraryUiState {
        val scope = rememberCoroutineScope()

        val followedLoadingState = remember { ObservableLoadingCounter() }
        val watchedLoadingState = remember { ObservableLoadingCounter() }
        val uiMessageManager = remember { UiMessageManager() }

        val items = observePagedLibraryShows.flow.collectAsLazyPagingItems()

        var filter by remember { mutableStateOf<String?>(null) }
        var sort by remember { mutableStateOf(SortOption.LAST_WATCHED) }

        val followedLoading by followedLoadingState.observable.collectAsState(false)
        val watchedLoading by watchedLoadingState.observable.collectAsState(false)
        val message by uiMessageManager.message.collectAsState(null)

        val user by observeUserDetails.flow.collectAsState(null)
        val authState by observeTraktAuthState.flow.collectAsState(TraktAuthState.LOGGED_OUT)

        val includeWatchedShows by produceState(false, preferences) {
            preferences.observeLibraryWatchedActive().collect { value = it }
        }
        val includeFollowedShows by produceState(false, preferences) {
            preferences.observeLibraryFollowedActive().collect { value = it }
        }

        fun eventSink(event: LibraryUiEvent) {
            when (event) {
                is LibraryUiEvent.ChangeFilter -> filter = event.filter
                is LibraryUiEvent.ChangeSort -> sort = event.sort
                is LibraryUiEvent.ClearMessage -> {
                    scope.launch {
                        uiMessageManager.clearMessage(event.id)
                    }
                }
                is LibraryUiEvent.Refresh -> {
                    scope.launch {
                        if (getTraktAuthState.executeSync() == TraktAuthState.LOGGED_IN) {
                            updateLibraryShows(
                                UpdateLibraryShows.Params(event.fromUser),
                            ).collectStatus(followedLoadingState, logger, uiMessageManager)
                        }
                    }
                }
                LibraryUiEvent.ToggleFollowedShowsIncluded -> {
                    preferences.libraryFollowedActive = !preferences.libraryFollowedActive
                }
                LibraryUiEvent.ToggleWatchedShowsIncluded -> {
                    preferences.libraryWatchedActive = !preferences.libraryWatchedActive
                }
                LibraryUiEvent.OpenAccount -> navigator.goTo(AccountScreen)
                is LibraryUiEvent.OpenShowDetails -> {
                    navigator.goTo(ShowDetailsScreen(event.showId))
                }
            }
        }

        LaunchedEffect(Unit) {
            observeTraktAuthState(Unit)
            observeUserDetails(ObserveUserDetails.Params("me"))
        }

        LaunchedEffect(observeTraktAuthState) {
            observeTraktAuthState.flow
                .filter { it == TraktAuthState.LOGGED_IN }
                .collect {
                    eventSink(LibraryUiEvent.Refresh(false))
                }
        }

        LaunchedEffect(filter, sort, includeFollowedShows, includeWatchedShows) {
            // When the filter and sort options change, update the data source
            observePagedLibraryShows(
                ObservePagedLibraryShows.Parameters(
                    sort = sort,
                    filter = filter,
                    includeFollowed = preferences.libraryFollowedActive,
                    includeWatched = preferences.libraryWatchedActive,
                    pagingConfig = PAGING_CONFIG,
                ),
            )
        }

        return LibraryUiState(
            items = items,
            user = user,
            authState = authState,
            isLoading = followedLoading || watchedLoading,
            filter = filter,
            filterActive = !filter.isNullOrEmpty(),
            availableSorts = AVAILABLE_SORT_OPTIONS,
            sort = sort,
            message = message,
            watchedShowsIncluded = includeWatchedShows,
            followedShowsIncluded = includeFollowedShows,
            eventSink = ::eventSink,
        )
    }

    companion object {
        private val PAGING_CONFIG = PagingConfig(
            pageSize = 16,
            initialLoadSize = 32,
        )

        private val AVAILABLE_SORT_OPTIONS = listOf(
            SortOption.LAST_WATCHED,
            SortOption.ALPHABETICAL,
        )
    }
}
