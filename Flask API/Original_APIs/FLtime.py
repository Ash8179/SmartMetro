from flask import Flask, jsonify
import mysql.connector
from flask_cors import CORS

app = Flask(__name__)
CORS(app)

# Database Config
db_config = {
    'host': 'localhost',
    'user': 'root',
    'password': 'zwy040103',
    'database': 'new_schema'
}

@app.route('/fltime/all', methods=['GET'])
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

if __name__ == '__main__':
    app.run(host='0.0.0.0', port=5006, debug=True)
