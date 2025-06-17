from flask import Flask, request, jsonify
import mysql.connector
from flask_cors import CORS

app = Flask(__name__)
CORS(app)

# Database config
db_config = {
    'host': 'localhost',
    'user': 'root',
    'password': 'zwy040103',
    'database': 'new_schema'
}

@app.route('/station/order', methods=['GET'])
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

if __name__ == '__main__':
    app.run(host='0.0.0.0', port=5007, debug=True)
