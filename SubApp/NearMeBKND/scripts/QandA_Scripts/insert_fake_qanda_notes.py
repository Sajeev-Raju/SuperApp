import sqlite3
import random
from datetime import datetime, timedelta

DB_PATH = r"../nearmecommunity.db"

EXAMPLE_NOTES = [
    "Thanks for the answers!",
    "Adding more details to my question.",
    "This is an important point.",
    "I found some more information.",
    "Please clarify if you need more info.",
    "Here is an update.",
    "I appreciate the help from the community.",
    "This is a follow-up note.",
    "Let me know if you have similar experiences.",
    "I will update when I get more answers."
]

def random_note():
    return random.choice(EXAMPLE_NOTES)

def main():
    conn = sqlite3.connect(DB_PATH)
    cur = conn.cursor()
    notes = []
    total_notes = 0
    cur.execute("SELECT id, user_id FROM qna_questions")
    questions = cur.fetchall()
    for q_id, user_id in questions:
        num_notes = random.randint(1, 3)
        for _ in range(num_notes):
            note = random_note()
            notes.append((q_id, note))
            total_notes += 1
    # Batch insert
    BATCH_SIZE = 1000
    for i in range(0, len(notes), BATCH_SIZE):
        batch = notes[i:i+BATCH_SIZE]
        cur.executemany(
            """
            INSERT INTO qna_note (question_id, note)
            VALUES (?, ?)
            """,
            batch
        )
    conn.commit()
    conn.close()
    print(f"Inserted {total_notes} fake notes into qna_note table.")

if __name__ == "__main__":
    main() 