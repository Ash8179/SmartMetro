import mysql.connector
import heapq
import logging

# 配置日志
logging.basicConfig(level=logging.DEBUG)
logger = logging.getLogger(__name__)

class StationGraph:
    def __init__(self, db_config):
        self.graph = {}
        self.name_to_groups = {}
        self.station_info = {}
        self.connections = {}  # 存储站点间的连接信息
        self.db_config = db_config
        self.build_graph()

    def _get_db_connection(self):
        try:
            return mysql.connector.connect(**self.db_config)
        except Exception as e:
            logger.error(f"获取数据库连接失败: {str(e)}")
            raise

    def build_graph(self):
        conn = None
        cursor = None
        try:
            conn = self._get_db_connection()
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

            logger.info(f"Graph built successfully with {len(self.graph)} stations")

        except Exception as e:
            logger.error(f"构建图结构时出错: {str(e)}", exc_info=True)
            raise
        finally:
            if cursor:
                cursor.close()
            if conn:
                conn.close()

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


# 数据库配置
db_config = {
    'host': 'localhost',
    'database': 'smart_metro',
    'user': 'root',
    'password': 'Zz13193515431'
}

# 初始化图结构
station_graph = StationGraph(db_config)
try:
    station_graph.build_graph()
    logger.info("地铁图结构构建成功")
except Exception as e:
    logger.error(f"初始化地铁图结构失败: {str(e)}")

