package br.com.jarvis.voicea

import org.junit.Assert.*
import org.junit.Test
import kotlin.math.*

class ExecutiveDspTest {
    @Test fun keepsNaturalPitchAndProducesFiniteBoundedPcm(){
        val sampleRate=22050;val frequency=140.0
        val input=FloatArray(sampleRate*2){(0.2*sin(2*PI*frequency*it/sampleRate)).toFloat()}
        val output=ExecutiveDsp.process(input)
        assertEquals(input.size,output.size);assertTrue(output.all{it.isFinite()&&abs(it)<=0.841f})
        fun crossings(values:FloatArray)=values.indices.drop(1).count{values[it-1]<=0&&values[it]>0}
        assertTrue(abs(crossings(input)-crossings(output))<=1)
        assertEquals(-20.0,ExecutiveDsp.integratedLoudness(output),0.3)
    }
    @Test fun silenceIsNotAmplifiedAndNoiseCannotCreateSpeech(){
        val silence=FloatArray(22050)
        assertTrue(ExecutiveDsp.process(silence).all{it==0f})
        assertEquals(Double.NEGATIVE_INFINITY,ExecutiveDsp.integratedLoudness(silence),0.0)
        assertArrayEquals(FloatArray(0),ExecutiveDsp.process(FloatArray(0)),0f)
    }
    @Test fun rejectsInvalidAudioAndUnsupportedRate(){
        assertThrows(IllegalArgumentException::class.java){ExecutiveDsp.process(floatArrayOf(Float.NaN))}
        assertThrows(IllegalArgumentException::class.java){ExecutiveDsp.process(FloatArray(100),8000)}
    }
    @Test fun longLoudInputIsLimitedWithoutPitchOrLengthChanges(){
        val input=FloatArray(22050){if(it%150<75)0.99f else -0.99f}
        val output=ExecutiveDsp.process(input)
        assertEquals(input.size,output.size)
        assertTrue(output.maxOf{abs(it)}<=0.841f)
        assertTrue(output.any{abs(it)>0.01f})
    }
}
