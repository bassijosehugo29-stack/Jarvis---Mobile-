// SPDX-License-Identifier: GPL-3.0-or-later
package br.com.jarvis.voicea

internal object NativePrivacy {
    init {System.loadLibrary("jarvis_voice_privacy")}
    external fun installImpl():Boolean
}
