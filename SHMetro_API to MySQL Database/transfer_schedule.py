import requests
import mysql.connector
from mysql.connector import Error

# MySQL 配置
DB_CONFIG = {
    'host': 'localhost',  # 数据库地址
    'database': 'new_schema',  # 数据库名称
    'user': 'root',  # 用户名
    'password': 'zwy040103'  # 密码
}

def connect_to_mysql(config):
    """连接到 MySQL 数据库"""
    try:
        conn = mysql.connector.connect(**config)
        if conn.is_connected():
            print(f"Connected to MySQL database: {config['database']}")
            return conn
    except Error as e:
        print(f"Error while connecting to MySQL: {e}")
        return None

def store_transfer_data_to_mysql(station_data, conn):
    cursor = conn.cursor()

    # 处理时间字段，检查是否为 '--'，若是，则设置为 NULL (有些站点就没有时间)
    def check_time(value):
        return value if value != "--" else None

    # 插入数据到 MySQL 数据库
    insert_query = """
    INSERT INTO transfer_schedule (
        line, stat_id, name, station_code, first_time, first_time_desc, 
        last_time, last_time_desc, direction, description, firstarrival_time, 
        lastarrival_time
    ) 
    VALUES (%s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s)
    """
    
    data = (
        station_data.get("line"),
        station_data.get("stat_id"),
        station_data.get("name"),
        station_data.get("station_code"),
        check_time(station_data.get("first_time")),  # 检查首班车时间
        station_data.get("first_time_desc", ""),
        check_time(station_data.get("last_time")),   # 检查末班车时间
        station_data.get("last_time_desc", ""),
        station_data.get("direction"),
        station_data.get("description"),
        station_data.get("firstarrival_time", 0),
        station_data.get("lastarrival_time", 0)
    )

    try:
        cursor.execute(insert_query, data)
        conn.commit()
        print(f"Station {station_data.get('name')} data inserted into database.")
    except Error as e:
        print(f"Error inserting data: {e}")

def get_transfer_schedule_from_api(line):
    """从 API 获取换乘时刻表数据"""
    API_URL = f"http://m.shmetro.com/interface/metromap/metromap.aspx?func=exfltime&line1={line}"
    try:
        response = requests.get(API_URL)
        response.raise_for_status()
        data = response.json()
        # 处理返回数据，假设返回的是一个列表，且每个元素包含站点数据
        if isinstance(data, list):
            return data
        else:
            print(f"Unexpected data format for line {line}")
            return None
    except requests.exceptions.RequestException as e:
        print(f"Error fetching data from API for line {line}: {e}")
        return None

def main():
    # Connect to MySQL database
    conn = connect_to_mysql(DB_CONFIG)
    if conn:
        # Numbering all lines
        lines_to_process = list(range(1, 19)) + [41, 51]

        for line in lines_to_process:
            print(f"Fetching transfer schedule for line {line}...")
            # fetch the transferring schedule
            station_data_list = get_transfer_schedule_from_api(line)

            if station_data_list:
                for station_data in station_data_list:
                    store_transfer_data_to_mysql(station_data, conn)

        conn.close()
    else:
        print("Failed to connect to MySQL database.")

if __name__ == "__main__":
    main()
