from flask_sqlalchemy import SQLAlchemy

db = SQLAlchemy()

class MetroArriveTime(db.Model):
    __tablename__ = 'dijkstra'  # 与 dijkstra 表对应

    id = db.Column(db.Integer, primary_key=True)
    from_station_cn = db.Column(db.String(100), nullable=False)
    travel_time = db.Column(db.Integer, nullable=False)
    line_id = db.Column(db.Integer, nullable=False)

    def __init__(self, from_station_cn, travel_time, line_id):
        self.from_station_cn = from_station_cn
        self.travel_time = travel_time
        self.line_id = line_id

    def __repr__(self):
        return f'<MetroArriveTime {self.from_station_cn}, Line {self.line_id}>'
