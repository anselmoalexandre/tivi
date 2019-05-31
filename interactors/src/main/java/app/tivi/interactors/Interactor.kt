/*
 * Copyright 2018 Google LLC
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

package app.tivi.interactors

import androidx.paging.PagedList
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.coroutines.rx2.asObservable
import kotlinx.coroutines.withContext

interface Interactor<in P> {
    val dispatcher: CoroutineDispatcher
    suspend operator fun invoke(params: P)
}

abstract class PagingInteractor<P : PagingInteractor.Parameters<T>, T> : SubjectInteractor<P, PagedList<T>>() {
    interface Parameters<T> {
        val pagingConfig: PagedList.Config
        val boundaryCallback: PagedList.BoundaryCallback<T>?
    }
}

abstract class ChannelInteractor<P, T : Any> : Interactor<P> {
    private val channel = Channel<T>()

    final override suspend fun invoke(params: P) {
        channel.offer(execute(params))
    }

    fun observe(): Observable<T> = channel.asObservable(dispatcher)

    protected abstract suspend fun execute(executeParams: P): T

    fun clear() {
        channel.close()
    }
}

abstract class SubjectInteractor<P : Any, T> {
    private val subject: PublishSubject<P> = PublishSubject.create()
    private val observable = subject.switchMap(::createObservable)

    operator fun invoke(params: P) = subject.onNext(params)

    protected abstract fun createObservable(params: P): Observable<T>

    fun observe(): Observable<T> = observable
}

fun <P> CoroutineScope.launchInteractor(interactor: Interactor<P>, param: P): Job {
    return launch(context = interactor.dispatcher, block = { interactor(param) })
}

suspend fun <P> Interactor<P>.execute(param: P) = withContext(context = dispatcher) {
    invoke(param)
}

fun CoroutineScope.launchInteractor(interactor: Interactor<Unit>) = launchInteractor(interactor, Unit)