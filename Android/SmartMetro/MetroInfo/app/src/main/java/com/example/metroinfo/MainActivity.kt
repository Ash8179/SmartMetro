package com.example.metroinfo

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import android.os.Environment
import androidx.annotation.RequiresApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path

class MainActivity : AppCompatActivity() {

    private val REQUEST_CODE_PERMISSION = 100
    private val REQUEST_CODE_MANAGE_EXTERNAL_STORAGE = 101

    // 创建 Retrofit API 接口实例
    object RetrofitInstance {
        private const val BASE_URL = "http://192.168.135.244:5000/" // 请替换为你的后端地址，注意协议 http://

        private val retrofit by lazy {
            Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())  // 选择 JSON 转换器
                .build()
        }

        val api: MetroApiService by lazy {
            retrofit.create(MetroApiService::class.java)
        }
    }

    // 定义 Retrofit API 接口
    interface MetroApiService {
        @GET("/api/metro/arrival-time/{fromStationCn}/{lineId}")
        suspend fun getArrivalTime(
            @Path("fromStationCn") fromStationCn: String,
            @Path("lineId") lineId: Int
        ): Response<Map<String, String>> // 返回 Map 类型的 JSON 格式
    }

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // 绑定 UI 组件
        val editTextStation = findViewById<EditText>(R.id.editTextStation)
        val buttonQuery = findViewById<Button>(R.id.buttonQuery)
        val textViewResult = findViewById<TextView>(R.id.textViewResult)

        // 请求外部存储权限
        val permission = Manifest.permission.READ_EXTERNAL_STORAGE
        if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(permission), REQUEST_CODE_PERMISSION)
        }

        // 设置按钮点击事件
        buttonQuery.setOnClickListener {
            val stationName = editTextStation.text.toString().trim() // 获取用户输入的站名
            if (stationName.isNotEmpty()) {
                val lineId = 1 // 假设 LineId 为 1，实际应用中可以获取 LineId
                getArrivalTime(stationName, lineId, textViewResult)
            } else {
                textViewResult.text = "请输入地铁站名称！"
            }
        }
    }

    // 调用 Retrofit API 获取列车到达时间
    private fun getArrivalTime(stationName: String, lineId: Int, textView: TextView) {
        CoroutineScope(Dispatchers.Main).launch {
            try {
                val response = RetrofitInstance.api.getArrivalTime(stationName, lineId)
                if (response.isSuccessful) {
                    val arrivalTime = response.body()?.get("arrivalTime") // 从返回的 Map 中获取 arrivalTime
                    textView.text = "下一班列车到达时间：$arrivalTime"
                } else {
                    textView.text = "查询失败，请重试。"
                }
            } catch (e: Exception) {
                textView.text = "网络请求失败，请检查网络连接。"
            }
        }
    }

    // 处理权限请求结果
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_PERMISSION) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                println("Permission granted!")
            } else {
                println("Permission denied!")
            }
        }
    }

    // 处理外部存储访问权限请求
    @RequiresApi(Build.VERSION_CODES.R)
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE_MANAGE_EXTERNAL_STORAGE) {
            if (Environment.isExternalStorageManager()) {
                println("Access granted to all files")
            } else {
                println("Access denied to all files")
            }
        }
    }
}

