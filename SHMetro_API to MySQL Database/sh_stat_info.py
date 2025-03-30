import requests
import mysql.connector
from mysql.connector import Error
import json

# MySQL Config
DB_CONFIG = {
    'host': 'localhost',
    'database': 'new_schema',
    'user': 'root',
    'password': 'zwy040103'
}

# API URL
API_URL = "http://m.shmetro.com/core/shmetro/mdstationinfoback.ashx?act=getAllStations"

def connect_to_mysql():
    """Connecting to MySQL"""
    try:
        conn = mysql.connector.connect(**DB_CONFIG)
        if conn.is_connected():
            print('Connected to MySQL database')
            return conn
    except Error as e:
        print(f"Error while connecting to MySQL: {e}")
        return None

def store_station_data_to_mysql(station_data, conn):
    """insert data to MySQL database"""
    cursor = conn.cursor()

    # insert data to MySQL database
    insert_query = """
    INSERT INTO sh_stat_info (
        stat_id, name_cn
    )
    VALUES (%s, %s)
    """

    # Extract and Insert JSON data
    data = (
        station_data.get("key"),
        station_data.get("value"),
    )

    # Print
    print(f"Data being inserted: {data}")
    print(f"Data length: {len(data)}")

    # Insert
    try:
        cursor.execute(insert_query, data)
        conn.commit()
        print(f"sh_stat_info {station_data.get('name_cn')} data inserted into database.")
    except Error as e:
        print(f"Error inserting data: {e}")

def get_station_data_from_api():
    """从 API 获取站点数据"""
    try:
        response = requests.get(API_URL)
        response.raise_for_status()
        return response.json()
    except requests.exceptions.RequestException as e:
        print(f"Error fetching data from API: {e}")
        return None

def main():
    station_data_list = get_station_data_from_api()

    if station_data_list:
        # connect
        conn = connect_to_mysql()
        if conn:
            for station_data in station_data_list:
                store_station_data_to_mysql(station_data, conn)
            # close the connection
            conn.close()
        else:
            print("Failed to connect to MySQL database.")
    else:
        print("Failed to retrieve station data from API.")

if __name__ == "__main__":
    main()
