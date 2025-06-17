package com.example.myapplication;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import org.json.JSONException;
import org.json.JSONObject;

public class MainActivity extends AppCompatActivity {

    private EditText etLineId;
    private EditText etLineNumber;
    private EditText etCarriage;
    private TextView tvResult;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        // 边缘到边缘布局处理
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // 绑定UI组件
        etLineId = findViewById(R.id.etLineId);
        etLineNumber = findViewById(R.id.etLineNumber);
        etCarriage = findViewById(R.id.etCarriage);
        tvResult = findViewById(R.id.tvResult);

        Button btnQuery = findViewById(R.id.btnQuery);
        btnQuery.setOnClickListener(v -> queryCrowding());
    }

    private void queryCrowding() {
        String lineId = etLineId.getText().toString();
        String lineNumber = etLineNumber.getText().toString();
        String carriage = etCarriage.getText().toString();

        // API服务器地址
        // 模拟器使用10.0.2.2访问主机的5001端口
        // 真机测试请修改为实际的服务器IP地址
        String serverIp = "10.0.2.2"; // 可以根据需要修改为实际的服务器IP
        
        String url = "http://" + serverIp + ":5001/api/crowding?line_id=" + lineId +
                "&line_number=" + lineNumber +
                "&line_carriage=" + carriage;

        // 显示正在查询的提示
        tvResult.setText("正在查询...");
        
        StringRequest request = new StringRequest(
                Request.Method.GET, url,
                response -> {
                    try {
                        JSONObject json = new JSONObject(response);
                        
                        // 先检查API返回状态
                        String status = json.getString("status");
                        if (!"success".equals(status)) {
                            tvResult.setText("查询失败: " + json.optString("message", "未知错误"));
                            return;
                        }
                        
                        // 检查data是否为null
                        if (json.isNull("data")) {
                            tvResult.setText("未找到匹配的数据");
                            return;
                        }
                        
                        JSONObject data = json.getJSONObject("data");
                        String result = "线路ID: " + data.getString("line_id") + "\n" +
                                "车次: " + data.getString("line_number") + "\n" +
                                "车厢: " + data.getString("line_carriage") + "\n" +
                                "人数: " + data.getInt("person_num") + "\n" +
                                "拥挤度: " + getCrowdLevel(data.getInt("crowd_level"));
                        tvResult.setText(result);
                    } catch (JSONException e) {
                        tvResult.setText("解析错误: " + e.getMessage());
                        e.printStackTrace(); // 在日志中打印详细错误
                    }
                },
                error -> {
                    tvResult.setText("请求失败: " + error.getMessage());
                    error.printStackTrace(); // 在日志中打印详细错误
                }
        );
        
        // 设置请求超时时间
        request.setRetryPolicy(new com.android.volley.DefaultRetryPolicy(
                10000, // 超时时间10秒
                1,     // 最大重试次数
                1.0f   // 重试倍增因子
        ));
        
        Volley.newRequestQueue(this).add(request);
    }

    private String getCrowdLevel(int level) {
        switch (level) {
            case 0:
                return "宽松";
            case 1:
                return "适中";
            case 2:
                return "拥挤";
            default:
                return "未知";
        }
    }
}