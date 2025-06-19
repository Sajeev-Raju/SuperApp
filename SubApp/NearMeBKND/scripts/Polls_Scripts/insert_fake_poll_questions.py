import sqlite3
import random
from faker import Faker
from datetime import datetime, timedelta

DB_PATH = r'..\nearmecommunity.db'
fake = Faker()

conn = sqlite3.connect(DB_PATH)
cur = conn.cursor()

# Get first 10k users from each area
cur.execute('SELECT location_name FROM user_locations GROUP BY location_name')
areas = [row[0] for row in cur.fetchall()]
area_users = {}
for area in areas:
    cur.execute('SELECT user_id, latitude, longitude FROM user_locations WHERE location_name = ? ORDER BY id LIMIT 10000', (area,))
    area_users[area] = cur.fetchall()

question_count = 0
option_count = 0
for area in areas:
    users = area_users[area]
    for idx, (user_id, lat, lon) in enumerate(users):
        # 80%: one poll, 4 options, limit 1, mode single
        if idx < int(0.8 * len(users)):
            num_questions = 1
            num_options = 4
            selection_limit = 1
        else:
            num_questions = random.randint(1, 5)
        for _ in range(num_questions):
            if idx < int(0.8 * len(users)):
                num_options = 4
                selection_limit = 1
            else:
                num_options = random.randint(2, 4)
                selection_limit = random.randint(1, min(3, num_options-1)) if num_options > 2 else 1
            selection_mode = 'single' if selection_limit == 1 else 'multiple'
            question_text = fake.sentence(nb_words=random.randint(6, 12))
            created_at = fake.date_time_between(start_date='-2y', end_date='now')
            # Insert poll question
            cur.execute(
                'INSERT INTO poll_questions (user_id, question_text, selection_limit, selection_mode, latitude, longitude, created_at) VALUES (?, ?, ?, ?, ?, ?, ?)',
                (user_id, question_text, selection_limit, selection_mode, lat, lon, created_at)
            )
            question_id = cur.lastrowid
            question_count += 1
            # Insert poll options
            for _ in range(num_options):
                option_text = fake.word().capitalize()
                cur.execute(
                    'INSERT INTO poll_options (question_id, option_text) VALUES (?, ?)',
                    (question_id, option_text)
                )
                option_count += 1
            if question_count % 1000 == 0:
                print(f"Inserted {question_count} poll questions...")

conn.commit()
conn.close()
print(f"Inserted {question_count} poll questions and {option_count} poll options into the database.") 