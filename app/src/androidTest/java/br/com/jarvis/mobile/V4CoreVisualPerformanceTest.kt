package br.com.jarvis.mobile

import android.os.Debug
import android.os.Handler
import android.os.HandlerThread
import android.os.Process
import android.os.SystemClock
import android.view.FrameMetrics
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import br.com.jarvis.mobile.diagnostics.CoreShowcaseActivity
import br.com.jarvis.mobile.experience.*
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.Collections

@RunWith(AndroidJUnit4::class)
class V4CoreVisualPerformanceTest {
    @get:Rule val compose=createAndroidComposeRule<CoreShowcaseActivity>()
    @Test fun captureAllStatesAndMeasureActualWindowFramesAndProcessCpu() {
        val inst=InstrumentationRegistry.getInstrumentation()
        val thread=HandlerThread("v4-frame-metrics").apply{start()}
        val frames=Collections.synchronizedList(mutableListOf<Long>())
        val listener=android.view.Window.OnFrameMetricsAvailableListener{_,metrics,_->frames.add(metrics.getMetric(FrameMetrics.TOTAL_DURATION))}
        val results=JSONArray()
        compose.runOnUiThread{compose.activity.window.addOnFrameMetricsAvailableListener(listener,Handler(thread.looper))}
        try {
            for(phase in CorePhase.entries){
                compose.runOnUiThread{compose.activity.phase=phase;compose.activity.mode=OperationMode.CINEMATIC}
                SystemClock.sleep(1300)
                saveUiScreenshot("jarvis-v4-${phase.name.lowercase()}.png",requireNotNull(inst.uiAutomation.takeScreenshot()))
                frames.clear();val start=SystemClock.elapsedRealtime();val cpu=Process.getElapsedCpuTime()
                SystemClock.sleep(2200)
                val elapsed=SystemClock.elapsedRealtime()-start;val usedCpu=Process.getElapsedCpuTime()-cpu
                val values=synchronized(frames){frames.toList().sorted()}
                fun percentile(q:Double)=if(values.isEmpty())JSONObject.NULL else values[((values.size-1)*q).toInt()]/1_000_000.0
                val memory=Debug.MemoryInfo();Debug.getMemoryInfo(memory)
                results.put(JSONObject().put("phase",phase.name).put("mode","CINEMATIC").put("elapsed_ms",elapsed)
                    .put("cpu_ms",usedCpu).put("cpu_one_core_percent",usedCpu*100.0/elapsed).put("pss_kb",memory.totalPss)
                    .put("frames",values.size).put("total_frame_p50_ms",percentile(.5)).put("total_frame_p95_ms",percentile(.95)))
                assertTrue("Expected actual rendered frames",values.isNotEmpty())
            }
            compose.runOnUiThread{compose.activity.phase=CorePhase.IDLE;compose.activity.mode=OperationMode.SILENT}
            SystemClock.sleep(500);frames.clear();val cpu=Process.getElapsedCpuTime();val start=SystemClock.elapsedRealtime()
            SystemClock.sleep(2500)
            results.put(JSONObject().put("phase","IDLE").put("mode","SILENT").put("elapsed_ms",SystemClock.elapsedRealtime()-start)
                .put("cpu_ms",Process.getElapsedCpuTime()-cpu).put("frames",frames.size).put("core_clock_scheduled",false))
            val uri=inst.targetContext.contentResolver.insert(android.provider.MediaStore.Downloads.EXTERNAL_CONTENT_URI,
                android.content.ContentValues().apply{put(android.provider.MediaStore.MediaColumns.DISPLAY_NAME,"jarvis-v4-render-metrics.json")
                    put(android.provider.MediaStore.MediaColumns.MIME_TYPE,"application/json");put(android.provider.MediaStore.MediaColumns.RELATIVE_PATH,"Download/JARVIS-testes")})!!
            inst.targetContext.contentResolver.openOutputStream(uri)!!.use{it.write(JSONObject().put("hardware","API35 x86_64 emulator")
                .put("scope","UI-only synthetic states; no microphone/TTS/model; not Galaxy measurements or battery energy")
                .put("results",results).toString(2).toByteArray())}
        } finally {compose.runOnUiThread{compose.activity.window.removeOnFrameMetricsAvailableListener(listener)};thread.quitSafely()}
    }
}
