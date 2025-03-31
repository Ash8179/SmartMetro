from flask import Blueprint, jsonify
from models import db, MetroArriveTime
from datetime import datetime, timedelta

routes = Blueprint('routes', __name__)


@routes.route('/api/metro/arrival-time/<from_station_cn>/<int:line_id>', methods=['GET'])
def get_arrival_time(from_station_cn, line_id):
    # 从数据库查询到达时间
    metro_arrive_time = MetroArriveTime.query.filter_by(from_station_cn=from_station_cn, line_id=line_id).first()

    if metro_arrive_time:
        current_time = datetime.now()
        arrival_time = current_time + timedelta(minutes=metro_arrive_time.travel_time)
        return jsonify({'arrivalTime': arrival_time.strftime('%H:%M:%S')}), 200
    else:
        return jsonify({'error': f'没有找到该站点：{from_station_cn}，请检查站点名称是否正确'}), 404
