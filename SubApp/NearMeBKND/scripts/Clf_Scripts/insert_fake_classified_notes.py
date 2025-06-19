import sqlite3
import random
from faker import Faker

DB_PATH = r'C:\Users\chandra kiran\Downloads\nearmecommunity\nearmecommunity\nearmecommunity.db'
fake = Faker()

conn = sqlite3.connect(DB_PATH)
cur = conn.cursor()

# Get all classified IDs
cur.execute('SELECT id FROM clf_classified')
classified_ids = [row[0] for row in cur.fetchall()]

# Randomly select half of the classifieds
half_count = len(classified_ids) // 2
selected_ids = random.sample(classified_ids, half_count)

note_count = 0
for clf_id in selected_ids:
    num_notes = random.choice([1, 2])
    for _ in range(num_notes):
        note = fake.sentence(nb_words=random.randint(6, 15))
        cur.execute(
            'INSERT INTO clf_note (classified_id, note) VALUES (?, ?)',
            (clf_id, note)
        )
        note_count += 1
        if note_count % 1000 == 0:
            print(f"Inserted {note_count} notes...")

conn.commit()
conn.close()
print(f"Inserted {note_count} fake notes for {half_count} classifieds into clf_note table.") 