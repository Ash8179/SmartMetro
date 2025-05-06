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

def extract_station_entrances():
    try:
        with connection.cursor() as cursor:
            # 查询 stations 表
            cursor.execute("SELECT stat_id, name_cn, name_en, entrance_info FROM stations;")
            stations = cursor.fetchall()

            for station in stations:
                stat_id = station['stat_id']
                name_cn = station['name_cn']
                name_en = station['name_en']
                entrance_info_json = station['entrance_info']

                if entrance_info_json:
                    try:
                        entrance_data = json.loads(entrance_info_json).get('line', [])
                        for line_entry in entrance_data:
                            entrances = line_entry.get('entrance', [])
                            for entrance in entrances:
                                entrance_id = entrance.get('id')
                                id_alias = entrance.get('id_alias')
                                description = entrance.get('description')
                                icon1 = entrance.get('icon1')
                                icon2 = entrance.get('icon2')
                                status = entrance.get('status')
                                memo = entrance.get('memo')

                                if not entrance_id:
                                    continue  # 空ID直接跳过

                                # 查询是否存在相同 name_cn + entrance_id 的记录
                                check_sql = """
                                    SELECT id FROM station_entrances
                                    WHERE name_cn = %s AND entrance_id = %s;
                                """
                                cursor.execute(check_sql, (name_cn, entrance_id))
                                exists = cursor.fetchone()

                                if exists:
                                    print(f"⚠️ 已存在: name_cn='{name_cn}' 和 entrance_id='{entrance_id}'，已跳过。")
                                    continue

                                # 插入新记录
                                insert_sql = """
                                    INSERT INTO station_entrances
                                    (stat_id, name_cn, name_en, entrance_id, id_alias, description, icon1, icon2, status, memo)
                                    VALUES (%s, %s, %s, %s, %s, %s, %s, %s, %s, %s);
                                """
                                cursor.execute(
                                    insert_sql,
                                    (
                                        stat_id, name_cn, name_en, entrance_id,
                                        id_alias, description, icon1, icon2, status, memo
                                    )
                                )
                    except json.JSONDecodeError:
                        print(f"⚠️ 站点 {stat_id} 的 entrance_info 字段无法解析，已跳过。")

        connection.commit()
        print("✅ 出入口数据已全部导入 station_entrances 表！")

    except Exception as e:
        print("发生错误:", e)
        connection.rollback()
    finally:
        connection.close()

if __name__ == '__main__':
    extract_station_entrances()
