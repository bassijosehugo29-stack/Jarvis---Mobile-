// SPDX-License-Identifier: GPL-3.0-or-later
package br.com.jarvis.voicea;

import kotlin.jvm.functions.Function1;

/** Stable boxed JNI signature required by sherpa-onnx's CallCallback.
 * Kotlin 2's invokedynamic lambdas expose invoke(Object), whereas JNI looks up
 * invoke(float[]): Integer. Keep this concrete adapter at the native boundary.
 */
public final class NativePcmCallback implements Function1<float[], Integer> {
    private final Function1<float[], Integer> consumer;

    public NativePcmCallback(Function1<float[], Integer> consumer) {
        this.consumer = consumer;
    }

    @Override public Integer invoke(float[] samples) {
        return consumer.invoke(samples);
    }
}
