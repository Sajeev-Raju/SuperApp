import sqlite3
import random
from faker import Faker

DB_PATH = r'..\nearmecommunity.db'
fake = Faker()

conn = sqlite3.connect(DB_PATH)
cur = conn.cursor()

# Get all emergency message IDs
cur.execute('SELECT id FROM emergency_message')
emergency_ids = [row[0] for row in cur.fetchall()]

# Randomly select half of the emergency messages
half_count = len(emergency_ids) // 2
selected_ids = random.sample(emergency_ids, half_count)

note_count = 0
for emergency_id in selected_ids:
    num_notes = random.choice([1, 2])
    for _ in range(num_notes):
        note = fake.sentence(nb_words=random.randint(8, 20))
        cur.execute(
            'INSERT INTO emergency_note (emergency_id, note) VALUES (?, ?)',
            (emergency_id, note)
        )
        note_count += 1
        if note_count % 1000 == 0:
            print(f"Inserted {note_count} emergency notes...")

conn.commit()
conn.close()
print(f"Inserted {note_count} fake emergency notes into emergency_note table.") 