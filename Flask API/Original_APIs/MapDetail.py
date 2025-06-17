from flask import Flask, jsonify
import mysql.connector

app = Flask(__name__)

# 数据库配置
db_config = {
    'host': 'localhost',
    'database': 'new_schema',
    'user': 'root',
    'password': 'zwy040103',
}

@app.route('/smartmetro/map_details', methods=['GET'])
def get_map_details():
    try:
        # 连接数据库
        connection = mysql.connector.connect(**db_config)
        cursor = connection.cursor(dictionary=True)

        # 执行查询
        query = '''
            SELECT stat_id, name_cn, line, longitude, latitude, gao_lng, gao_lat
            FROM MapView
        '''
        cursor.execute(query)

        # 获取结果并封装为 JSON 格式
        results = cursor.fetchall()

        return jsonify(results), 200

    except mysql.connector.Error as err:
        return jsonify({'error': str(err)}), 500

    finally:
        if cursor:
            cursor.close()
        if connection:
            connection.close()

if __name__ == '__main__':
    app.run(host='127.0.0.1', port=5001)
