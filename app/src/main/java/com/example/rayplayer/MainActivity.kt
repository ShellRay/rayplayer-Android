
package com.example.rayplayer

import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import java.io.File

class MainActivity : AppCompatActivity() {

    private val rayPlayer = RayPlayer()

    // 标记surface是否已就绪
    private var surfaceReady = false
    // 标记是否需要延迟启动
    private var pendingStart = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val surfaceView = findViewById<SurfaceView>(R.id.surfaceVideo)

        val btnInit = findViewById<Button>(R.id.btnInit)
        val btnPrepare = findViewById<Button>(R.id.btnPrepare)
        val btnStart = findViewById<Button>(R.id.btnStart)
        val btnStop = findViewById<Button>(R.id.btnStop)
        val btnRelease = findViewById<Button>(R.id.btnRelease)

        // 获取SurfaceHolder并设置回调 - 关键修复！
        val holder = surfaceView.holder
        holder.addCallback(object : SurfaceHolder.Callback {
            override fun surfaceCreated(holder: SurfaceHolder) {
                Log.d("Test", ">>> SurfaceCreated called!")
                surfaceReady = true

                surfaceView.setZOrderOnTop(true)
                // 如果用户在surface创建前就点了Start，现在启动
                if (pendingStart) {
                    Log.d("Test", ">>> Starting playback (deferred from pendingStart)")
                    pendingStart = false
                    startPlayback(holder.surface)
                }
            }

            override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
                Log.d("Test", ">>> SurfaceChanged: $width x $height, format=0x$format")
            }

            override fun surfaceDestroyed(holder: SurfaceHolder) {
                Log.d("Test", ">>> SurfaceDestroyed called!")
                surfaceReady = false
            }
        })

        btnInit.setOnClickListener {
            rayPlayer.nativeInit()
            Toast.makeText(this, "Player 已初始化", Toast.LENGTH_SHORT).show()
        }

        btnPrepare.setOnClickListener {
            val path = getTestVideoPath()
            if (path == null) {
                Toast.makeText(this, "未找到测试视频文件", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            rayPlayer.nativePrepare(path)
            Toast.makeText(this, "已设置数据源: $path", Toast.LENGTH_SHORT).show()
        }

        btnStart.setOnClickListener {
            Log.d("Test", ">>> Start button clicked, surfaceReady=$surfaceReady")

            if (surfaceReady) {
                // surface已经就绪，直接传递
                val currentHolder = surfaceView.holder
                val currentSurface = currentHolder.surface
                if (currentSurface != null) {
                    Log.d("Test", ">>> Surface is ready, passing to native layer")
                    startPlayback(currentSurface)
                } else {
                    Log.e("Test", ">>> Surface is null even though surfaceReady=true!")
                    pendingStart = true
                }
            } else {
                // surface还没就绪，标记待启动，等surfaceCreated时再启动
                Log.d("Test", ">>> Surface not ready yet, will start when surfaceCreated fires")
                pendingStart = true
            }
        }

        btnStop.setOnClickListener {
            rayPlayer.nativeStop()
            Toast.makeText(this, "停止播放", Toast.LENGTH_SHORT).show()
        }

        btnRelease.setOnClickListener {
            rayPlayer.nativeRelease()
            Toast.makeText(this, "播放器已释放", Toast.LENGTH_SHORT).show()
        }
    }

    private fun startPlayback(surface: android.view.Surface) {
        Log.d("Test", ">>> nativeSetSurface called with surface=$surface")
        rayPlayer.nativeSetSurface(surface)
        Log.d("Test", ">>> nativeStart called")
        rayPlayer.nativeStart()
    }

    private fun getTestVideoPath(): String? {
        val destFile = File(cacheDir, "test1.mp4")
        Log.d("Test", "test1.mp4 size: ${destFile.length()} bytes")
        if (destFile.exists()) {
            return destFile.absolutePath
        }

        return try {
            val inputStream = assets.open("test1.mp4")
            destFile.outputStream().use { output ->
                inputStream.copyTo(output, bufferSize = 8192)
            }
            inputStream.close()
            destFile.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        rayPlayer.nativeRelease()
    }
}