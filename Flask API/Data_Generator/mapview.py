import requests
import mysql.connector
from mysql.connector import Error
import json
import time

# MySQL 配置
DB_CONFIG = {
    'host': 'localhost',
    'database': 'new_schema',
    'user': 'root',
    'password': 'zwy040103'
}

def connect_to_mysql():
    try:
        conn = mysql.connector.connect(**DB_CONFIG)
        if conn.is_connected():
            print('Connected to MySQL database')
            return conn
    except Error as e:
        print(f"Error while connecting to MySQL: {e}")
        return None

def store_station_data_to_mysql(station_data, conn):
    cursor = conn.cursor()

    insert_query = """
    INSERT INTO MapView (
        stat_id, name_cn, name_en, line, longitude, latitude, gao_lng, gao_lat
    ) VALUES (%s, %s, %s, %s, %s, %s, %s, %s)
    """

    data = (
        station_data.get("stat_id"),
        station_data.get("name_cn"),
        station_data.get("name_en"),
        station_data.get("lines"),
        station_data.get("longitude"),
        station_data.get("latitude"),
        station_data.get("gao_lng"),
        station_data.get("gao_lat")
    )

    try:
        cursor.execute(insert_query, data)
        conn.commit()
        print(f"Station {station_data.get('name_cn')} (ID: {station_data.get('stat_id')}) inserted.")
    except Error as e:
        print(f"Error inserting data for stat_id {station_data.get('stat_id')}: {e}")

def get_station_data_from_api(stat_id):
    url = f"http://m.shmetro.com/interface/metromap/metromap.aspx?func=stationInfo&stat_id={stat_id:04d}"
    try:
        response = requests.get(url)
        response.raise_for_status()
        return response.json()  # 返回的是列表
    except requests.exceptions.RequestException as e:
        print(f"Error fetching data for stat_id {stat_id}: {e}")
        return None

def main():
    stat_ids = range(5131, 5138)

    conn = connect_to_mysql()
    if not conn:
        return

    for stat_id in stat_ids:
        print(f"Fetching data for stat_id: {stat_id:04d}")
        data_list = get_station_data_from_api(stat_id)
        if data_list:
            for station_data in data_list:
                store_station_data_to_mysql(station_data, conn)
        else:
            print(f"No data returned for stat_id {stat_id}")
        time.sleep(3)  # 防止频率太快被封IP

    conn.close()

if __name__ == "__main__":
    main()
