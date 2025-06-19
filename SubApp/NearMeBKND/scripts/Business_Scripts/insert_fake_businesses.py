import sqlite3
import random
import os
import math
from datetime import datetime, timedelta

# Path to your SQLite DB
DB_PATH = '../nearmecommunity.db'

# Path to images directory
IMAGES_DIR = '../src/main/resources/images'

# Area locations and Google Maps URLs
AREAS = [
    ("Patancheruvu", 17.5333, 78.2651),
    ("Kokapet", 17.3849, 78.3317),
    ("Hayathnagar", 17.3295, 78.6092),
    ("Nagaram", 17.4863, 78.6235),
]

OWN_LOCATION_BUSINESSES = 5000
RANDOM_BUSINESSES = 15000
TOTAL_BUSINESSES = OWN_LOCATION_BUSINESSES + RANDOM_BUSINESSES

def random_string(prefix):
    return f"{prefix}_{random.randint(10000, 99999)}"

def random_mobile():
    return f"9{random.randint(100000000, 999999999)}"

def random_timings():
    days = ["Mon-Fri", "Mon-Sat", "All Week"]
    return f"{random.choice(days)}: {random.randint(8, 11)}am-{random.randint(5, 10)}pm"

def random_date():
    return (datetime.now() - timedelta(days=random.randint(0, 365))).strftime('%Y-%m-%d %H:%M:%S')

def random_point_away(lat, lon, min_km=1, max_km=15):
    # Random point between min_km and max_km from (lat, lon)
    radius_km = random.uniform(min_km, max_km)
    radius_in_degrees = radius_km / 111  # Approximate
    u = random.random()
    v = random.random()
    w = radius_in_degrees * math.sqrt(u)
    t = 2 * math.pi * v
    dx = w * math.cos(t)
    dy = w * math.sin(t)
    return lat + dy, lon + dx / math.cos(math.radians(lat))

# Get image filenames
image_files = [f for f in os.listdir(IMAGES_DIR) if os.path.isfile(os.path.join(IMAGES_DIR, f))]
if not image_files:
    raise Exception(f"No images found in {IMAGES_DIR}")

# Connect to DB
conn = sqlite3.connect(DB_PATH)
cur = conn.cursor()

# Fetch all users and their locations
cur.execute("SELECT user_id, latitude, longitude, location_name FROM user_locations")
users = cur.fetchall()  # List of (user_id, lat, lon, location_name)

used_user_ids = set()
businesses = []

# 1. 5,000 businesses at user's own location (users from any area, selected randomly)
own_location_users = random.sample(users, OWN_LOCATION_BUSINESSES)
for user_id, lat, lon, location_name in own_location_users:
    used_user_ids.add(user_id)
    gmaps_url = f"https://www.google.com/maps?q={lat},{lon}"
    image_file = random.choice(image_files)
    with open(os.path.join(IMAGES_DIR, image_file), 'rb') as imgf:
        image_bytes = imgf.read()
    businesses.append((
        user_id,
        random_string("Business"),
        ",".join([random_string("tag") for _ in range(3)]),
        random_string("Title"),
        f"This is a fake business description.",
        location_name,
        random_mobile(),
        random_timings(),
        gmaps_url,
        lat,
        lon,
        image_bytes,
        random_date()
    ))

# 2. 15,000 businesses at random location 1-15km away from user's location (no repeats)
remaining_users = [u for u in users if u[0] not in used_user_ids]
selected_random = random.sample(remaining_users, RANDOM_BUSINESSES)
for user_id, lat, lon, location_name in selected_random:
    used_user_ids.add(user_id)
    # Generate random location 1-15km away
    new_lat, new_lon = random_point_away(lat, lon, 1, 15)
    gmaps_url = f"https://www.google.com/maps?q={new_lat},{new_lon}"
    image_file = random.choice(image_files)
    with open(os.path.join(IMAGES_DIR, image_file), 'rb') as imgf:
        image_bytes = imgf.read()
    businesses.append((
        user_id,
        random_string("Business"),
        ",".join([random_string("tag") for _ in range(3)]),
        random_string("Title"),
        f"This is a fake business description.",
        location_name,
        random_mobile(),
        random_timings(),
        gmaps_url,
        new_lat,
        new_lon,
        image_bytes,
        random_date()
    ))

# Insert businesses in batches
BATCH_SIZE = 500
for i in range(0, len(businesses), BATCH_SIZE):
    batch = businesses[i:i+BATCH_SIZE]
    cur.executemany(
        """
        INSERT INTO business (user_id, name, tags, title, description, address, mobile_number, timings, googlemapsURL, latitude, longitude, image, created_at)
        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """,
        batch
    )

conn.commit()
conn.close()
print(f"Inserted {len(businesses)} fake businesses into business table.") 