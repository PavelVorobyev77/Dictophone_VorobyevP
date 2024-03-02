package com.example.dictophone_vorobyevp

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View

class WaveformView(context: Context?, attrs: AttributeSet?) : View(context, attrs) {

    private var paint = Paint()

    private var amplitudes = ArrayList<Float>()
    private var spikes = ArrayList<RectF>()

    private var radius = 6f
    private var w = 9f //ширина так называемого шипа

    private var sw = 0f //ширина экрана
    private var sh = 400f //высота экрана

    init{
        paint.color = Color.rgb(244, 81, 30)

        sw = resources.displayMetrics.widthPixels.toFloat()
    }

    fun addAmplitude(amp:Float){
        amplitudes.add(amp)

        var left = sw - w
        var top = 0f
        var right = left + w
        var bottom = amp

        spikes.add(RectF(left,top,right,bottom))

        invalidate()
    }
    override fun draw(canvas: Canvas) {
        super.draw(canvas)
        spikes.forEach{
            canvas?.drawRoundRect(it, radius, radius, paint)
        }
    }
}
