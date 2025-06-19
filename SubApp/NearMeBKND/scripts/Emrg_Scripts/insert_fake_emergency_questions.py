import sqlite3
import random
from faker import Faker
from datetime import datetime

DB_PATH = r'..\nearmecommunity.db'
fake = Faker()

conn = sqlite3.connect(DB_PATH)
cur = conn.cursor()

# Get all location names
cur.execute('SELECT DISTINCT location_name FROM user_locations')
areas = [row[0] for row in cur.fetchall()]

# For each area, get last 1k users
area_users = {}
for area in areas:
    cur.execute('SELECT user_id FROM user_locations WHERE location_name = ? ORDER BY id DESC LIMIT 1000', (area,))
    area_users[area] = [row[0] for row in cur.fetchall()]

# Get all emergency messages with their id and created_at
cur.execute('SELECT id, created_at FROM emergency_message')
emergencies = cur.fetchall()

def parse_datetime(dt_str):
    try:
        return datetime.strptime(dt_str, '%Y-%m-%d %H:%M:%S')
    except ValueError:
        return datetime.strptime(dt_str, '%Y-%m-%d %H:%M:%S.%f')

question_count = 0
for area in areas:
    users = area_users[area]
    if not users or not emergencies:
        continue
    for user_id in users:
        num_questions = random.choice([1, 2])
        for _ in range(num_questions):
            em_id, em_created_at = random.choice(emergencies)
            em_created_at_dt = parse_datetime(em_created_at)
            question_content = fake.sentence(nb_words=random.randint(8, 20))
            question_created_at = fake.date_time_between(start_date=em_created_at_dt, end_date='now')
            cur.execute(
                'INSERT INTO emergency_question (emergency_id, content, created_at, user_id) VALUES (?, ?, ?, ?)',
                (em_id, question_content, question_created_at, user_id)
            )
            question_count += 1
            if question_count % 500 == 0:
                print(f"Inserted {question_count} emergency questions...")

conn.commit()
conn.close()
print(f"Inserted {question_count} fake emergency questions into emergency_question table.") 