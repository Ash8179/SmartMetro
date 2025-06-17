from flask import Flask, request, jsonify
from flask_cors import CORS
import mysql.connector
from mysql.connector import pooling
import math
import logging
from functools import wraps
import heapq
from datetime import datetime, timedelta

# 配置日志
logging.basicConfig(level=logging.DEBUG)
logger = logging.getLogger(__name__)

app = Flask(__name__)
CORS(app)

# 数据库配置
db_config = {
    'host': 'localhost',
    'database': 'smart_metro',
    'user': 'root',
    'password': 'CZz13193515431',
    'pool_name': 'metro_pool',
    'pool_size': 5
}

# 创建数据库连接池
try:
    connection_pool = pooling.MySQLConnectionPool(**db_config)
    logger.info("数据库连接池创建成功")
except Exception as e:
    logger.error(f"创建数据库连接池失败: {str(e)}")
    raise

def get_db_connection():
    try:
        return connection_pool.get_connection()
    except Exception as e:
        logger.error(f"获取数据库连接失败: {str(e)}")
        raise

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

def haversine(lat1, lon1, lat2, lon2):
    R = 6371  # 地球半径(km)
    lat1, lon1, lat2, lon2 = map(math.radians, [lat1, lon1, lat2, lon2])
    dlat = lat2 - lat1
    dlon = lon2 - lon1
    a = math.sin(dlat/2)**2 + math.cos(lat1)*math.cos(lat2)*math.sin(dlon/2)**2
    return R * 2 * math.asin(math.sqrt(a))

# 获取所有地铁线路
@app.route('/api/lines', methods=['GET'])
@handle_errors
def get_lines():
    conn = get_db_connection()
    cursor = conn.cursor(dictionary=True)
    
    try:
        # 从dijkstra表获取所有唯一的线路
        cursor.execute("""
            SELECT DISTINCT line_id, line_id as name_cn, line_id as name_en
            FROM dijkstra
        """)
        
        lines = cursor.fetchall()
        if not lines:
            return jsonify({'error': 'No lines found'}), 404
            
        return jsonify(lines)
        
    finally:
        cursor.close()
        conn.close()

# 拥挤度相关API
@app.route('/api/crowding', methods=['GET', 'POST'])
@handle_errors
def handle_crowding():
    if request.method == 'GET':
        line_id = request.args.get('line_id')
        line_number = request.args.get('line_number')
        line_carriage = request.args.get('line_carriage')
        
        # 检查参数是否存在
        if not line_id or not line_number or not line_carriage:
            return jsonify({'error': 'Missing required parameters'}), 400
            
        # 转换参数类型
        try:
            line_id = int(line_id)
            line_carriage = int(line_carriage)
        except ValueError:
            return jsonify({'error': 'Invalid parameter type'}), 400
            
        conn = get_db_connection()
        cursor = conn.cursor(dictionary=True)
        
        try:
            # 获取最新的拥挤度数据
            cursor.execute("""
                SELECT * FROM line_crowding 
                WHERE line_id = %s AND CAST(line_number AS CHAR) = %s AND line_carriage = %s
                ORDER BY timestamp DESC 
                LIMIT 1
            """, (line_id, line_number, line_carriage))
            
            result = cursor.fetchone()
            if result:
                return jsonify(result)
            else:
                return jsonify({'error': 'No data found'}), 404
                
        finally:
            cursor.close()
            conn.close()
            
    elif request.method == 'POST':
        data = request.json
        required_fields = ['line_id', 'line_number', 'line_carriage', 'person_num', 'crowd_level']
        
        if not all(field in data for field in required_fields):
            return jsonify({'error': 'Missing required fields'}), 400
            
        conn = get_db_connection()
        cursor = conn.cursor()
        
        try:
            # 插入新的拥挤度数据
            cursor.execute("""
                INSERT INTO line_crowding 
                (line_id, line_number, line_carriage, person_num, timestamp, crowd_level)
                VALUES (%s, %s, %s, %s, NOW(), %s)
            """, (
                data['line_id'],
                data['line_number'],
                data['line_carriage'],
                data['person_num'],
                data['crowd_level']
            ))
            
            conn.commit()
            return jsonify({'message': 'Data inserted successfully'}), 201
            
        finally:
            cursor.close()
            conn.close()

# 位置服务相关API
@app.route('/api/metro/nearest-stations', methods=['GET'])
@handle_errors
def get_nearest_stations():
    """获取最近的站点"""
    try:
        latitude = float(request.args.get('latitude'))
        longitude = float(request.args.get('longitude'))
        radius = float(request.args.get('radius', 1000))  # 默认1000米
    except (TypeError, ValueError) as e:
        return jsonify({'error': 'Invalid parameters'}), 400
        
    conn = get_db_connection()
    cursor = conn.cursor(dictionary=True)
    
    try:
        # 获取所有站点
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
                        latitude, longitude,
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

        # 按距离排序并限制返回5个站点
        sorted_results = sorted(
            seen_groups.values(),
            key=lambda x: x['distance_m']
        )[:5]  # 直接限制返回5个站点
        
        return jsonify({
            'user_location': {'lat': latitude, 'lng': longitude},
            'nearest_stations': sorted_results
        })
        
    finally:
        cursor.close()
        conn.close()

# 路线规划相关API
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
                FROM dijkstra
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

@app.route('/Dijkstra', methods=['GET'])
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
            transfer_count += 1
            cumulative_time += 5  # 增加换乘时间
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

# 所有站点API
@app.route('/api/stations', methods=['GET'])
@handle_errors
def get_all_stations():
    """获取所有站点信息"""
    conn = get_db_connection()
    cursor = conn.cursor(dictionary=True)
    
    try:
        cursor.execute("""
            SELECT 
                station_id,
                station_name,
                station_name_en,
                line_id,
                line_name,
                line_name_en,
                latitude,
                longitude
            FROM all_stations
            ORDER BY line_id, station_id
        """)
        
        stations = cursor.fetchall()
        return jsonify(stations)
        
    finally:
        cursor.close()
        conn.close()

# 获取线路站点到达时间
@app.route('/api/arrival-time/line/<int:line_id>', methods=['GET'])
@handle_errors
def get_line_stations_arrival_time(line_id):
    conn = get_db_connection()
    cursor = conn.cursor(dictionary=True)
    current_time = datetime.now().time()
    current_date = datetime.now().date()
    
    try:
        # 修改查询语句，不使用sequence列，使用path_id和station_order替代
        query = """
            SELECT 
                s.line as line_id,
                s.station_id,
                s.station_name,
                s.station_order,
                fta.direction_desc,
                TIME_FORMAT(fta.first_arrival_time, '%H:%i:%s') as first_arrival_time,
                CONCAT(s.line, '号线') as line_name
            FROM (
                SELECT 
                    line,
                    station_id,
                    name_cn as station_name,
                    path_id,
                    station_order
                FROM all_stations
                WHERE line = %(line_id)s
            ) s
            LEFT JOIN first_train_arrival fta ON s.line = fta.line_id 
                AND s.station_id = fta.station_id
            ORDER BY s.station_order
        """
        cursor.execute(query, {'line_id': line_id})

        arrivals = cursor.fetchall()
        if not arrivals:
            return jsonify({'error': 'No stations found for this line'}), 404

        for arrival in arrivals:
            if arrival['first_arrival_time']:
                first_time_str = arrival['first_arrival_time']
                first_time = datetime.strptime(first_time_str, '%H:%M:%S').time()

                # 计算时间差（分钟）
                first_datetime = datetime.combine(current_date, first_time)
                current_datetime = datetime.combine(current_date, current_time)

                if first_time < current_time:
                    # 首班车已过，计算到下一班车的时间
                    minutes_passed = int((current_datetime - first_datetime).total_seconds() / 60)
                    next_arrival = (minutes_passed // 5 + 1) * 5
                    arrival['next_arrival_time'] = (first_datetime + timedelta(minutes=next_arrival)).strftime('%H:%M:%S')
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
        
    finally:
        cursor.close()
        conn.close()

# 搜索站点到达时间
@app.route('/api/arrival-time/search', methods=['GET'])
@handle_errors
def search_stations():
    try:
        query = request.args.get('q', '')
        if not query:
            return jsonify([])

        conn = get_db_connection()
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
        if not arrivals:
            return jsonify([])

        # 处理到达时间数据
        for arrival in arrivals:
            minutes_passed = int(float(arrival['minutes_passed']))

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
    finally:
        cursor.close()
        conn.close()

@app.route('/api/route', methods=['POST'])
@handle_errors
def find_route():
    try:
        data = request.get_json()
        start_station = data.get('start_station')
        end_station = data.get('end_station')
        
        if not start_station or not end_station:
            return jsonify({
                'success': False,
                'data': None,
                'message': '请提供起始站和终点站'
            }), 400
            
        # 使用 Dijkstra 算法查找最短路径
        start_group = station_graph.find_travel_group(start_station)
        end_group = station_graph.find_travel_group(end_station)
        
        if not start_group or not end_group:
            return jsonify({
                'success': False,
                'data': None,
                'message': '无效的车站名称'
            }), 400
            
        result = station_graph.dijkstra_shortest_path(start_group, end_group)
        if not result:
            return jsonify({
                'success': False,
                'data': None,
                'message': '未找到路线'
            }), 404
            
        formatted_path = []
        cumulative_time = 0
        previous_line = None
        transfer_count = 0
        
        for i in range(len(result['path']) - 1):
            current = result['path'][i]
            next_node = result['path'][i+1]
            
            # 获取连接信息
            conn_info = station_graph.connections.get((current, next_node))
            if not conn_info:
                continue
                
            line_id = conn_info['line_id']
            travel_time = conn_info['time']
            
            # 检查是否换乘
            if previous_line is not None and line_id != previous_line:
                transfer_count += 1
                cumulative_time += 5  # 增加换乘时间
                formatted_path.append({
                    'transfer': True,
                    'transfer_time': 5,
                    'cumulative_time': cumulative_time,
                    'message': f"换乘到{line_id}号线",
                    'from_line': previous_line,
                    'to_line': line_id
                })
            
            cumulative_time += travel_time
            previous_line = line_id
            
            # 获取站点信息
            from_station_info = station_graph.station_info.get(current, {'cn': '未知', 'en': 'Unknown'})
            to_station_info = station_graph.station_info.get(next_node, {'cn': '未知', 'en': 'Unknown'})
            
            formatted_path.append({
                'from_station': {
                    'station_id': current,
                    'name_cn': from_station_info['cn'],
                    'name_en': from_station_info['en'],
                    'line_id': line_id
                },
                'to_station': {
                    'station_id': next_node,
                    'name_cn': to_station_info['cn'],
                    'name_en': to_station_info['en'],
                    'line_id': line_id
                },
                'line_id': line_id,
                'time': travel_time,
                'transfer': False
            })
        
        return jsonify({
            'success': True,
            'data': {
                'total_time': cumulative_time,
                'path': formatted_path,
                'transfer_count': transfer_count
            },
            'message': 'Route found successfully'
        })
        
    except Exception as e:
        logger.error(f"Error in find_route: {str(e)}")
        return jsonify({
            'success': False,
            'data': None,
            'message': str(e)
        }), 500

if __name__ == '__main__':
    app.run(host='0.0.0.0', port=5000, debug=True) 