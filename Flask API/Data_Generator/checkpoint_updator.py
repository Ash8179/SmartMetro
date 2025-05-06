import random
import time
import mysql.connector

def get_db_connection():
    return mysql.connector.connect(
        host='localhost',
        user='root',
        password='zwy040103',
        database='new_schema'
    )

def update_person_num():
    conn = get_db_connection()
    cursor = conn.cursor()

    # 获取所有 travel_group + checkpoint_id 对
    cursor.execute("""
        SELECT DISTINCT travel_group, checkpoint_id
        FROM checkpoint_congestion
        WHERE travel_group IS NOT NULL
    """)
    group_checkpoint_pairs = cursor.fetchall()

    # 为每个 (travel_group, checkpoint_id) 生成一个随机的 person_num
    person_num_map = {
        (group, checkpoint): random.randint(0, 12)
        for group, checkpoint in group_checkpoint_pairs
    }

    # 遍历并更新每条记录
    for (group, checkpoint), person_num in person_num_map.items():
        cursor.execute("""
            UPDATE checkpoint_congestion
            SET person_num = %s
            WHERE travel_group = %s AND checkpoint_id = %s
        """, (person_num, group, checkpoint))

    conn.commit()
    cursor.close()
    conn.close()

    print(f"[{time.strftime('%Y-%m-%d %H:%M:%S')}] Updated person_num for all (travel_group, checkpoint_id) pairs.")

if __name__ == '__main__':
    while True:
        update_person_num()
        time.sleep(60)  # 每 60 秒更新一次
