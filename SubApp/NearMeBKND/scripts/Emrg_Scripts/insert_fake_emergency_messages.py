import sqlite3
import random
from faker import Faker
from datetime import datetime, timedelta

DB_PATH = r'..\nearmecommunity.db'
fake = Faker()

emergency_types = [
    'Medical Emergency',
    'Financial Funds',
    'Accident',
    'Weather Alert',
    'Fire',
    'Natural Disaster',
    'Crime',
    'Missing Person',
    'Animal Rescue',
    'Other'
]

conn = sqlite3.connect(DB_PATH)
cur = conn.cursor()

# Get first 2k users from each area
cur.execute('SELECT location_name FROM user_locations GROUP BY location_name')
areas = [row[0] for row in cur.fetchall()]
area_users = {}
for area in areas:
    cur.execute('SELECT user_id, latitude, longitude FROM user_locations WHERE location_name = ? ORDER BY id LIMIT 2000', (area,))
    area_users[area] = cur.fetchall()

message_count = 0
type_count = 0
for area in areas:
    users = area_users[area]
    for user_id, lat, lon in users:
        title = fake.sentence(nb_words=6)
        description = fake.paragraph(nb_sentences=3)
        details = fake.text(max_nb_chars=100)
        created_at = fake.date_time_between(start_date='-2y', end_date='now')
        google_maps_location = f'https://www.google.com/maps?q={lat},{lon}'
        # Pick 1-3 random types
        types = random.sample(emergency_types, k=random.randint(1, 3))
        types_csv = ','.join(types)
        # Insert into emergency_message
        cur.execute(
            'INSERT INTO emergency_message (title, description, types, details, google_maps_location, user_id, latitude, longitude, created_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)',
            (title, description, types_csv, details, google_maps_location, user_id, lat, lon, created_at)
        )
        message_id = cur.lastrowid
        # Insert each type into message_type_map
        for t in types:
            cur.execute(
                'INSERT INTO message_type_map (message_id, type) VALUES (?, ?)',
                (message_id, t)
            )
            type_count += 1
        message_count += 1
        if message_count % 500 == 0:
            print(f"Inserted {message_count} emergency messages...")

conn.commit()
conn.close()
print(f"Inserted {message_count} fake emergency messages and {type_count} types into emergency_message and message_type_map tables.") 