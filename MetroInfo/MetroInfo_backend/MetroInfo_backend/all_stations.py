from flask import Blueprint, jsonify
import mysql.connector

# 创建蓝图
app = Blueprint('stations', __name__, url_prefix='')

# 数据库配置
db_config = {
    'host': 'localhost',
    'database': 'smart_metro',
    'user': 'root',
    'password': 'Zz13193515431',
}

def get_stations():
    """从数据库获取 line 和 all_stations 列的数据"""
    try:
        conn = mysql.connector.connect(**db_config)
        cursor = conn.cursor(dictionary=True)
        cursor.execute("SELECT line, all_stations FROM All_Stations")
        stations = cursor.fetchall()
        cursor.close()
        conn.close()
        return stations
    except mysql.connector.Error as err:
        print(f"数据库错误: {err}")
        return []

@app.route('/allstations', methods=['GET'])
def fetch_stations():
    """API 端点，返回所有地铁线路及其站点"""
    data = get_stations()
    return jsonify(data)
