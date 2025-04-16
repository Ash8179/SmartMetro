from flask import jsonify, request
import logging
import mysql.connector
from datetime import datetime, timedelta
import random

logger = logging.getLogger(__name__)

# 数据库配置
db_config = {
    'host': 'localhost',
    'database': 'smart_metro',
    'user': 'root',
    'password': 'Zz13193515431'
}

def get_connection():
    return mysql.connector.connect(**db_config)

def register_routes(app, station_graph):
    @app.route('/api/route', methods=['POST'])
    def find_route():
        try:
            data = request.get_json()
            start_station = data.get('start_station')
            end_station = data.get('end_station')
            
            if not start_station or not end_station:
                return jsonify({'error': 'Missing start_station or end_station'}), 400
            
            # 使用 Dijkstra 算法查找最短路径
            result = station_graph.find_shortest_path(start_station, end_station)
            
            if not result:
                return jsonify({'error': 'No path found'}), 404
            
            return jsonify(result)
            
        except Exception as e:
            logger.error(f"Error in find_route: {str(e)}")
            return jsonify({'error': str(e)}), 500

    @app.route('/api/stations', methods=['GET'])
    def get_stations():
        try:
            conn = get_connection()
            cursor = conn.cursor(dictionary=True)
            
            # 从 dijkstra 表获取所有唯一的站点
            cursor.execute("""
                SELECT DISTINCT from_station as station_id, from_station_cn as name_cn, from_station_en as name_en
                FROM dijkstra
                UNION
                SELECT DISTINCT to_station as station_id, to_station_cn as name_cn, to_station_en as name_en
                FROM dijkstra
            """)
            
            stations = cursor.fetchall()
            cursor.close()
            conn.close()
            
            return jsonify(stations)
            
        except Exception as e:
            logger.error(f"Error in get_stations: {str(e)}")
            return jsonify({'error': str(e)}), 500

    @app.route('/api/lines', methods=['GET'])
    def get_lines():
        try:
            conn = get_connection()
            cursor = conn.cursor(dictionary=True)
            
            # 从 dijkstra 表获取所有唯一的线路
            cursor.execute("""
                SELECT DISTINCT line_id, line_id as name_cn, line_id as name_en
                FROM dijkstra
            """)
            
            lines = cursor.fetchall()
            cursor.close()
            conn.close()
            
            return jsonify(lines)
            
        except Exception as e:
            logger.error(f"Error in get_lines: {str(e)}")
            return jsonify({'error': str(e)}), 500

    @app.route('/api/metro/nearest-stations', methods=['GET'])
    def get_nearest_stations():
        try:
            latitude = float(request.args.get('latitude'))
            longitude = float(request.args.get('longitude'))
            
            if latitude is None or longitude is None:
                return jsonify({'error': 'Missing latitude or longitude'}), 400
            
            conn = get_connection()
            cursor = conn.cursor(dictionary=True)
            
            # 使用 Haversine 公式计算距离并找出最近的站点
            cursor.execute("""
                SELECT 
                    us.station_id,
                    us.name_cn as name_cn,
                    us.name_en as name_en,
                    us.line_ids as associated_lines,
                    (
                        6371 * acos(
                            cos(radians(%s)) * cos(radians(us.latitude)) *
                            cos(radians(us.longitude) - radians(%s)) +
                            sin(radians(%s)) * sin(radians(us.latitude))
                        )
                    ) AS distance
                FROM unique_stations us
                WHERE us.latitude IS NOT NULL AND us.longitude IS NOT NULL
                ORDER BY distance
                LIMIT 5
            """, (latitude, longitude, latitude))
            
            stations = cursor.fetchall()
            cursor.close()
            conn.close()
            
            return jsonify(stations)
            
        except Exception as e:
            logger.error(f"Error in get_nearest_stations: {str(e)}")
            return jsonify({'error': str(e)}), 500

    @app.route('/api/arrival-time/line/<int:line_id>', methods=['GET'])
    def get_line_stations_arrival_time(line_id):
        try:
            conn = get_connection()
            cursor = conn.cursor(dictionary=True)

            # 获取当前时间
            now = datetime.now()
            current_time = now.time()
            current_date = now.date()

            # 获取该线路所有站点的首班车时间
            query = """
                SELECT 
                    ls.line_id,
                    ls.station_id,
                    ls.station_name,
                    ls.direction_desc,
                    ls.sequence,
                    TIME_FORMAT(fta.first_arrival_time, '%H:%i:%s') as first_arrival_time,
                    CONCAT(ls.line_id, '号线') as line_name
                FROM line_station_sequence ls
                LEFT JOIN first_train_arrival fta ON ls.line_id = fta.line_id 
                    AND ls.station_id = fta.station_id
                    AND ls.direction_desc = fta.direction_desc
                WHERE ls.line_id = %(line_id)s
                ORDER BY ls.sequence
            """
            cursor.execute(query, {'line_id': line_id})

            arrivals = cursor.fetchall()
            cursor.close()
            conn.close()

            for arrival in arrivals:
                logger.info(f"Processing arrival: {arrival}")
                if arrival['first_arrival_time']:
                    first_time_str = arrival['first_arrival_time']
                    first_time = datetime.strptime(first_time_str, '%H:%M:%S').time()

                    logger.info(f"First time string: {first_time_str}")
                    logger.info(f"Current time: {current_time}")

                    # 计算时间差（分钟）
                    first_datetime = datetime.combine(current_date, first_time)
                    current_datetime = datetime.combine(current_date, current_time)

                    if first_time < current_time:
                        # 首班车已过，计算到下一班车的时间
                        minutes_passed = int((current_datetime - first_datetime).total_seconds() / 60)
                        next_arrival = (minutes_passed // 5 + 1) * 5
                        arrival['next_arrival_time'] = (first_datetime + timedelta(minutes=next_arrival)).strftime(
                            '%H:%M:%S')
                        arrival['minutes_remaining'] = next_arrival - minutes_passed
                    else:
                        # 首班车还没到
                        minutes_remaining = int((first_datetime - current_datetime).total_seconds() / 60)
                        arrival['next_arrival_time'] = first_time_str
                        arrival['minutes_remaining'] = minutes_remaining
                else:
                    arrival['next_arrival_time'] = None
                    arrival['minutes_remaining'] = 0

            return jsonify(arrivals)

        except Exception as e:
            logger.error(f"Error in get_line_stations_arrival_time: {str(e)}")
            return jsonify({'error': str(e)}), 500

    @app.route('/api/arrival-time/search', methods=['GET'])
    def search_stations():
        try:
            query = request.args.get('q', '')
            if not query:
                return jsonify([])

            conn = get_connection()
            cursor = conn.cursor(dictionary=True)

            # 搜索站点
            search_query = f"%{query}%"
            cursor.execute("""
                SELECT DISTINCT
                    f.line_id,
                    f.station_id,
                    f.station_name,
                    f.direction_desc,
                    TIME_FORMAT(f.first_arrival_time, '%H:%i:%s') as first_arrival_time,
                    COALESCE(TIMESTAMPDIFF(MINUTE, 
                        TIMESTAMP(CURRENT_DATE(), f.first_arrival_time),
                        CURRENT_TIMESTAMP()), 0) as minutes_passed,
                    CONCAT(f.line_id, '号线') as line_name
                FROM first_train_arrival f
                WHERE f.station_name LIKE %(search_query)s
                LIMIT 10
            """, {'search_query': search_query})

            arrivals = cursor.fetchall()
            cursor.close()
            conn.close()

            # 处理到达时间数据
            for arrival in arrivals:
                logger.info(f"Processing arrival: {arrival}")
                minutes_passed = int(float(arrival['minutes_passed']))
                logger.info(f"Minutes passed: {minutes_passed}")

                if minutes_passed < 0:
                    # 首班车还没到
                    arrival['next_arrival_time'] = arrival['first_arrival_time']
                    arrival['minutes_remaining'] = abs(minutes_passed)
                else:
                    # 首班车已过，计算到下一班车的时间
                    next_arrival = (minutes_passed // 5 + 1) * 5
                    try:
                        # 将字符串时间转换为time对象
                        first_time = datetime.strptime(arrival['first_arrival_time'], '%H:%M:%S').time()
                        first_time_dt = datetime.combine(datetime.now().date(), first_time)
                        next_time = first_time_dt + timedelta(minutes=next_arrival)
                        arrival['next_arrival_time'] = next_time.strftime('%H:%M:%S')
                        arrival['minutes_remaining'] = next_arrival - minutes_passed
                    except Exception as e:
                        logger.error(f"Error processing time: {str(e)}")
                        arrival['next_arrival_time'] = arrival['first_arrival_time']
                        arrival['minutes_remaining'] = 0

                    # 如果剩余时间小于0，说明是明天的首班车
                    if arrival['minutes_remaining'] < 0:
                        arrival['minutes_remaining'] = 24 * 60 + arrival['minutes_remaining']

            return jsonify(arrivals)

        except Exception as e:
            logger.error(f"Error in search_stations: {str(e)}")
            return jsonify({'error': str(e)}), 500