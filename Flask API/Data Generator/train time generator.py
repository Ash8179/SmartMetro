import requests
import random
from datetime import datetime, timedelta
import time
import json

def generate_station_pair():
    ranges = [
        (111, 138), (234, 263), (311, 339), (401, 426),
        (507, 513), (531, 538), (621, 648), (721, 753),
        (820, 849), (918, 952), (1041, 1074), (1114, 1120),
        (1131, 1163), (1220, 1251), (1321, 1351), (1421, 1451),
        (1521, 1550), (1621, 1633), (1721, 1734), (1821, 1846)
    ]
    selected_range = random.choice(ranges)
    start, end = selected_range
    current_id = random.randint(start, end)
    direction = random.choice([1, -1])
    next_id = current_id + direction
    if next_id < start or next_id > end:
        direction *= -1
        next_id = current_id + direction
    return current_id, next_id, direction

def generate_timestamp():
    now = datetime.now()
    delta_seconds = random.randint(-1800, 1800)  # 正负30分钟，单位秒
    random_time = now + timedelta(seconds=delta_seconds)
    return random_time.strftime("%Y-%m-%dT%H:%M:%S")

def generate_train_data():
    station_id, next_station_id, direction = generate_station_pair()
    line_id = station_id // 100  # 根据station_id计算line_id
    train_number = f"{random.randint(1001, 9999)}"  # 随机生成列车编号
    return {
        "train_number": train_number,
        "line_id": line_id,
        "station_id": str(station_id),
        "next_station_id": str(next_station_id),
        "timestamp": generate_timestamp(),
        "path_id": "1",
        "direction": direction
    }

def post_data(record):
    try:
        print("准备发送：")
        print(json.dumps(record, indent=4, ensure_ascii=False))
        response = requests.post(
            "http://localhost:5005/api/train/report_status",
            json=record,
            timeout=5
        )
        if response.status_code == 200:
            print("成功提交\n")
        else:
            print(f"提交失败，状态码: {response.status_code}\n")
        return response
    except Exception as e:
        print(f"网络错误: {str(e)}\n")
        return None

if __name__ == "__main__":
    print("Starting train data generator (with line_id)...")
    while True:
        records = [generate_train_data() for _ in range(10)]
        success_count = 0
        for record in records:
            response = post_data(record)
            if response and response.status_code == 200:
                success_count += 1
        print(f"本轮成功提交 {success_count}/10 条记录 {datetime.now().strftime('%H:%M:%S')}\n")
        time.sleep(1)
