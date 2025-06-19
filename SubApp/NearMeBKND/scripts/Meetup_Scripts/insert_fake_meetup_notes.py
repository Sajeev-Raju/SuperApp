import sqlite3
import random
from faker import Faker

DB_PATH = r'C:\Users\chandra kiran\Downloads\nearmecommunity\nearmecommunity\nearmecommunity.db'
fake = Faker()

conn = sqlite3.connect(DB_PATH)
cur = conn.cursor()

# Get all meetup IDs
cur.execute('SELECT id FROM mtp_meetup')
meetup_ids = [row[0] for row in cur.fetchall()]

note_count = 0
for meetup_id in meetup_ids:
    num_notes = random.choice([1, 2])
    for _ in range(num_notes):
        note = fake.sentence(nb_words=random.randint(8, 20))
        cur.execute(
            'INSERT INTO mtp_note (meetup_id, note) VALUES (?, ?)',
            (meetup_id, note)
        )
        note_count += 1
        if note_count % 1000 == 0:
            print(f"Inserted {note_count} notes...")

conn.commit()
conn.close()
print(f"Inserted {note_count} fake notes into mtp_note table.") 