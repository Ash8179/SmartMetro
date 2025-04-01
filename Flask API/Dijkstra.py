from flask import Flask, request, jsonify
import mysql.connector
from mysql.connector import pooling
import heapq
import logging
from functools import wraps

# 配置日志
logging.basicConfig(level=logging.DEBUG)
logger = logging.getLogger(__name__)

app = Flask(__name__)

# 数据库配置
db_config = {
    'host': 'localhost',
    'database': 'new_schema',
    'user': 'root',
    'password': 'zwy040103',
    'pool_name': 'mypool',
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

if __name__ == '__main__':
    try:
        app.run(port=5001, debug=True)
    except Exception as e:
        logger.error(f"应用启动失败: {str(e)}")
