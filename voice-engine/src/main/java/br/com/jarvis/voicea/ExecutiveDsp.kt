// SPDX-License-Identifier: GPL-3.0-or-later
package br.com.jarvis.voicea

import kotlin.math.*

/** Voice A: 65 Hz high-pass, +1 dB at 170 Hz, gentle compression, gated -20 LUFS.
 * All processing preserves sample rate and fundamental pitch; never time-stretches/resamples.
 */
object ExecutiveDsp {
    fun process(input:FloatArray,sampleRate:Int=22050):FloatArray {
        require(sampleRate==22050)
        if(input.isEmpty())return input
        require(input.all{it.isFinite()})
        val high=BiQuad.highPass(65.0,0.70710678,sampleRate)
        val warmth=BiQuad.peak(170.0,0.8,1.0,sampleRate)
        val attack=exp(-1.0/(sampleRate*0.015));val release=exp(-1.0/(sampleRate*0.120))
        var envelope=0.0
        val output=FloatArray(input.size){n->
            val x=warmth.apply(high.apply(input[n].toDouble()))
            val level=abs(x);val coefficient=if(level>envelope)attack else release
            envelope=coefficient*envelope+(1-coefficient)*level
            val gain=if(envelope>0.125)(envelope/0.125).pow(1.0/1.5-1.0)else 1.0
            (x*gain).toFloat()
        }
        val loudness=integratedLoudness(output,sampleRate)
        if(loudness.isFinite()){
            var gain=10.0.pow((-20-loudness)/20).coerceIn(0.05,10.0)
            val peak=output.maxOf{abs(it.toDouble())}
            if(peak*gain>0.84)gain=0.84/peak
            output.indices.forEach{output[it]=(output[it]*gain).toFloat()}
        }
        return output
    }
    fun integratedLoudness(input:FloatArray,sampleRate:Int=22050):Double {
        if(input.isEmpty())return Double.NEGATIVE_INFINITY
        val shelf=BiQuad.shelf(1681.974450955533,3.999843853973347,0.7071752369554196,sampleRate)
        val high=BiQuad.highPass(38.13547087602444,0.5003270373238773,sampleRate)
        val weighted=DoubleArray(input.size){high.apply(shelf.apply(input[it].toDouble())).let{x->x*x}}
        val size=minOf(input.size,(sampleRate*0.4).toInt());val step=(sampleRate*0.1).toInt()
        val prefix=DoubleArray(weighted.size+1);weighted.indices.forEach{prefix[it+1]=prefix[it]+weighted[it]}
        val energies=mutableListOf<Double>()
        for(start in 0..weighted.size-size step step){val energy=(prefix[start+size]-prefix[start])/size
            if(energy>0&&-0.691+10*log10(energy)>-70)energies+=energy}
        if(energies.isEmpty())return Double.NEGATIVE_INFINITY
        val gate=energies.average()/10
        return -0.691+10*log10(energies.filter{it>=gate}.average())
    }
    private class BiQuad(val b0:Double,val b1:Double,val b2:Double,val a1:Double,val a2:Double){
        private var x1=0.0;private var x2=0.0;private var y1=0.0;private var y2=0.0
        fun apply(x:Double):Double{val y=b0*x+b1*x1+b2*x2-a1*y1-a2*y2;x2=x1;x1=x;y2=y1;y1=y;return y}
        companion object {
            fun highPass(f:Double,q:Double,sr:Int):BiQuad{
                val w=2*PI*f/sr;val c=cos(w);val a=sin(w)/(2*q);val d=1+a
                return BiQuad((1+c)/2/d,-(1+c)/d,(1+c)/2/d,-2*c/d,(1-a)/d)
            }
            fun peak(f:Double,q:Double,db:Double,sr:Int):BiQuad{
                val w=2*PI*f/sr;val c=cos(w);val alpha=sin(w)/(2*q);val a=10.0.pow(db/40);val d=1+alpha/a
                return BiQuad((1+alpha*a)/d,-2*c/d,(1-alpha*a)/d,-2*c/d,(1-alpha/a)/d)
            }
            fun shelf(f:Double,db:Double,q:Double,sr:Int):BiQuad{
                val w=2*PI*f/sr;val c=cos(w);val a=10.0.pow(db/40);val alpha=sin(w)/(2*q);val root=2*sqrt(a)*alpha
                val d=(a+1)-(a-1)*c+root
                return BiQuad(a*((a+1)+(a-1)*c+root)/d,-2*a*((a-1)+(a+1)*c)/d,a*((a+1)+(a-1)*c-root)/d,
                    2*((a-1)-(a+1)*c)/d,((a+1)-(a-1)*c-root)/d)
            }
        }
    }
}
