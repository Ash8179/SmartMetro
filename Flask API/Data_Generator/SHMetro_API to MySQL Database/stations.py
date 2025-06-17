import requests
import mysql.connector
from mysql.connector import Error

# MySQL Config
DB_CONFIG = {
    'host': 'localhost',  # stations database
    'database': 'new_schema',
    'user': 'root',
    'password': 'zwy040103'
}

SH_STAT_INFO_DB_CONFIG = {
    'host': 'localhost',  # sh_stat_info database
    'database': 'new_schema',
    'user': 'root',
    'password': 'zwy040103'
}

def connect_to_mysql(config):
    """连接到 MySQL"""
    try:
        conn = mysql.connector.connect(**config)
        if conn.is_connected():
            print(f"Connected to MySQL database: {config['database']}")
            return conn
    except Error as e:
        print(f"Error while connecting to MySQL: {e}")
        return None

def get_stat_ids_from_sh_stat_info(conn):
    """从 sh_stat_info 获取所有 stat_id"""
    cursor = conn.cursor()
    cursor.execute("SELECT stat_id FROM sh_stat_info")
    stat_ids = cursor.fetchall()  # fetch all stat_id
    return [stat_id[0] for stat_id in stat_ids]

def store_station_data_to_mysql(station_data, conn):
    """将站点数据存储到 MySQL 数据库"""
    cursor = conn.cursor()

    insert_query = """
    INSERT INTO stations (
        stat_id, name_cn, name_en, pinyin, station_code, line, longitude, latitude, 
        gao_lng, gao_lat, x, y, station_pic, toilet_inside, toilet_position, 
        toilet_position_en, entrance_info, entrance_info_en, street_pic, elevator, elevator_en, terminal
    )
    VALUES (%s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s)
    """
    data = (
        station_data.get("stat_id"),
        station_data.get("name_cn"),
        station_data.get("name_en", ""),
        station_data.get("pinyin", ""),
        station_data.get("station_code", ""),
        station_data.get("lines", ""),
        station_data.get("longitude", 0),
        station_data.get("latitude", 0),
        station_data.get("gao_lng", 0),
        station_data.get("gao_lat", 0),
        station_data.get("x", 0),
        station_data.get("y", 0),
        station_data.get("station_pic", ""),
        station_data.get("toilet_inside", False),
        station_data.get("toilet_position", ""),
        station_data.get("toilet_position_en", ""),
        station_data.get("entrance_info", ""),
        station_data.get("entrance_info_en", ""),
        station_data.get("street_pic", ""),
        station_data.get("elevator", ""),
        station_data.get("elevator_en", ""),
        station_data.get("terminal", 0)
    )

    try:
        cursor.execute(insert_query, data)
        conn.commit()
        print(f"Station {station_data.get('name_cn')} data inserted into database.")
    except Error as e:
        print(f"Error inserting data: {e}")

def get_station_data_from_api(stat_id):
    """根据 stat_id 从 API 获取站点数据"""
    API_URL = f"http://m.shmetro.com/interface/metromap/metromap.aspx?func=stationInfo&stat_id={stat_id}"
    try:
        response = requests.get(API_URL)
        response.raise_for_status()
        data = response.json()
        
        # Print
        print(f"API Data for stat_id {stat_id}: {data}")
        
        # 如果返回的数据是一个列表（假设第一个元素是站点数据）
        if isinstance(data, list):
            return data[0]  # 返回列表中的第一个站点数据字典
        elif isinstance(data, dict):
            return data  # 如果返回的是字典，则直接返回字典
        else:
            print(f"Unexpected data format for stat_id {stat_id}")
            return None
    except requests.exceptions.RequestException as e:
        print(f"Error fetching data from API for stat_id {stat_id}: {e}")
        return None

def main():
    # fetch stat_id from sh_stat_info
    map_info_conn = connect_to_mysql(SH_STAT_INFO_DB_CONFIG)
    if map_info_conn:
        stat_ids = get_stat_ids_from_sh_stat_info(map_info_conn)

        conn = connect_to_mysql(DB_CONFIG)
        if conn:
            for stat_id in stat_ids:
                station_data = get_station_data_from_api(stat_id)
                if station_data:
                    store_station_data_to_mysql(station_data, conn)
            conn.close()
        map_info_conn.close()
    else:
        print("Failed to connect to Map_Info database.")

if __name__ == "__main__":
    main()
