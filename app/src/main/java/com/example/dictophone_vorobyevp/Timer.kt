package com.example.dictophone_vorobyevp

import android.os.Handler
import android.os.Looper

class Timer(listener:OnTimerTickListener) {

    interface OnTimerTickListener{
        fun onTimerTick(duration: String)
    }
    private var handler = Handler(Looper.getMainLooper())
    private lateinit var runnable: Runnable

    private var duration = 0L //продолжительность
    private var delay = 100L //задержка 100мс

    init{
        runnable = Runnable{
            duration += delay
            handler.postDelayed(runnable, delay)
            listener.onTimerTick(format())
        }
    }

    fun start(){
        handler.postDelayed(runnable,delay)//когда вызываем эту функцию обработчик запускает исполняемый файл за 100мс

    }

    fun pause(){
        handler.removeCallbacks(runnable)
    }

    fun stop(){
        handler.removeCallbacks(runnable)
        duration = 0L
    }

    fun format(): String{
        val millis = duration % 1000
        val seconds = (duration / 1000) % 60
        val minutes = (duration / (1000 * 60)) % 60
        val hours = (duration / (1000 * 60 * 60))

        var formatted = if(hours > 0)
            "%02d:%02d:%02d.%02d".format(hours, minutes, seconds, millis/10)
        else
            "%02d:%02d.%02d".format(minutes, seconds, millis/10)

        return formatted

    }
}