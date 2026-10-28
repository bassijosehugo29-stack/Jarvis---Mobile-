// SPDX-License-Identifier: GPL-3.0-or-later
package br.com.jarvis.voicea

import org.junit.Assert.*
import org.junit.Test

class NativePcmCallbackTest {
    @Test fun nativeLookupFindsBoxedFloatArraySignatureAndPropagatesCancellation() {
        var received: FloatArray? = null
        val callback = NativePcmCallback { samples -> received = samples; 0 }
        val method = callback.javaClass.getMethod("invoke", FloatArray::class.java)
        assertEquals(java.lang.Integer::class.java, method.returnType)
        val samples = floatArrayOf(0.2f, -0.1f)
        assertEquals(0, method.invoke(callback, samples))
        assertSame(samples, received)
    }
}
