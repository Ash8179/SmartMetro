from flask import Flask, jsonify, request
from flask_cors import CORS
from routes import register_routes
from Dijkstra import StationGraph
import logging

# 配置日志
logging.basicConfig(level=logging.DEBUG)
logger = logging.getLogger(__name__)

# 数据库配置
db_config = {
    'host': 'localhost',
    'database': 'smart_metro',
    'user': 'root',
    'password': 'Zz13193515431'
}

# 创建 Flask 应用
app = Flask(__name__)
CORS(app)

# 创建 StationGraph 实例
station_graph = StationGraph(db_config)

# 注册路由
register_routes(app, station_graph)

if __name__ == '__main__':
    app.run(host='0.0.0.0', port=5000, debug=True)
