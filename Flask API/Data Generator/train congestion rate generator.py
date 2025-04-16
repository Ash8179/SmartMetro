import requests
import random
import time

# Replace with your real endpoint
POST_URL = "http://127.0.0.1:5004/crowding"
TRUNCATE_URL = "http://127.0.0.1:5004/clear_crowding"

def truncate_table():
    try:
        response = requests.post(TRUNCATE_URL)
        if response.status_code == 200:
            print("Table truncated successfully.")
        else:
            print(f"Failed to truncate table: {response.status_code}")
    except Exception as e:
        print(f"Error truncating table: {e}")

def update_crowding_data():
    for line_number in range(1, 19):  # Lines 1 to 18
        for line_carriage in range(1, 9):  # Carriages 1 to 8
            # For each carriage, update both path_id = 0 and path_id = 1
            for path_id in [0, 1]:
                person_num = random.randint(0, 36)  # Random number of people between 0 and 50

                payload = {
                    "line_number": line_number,
                    "line_carriage": line_carriage,
                    "person_num": person_num,
                    "path_id": path_id
                }

                try:
                    response = requests.post(POST_URL, params=payload)
                    if response.status_code == 200:
                        print(f"Updated: {payload}")
                    else:
                        print(f"Failed to update: {payload} | Status: {response.status_code}")
                except Exception as e:
                    print(f"Error sending data: {e}")

def main():
    while True:
        truncate_table()  # Clear previous data
        update_crowding_data()  # Update with new random data
        print("Waiting for 30 seconds before next update...")
        time.sleep(30)  # Wait for 30 seconds before the next update cycle

if __name__ == "__main__":
    main()
