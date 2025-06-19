CREATE TABLE IF NOT EXISTS clf_categories (
    id INTEGER PRIMARY KEY,
    clf_id INTEGER NOT NULL,
    category TEXT NOT NULL,
    FOREIGN KEY (clf_id) REFERENCES clf_classified(id) ON DELETE CASCADE
); 