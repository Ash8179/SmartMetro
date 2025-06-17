from flask import Flask, request, jsonify
import mysql.connector
from collections import defaultdict

app = Flask(__name__)

def get_db_connection():
    return mysql.connector.connect(
        host='localhost',
        user='root',
        password='zwy040103',
        database='new_schema'
    )

@app.route('/congestion_details', methods=['GET'])
def congestion_details():
    name_cn = request.args.get('name_cn')

    if not name_cn:
        return jsonify({'error': 'Missing name_cn parameter'}), 400

    conn = get_db_connection()
    cursor = conn.cursor(dictionary=True)

    query = """
        SELECT stat_id, name_cn, travel_group, checkpoint_id, person_num, id, created_at
        FROM checkpoint_congestion
        WHERE name_cn = %s
    """
    cursor.execute(query, (name_cn,))
    results = cursor.fetchall()

    if not results:
        return jsonify({'error': 'No data found for the given station'}), 404

    # Shared info
    base_info = {
        "stat_id": results[0]['stat_id'],
        "name_cn": results[0]['name_cn'],
        "travel_group": results[0]['travel_group'],
        "checkpoints": []
    }

    # Group by checkpoint_id
    checkpoint_map = defaultdict(lambda: {"id": [], "person_num": None, "created_at": None})

    for row in results:
        cp_id = row['checkpoint_id']
        checkpoint_map[cp_id]["id"].append(row['id'])
        checkpoint_map[cp_id]["person_num"] = row['person_num']
        checkpoint_map[cp_id]["created_at"] = row['created_at'].isoformat() if row['created_at'] else None

    # Reformat for JSON
    for cp_id, data in checkpoint_map.items():
        base_info["checkpoints"].append({
            "checkpoint_id": cp_id,
            "id": data["id"],
            "person_num": data["person_num"],
            "created_at": data["created_at"]
        })

    cursor.close()
    conn.close()

    return jsonify(base_info)

if __name__ == '__main__':
    app.run(host='127.0.0.1', port=5009, debug=True)
