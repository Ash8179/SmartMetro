import math
import logging
from flask import Flask, request, jsonify
import mysql.connector

import heapq

app = Flask(__name__)

# 数据库配置
db_config = {
    'host': 'localhost',
    'database': 'smartmetro',
    'user': 'root',
    'password': '234800zzr',
    'port':3306}



@app.route('/crowding', methods=['POST'])
def post_line_crowding():
    """
       POST /crowding
       提交车厢拥挤度数据

       Parameters:
       - line_id: int 线路ID (required)
       - line_number: int 线路编号 (required)
       - line_carriage: int 车厢号 (required)
       - person_num: int 当前人数 (required)

       Returns:
       {
           "status": "success/error",
           "data": {
               "line_id": int,
               # ...其他字段
           }
       }
       """
    try:
        data = request.json
        line_id = int(data['line_id'])  # 使用 data 字典来提取参数
        line_number = int(data['line_number'])
        line_carriage = int(data['line_carriage'])
        person_num = int(data['person_num'])

        crowd_level = 0 if person_num < 15 else 2 if person_num > 30 else 1
        print(crowd_level)


    except (KeyError, ValueError) as e:
        return jsonify({'error': f'Invalid parameters: {str(e)}'}), 400

    conn = None
    try:
        #conn = connection_pool.get_connection()
        conn = mysql.connector.connect(
            host="localhost",
            user="root",
            password="234800zzr",
            database = "smartmetro"
        )
        cursor = conn.cursor(dictionary=True)
        sql = "insert into line_crowding (line_id, line_number, line_carriage, person_num, timestamp, crowd_level) values (%s,%s,%s,%s,now(),%s)"
        val = (line_id, line_number, line_carriage, person_num, crowd_level)

        cursor.execute(sql,val)
        #stations = cursor.fetchall()
        conn.commit()  # 提交事务


        return jsonify({
            'user_location': 'success'
        })

    except Exception as e:

        app.logger.error(f"Error: {str(e)}")
        return jsonify({'error': str(e)}), 500
    finally:
        if conn and conn.is_connected():
            conn.close()

if __name__ == '__main__':

    app.run(host='0.0.0.0', port=5002, debug=True)