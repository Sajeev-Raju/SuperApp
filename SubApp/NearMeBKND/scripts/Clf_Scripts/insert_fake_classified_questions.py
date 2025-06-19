import sqlite3
import random
from faker import Faker
from datetime import timedelta, datetime

DB_PATH = r'C:\Users\chandra kiran\Downloads\nearmecommunity\nearmecommunity\nearmecommunity.db'
fake = Faker()

conn = sqlite3.connect(DB_PATH)
cur = conn.cursor()

# Get last 10k users from each area
cur.execute('SELECT location_name FROM user_locations GROUP BY location_name')
areas = [row[0] for row in cur.fetchall()]
area_users = {}
for area in areas:
    cur.execute('SELECT user_id FROM user_locations WHERE location_name = ? ORDER BY id DESC LIMIT 10000', (area,))
    area_users[area] = [row[0] for row in cur.fetchall()]

# Get classifieds: id, user_id, created_at, area
cur.execute('SELECT id, user_id, created_at FROM clf_classified')
classifieds = cur.fetchall()
classifieds_by_area = {area: [] for area in areas}
user_area = {}
for area in areas:
    for user_id in area_users[area]:
        user_area[user_id] = area
for clf_id, owner_id, clf_created_at in classifieds:
    area = user_area.get(owner_id)
    if area:
        classifieds_by_area[area].append((clf_id, owner_id, clf_created_at))

question_count = 0
for area in areas:
    users = area_users[area]
    clfs = classifieds_by_area[area]
    if not users or not clfs:
        continue
    for user_id in users:
        # Exclude classifieds posted by this user
        eligible_clfs = [c for c in clfs if c[1] != user_id]
        if not eligible_clfs:
            continue
        clf_id, owner_id, clf_created_at = random.choice(eligible_clfs)
        # Convert string to datetime object
        try:
            clf_created_at_dt = datetime.strptime(clf_created_at, '%Y-%m-%d %H:%M:%S')
        except ValueError:
            clf_created_at_dt = datetime.strptime(clf_created_at, '%Y-%m-%d %H:%M:%S.%f')
        clf_created_dt = fake.date_time_between(start_date=clf_created_at_dt, end_date='now')
        question_content = fake.sentence(nb_words=random.randint(8, 20))
        cur.execute(
            'INSERT INTO clf_question (classified_id, content, created_at, user_id) VALUES (?, ?, ?, ?)',
            (clf_id, question_content, clf_created_dt, user_id)
        )
        question_count += 1
        if question_count % 1000 == 0:
            print(f"Area {area}: {question_count} questions inserted...")

conn.commit()
conn.close()
print(f"Inserted {question_count} fake questions into clf_question table (one per user, per area).") 