from flask import Flask, jsonify, request
import mysql.connector
from mysql.connector import pooling
import math
import logging

app = Flask(__name__)

# 数据库配置
db_config = {
    'host': 'localhost',
    'database': 'new_schema',
    'user': 'root',
    'password': 'zwy040103',
    'pool_size': 5
}

# 连接池初始化
connection_pool = pooling.MySQLConnectionPool(
    pool_name="metro_pool",
    **db_config
)

def haversine(lat1, lon1, lat2, lon2):
    R = 6371  # 地球半径(km)
    lat1, lon1, lat2, lon2 = map(math.radians, [lat1, lon1, lat2, lon2])
    dlat = lat2 - lat1
    dlon = lon2 - lon1
    a = math.sin(dlat/2)**2 + math.cos(lat1)*math.cos(lat2)*math.sin(dlon/2)**2
    return R * 2 * math.asin(math.sqrt(a))

@app.route('/nearest_stations', methods=['GET'])
def get_nearest_stations():
    try:
        user_lat = float(request.args.get('lat'))
        user_lng = float(request.args.get('lng'))
        max_results = int(request.args.get('limit', 5))
    except (KeyError, ValueError) as e:
        return jsonify({'error': f'Invalid parameters: {str(e)}'}), 400

    if not (30.5 <= user_lat <= 32.5) or not (120.8 <= user_lng <= 122.2):
        return jsonify({'error': 'Location outside Shanghai area'}), 400

    conn = None
    try:
        conn = connection_pool.get_connection()
        cursor = conn.cursor(dictionary=True)
        
        cursor.execute("""
            SELECT 
                stat_id,
                name_cn,
                name_en,
                latitude,
                longitude,
                travel_group,
                line,
                all_stations,
                associated_lines
            FROM station_map
            WHERE latitude IS NOT NULL 
              AND longitude IS NOT NULL
        """)
        stations = cursor.fetchall()

        # 使用travel_group作为唯一键
        seen_groups = {}
        for station in stations:
            try:
                travel_group = str(station.get('travel_group', '')).strip()
                if not travel_group:  # 如果travel_group为空，跳过去重
                    continue
                    
                if travel_group not in seen_groups:
                    # 计算距离
                    distance_km = haversine(
                        user_lat, user_lng,
                        float(station['latitude']),
                        float(station['longitude'])
                    )
                    
                    # 处理数据
                    all_stations = [
                        s.strip('"\' ')
                        for s in station['all_stations'].split(',')
                        if s.strip()
                    ]
                    
                    associated_lines = [
                        int(line.strip())
                        for line in (station['associated_lines'] or '').split(',')
                        if line.strip().isdigit()
                    ]
                    
                    seen_groups[travel_group] = {
                        "stat_id": station['stat_id'],
                        "name_cn": station['name_cn'],
                        "name_en": station['name_en'],
                        "travel_group": travel_group,
                        "distance_m": round(distance_km * 1000),
                        "line_info": {
                            "line": station['line'],
                            "all_stations": all_stations
                        },
                        "associated_lines": associated_lines
                    }

            except (KeyError, ValueError) as e:
                app.logger.error(f"Data error: {e}")
                continue

        # 按距离排序
        sorted_results = sorted(
            seen_groups.values(),
            key=lambda x: x['distance_m']
        )[:max_results]

        return jsonify({
            'user_location': {'lat': user_lat, 'lng': user_lng},
            'nearest_stations': sorted_results
        })

    except Exception as e:
        app.logger.error(f"Error: {str(e)}")
        return jsonify({'error': str(e)}), 500
    finally:
        if conn and conn.is_connected():
            conn.close()

if __name__ == '__main__':
    app.run(host='0.0.0.0', port=5001, debug=True)
