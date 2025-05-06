from flask import Flask, request, jsonify
import pymysql
from datetime import datetime, timedelta

app = Flask(__name__)

def get_db_connection():
    return pymysql.connect(
        host='localhost',
        user='root',
        password='zwy040103',
        db='new_schema',
        cursorclass=pymysql.cursors.DictCursor
    )

def calculate_travel_time(conn, path_id, from_station_id, to_station_id):
    with conn.cursor() as cursor:
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

@app.route('/api/train/report_status', methods=['POST'])
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

@app.route('/api/next_trains', methods=['GET'])
def get_next_trains():
    station_name = request.args.get('station_name')

    if not station_name:
        return jsonify({"error": "Missing station_name parameter"}), 400

    conn = get_db_connection()
    try:
        with conn.cursor() as cursor:
            # 查询所有同名换乘站的记录
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

                # 查询该线路的实时列车
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
                    direction = "up" if next_order < target_order else "down"

                    total_travel_time = calculate_travel_time(
                        conn, path_id, train['next_station_id'], target_station_id
                    )

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

                # 用线路编号作为返回字典的Key
                result[f"Line_{target_line_id}"] = {
                    "up_direction": up_trains,
                    "down_direction": down_trains
                }

            return jsonify({
                "station_name": station_name,
                "lines": result
            })

    finally:
        conn.close()

if __name__ == '__main__':
    app.run(debug=True, port=5005)
