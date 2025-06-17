from flask import Flask, request, jsonify
import mysql.connector
from flask_cors import CORS
from functools import wraps
from contextlib import closing
from datetime import datetime

app = Flask(__name__)
CORS(app)

# 数据库配置
db_config = {
    'host': 'localhost',
    'user': 'root',
    'password': '234800zzr',
    'database': 'smartmetro',
    'pool_name': 'metro_pool',
    'pool_size': 5
}

#参数验证
def validate_params(func):
    @wraps(func)
    def wrapper(*args, **kwargs):
        try:
            line_id = int(request.args['line_id'])  # 强制转换并必填校验
            line_number = str(request.args['line_number']).strip()
            line_carriage = int(request.args['line_carriage'])

            if not line_number:  # 车次号非空校验
                raise ValueError("车次号不能为空")

            return func(line_id, line_number, line_carriage)
        except (KeyError, ValueError) as e:
            return jsonify({"status": "error", "message": f"参数错误: {str(e)}"}), 400

    return wrapper


@app.route('/api/crowding', methods=['GET'])
@validate_params
def get_crowding(line_id, line_number, line_carriage):
    try:
        with closing(mysql.connector.connect(**db_config)) as conn:
            with closing(conn.cursor(dictionary=True)) as cursor:
                sql = """
                SELECT line_id, line_number, line_carriage, 
                       person_num, crowd_level, timestamp
                FROM line_crowding 
                WHERE line_id=%s AND line_number=%s AND line_carriage=%s
                ORDER BY timestamp DESC LIMIT 1
                """
                cursor.execute(sql, (line_id, line_number, line_carriage))
                result = cursor.fetchone()

                return jsonify({
                    "status": "success",
                    "data": result if result else None,
                    "server_time": datetime.now().isoformat()
                })

    except mysql.connector.Error as e:
        return jsonify({"status": "error", "message": "数据库操作失败"}), 500
    except Exception as e:
        return jsonify({"status": "error", "message": str(e)}), 500


if __name__ == '__main__':
    app.run(host='0.0.0.0', port=5001, debug=False)  # 生产环境关闭debug