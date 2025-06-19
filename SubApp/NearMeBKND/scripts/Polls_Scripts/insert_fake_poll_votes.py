import sqlite3
import random
from faker import Faker
from datetime import datetime, timedelta

DB_PATH = r'..\nearmecommunity.db'
fake = Faker()

conn = sqlite3.connect(DB_PATH)
cur = conn.cursor()

# Begin a single transaction for all inserts
cur.execute('BEGIN TRANSACTION')

# Get last 10k users from each area
cur.execute('SELECT location_name FROM user_locations GROUP BY location_name')
areas = [row[0] for row in cur.fetchall()]
area_users = {}
for area in areas:
    cur.execute('SELECT user_id FROM user_locations WHERE location_name = ? ORDER BY id DESC LIMIT 10000', (area,))
    area_users[area] = [row[0] for row in cur.fetchall()]

# Get all poll questions and their options, and created_at
cur.execute('SELECT question_id, selection_limit, created_at FROM poll_questions')
questions = cur.fetchall()
question_options = {}
question_created_at = {}
for qid, selection_limit, created_at in questions:
    question_options[qid] = []
    question_created_at[qid] = created_at
# Fetch all poll options at once and group by question_id
cur.execute('SELECT question_id, option_id FROM poll_options')
for qid, option_id in cur.fetchall():
    if qid in question_options:
        question_options[qid].append(option_id)

def parse_datetime(dt_str):
    try:
        return datetime.strptime(dt_str, '%Y-%m-%d %H:%M:%S')
    except ValueError:
        return datetime.strptime(dt_str, '%Y-%m-%d %H:%M:%S.%f')

vote_count = 0
for area in areas:
    users = area_users[area]
    for user_id in users:
        # Pick a random poll question
        qid, selection_limit, created_at = random.choice(questions)
        options = question_options[qid]
        # Pick up to selection_limit options (at least 1)
        num_votes = random.randint(1, selection_limit)
        voted_options = random.sample(options, k=num_votes)
        # Ensure vote time is after poll question's created_at
        poll_created_at_dt = parse_datetime(created_at)
        for option_id in voted_options:
            vote_created_at = fake.date_time_between(start_date=poll_created_at_dt, end_date='now')
            try:
                cur.execute(
                    'INSERT OR IGNORE INTO poll_votes (user_id, question_id, option_id, created_at) VALUES (?, ?, ?, ?)',
                    (user_id, qid, option_id, vote_created_at)
                )
                vote_count += 1
            except Exception as e:
                print(f"Vote insert error: {e}")
        if vote_count % 10000 == 0:
            print(f"Inserted {vote_count} poll votes...")

conn.commit()
conn.close()
print(f"Inserted {vote_count} poll votes into the database.") 