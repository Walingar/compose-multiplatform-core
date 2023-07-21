/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.compose.ui.platform

internal actual class AtomicInt actual constructor(value_: Int) {
    private var delegate: Int = value_

    actual fun addAndGet(delta: Int): Int {
        delegate += delta
        return delegate
    }
    actual fun compareAndSet(expected: Int, new: Int): Boolean {
        return if (delegate == expected) {
            delegate = new
            true
        } else {
            false
        }
    }
}

internal actual fun Any.nativeClass(): Any = this::class

internal actual fun simpleIdentityToString(obj: Any, name: String?): String {
    val className = name ?: "<object>"
    return "$className@${obj.hashCode()}"
}
