/*
 * Copyright 2017 Google LLC
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

package app.tivi.data

import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import app.tivi.data.db.TiviDatabase
import app.tivi.data.db.TiviTypeConverters
import app.tivi.data.models.Episode
import app.tivi.data.models.EpisodeWatchEntry
import app.tivi.data.models.FollowedShowEntry
import app.tivi.data.models.LastRequest
import app.tivi.data.models.PopularShowEntry
import app.tivi.data.models.RecommendedShowEntry
import app.tivi.data.models.RelatedShowEntry
import app.tivi.data.models.Season
import app.tivi.data.models.ShowTmdbImage
import app.tivi.data.models.TiviShow
import app.tivi.data.models.TiviShowFts
import app.tivi.data.models.TraktUser
import app.tivi.data.models.TrendingShowEntry
import app.tivi.data.models.WatchedShowEntry
import app.tivi.data.views.FollowedShowsLastWatched
import app.tivi.data.views.FollowedShowsNextToWatch
import app.tivi.data.views.FollowedShowsWatchStats

@Database(
    entities = [
        TiviShow::class,
        TiviShowFts::class,
        TrendingShowEntry::class,
        PopularShowEntry::class,
        TraktUser::class,
        WatchedShowEntry::class,
        FollowedShowEntry::class,
        Season::class,
        Episode::class,
        RelatedShowEntry::class,
        EpisodeWatchEntry::class,
        LastRequest::class,
        ShowTmdbImage::class,
        RecommendedShowEntry::class,
    ],
    views = [
        FollowedShowsWatchStats::class,
        FollowedShowsLastWatched::class,
        FollowedShowsNextToWatch::class,
    ],
    version = 30,
    autoMigrations = [
        AutoMigration(from = 24, to = 25),
        AutoMigration(from = 25, to = 26),
        AutoMigration(from = 26, to = 27),
        AutoMigration(from = 27, to = 28), // can remove this later
        AutoMigration(from = 28, to = 29), // can remove this later
        AutoMigration(from = 29, to = 30), // can remove this later
        AutoMigration(from = 27, to = 30),
    ],
)
@TypeConverters(TiviTypeConverters::class)
abstract class TiviRoomDatabase : RoomDatabase(), TiviDatabase
