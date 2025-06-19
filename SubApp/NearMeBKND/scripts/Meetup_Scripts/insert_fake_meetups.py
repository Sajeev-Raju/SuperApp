import sqlite3
import random
import os
from faker import Faker
from datetime import datetime, timedelta

DB_PATH = r'..\nearmecommunity.db'
IMAGES_DIR = r'../src/main/resources/images'
fake = Faker()

# Hyderabad names (sample pool)
hyd_men = ["Ravi Kumar", "Srinivas Reddy", "Rajesh Goud", "Venkatesh Rao", "Anil Kumar"]
hyd_women = ["Suma Reddy", "Lakshmi Rao", "Anitha Goud", "Swapna Reddy", "Divya Rao"]
possible_tags = ["Tech", "Networking", "Hyderabad", "Startup", "Women", "Education", "Health", "Career", "Coding", "Business"]

conn = sqlite3.connect(DB_PATH)
cur = conn.cursor()

# Get first 3k users from each area with location_name
cur.execute('SELECT location_name FROM user_locations GROUP BY location_name')
areas = [row[0] for row in cur.fetchall()]
area_users = {}
for area in areas:
    cur.execute('SELECT user_id, latitude, longitude, location_name FROM user_locations WHERE location_name = ? ORDER BY id LIMIT 3000', (area,))
    area_users[area] = cur.fetchall()

# Get image file paths
image_files = [os.path.join(IMAGES_DIR, f) for f in os.listdir(IMAGES_DIR) if os.path.isfile(os.path.join(IMAGES_DIR, f))]

meetup_count = 0
tag_count = 0
for area in areas:
    users = area_users[area]
    for user_id, lat, lon, location_name in users:
        title = fake.sentence(nb_words=6)
        description = fake.paragraph(nb_sentences=3)
        created_at = fake.date_time_between(start_date='-2y', end_date='now')
        image_path = random.choice(image_files)
        with open(image_path, 'rb') as imgf:
            image_blob = imgf.read()
        # Random Hyderabad name
        organizer_name = random.choice(hyd_men + hyd_women)
        # Event address based on location_name
        event_address = f"{fake.building_number()} {fake.street_name()}, {location_name}, Hyderabad"
        is_active = 1
        max_participants = random.randint(10, 100)
        contact_info = fake.phone_number()
        # Generate future start/end date/time
        start_dt = fake.date_time_between(start_date='+1d', end_date='+30d')
        end_dt = start_dt + timedelta(hours=random.randint(1, 8))
        start_date = start_dt.date().isoformat()
        start_time = start_dt.time().strftime('%H:%M:%S')
        end_date = end_dt.date().isoformat()
        end_time = end_dt.time().strftime('%H:%M:%S')
        google_location_url = f'https://www.google.com/maps?q={lat},{lon}'
        # Insert into mtp_meetup
        cur.execute(
            'INSERT INTO mtp_meetup (organizer_name, title, description, image_url, latitude, longitude, google_location_url, created_at, organizer_id, event_address, is_active, max_participants, contact_info, start_date, start_time, end_date, end_time) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)',
            (organizer_name, title, description, image_blob, lat, lon, google_location_url, created_at, user_id, event_address, is_active, max_participants, contact_info, start_date, start_time, end_date, end_time)
        )
        meetup_id = cur.lastrowid
        # Generate 1-3 random tags and insert into mtp_meetup_tag
        tags = random.sample(possible_tags, k=random.randint(1, 3))
        for tag in tags:
            cur.execute(
                'INSERT INTO mtp_meetup_tag (meetupId, tag) VALUES (?, ?)',
                (meetup_id, tag)
            )
            tag_count += 1
        meetup_count += 1
        if meetup_count % 500 == 0:
            print(f"Inserted {meetup_count} meetups...")

conn.commit()
conn.close()
print(f"Inserted {meetup_count} fake meetups and {tag_count} tags into mtp_meetup and mtp_meetup_tag tables.") 