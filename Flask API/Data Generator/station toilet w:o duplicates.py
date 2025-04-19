import pymysql
import json

# 判断是否是合法 JSON 且包含 toilet 列表
def is_valid_toilet_json(text):
    try:
        obj = json.loads(text)
        return isinstance(obj, dict) and 'toilet' in obj and isinstance(obj['toilet'], list)
    except ValueError:
        return False

# 连接数据库
connection = pymysql.connect(
    host='localhost',
    user='root',
    password='zwy040103',
    database='new_schema',
    charset='utf8mb4',
    cursorclass=pymysql.cursors.DictCursor
)

def extract_station_toilets():
    try:
        with connection.cursor() as cursor:
            cursor.execute("SELECT stat_id, name_cn, name_en, toilet_inside, toilet_position, toilet_position_en FROM stations;")
            stations = cursor.fetchall()

            for station in stations:
                stat_id = station['stat_id']
                name_cn = station['name_cn']
                name_en = station['name_en']
                toilet_inside = bool(station['toilet_inside'])
                position_json = station['toilet_position']
                position_en_text = station['toilet_position_en']

                if not position_json:
                    print(f"⚠️ 站点 {stat_id} 的 toilet_position 为空，跳过。")
                    continue

                if is_valid_toilet_json(position_json.strip()):
                    toilets = json.loads(position_json.strip()).get('toilet', [])

                    for i, t in enumerate(toilets):
                        lineno = t.get('lineno')
                        description = t.get('description')
                        icon1 = t.get('icon1')
                        icon2 = t.get('icon2')
                        status = t.get('status')
                        plan_close_date = t.get('plan_close_date')
                        plan_open_date = t.get('plan_open_date')

                        description_en = position_en_text.strip() if isinstance(position_en_text, str) else ""
                        cleaned_description = description.strip() if description else None

                        cursor.execute("""
                            INSERT INTO station_toilets
                            (stat_id, name_cn, name_en, line, description, description_en, icon1, icon2, toilet_inside, status, plan_close_date, plan_open_date)
                            VALUES (%s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s);
                        """, (
                            stat_id, name_cn, name_en, lineno, cleaned_description, description_en,
                            icon1, icon2, toilet_inside, status, plan_close_date, plan_open_date
                        ))
                        print(f"✅ 插入: {stat_id} - {cleaned_description} (line {lineno})")
                else:
                    print(f"⚠️ 站点 {stat_id} 的 toilet_position 不是有效的 JSON，跳过。")

        connection.commit()
        print("🎉 所有数据导入 station_toilets 成功！")

    except Exception as e:
        print("❌ 发生错误:", e)
        connection.rollback()
    finally:
        connection.close()

if __name__ == '__main__':
    extract_station_toilets()
