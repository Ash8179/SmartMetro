from flask import Flask, request, jsonify
import pymysql

app = Flask(__name__)

# 数据库配置
DB_CONFIG = {
    'host': 'localhost',
    'user': 'root',
    'password': 'zwy040103',
    'database': 'new_schema',
    'cursorclass': pymysql.cursors.DictCursor
}

@app.route('/station_details', methods=['GET'])
def get_station_details():
    name_cn = request.args.get('name_cn')
    if not name_cn:
        return jsonify({"error": "Missing parameter: name_cn"}), 400

    connection = pymysql.connect(**DB_CONFIG)

    try:
        with connection.cursor() as cursor:
            # 查询 elevators
            cursor.execute("""
                SELECT stat_id, name_cn, name_en, line, description, icon1, icon2, id_alias
                FROM station_elevators
                WHERE name_cn = %s
            """, (name_cn,))
            elevators = cursor.fetchall()

            # 查询 entrances
            cursor.execute("""
                SELECT stat_id, name_cn, name_en, entrance_id, id_alias, description, icon1, icon2, status, memo
                FROM station_entrances
                WHERE name_cn = %s
            """, (name_cn,))
            entrances = cursor.fetchall()

            # 查询 toilets
            cursor.execute("""
                SELECT stat_id, name_cn, name_en, line, description, description_en, icon1, icon2, toilet_inside, status, plan_close_date, plan_open_date
                FROM station_toilets
                WHERE name_cn = %s
            """, (name_cn,))
            toilets = cursor.fetchall()

            # 获取基础信息：优先从 toilets 获取
            base_info = {}
            if toilets:
                base_info = {
                    "name_cn": toilets[0]["name_cn"],
                    "name_en": toilets[0]["name_en"],
                    "stat_id": toilets[0]["stat_id"]
                }
            elif elevators:
                base_info = {
                    "name_cn": elevators[0]["name_cn"],
                    "name_en": elevators[0]["name_en"],
                    "stat_id": elevators[0]["stat_id"]
                }
            elif entrances:
                base_info = {
                    "name_cn": entrances[0]["name_cn"],
                    "name_en": entrances[0]["name_en"],
                    "stat_id": entrances[0]["stat_id"]
                }
            else:
                base_info = {
                    "name_cn": name_cn,
                    "name_en": None,
                    "stat_id": None
                }

            # 返回一个标准对象，不嵌套 name_cn 键
            result = {
                **base_info,
                "elevators": elevators,
                "entrances": entrances,
                "toilets": toilets
            }

        return jsonify(result)

    except Exception as e:
        return jsonify({"error": str(e)})

    finally:
        connection.close()

if __name__ == '__main__':
    app.run(debug=True, port=5008)
