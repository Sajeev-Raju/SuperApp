import sqlite3
import random
from faker import Faker
from datetime import datetime

DB_PATH = r'C:\Users\chandra kiran\Downloads\nearmecommunity\nearmecommunity\nearmecommunity.db'
fake = Faker()

conn = sqlite3.connect(DB_PATH)
cur = conn.cursor()

# Get all location names
cur.execute('SELECT DISTINCT location_name FROM user_locations')
areas = [row[0] for row in cur.fetchall()]

# For each area, get last 5k users
area_users = {}
for area in areas:
    cur.execute('SELECT user_id FROM user_locations WHERE location_name = ? ORDER BY id DESC LIMIT 5000', (area,))
    area_users[area] = [row[0] for row in cur.fetchall()]

# Get all meetups with their id, event_address, created_at
cur.execute('SELECT id, event_address, created_at FROM mtp_meetup')
meetups = cur.fetchall()

# Group meetups by area (location_name in event_address)
area_meetups = {area: [] for area in areas}
for meetup_id, event_address, created_at in meetups:
    for area in areas:
        if area in event_address:
            area_meetups[area].append((meetup_id, created_at))
            break

def parse_datetime(dt_str):
    try:
        return datetime.strptime(dt_str, '%Y-%m-%d %H:%M:%S')
    except ValueError:
        return datetime.strptime(dt_str, '%Y-%m-%d %H:%M:%S.%f')

question_count = 0
for area in areas:
    users = area_users[area]
    meetups_in_area = area_meetups[area]
    if not users or not meetups_in_area:
        continue
    for user_id in users:
        meetup_id, meetup_created_at = random.choice(meetups_in_area)
        meetup_created_at_dt = parse_datetime(meetup_created_at)
        question_content = fake.sentence(nb_words=random.randint(8, 20))
        question_created_at = fake.date_time_between(start_date=meetup_created_at_dt, end_date='now')
        cur.execute(
            'INSERT INTO mtp_meetup_question (meetup_id, user_id, content, created_at) VALUES (?, ?, ?, ?)',
            (meetup_id, user_id, question_content, question_created_at)
        )
        question_count += 1
        if question_count % 1000 == 0:
            print(f"Inserted {question_count} meetup questions...")

conn.commit()
conn.close()
print(f"Inserted {question_count} fake meetup questions into mtp_meetup_question table.") 