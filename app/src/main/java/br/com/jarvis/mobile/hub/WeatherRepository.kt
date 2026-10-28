package br.com.jarvis.mobile.hub

import org.json.JSONObject
import java.net.URLEncoder

data class WeatherCity(val name:String,val latitude:Double,val longitude:Double)
class WeatherRepository(private val storage:HubStorage,private val http:HttpTransport){
    suspend fun search(city:String):List<WeatherCity>{
        require(city.trim().length in 2..100)
        val response=http.get("https://geocoding-api.open-meteo.com/v1/search?name=${URLEncoder.encode(city.trim(),"UTF-8")}&count=5&language=pt&countryCode=BR")
        check(response.code==200);val result=JSONObject(response.body).optJSONArray("results")?:return emptyList()
        return(0 until result.length()).map{val o=result.getJSONObject(it);WeatherCity(o.getString("name")+o.optString("admin1").takeIf{it.isNotBlank()}?.let{", $it"}.orEmpty(),o.getDouble("latitude"),o.getDouble("longitude"))}
    }
    suspend fun configure(city:WeatherCity){require(city.latitude.isFinite()&&city.longitude.isFinite()&&city.latitude in -90.0..90.0&&city.longitude in -180.0..180.0)
        storage.put("city",JSONObject().put("name",city.name).put("lat",city.latitude).put("lon",city.longitude),storage.epoch());storage.remove("weather")}
    suspend fun disable(){storage.remove("city");storage.remove("weather")}
    suspend fun refresh(force:Boolean=false):WeatherSnapshot{
        val epoch=storage.epoch();val city=storage.get("city")?:return WeatherSnapshot()
        val saved=storage.get("weather")?.takeIf{it.optString("city")==city.getString("name")}
        val now=System.currentTimeMillis()
        fun snapshot(value:JSONObject,status:SourceStatus)=WeatherSnapshot(value.getString("city"),value.getDouble("low"),value.getDouble("high"),
            if(value.isNull("rain"))null else value.optInt("rain",-1).takeIf{it in 0..100},
            SourceInfo(status,"Previsão Open-Meteo · ${value.optString("date","data não informada")} · CC BY 4.0",value.getLong("time")))
        val currentDate=saved?.let{runCatching{java.time.LocalDate.now(java.time.ZoneId.of(it.optString("zone","America/Sao_Paulo"))).toString()}.getOrNull()}
        if(!force&&saved!=null&&now-saved.getLong("time")<3600000&&saved.optString("date")==currentDate)return snapshot(saved,SourceStatus.READY)
        return try{
            val reply=http.get("https://api.open-meteo.com/v1/forecast?latitude=${city.getDouble("lat")}&longitude=${city.getDouble("lon")}&daily=temperature_2m_min,temperature_2m_max,precipitation_probability_max&timezone=auto&forecast_days=1")
            check(reply.code==200);val response=JSONObject(reply.body);val daily=response.getJSONObject("daily")
            val low=daily.getJSONArray("temperature_2m_min").getDouble(0);val high=daily.getJSONArray("temperature_2m_max").getDouble(0)
            require(low.isFinite()&&high.isFinite()&&low in -100.0..80.0&&high in -100.0..80.0&&low<=high)
            val rain=daily.optJSONArray("precipitation_probability_max")?.let{if(it.isNull(0))null else it.optInt(0,-1).takeIf{v->v in 0..100}}
            val date=daily.getJSONArray("time").getString(0);java.time.LocalDate.parse(date)
            val data=JSONObject().put("city",city.getString("name")).put("low",low).put("high",high).put("rain",rain?:JSONObject.NULL)
                .put("date",date).put("zone",response.optString("timezone","America/Sao_Paulo")).put("time",now)
            if(storage.get("city")?.optString("name")==city.getString("name"))storage.put("weather",data,epoch)
            snapshot(data,SourceStatus.READY)
        }catch(e:kotlinx.coroutines.CancellationException){throw e}catch(_:Exception){saved?.let{snapshot(it,SourceStatus.STALE)}?:WeatherSnapshot(city=city.getString("name"),info=SourceInfo(SourceStatus.ERROR,"Previsão indisponível. Nenhum valor foi estimado pelo JARVIS."))}
    }
}
