import sqlite3
import random
import os
from datetime import datetime, timedelta
from faker import Faker

# Use the absolute path to the database
DB_PATH = r'..\nearmecommunity.db'
# IMAGES_DIR = 'src/main/resources/images'
IMAGES_DIR = r'..\src\main\resources\images'
AREAS = [
    "Patancheruvu",
    "Kokapet",
    "Hayathnagar",
    "Nagaram"
]
USERS_PER_AREA = 1000
CATEGORIES = ["Electronics", "Fashion", "Home", "Health", "Automobile", "Books", "Sports", "Toys"]
DETAIL_KEYS = ["brand", "condition", "color", "size", "warranty", "material", "usage", "features"]

fake = Faker()

# Connect to DB
conn = sqlite3.connect(DB_PATH)
cur = conn.cursor()

# Get 10k random users from each area
user_query = f"""
SELECT user_id, latitude, longitude, location_name FROM user_locations
WHERE location_name = ?
ORDER BY RANDOM() LIMIT ?
"""
users = []
for area in AREAS:
    cur.execute(user_query, (area, USERS_PER_AREA))
    users += cur.fetchall()
print(f"Selected {len(users)} users from 4 areas.")

# Get image file paths
image_files = [os.path.join(IMAGES_DIR, f) for f in os.listdir(IMAGES_DIR) if os.path.isfile(os.path.join(IMAGES_DIR, f))]

classifieds = []
details = []
classified_count = 0
for user in users:
    user_id, lat, lon, area = user
    num_classifieds = 1  # Each user creates exactly one classified
    for _ in range(num_classifieds):
        title = fake.sentence(nb_words=6)
        description = fake.paragraph(nb_sentences=3)
        price = random.randint(100, 50000)
        categories = ','.join(random.sample(CATEGORIES, k=random.randint(1, 3)))
        created_at = fake.date_time_between(start_date='-2y', end_date='now')
        image_path = random.choice(image_files)
        with open(image_path, 'rb') as imgf:
            image_blob = imgf.read()
        # Insert into clf_classified
        cur.execute(
            """
            INSERT INTO clf_classified (title, description, price, image_url, categories, user_id, latitude, longitude, created_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
            """,
            (title, description, price, image_blob, categories, user_id, lat, lon, created_at)
        )
        classified_id = cur.lastrowid
        # Insert each category into clf_categories
        for cat in categories.split(","):
            cur.execute(
                """
                INSERT INTO clf_categories (clf_id, category)
                VALUES (?, ?)
                """,
                (classified_id, cat.strip())
            )
        # Generate classified details
        num_details = random.randint(2, 5)
        used_keys = random.sample(DETAIL_KEYS, k=num_details)
        for key in used_keys:
            value = fake.word() if key != "features" else ', '.join(fake.words(nb=random.randint(2, 5)))
            cur.execute(
                """
                INSERT INTO clf_classified_detail (classified_id, key, value)
                VALUES (?, ?, ?)
                """,
                (classified_id, key, value)
            )
        classified_count += 1
        if classified_count % 1000 == 0:
            print(f"Inserted {classified_count} classifieds...")

conn.commit()
conn.close()
print(f"Inserted {classified_count} classifieds and their details into the database.") 