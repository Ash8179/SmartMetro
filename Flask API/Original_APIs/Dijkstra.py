from flask import Flask, request, jsonify
import mysql.connector
from mysql.connector import pooling
import heapq

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

connection_pool = pooling.MySQLConnectionPool(**db_config)

def get_db_connection():
    return connection_pool.get_connection()

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
        conn = get_db_connection()
        cursor = conn.cursor(dictionary=True)
        
        try:
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
                    
        finally:
            cursor.close()
            conn.close()
    
    def find_travel_group(self, station_name):
        if not station_name:
            return None
        return self.name_to_groups.get(station_name.lower().strip())
    
    def dijkstra_shortest_path(self, start_group, end_group):
        if not start_group or not end_group or start_group not in self.graph or end_group not in self.graph:
            return None
            
        heap = [(0, start_group, [])]
        visited = set()
        
        while heap:
            (total_time, current, path) = heapq.heappop(heap)
            
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
                    heapq.heappush(heap, (total_time + time, neighbor, path))
                    
        return None

# 初始化图结构
station_graph = StationGraph()
station_graph.build_graph()

@app.route('/Dijkstra', methods=['GET'])
def find_shortest_path():
    from_station = request.args.get('from')
    to_station = request.args.get('to')
    
    start_group = station_graph.find_travel_group(from_station)
    end_group = station_graph.find_travel_group(to_station)
    
    if not start_group or not end_group:
        return jsonify({
            'success': False,
            'message': 'Invalid station name'
        }), 400
        
    result = station_graph.dijkstra_shortest_path(start_group, end_group)
    
    if not result:
        return jsonify({
            'success': False,
            'message': 'No path found'
        }), 404
        
    # 构建完整路径信息
    formatted_path = []
    cumulative_time = 0
    
    for i in range(len(result['path']) - 1):
        current = result['path'][i]
        next_node = result['path'][i+1]
        
        # 获取连接信息
        conn_info = station_graph.connections[(current, next_node)]
        line_id = conn_info['line_id']
        segment_time = conn_info['time']
        
        cumulative_time += segment_time
        
        formatted_path.append({
            'from_station': station_graph.station_info[current],
            'to_station': station_graph.station_info[next_node],
            'line_id': line_id,
            'segment_time': segment_time,
            'cumulative_time': cumulative_time
        })
    
    return jsonify({
        'success': True,
        'data': {
            'path': formatted_path,
            'total_time': result['total_time'],
            'from_station': station_graph.station_info[start_group],
            'to_station': station_graph.station_info[end_group]
        }
    })

if __name__ == '__main__':
    app.run(port=5001)
