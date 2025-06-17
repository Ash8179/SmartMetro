import random
import mysql.connector

# Connect to MySQL
conn = mysql.connector.connect(
    host='localhost',
    user='root',
    password='zwy040103',
    database='new_schema'
)

cursor = conn.cursor()

# Fetch all stations with travel_group
cursor.execute("SELECT stat_id, name_cn, travel_group FROM travel_info WHERE travel_group IS NOT NULL")
stations = cursor.fetchall()

# Build travel_group to checkpoint count mapping
travel_group_map = {}
for station in stations:
    travel_group = station[2]
    if travel_group not in travel_group_map:
        # Assign once per travel group
        travel_group_map[travel_group] = random.randint(2, 5)

# Insert data for each station using its group's checkpoint count
for station in stations:
    stat_id, name_cn, travel_group = station
    num_checkpoints = travel_group_map[travel_group]

    for checkpoint_id in range(1, num_checkpoints + 1):
        person_num = random.randint(10, 200)  # Simulate number of people
        cursor.execute(
            """
            INSERT INTO checkpoint_congestion (stat_id, name_cn, travel_group, checkpoint_id, person_num)
            VALUES (%s, %s, %s, %s, %s)
            """,
            (stat_id, name_cn, travel_group, checkpoint_id, person_num)
        )

# Commit and close
conn.commit()
cursor.close()
conn.close()

print("Simulation data inserted successfully.")
