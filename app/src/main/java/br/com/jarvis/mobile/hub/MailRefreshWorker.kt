package br.com.jarvis.mobile.hub

import android.content.Context
import androidx.work.*
import br.com.jarvis.mobile.JarvisApplication
import java.util.concurrent.TimeUnit

class MailRefreshWorker(context:Context,parameters:WorkerParameters):CoroutineWorker(context,parameters){
    override suspend fun doWork():Result{
        val data=(applicationContext as JarvisApplication).data
        return try{
            if(data.gmail.connectedAccount()==null)return Result.success()
            val result=data.gmail.refresh()
            when(result.info.status){
                SourceStatus.NEEDS_AUTH->{cancel(applicationContext);Result.success()}
                SourceStatus.ERROR,SourceStatus.STALE->Result.retry()
                else->Result.success()
            }
        }catch(e:kotlinx.coroutines.CancellationException){throw e}catch(_:Exception){Result.retry()}
    }
    companion object{
        fun schedule(context:Context){
            val request=PeriodicWorkRequestBuilder<MailRefreshWorker>(1,TimeUnit.HOURS,15,TimeUnit.MINUTES)
                .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).setRequiresBatteryNotLow(true).build())
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL,30,TimeUnit.MINUTES).build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork("jarvis-mail-read",ExistingPeriodicWorkPolicy.UPDATE,request)
        }
        fun cancel(context:Context){WorkManager.getInstance(context).cancelUniqueWork("jarvis-mail-read")}
    }
}
