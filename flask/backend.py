from flask import Flask, request, jsonify
import mysql.connector
from flask_cors import CORS

app = Flask(__name__)
CORS(app)

# 数据库配置
db_config = {
    'host': 'localhost',
    'user': 'root',
    'password': '234800zzr',  #
    'database': 'smartmetro'
}


@app.route('/api/crowding', methods=['GET'])
def get_crowding():
    """查询拥挤度数据"""
    try:
        line_id = request.args.get('line_id')
        line_number = request.args.get('line_number')
        line_carriage = request.args.get('line_carriage')

        conn = mysql.connector.connect(**db_config)
        cursor = conn.cursor(dictionary=True)

        sql = """
        SELECT line_id, line_number, line_carriage, person_num, crowd_level 
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

if __name__ == '__main__':
    app.run(host='0.0.0.0', port=5001)