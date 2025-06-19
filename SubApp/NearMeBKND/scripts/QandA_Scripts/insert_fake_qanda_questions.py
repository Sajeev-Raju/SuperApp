import sqlite3
import random
from datetime import datetime, timedelta

DB_PATH = r"../nearmecommunity.db"

AREAS = ["Patancheruvu", "Kokapet", "Hayathnagar", "Nagaram"]
USERS_PER_AREA = 10000
TAGS = [
    "food", "restaurants", "recommendation", "shopping", "health", "education", "events", "services", "travel", "sports",
    "entertainment", "kids", "family", "offers", "emergency", "transport", "jobs", "housing", "community", "news"
]

QUESTION_TITLES = [
    "What is the best place for {tag}?",
    "Any recommendations for {tag}?",
    "How to find good {tag} in this area?",
    "Looking for advice on {tag}.",
    "Where can I get {tag}?",
    "Suggestions for {tag} nearby?",
    "Is there any {tag} open now?",
    "Best time for {tag}?",
    "Affordable {tag} options?",
    "Who provides {tag} services?"
]

QUESTION_DESCRIPTIONS = [
    "I am new to the area and looking for some help.",
    "Any locals with tips?",
    "Would appreciate your suggestions!",
    "Please share your experiences.",
    "Trying to find the best options.",
    "What do you recommend?",
    "Is there a hidden gem?",
    "How do you usually find this?",
    "Any recent experiences?",
    "Thanks in advance for your help!"
]

def random_tags():
    n = random.randint(1, 3)
    return random.sample(TAGS, n)

def random_title(tag):
    return random.choice(QUESTION_TITLES).format(tag=tag)

def random_description():
    return random.choice(QUESTION_DESCRIPTIONS)

def random_date():
    # Random date within the last 180 days
    return (datetime.now() - timedelta(days=random.randint(0, 180), seconds=random.randint(0, 86400))).strftime('%Y-%m-%d %H:%M:%S')

def main():
    conn = sqlite3.connect(DB_PATH)
    cur = conn.cursor()
    questions = []
    total_questions = 0
    for area in AREAS:
        cur.execute("SELECT user_id, latitude, longitude FROM user_locations WHERE location_name = ?", (area,))
        users = cur.fetchall()
        if len(users) > USERS_PER_AREA:
            users = random.sample(users, USERS_PER_AREA)
        for user_id, lat, lon in users:
            num_questions = random.choice([1, 2])
            for _ in range(num_questions):
                tags = random_tags()
                title = random_title(tags[0])
                description = random_description()
                created_at = random_date()
                questions.append((user_id, title, description, ','.join(tags), lat, lon, created_at))
                total_questions += 1
    # Batch insert
    BATCH_SIZE = 1000
    for i in range(0, len(questions), BATCH_SIZE):
        batch = questions[i:i+BATCH_SIZE]
        cur.executemany(
            """
            INSERT INTO qna_questions (user_id, title, description, tags, latitude, longitude, created_at)
            VALUES (?, ?, ?, ?, ?, ?, ?)
            """,
            batch
        )
    conn.commit()
    conn.close()
    print(f"Inserted {total_questions} fake questions into qna_questions table.")

if __name__ == "__main__":
    main() 