/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.compose.ui.demos.focus

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color.Companion.LightGray
import androidx.compose.ui.graphics.Color.Companion.White
import androidx.compose.ui.platform.AmbientWindowManager
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup

@Composable
fun FocusInPopup() {
    var showPopup by remember { mutableStateOf(false) }
    var mainText by remember { mutableStateOf(TextFieldValue("Enter Value")) }
    var popupText by remember { mutableStateOf(TextFieldValue("Enter Value")) }
    val windowManager = AmbientWindowManager.current

    Column(Modifier.background(if (windowManager.isWindowFocused) White else LightGray)) {
        Text("Click the button to show the popup. Click outside the popup to dismiss it.")
        Spacer(Modifier.height(10.dp))
        Button(onClick = { showPopup = true }) {
            Text("Show Popup")
        }

        Spacer(Modifier.height(50.dp))

        Text("Click this text field to bring the main app in focus.")
        TextField(value = mainText, onValueChange = { mainText = it })
        FocusStatus()

        if (showPopup) {
            Popup(
                alignment = Alignment.Center,
                isFocusable = true,
                onDismissRequest = { showPopup = false }
            ) {
                Column(Modifier.background(White)) {
                    Text("Click this text field to bring the popup in focus")
                    TextField(value = popupText, onValueChange = { popupText = it })
                    FocusStatus()
                }
            }
        }
    }
}

@Composable
private fun FocusStatus() {
    val windowManager = AmbientWindowManager.current
    Text("Status: Window ${if (windowManager.isWindowFocused) "is" else "is not"} focused.")
}