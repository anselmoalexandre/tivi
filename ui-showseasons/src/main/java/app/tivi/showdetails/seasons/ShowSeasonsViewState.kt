/*
 * Copyright 2021 Google LLC
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

package app.tivi.showdetails.seasons

import androidx.compose.runtime.Immutable
import app.tivi.api.UiMessage
import app.tivi.data.entities.TiviShow
import app.tivi.data.resultentities.SeasonWithEpisodesAndWatches
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf

@Immutable
internal data class ShowSeasonsViewState(
    val show: TiviShow = TiviShow.EMPTY_SHOW,
    val seasons: PersistentList<SeasonWithEpisodesAndWatches> = persistentListOf(),
    val refreshing: Boolean = false,
    val message: UiMessage? = null
) {
    companion object {
        val Empty = ShowSeasonsViewState()
    }
}
