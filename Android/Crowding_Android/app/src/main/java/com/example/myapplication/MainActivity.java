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

        // 模拟器访问本地后端
        String url = "http://10.0.2.2:5001/api/crowding?line_id=" + lineId +
                "&line_number=" + lineNumber +
                "&line_carriage=" + carriage;

        StringRequest request = new StringRequest(
                Request.Method.GET, url,
                response -> {
                    try {
                        JSONObject json = new JSONObject(response);
                        JSONObject data = json.getJSONObject("data");
                        String result = "线路ID: " + data.getString("line_id") + "\n" +
                                "车次: " + data.getString("line_number") + "\n" +
                                "车厢: " + data.getString("line_carriage") + "\n" +
                                "人数: " + data.getInt("person_num") + "\n" +
                                "拥挤度: " + getCrowdLevel(data.getInt("crowd_level"));
                        tvResult.setText(result);
                    } catch (Exception e) {
                        tvResult.setText("解析错误: " + e.getMessage());
                    }
                },
                error -> tvResult.setText("请求失败: " + error.getMessage())
        );
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