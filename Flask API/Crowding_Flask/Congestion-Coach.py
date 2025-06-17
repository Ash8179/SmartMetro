import mysql.connector
from flask import Flask, request, jsonify
from flask_cors import CORS

app = Flask(__name__)
CORS(app)

# 数据库配置
db_config = {
    'host': 'localhost',
    'user': 'root',
    'password': 'zwy040103',
    'database': 'new_schema'
}

@app.route('/crowding', methods=['POST'])
def post_line_crowding():
    """
    提交车厢拥挤度数据（使用URL参数）
    POST /crowding?line_number=3&line_carriage=2&person_num=25&path_id=1
    """
    conn = None
    try:
        # 从URL参数获取数据
        line_number = int(request.args.get('line_number'))
        line_carriage = int(request.args.get('line_carriage'))
        person_num = int(request.args.get('person_num'))
        path_id = int(request.args.get('path_id', 0))  # 默认为0如果未提供
        
        # 计算拥挤等级
        crowd_level = 0 if person_num < 15 else 2 if person_num > 30 else 1
        
        # 数据库操作
        conn = mysql.connector.connect(**db_config)
        cursor = conn.cursor()
        sql = """INSERT INTO coach_congestion 
                 (line_number, line_carriage, person_num, crowd_level, path_id) 
                 VALUES (%s, %s, %s, %s, %s)"""
        cursor.execute(sql, (line_number, line_carriage, person_num, crowd_level, path_id))
        conn.commit()
        
        return jsonify({
            'status': 'success',
            'data': {
                'line_number': line_number,
                'line_carriage': line_carriage,
                'person_num': person_num,
                'crowd_level': crowd_level,
                'path_id': path_id  # 在响应中包含path_id
            }
        })
        
    except Exception as e:
        return jsonify({'status': 'error', 'message': str(e)}), 500
    finally:
        if conn and conn.is_connected():
            conn.close()

@app.route('/api/crowding/batch', methods=['GET'])
def get_batch_crowding():
    """
    批量查询某线路所有车厢的拥挤度数据（按path_id分类）
    GET /api/crowding/batch?line_number=3
    返回数据结构：
    {
        "status": "success",
        "data": {
            "path_1": [车厢1数据, 车厢2数据...],
            "path_2": [车厢1数据, 车厢2数据...],
            ...
        },
        "line_number": 3
    }
    """
    conn = None
    try:
        line_number = request.args.get('line_number')
        if not line_number:
            return jsonify({"status": "error", "message": "line_number is required"}), 400

        conn = mysql.connector.connect(**db_config)
        cursor = conn.cursor(dictionary=True)

        # 查询该线路各车厢最新数据（按path_id分组）
        sql = """
        SELECT cc1.* FROM coach_congestion cc1
        JOIN (
            SELECT line_carriage, path_id, MAX(timestamp) as max_time 
            FROM coach_congestion 
            WHERE line_number=%s 
            GROUP BY line_carriage, path_id
        ) cc2 ON cc1.line_carriage = cc2.line_carriage 
              AND cc1.path_id = cc2.path_id
              AND cc1.timestamp = cc2.max_time
        WHERE cc1.line_number=%s
        ORDER BY cc1.path_id, cc1.line_carriage
        """
        cursor.execute(sql, (line_number, line_number))
        results = cursor.fetchall()

        # 按path_id分类组织数据
        grouped_data = {}
        for row in results:
            path_id = f"path_{row['path_id']}"
            if path_id not in grouped_data:
                grouped_data[path_id] = []
            
            grouped_data[path_id].append({
                "crowd_level": row["crowd_level"],
                "line_carriage": row["line_carriage"],
                "person_num": row["person_num"],
                "timestamp": row["timestamp"].isoformat() if row["timestamp"] else None
            })

        return jsonify({
            "status": "success",
            "data": grouped_data,
            "line_number": int(line_number)
        })

    except Exception as e:
        return jsonify({"status": "error", "message": str(e)}), 500
    finally:
        if conn and conn.is_connected():
            conn.close()

if __name__ == '__main__':
    app.run(host='0.0.0.0', port=5004, debug=True)
