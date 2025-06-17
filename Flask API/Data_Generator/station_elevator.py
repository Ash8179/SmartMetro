import pymysql
import json

# 数据库连接
connection = pymysql.connect(
    host='localhost',
    user='root',
    password='zwy040103',
    database='new_schema',
    charset='utf8mb4',
    cursorclass=pymysql.cursors.DictCursor
)

def extract_station_elevators():
    try:
        with connection.cursor() as cursor:
            # 查询stations表
            cursor.execute("SELECT stat_id, name_cn, name_en, elevator FROM stations;")
            stations = cursor.fetchall()

            for station in stations:
                stat_id = station['stat_id']
                name_cn = station['name_cn']
                name_en = station['name_en']
                elevator_json = station['elevator']

                if elevator_json:
                    try:
                        elevator_data = json.loads(elevator_json).get('line', [])
                        for line_entry in elevator_data:
                            lineno = line_entry.get('lineno')
                            elevators = line_entry.get('elevator', [])
                            for e in elevators:
                                description = e.get('description')
                                icon1 = e.get('icon1')
                                icon2 = e.get('icon2')

                                # 先检查是否存在相同 description
                                check_sql = "SELECT id FROM station_elevators WHERE description = %s"
                                cursor.execute(check_sql, (description,))
                                existing = cursor.fetchone()

                                if not existing:
                                    insert_sql = """
                                        INSERT INTO station_elevators
                                        (stat_id, name_cn, name_en, line, description, icon1, icon2)
                                        VALUES (%s, %s, %s, %s, %s, %s, %s);
                                    """
                                    cursor.execute(
                                        insert_sql,
                                        (
                                            stat_id, name_cn, name_en, lineno,
                                            description, icon1, icon2
                                        )
                                    )
                                else:
                                    print(f"⚠️ 已跳过重复电梯：{description}")
                    except json.JSONDecodeError:
                        print(f"⚠️ 站点 {stat_id} 的 elevator 字段无法解析，已跳过。")

        connection.commit()
        print("✅ 电梯数据已全部导入 station_elevators 表！")

    except Exception as e:
        print("发生错误:", e)
        connection.rollback()
    finally:
        connection.close()

if __name__ == '__main__':
    extract_station_elevators()
