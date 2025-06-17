from flask import Flask, jsonify, request
import mysql.connector
from mysql.connector import pooling
import math
import logging
import heapq
from flask_cors import CORS
from collections import defaultdict
from datetime import datetime, timedelta
from functools import wraps

# 配置日志
logging.basicConfig(level=logging.DEBUG)
logger = logging.getLogger(__name__)

app = Flask(__name__)
CORS(app)

# MySQL 数据库配置
db_config = {
    'host': 'localhost',
    'database': 'new_schema',
    'user': 'root',
    'password': 'zwy040103',
    'pool_name': 'metro_pool',
    'pool_size': 5
}

# 连接池
connection_pool = pooling.MySQLConnectionPool(**db_config)

# ======= 公共函数 =======

def get_db_connection():
    return connection_pool.get_connection()

def haversine(lat1, lon1, lat2, lon2):
    R = 6371  # 地球半径km
    lat1, lon1, lat2, lon2 = map(math.radians, [lat1, lon1, lat2, lon2])
    dlat = lat2 - lat1
    dlon = lon2 - lon1
    a = math.sin(dlat/2)**2 + math.cos(lat1)*math.cos(lat2)*math.sin(dlon/2)**2
    return R * 2 * math.asin(math.sqrt(a))

def get_stations():
    """从数据库获取 line 和 all_stations 列的数据"""
    try:
        conn = get_db_connection()
        cursor = conn.cursor(dictionary=True)
        cursor.execute("SELECT line, all_stations FROM All_Stations")
        stations = cursor.fetchall()
        cursor.close()
        conn.close()
        return stations
    except mysql.connector.Error as err:
        print(f"数据库错误: {err}")
        return []

def calculate_travel_time(conn, path_id, from_station_id, to_station_id):
    with conn.cursor(buffered=True, dictionary=True) as cursor:
        cursor.execute("""
            SELECT station_order FROM station_path_order 
            WHERE path_id = %s AND station_id = %s
        """, (path_id, from_station_id))
        from_result = cursor.fetchone()

        cursor.execute("""
            SELECT station_order FROM station_path_order 
            WHERE path_id = %s AND station_id = %s
        """, (path_id, to_station_id))
        to_result = cursor.fetchone()

        if not from_result or not to_result:
            return None

        from_order = from_result['station_order']
        to_order = to_result['station_order']

        if from_order == to_order:
            return 0  # 已经在目标站

        total_time = 0
        # up方向，order递增
        if from_order < to_order:
            current_order = from_order
            while current_order < to_order:
                cursor.execute("""
                    SELECT travel_time 
                    FROM station_travel_time stt
                    JOIN station_path_order spo_from ON stt.from_station_id = spo_from.station_id
                    JOIN station_path_order spo_to ON stt.to_station_id = spo_to.station_id
                    WHERE spo_from.path_id = %s AND spo_to.path_id = %s
                    AND spo_from.station_order = %s AND spo_to.station_order = %s
                """, (path_id, path_id, current_order, current_order + 1))
                result = cursor.fetchone()
                if result:
                    total_time += result['travel_time']
                current_order += 1
        else:
            # down方向，order递减
            current_order = from_order
            while current_order > to_order:
                cursor.execute("""
                    SELECT travel_time 
                    FROM station_travel_time stt
                    JOIN station_path_order spo_from ON stt.from_station_id = spo_from.station_id
                    JOIN station_path_order spo_to ON stt.to_station_id = spo_to.station_id
                    WHERE spo_from.path_id = %s AND spo_to.path_id = %s
                    AND spo_from.station_order = %s AND spo_to.station_order = %s
                """, (path_id, path_id, current_order, current_order - 1))
                result = cursor.fetchone()
                if result:
                    total_time += result['travel_time']
                current_order -= 1

        return float(total_time) if total_time else None

def handle_errors(f):
    @wraps(f)
    def wrapped(*args, **kwargs):
        try:
            return f(*args, **kwargs)
        except Exception as e:
            logger.error(f"处理请求时发生错误: {str(e)}", exc_info=True)
            return jsonify({
                'success': False,
                'message': 'Internal Server Error',
                'error': str(e)
            }), 500
    return wrapped

# ======= 距离最近车站 API =======

@app.route('/smartmetro/nearest_stations', methods=['GET'])
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
        conn = get_db_connection()
        cursor = conn.cursor(dictionary=True)

        cursor.execute("""
            SELECT 
                stat_id, name_cn, name_en, latitude, longitude, 
                travel_group, line, all_stations, associated_lines
            FROM station_map
            WHERE latitude IS NOT NULL AND longitude IS NOT NULL
        """)
        stations = cursor.fetchall()

        seen_groups = {}
        for station in stations:
            try:
                travel_group = str(station.get('travel_group', '')).strip()
                if not travel_group:
                    continue
                if travel_group not in seen_groups:
                    distance_km = haversine(
                        user_lat, user_lng,
                        float(station['latitude']),
                        float(station['longitude'])
                    )
                    all_stations = [
                        s.strip('"\' ') for s in station['all_stations'].split(',') if s.strip()
                    ]
                    associated_lines = [
                        int(line.strip()) for line in (station['associated_lines'] or '').split(',')
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
        if conn:
            conn.close()

# ======= Dijkstra 最短路径 API =======

class StationGraph:
    def __init__(self):
        self.graph = {}
        self.name_to_groups = {}
        self.station_info = {}
        self.connections = {}  # 存储站点间的连接信息
        
    def _add_mapping(self, name_cn, name_en, group):
        name_cn = str(name_cn).strip() if name_cn else ''
        name_en = str(name_en).strip() if name_en else ''

        if name_cn:
            self.name_to_groups[name_cn.lower()] = group
        if name_en:
            self.name_to_groups[name_en.lower()] = group

        self.station_info[group] = {
            'cn': name_cn or f'UNKNOWN_CN_{group}',
            'en': name_en or f'UNKNOWN_EN_{group}'
        }
        
    def build_graph(self):
        conn = None
        cursor = None
        try:
            conn = get_db_connection()
            cursor = conn.cursor(dictionary=True)
            
            cursor.execute("""
                SELECT 
                    from_station_travel_group,
                    to_station_travel_group,
                    travel_time,
                    line_id,
                    from_station_cn,
                    to_station_cn,
                    from_station_en,
                    to_station_en
                FROM Dijkstra
                WHERE from_station_cn IS NOT NULL 
                  AND to_station_cn IS NOT NULL
            """)
            
            for row in cursor:
                from_node = row['from_station_travel_group']
                to_node = row['to_station_travel_group']
                line_id = row['line_id']
                time = row['travel_time']

                # 构建图结构
                if from_node not in self.graph:
                    self.graph[from_node] = []
                self.graph[from_node].append((to_node, time))
                
                # 存储连接信息
                self.connections[(from_node, to_node)] = {
                    'line_id': line_id,
                    'time': time
                }
                
                # 添加反向关系
                if to_node not in self.graph:
                    self.graph[to_node] = []
                self.graph[to_node].append((from_node, time))
                self.connections[(to_node, from_node)] = {
                    'line_id': line_id,
                    'time': time
                }
                
                # 添加名称映射
                self._add_mapping(row['from_station_cn'], row['from_station_en'], from_node)
                self._add_mapping(row['to_station_cn'], row['to_station_en'], to_node)
                    
        except Exception as e:
            logger.error(f"构建图结构时出错: {str(e)}", exc_info=True)
            raise
        finally:
            if cursor:
                cursor.close()
            if conn:
                conn.close()
    
    def find_travel_group(self, station_name):
        if not station_name:
            return None
        return self.name_to_groups.get(station_name.lower().strip())
    
    def dijkstra_shortest_path(self, start_group, end_group):
        try:
            if not start_group or not end_group:
                return None
            if start_group not in self.graph or end_group not in self.graph:
                return None
            
            # Heap (总时间, 当前节点, 路径, 当前线路)
            heap = [(0, start_group, [], None)]
            visited = set()
                
            while heap:
                (total_time, current, path, current_line) = heapq.heappop(heap)
            
                if current in visited:
                    continue
                    
                visited.add(current)
                path = path + [current]
            
                if current == end_group:
                    return {
                        'path': path,
                        'total_time': total_time
                    }
                
                for neighbor, time in self.graph.get(current, []):
                    if neighbor not in visited:
                        # 获取连接信息
                        conn_info = self.connections.get((current, neighbor))
                        if not conn_info:
                            continue
                            
                        new_line = conn_info['line_id']
                        new_total_time = total_time + time
                        
                        # 检查是否换乘（线路变更且不是初始状态）
                        if current_line is not None and new_line != current_line:
                            new_total_time += 5  # 增加换乘时间
                        
                        heapq.heappush(heap, (new_total_time, neighbor, path, new_line))
                            
            return None
        except Exception as e:
            logger.error(f"Dijkstra算法执行出错: {str(e)}", exc_info=True)
            return None

# 初始化图结构
station_graph = StationGraph()
try:
    station_graph.build_graph()
    logger.info("地铁图结构构建成功")
except Exception as e:
    logger.error(f"初始化地铁图结构失败: {str(e)}")
    raise

@app.route('/smartmetro/dijkstra', methods=['GET'])
@handle_errors
def find_shortest_path():
    from_station = request.args.get('from')
    to_station = request.args.get('to')
    
    if not from_station or not to_station:
        return jsonify({
            'success': False,
            'message': '必须提供起始站和目的站'
        }), 400
    
    start_group = station_graph.find_travel_group(from_station)
    end_group = station_graph.find_travel_group(to_station)
    
    if not start_group or not end_group:
        return jsonify({
            'success': False,
            'message': '无效的车站名称',
            'start_station': from_station,
            'end_station': to_station
        }), 400
        
    result = station_graph.dijkstra_shortest_path(start_group, end_group)
    
    if not result:
        return jsonify({
            'success': False,
            'message': '未找到路径',
            'start_station': station_graph.station_info.get(start_group, {}).get('cn'),
            'end_station': station_graph.station_info.get(end_group, {}).get('cn')
        }), 404
        
    # 构建完整路径信息
    formatted_path = []
    cumulative_time = 0
    previous_line = None

    for i in range(len(result['path']) - 1):
        current = result['path'][i]
        next_node = result['path'][i+1]
        
        # 获取连接信息
        conn_info = station_graph.connections.get((current, next_node))
        if not conn_info:
            continue
            
        line_id = conn_info['line_id']
        segment_time = conn_info['time']
        
        # 检查是否换乘
        if previous_line is not None and line_id != previous_line:
            # 添加换乘时间
            cumulative_time += 5
            formatted_path.append({
                'transfer': True,
                'transfer_time': 5,
                'cumulative_time': cumulative_time,
                'message': f"换乘到{line_id}号线",
                'from_line': previous_line,
                'to_line': line_id
            })
        
        cumulative_time += segment_time
        previous_line = line_id
        
        formatted_path.append({
            'from_station': station_graph.station_info.get(current, {'cn': '未知', 'en': 'Unknown'}),
            'to_station': station_graph.station_info.get(next_node, {'cn': '未知', 'en': 'Unknown'}),
            'line_id': line_id,
            'segment_time': segment_time,
            'cumulative_time': cumulative_time,
            'transfer': False
        })
    
    return jsonify({
        'success': True,
        'data': {
            'path': formatted_path,
            'total_time': result['total_time'],
            'from_station': station_graph.station_info.get(start_group, {'cn': '未知', 'en': 'Unknown'}),
            'to_station': station_graph.station_info.get(end_group, {'cn': '未知', 'en': 'Unknown'}),
            'transfer_count': len([p for p in formatted_path if p.get('transfer')])
        }
    })

# ======= 安检口拥挤情况 API =======

@app.route('/smartmetro/congestion_details', methods=['GET'])
def congestion_details():
    name_cn = request.args.get('name_cn')

    if not name_cn:
        return jsonify({'error': 'Missing name_cn parameter'}), 400

    conn = get_db_connection()
    cursor = conn.cursor(dictionary=True)

    query = """
        SELECT stat_id, name_cn, travel_group, checkpoint_id, person_num, id, created_at
        FROM checkpoint_congestion
        WHERE name_cn = %s
    """
    cursor.execute(query, (name_cn,))
    results = cursor.fetchall()

    if not results:
        return jsonify({'error': 'No data found for the given station'}), 404

    base_info = {
        "stat_id": results[0]['stat_id'],
        "name_cn": results[0]['name_cn'],
        "travel_group": results[0]['travel_group'],
        "checkpoints": []
    }

    checkpoint_map = defaultdict(lambda: {"id": [], "person_num": None, "created_at": None})

    for row in results:
        cp_id = row['checkpoint_id']
        checkpoint_map[cp_id]["id"].append(row['id'])
        checkpoint_map[cp_id]["person_num"] = row['person_num']
        checkpoint_map[cp_id]["created_at"] = row['created_at'].isoformat() if row['created_at'] else None

    for cp_id, data in checkpoint_map.items():
        base_info["checkpoints"].append({
            "checkpoint_id": cp_id,
            "id": data["id"],
            "person_num": data["person_num"],
            "created_at": data["created_at"]
        })

    cursor.close()
    conn.close()
    return jsonify(base_info)

# ======= 查询线路所有站点 API（并未使用） =======

@app.route('/smartmetro/allstations', methods=['GET'])
def fetch_stations():
    """返回所有地铁线路及其站点"""
    data = get_stations()
    return jsonify(data)
    
# ======= 车厢拥挤情况 API =======

@app.route('/smartmetro/crowding', methods=['POST'])
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
        conn = get_db_connection()
        cursor = conn.cursor()
        sql = """INSERT INTO coach_congestion 
                 (line_number, line_carriage, person_num, crowd_level, path_id) 
                 VALUES (%s, %s, %s, %s, %s)"""
        cursor.execute(sql, (line_number, line_carriage, person_num, crowd_level, path_id))
        conn.commit()
        cursor.close()
        
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
        if conn:
            conn.close()

@app.route('/smartmetro/crowding/batch', methods=['GET'])
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

        conn = get_db_connection()
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
        cursor.close()

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
        if conn:
            conn.close()

@app.route('/smartmetro/clear_crowding', methods=['POST'])
def clear_crowding():
    """
    清空 coach_congestion 表
    POST /clear_crowding
    """
    conn = None
    try:
        conn = get_db_connection()
        cursor = conn.cursor()

        # Truncate the table
        sql = "TRUNCATE TABLE coach_congestion"
        cursor.execute(sql)
        conn.commit()
        cursor.close()

        return jsonify({"status": "success", "message": "Table cleared successfully."})

    except Exception as e:
        return jsonify({"status": "error", "message": str(e)}), 500
    finally:
        if conn:
            conn.close()

# ======= 列车到站时间 API =======

@app.route('/smartmetro/train/report_status', methods=['POST'])
def report_train_status():
    data = request.get_json()
    required_fields = ["train_number", "station_id", "next_station_id", "timestamp", "path_id"]

    if not all(field in data for field in required_fields):
        return jsonify({"error": "Missing required fields"}), 400

    try:
        timestamp = datetime.strptime(data['timestamp'], "%Y-%m-%dT%H:%M:%S")
    except ValueError:
        return jsonify({"error": "Invalid timestamp format, use YYYY-MM-DDTHH:MM:SS"}), 400

    conn = get_db_connection()
    try:
        with conn.cursor() as cursor:
            sql = """
                INSERT INTO train_realtime_status 
                (train_number, station_id, next_station_id, timestamp, path_id)
                VALUES (%s, %s, %s, %s, %s)
                ON DUPLICATE KEY UPDATE 
                station_id = VALUES(station_id),
                next_station_id = VALUES(next_station_id),
                timestamp = VALUES(timestamp),
                path_id = VALUES(path_id)
            """
            cursor.execute(sql, (
                data['train_number'],
                data['station_id'],
                data['next_station_id'],
                timestamp,
                data['path_id']
            ))
            conn.commit()
        return jsonify({"status": "success", "message": "Train status updated!"})
    finally:
        conn.close()

@app.route('/smartmetro/next_trains', methods=['GET'])
def get_next_trains():
    station_name = request.args.get('station_name')

    if not station_name:
        return jsonify({"error": "Missing station_name parameter"}), 400

    conn = get_db_connection()
    try:
        with conn.cursor(buffered=True, dictionary=True) as cursor:
            cursor.execute("""
                SELECT path_id, station_id, station_order FROM station_path_order 
                WHERE name_cn = %s
            """, (station_name,))
            target_stations = cursor.fetchall()

            if not target_stations:
                return jsonify({"error": "Station not found"}), 404

            result = {}

            for target_station in target_stations:
                path_id = target_station['path_id']
                target_station_id = target_station['station_id']
                target_order = target_station['station_order']
                target_line_id = target_station_id // 100

                cursor.execute("""
                    SELECT train_number, station_id, next_station_id, timestamp, path_id 
                    FROM train_realtime_status 
                    WHERE path_id = %s
                """, (path_id,))
                trains = cursor.fetchall()

                arrivals = []
                for train in trains:
                    train_line_id = train['station_id'] // 100

                    if train_line_id != target_line_id:
                        continue

                    cursor.execute("""
                        SELECT station_order FROM station_path_order 
                        WHERE path_id = %s AND station_id = %s
                    """, (path_id, train['next_station_id']))
                    next_station_result = cursor.fetchone()

                    if not next_station_result:
                        continue

                    next_order = next_station_result['station_order']
                    direction = "up" if next_order > target_order else "down"

                    if (direction == "up" and next_order <= target_order) or (direction == "down" and next_order >= target_order):
                        continue

                    total_travel_time = calculate_travel_time(conn, path_id, train['next_station_id'], target_station_id)

                    if total_travel_time is None:
                        continue

                    arrival_time = train['timestamp'] + timedelta(minutes=total_travel_time)

                    arrivals.append({
                        "train_number": train['train_number'],
                        "direction": direction,
                        "expected_arrival_time": arrival_time.strftime('%Y-%m-%d %H:%M:%S'),
                        "path_id": train['path_id'],
                        "line_id": train_line_id
                    })

                up_trains = sorted(
                    [t for t in arrivals if t['direction'] == "up"],
                    key=lambda x: x['expected_arrival_time']
                )[:2]

                down_trains = sorted(
                    [t for t in arrivals if t['direction'] == "down"],
                    key=lambda x: x['expected_arrival_time']
                )[:2]

                line_key = f"Line_{target_line_id}"
                if line_key not in result:
                    result[line_key] = {
                        "up_direction": up_trains,
                        "down_direction": down_trains
                    }

            return jsonify({
                "station_name": station_name,
                "lines": result
            })
    except Exception as e:
        logger.error(f"获取下一班列车信息出错: {str(e)}", exc_info=True)
        return jsonify({"error": str(e)}), 500
    finally:
        conn.close()

# ======= 地铁站点详情（电梯位置、卫生间位置、出口位置） API =======

@app.route('/smartmetro/station_details', methods=['GET'])
def get_station_details():
    name_cn = request.args.get('name_cn')
    if not name_cn:
        return jsonify({"error": "Missing parameter: name_cn"}), 400

    connection = mysql.connector.connect(**db_config)

    try:
        with connection.cursor(dictionary=True) as cursor:
            # 查询 elevators
            cursor.execute("""
                SELECT stat_id, name_cn, name_en, line, description, icon1, icon2, id_alias
                FROM station_elevators
                WHERE name_cn = %s
            """, (name_cn,))
            elevators = cursor.fetchall()

            # 查询 entrances
            cursor.execute("""
                SELECT stat_id, name_cn, name_en, entrance_id, id_alias, description, icon1, icon2, status, memo
                FROM station_entrances
                WHERE name_cn = %s
            """, (name_cn,))
            entrances = cursor.fetchall()

            # 查询 toilets
            cursor.execute("""
                SELECT stat_id, name_cn, name_en, line, description, description_en, icon1, icon2, toilet_inside, status, plan_close_date, plan_open_date
                FROM station_toilets
                WHERE name_cn = %s
            """, (name_cn,))
            toilets = cursor.fetchall()

            # 获取基础信息：优先从 toilets 获取
            base_info = {}
            if toilets:
                base_info = {
                    "name_cn": toilets[0]["name_cn"],
                    "name_en": toilets[0]["name_en"],
                    "stat_id": toilets[0]["stat_id"]
                }
            elif elevators:
                base_info = {
                    "name_cn": elevators[0]["name_cn"],
                    "name_en": elevators[0]["name_en"],
                    "stat_id": elevators[0]["stat_id"]
                }
            elif entrances:
                base_info = {
                    "name_cn": entrances[0]["name_cn"],
                    "name_en": entrances[0]["name_en"],
                    "stat_id": entrances[0]["stat_id"]
                }
            else:
                base_info = {
                    "name_cn": name_cn,
                    "name_en": None,
                    "stat_id": None
                }

            # 返回一个标准对象，不嵌套 name_cn 键
            result = {
                **base_info,
                "elevators": elevators,
                "entrances": entrances,
                "toilets": toilets
            }

        return jsonify(result)

    except Exception as e:
        return jsonify({"error": str(e)})

    finally:
        connection.close()

# ======= 列车首末班车查询 API =======

@app.route('/smartmetro/fltime/all', methods=['GET'])
def get_all_fltime():
    conn = None
    try:
        conn = mysql.connector.connect(**db_config)
        cursor = conn.cursor(dictionary=True)

        sql = "SELECT * FROM fltime ORDER BY line_id, station_id"
        cursor.execute(sql)
        results = cursor.fetchall()

        # Convert timedelta fields to string
        for row in results:
            if isinstance(row['first_time'], (str, bytes)):
                pass  # Already string, skip
            else:
                row['first_time'] = str(row['first_time'])
                row['last_time'] = str(row['last_time'])
                
        return jsonify({
            "status": "success",
            "data": results
        })

    except Exception as e:
        return jsonify({
            "status": "error",
            "message": str(e)
        }), 500
    finally:
        if conn and conn.is_connected():
            conn.close()

# ======= 列车站点顺序查询 API =======

@app.route('/smartmetro/station/order', methods=['GET'])
def get_station_order():
    line = request.args.get('line')
    conn = None

    if not line:
        return jsonify({"status": "error", "message": "line parameter is required!"}), 400

    try:
        conn = mysql.connector.connect(**db_config)
        cursor = conn.cursor(dictionary=True)

        sql = """
            SELECT spo.station_order, spo.path_id, spo.station_id, sm.name_cn
            FROM station_path_order spo
            JOIN station_map sm ON spo.station_id = sm.stat_id
            WHERE spo.line = %s
            ORDER BY spo.station_order, spo.path_id
        """
        cursor.execute(sql, (line,))
        rows = cursor.fetchall()

        # Organize data as requested
        result = {}
        for row in rows:
            station_order = row['station_order']
            path_entry = {
                "path_id": row['path_id'],
                "station_id": row['station_id'],
                "name_cn": row['name_cn']
            }

            if station_order not in result:
                result[station_order] = []
            result[station_order].append(path_entry)

        return jsonify({
            "status": "success",
            "line": int(line),
            "data": result
        })

    except Exception as e:
        return jsonify({
            "status": "error",
            "message": str(e)
        }), 500
    finally:
        if conn and conn.is_connected():
            conn.close()

# 启动服务
if __name__ == '__main__':
    app.run(host='0.0.0.0', port=5001, debug=True)
