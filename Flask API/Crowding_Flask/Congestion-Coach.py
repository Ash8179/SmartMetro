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
    POST /crowding?line_id=1&line_number=3&line_carriage=2&person_num=25
    """
    try:
        # 从URL参数获取数据
        line_id = int(request.args.get('line_id'))
        line_number = int(request.args.get('line_number'))
        line_carriage = int(request.args.get('line_carriage'))
        person_num = int(request.args.get('person_num'))
        
        # 计算拥挤等级
        crowd_level = 0 if person_num < 15 else 2 if person_num > 30 else 1
        
        # 数据库操作
        conn = mysql.connector.connect(**db_config)
        cursor = conn.cursor()
        sql = """INSERT INTO line_crowding 
                 (line_id, line_number, line_carriage, person_num, crowd_level) 
                 VALUES (%s, %s, %s, %s, %s)"""
        cursor.execute(sql, (line_id, line_number, line_carriage, person_num, crowd_level))
        conn.commit()
        
        return jsonify({
            'status': 'success',
            'data': {
                'line_id': line_id,
                'line_number': line_number,
                'line_carriage': line_carriage,
                'person_num': person_num,
                'crowd_level': crowd_level
            }
        })
        
    except Exception as e:
        return jsonify({'status': 'error', 'message': str(e)}), 500
    finally:
        if conn and conn.is_connected():
            conn.close()

@app.route('/api/crowding', methods=['GET'])
def get_crowding():
    """
    查询车厢最新拥挤度数据
    GET /api/crowding?line_id=1&line_number=3&line_carriage=2
    """
    conn = None
    try:
        line_id = request.args.get('line_id')
        line_number = request.args.get('line_number')
        line_carriage = request.args.get('line_carriage')

        conn = mysql.connector.connect(**db_config)
        cursor = conn.cursor(dictionary=True)

        sql = """
        SELECT line_id, line_number, line_carriage, person_num, crowd_level, timestamp 
        FROM line_crowding 
        WHERE line_id=%s AND line_number=%s AND line_carriage=%s
        ORDER BY timestamp DESC LIMIT 1
        """
        cursor.execute(sql, (line_id, line_number, line_carriage))
        result = cursor.fetchone()

        return jsonify({
            "status": "success",
            "data": result if result else {}
        })

    except Exception as e:
        return jsonify({"status": "error", "message": str(e)}), 500
    finally:
        if conn and conn.is_connected():
            conn.close()

if __name__ == '__main__':
    app.run(host='0.0.0.0', port=5004, debug=True)
