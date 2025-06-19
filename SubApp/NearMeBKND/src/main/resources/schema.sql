-- NearMe Module Tables
CREATE TABLE IF NOT EXISTS user_locations (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    user_id TEXT NOT NULL,
    latitude REAL NOT NULL,
    longitude REAL NOT NULL,
    location_name TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(user_id)
);




-- Classified Module Tables
CREATE TABLE IF NOT EXISTS clf_classified (
    id INTEGER PRIMARY KEY,
    title TEXT NOT NULL,
    description TEXT NOT NULL,
    price INTEGER NOT NULL,
    image_url BLOB,
    categories TEXT,
    user_id TEXT NOT NULL,
    latitude REAL NOT NULL,
    longitude REAL NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);


CREATE INDEX IF NOT EXISTS idx_clf_classified_latitude ON clf_classified(latitude);
CREATE INDEX IF NOT EXISTS idx_clf_classified_longitude ON clf_classified(longitude);
CREATE INDEX IF NOT EXISTS idx_clf_classified_user_id ON clf_classified(user_id);


CREATE TABLE IF NOT EXISTS clf_classified_detail (
    id INTEGER PRIMARY KEY,
    classified_id INTEGER,
    key TEXT NOT NULL,
    value TEXT NOT NULL,
    FOREIGN KEY (classified_id) REFERENCES clf_classified(id)
);


CREATE TABLE IF NOT EXISTS clf_categories (
    id INTEGER PRIMARY KEY,
    clf_id INTEGER NOT NULL,
    category TEXT NOT NULL,
    FOREIGN KEY (clf_id) REFERENCES clf_classified(id) ON DELETE CASCADE
);


CREATE TABLE IF NOT EXISTS clf_category (
    id INTEGER PRIMARY KEY,
    name TEXT UNIQUE
);

CREATE TABLE IF NOT EXISTS clf_classified_category_map (
    classified_id INTEGER,
    category_id INTEGER,
    PRIMARY KEY (classified_id, category_id),
    FOREIGN KEY (classified_id) REFERENCES clf_classified(id),
    FOREIGN KEY (category_id) REFERENCES clf_category(id)
);

CREATE TABLE IF NOT EXISTS clf_question (
    id INTEGER PRIMARY KEY,
    classified_id INTEGER,
    content TEXT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    user_id TEXT NOT NULL,
    FOREIGN KEY (classified_id) REFERENCES clf_classified(id)
);

CREATE TABLE IF NOT EXISTS clf_note (
    id INTEGER PRIMARY KEY,
    classified_id INTEGER NOT NULL,
    note TEXT NOT NULL,
    FOREIGN KEY (classified_id) REFERENCES clf_classified(id)
);

-- QnA Module Tables
CREATE TABLE IF NOT EXISTS qna_questions (
    id INTEGER PRIMARY KEY,
    user_id VARCHAR(255) NOT NULL,
    title VARCHAR(255) NOT NULL,
    description TEXT NOT NULL,
    tags TEXT,  -- Store tags as comma-separated values
    latitude REAL NOT NULL,
    longitude REAL NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS qna_note (
    id INTEGER PRIMARY KEY,
    question_id INTEGER NOT NULL,
    note TEXT NOT NULL,
    FOREIGN KEY (question_id) REFERENCES qna_questions(id)
);

CREATE TABLE IF NOT EXISTS qna_answers (
    id INTEGER PRIMARY KEY,
    question_id INTEGER NOT NULL,
    user_id TEXT NOT NULL,
    description TEXT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (question_id) REFERENCES qna_questions(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS qna_tags (
    tagId INTEGER PRIMARY KEY,
    tagName VARCHAR(50) NOT NULL UNIQUE
);

CREATE TABLE IF NOT EXISTS qna_question_tag (
    qId INTEGER,
    tagId INTEGER,
    PRIMARY KEY (qId, tagId),
    FOREIGN KEY (qId) REFERENCES qna_questions(id) ON DELETE CASCADE,
    FOREIGN KEY (tagId) REFERENCES qna_tags(tagId) ON DELETE CASCADE
);

-- Emergency Module Tables
CREATE TABLE IF NOT EXISTS emergency_message (
    id INTEGER PRIMARY KEY,
    title TEXT NOT NULL,
    description TEXT,
    types TEXT NOT NULL, -- CSV format of types
    details TEXT, -- JSON format
    google_maps_location TEXT,
    user_id TEXT NOT NULL, -- Added user_id column
    latitude REAL, -- Add this line for latitude
    longitude REAL, -- Add this line for longitude
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS emergency_note (
    id INTEGER PRIMARY KEY,
    emergency_id INTEGER NOT NULL,
    note TEXT NOT NULL,
    FOREIGN KEY (emergency_id) REFERENCES emergency_message(id)
);

CREATE TABLE IF NOT EXISTS message_type_map (
    message_id INTEGER,
    type TEXT,
    FOREIGN KEY (message_id) REFERENCES emergency_message(id)
);

CREATE TABLE IF NOT EXISTS emergency_question (
    id INTEGER PRIMARY KEY,
    emergency_id INTEGER NOT NULL,
    content TEXT NOT NULL,
    user_id TEXT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (emergency_id) REFERENCES emergency_message(id)
);

-- -- NearMe Module Tables
-- CREATE TABLE IF NOT EXISTS user_locations (
--     id INTEGER PRIMARY KEY AUTOINCREMENT,
--     user_id TEXT NOT NULL,
--     latitude REAL NOT NULL,
--     longitude REAL NOT NULL,
--     location_name TEXT,
--     created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
--     UNIQUE(user_id)
-- );

-- Polls Module Tables
CREATE TABLE IF NOT EXISTS poll_questions (
    question_id INTEGER PRIMARY KEY,
    user_id TEXT NOT NULL,
    question_text TEXT NOT NULL,
    selection_limit INTEGER NOT NULL,
    selection_mode TEXT NOT NULL,
    latitude REAL NOT NULL,
    longitude REAL NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES user_locations(user_id)
);

CREATE INDEX IF NOT EXISTS idx_poll_questions_latitude ON poll_questions(latitude);
CREATE INDEX IF NOT EXISTS idx_poll_questions_longitude ON poll_questions(longitude);
CREATE INDEX IF NOT EXISTS idx_poll_questions_created_at ON poll_questions(created_at);
CREATE INDEX IF NOT EXISTS idx_poll_questions_user_id ON poll_questions(user_id);


CREATE TABLE IF NOT EXISTS poll_options (
    option_id INTEGER PRIMARY KEY,
    question_id INTEGER NOT NULL,
    option_text TEXT NOT NULL,
    FOREIGN KEY (question_id) REFERENCES poll_questions(question_id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS poll_votes (
    vote_id INTEGER PRIMARY KEY,
    user_id TEXT NOT NULL,
    question_id INTEGER NOT NULL,
    option_id INTEGER NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES user_locations(user_id),
    FOREIGN KEY (question_id) REFERENCES poll_questions(question_id) ON DELETE CASCADE,
    FOREIGN KEY (option_id) REFERENCES poll_options(option_id) ON DELETE CASCADE,
    UNIQUE(user_id, question_id, option_id)
);

-- Meetup Module Tables
CREATE TABLE IF NOT EXISTS mtp_meetup (
    id INTEGER PRIMARY KEY,
    organizer_name VARCHAR(255) NOT NULL,
    title VARCHAR(255) NOT NULL,
    description TEXT NOT NULL,
    start_date DATE NOT NULL,
    start_time TIME NOT NULL,
    end_date DATE NOT NULL,
    end_time TIME NOT NULL,
    organizer_id VARCHAR(255) NOT NULL,
    event_address VARCHAR(255) NOT NULL,
    image_url BLOB,
    latitude DOUBLE NOT NULL,
    longitude DOUBLE NOT NULL,
    google_location_url VARCHAR(1000),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    max_participants INT,
    contact_info VARCHAR(255),
   
    FOREIGN KEY (organizer_id) REFERENCES user_locations(user_id)
);

CREATE INDEX IF NOT EXISTS idx_mtp_meetup_organizer_id ON mtp_meetup(organizer_id);
CREATE INDEX IF NOT EXISTS idx_mtp_meetup_start_time ON mtp_meetup(start_time);
CREATE INDEX IF NOT EXISTS idx_mtp_meetup_is_active ON mtp_meetup(is_active);
-- CREATE INDEX idx_meetup_is_active ON mtp_meetup(is_active);
CREATE INDEX idx_meetup_created_at ON mtp_meetup(created_at);
CREATE INDEX IF NOT EXISTS idx_meetup_latitude ON mtp_meetup(latitude);
CREATE INDEX IF NOT EXISTS idx_meetup_longitude ON mtp_meetup(longitude);
CREATE INDEX IF NOT EXISTS idx_meetup_is_active ON mtp_meetup(is_active);
-- CREATE INDEX idx_meetup_organizer_id ON mtp_meetup(organizer_id);

CREATE TABLE IF NOT EXISTS mtp_meetup_tag (
    meetupId INTEGER,
    tag TEXT NOT NULL,
    PRIMARY KEY (meetupId, tag),
    FOREIGN KEY (meetupId) REFERENCES mtp_meetup(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_mtp_meetup_tag_meetupId ON mtp_meetup_tag(meetupId);

CREATE TABLE IF NOT EXISTS mtp_meetup_question (
    id INTEGER PRIMARY KEY,
    meetup_id INTEGER NOT NULL,
    user_id TEXT NOT NULL,
    content TEXT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (meetup_id) REFERENCES mtp_meetup(id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES user_locations(user_id)
);

CREATE TABLE IF NOT EXISTS mtp_note (
    id INTEGER PRIMARY KEY,
    meetup_id INTEGER NOT NULL,
    note TEXT NOT NULL,
    FOREIGN KEY (meetup_id) REFERENCES mtp_meetup(id)
);

-- Business Module Tables

CREATE TABLE IF NOT EXISTS business (
    business_id INTEGER PRIMARY KEY,
    user_id VARCHAR(6) NOT NULL,
    name VARCHAR(255) NOT NULL,
    title VARCHAR(255) NOT NULL,
    tags VARCHAR(255),
    description TEXT NOT NULL,
    image BLOB,
    googlemapsURL VARCHAR(500),
    longitude REAL,
    latitude REAL,
    address VARCHAR(255) NOT NULL,
    mobile_number VARCHAR(20) NOT NULL,
    timings TEXT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    active BOOLEAN NOT NULL DEFAULT true,
    FOREIGN KEY (user_id) REFERENCES user_locations(user_id)
);

CREATE INDEX idx_business_user_id ON business(user_id);
CREATE INDEX idx_business_latitude ON business(latitude);
CREATE INDEX idx_business_longitude ON business(longitude);
CREATE INDEX idx_business_active ON business(active);
CREATE INDEX idx_business_created_at ON business(created_at);


CREATE TABLE IF NOT EXISTS business_tags (
    tag_id INTEGER PRIMARY KEY,
    business_id INT NOT NULL,
    tag VARCHAR(100) NOT NULL,
    FOREIGN KEY (business_id) REFERENCES business(business_id)
);

CREATE TABLE IF NOT EXISTS business_notification (
    notification_id INTEGER PRIMARY KEY,
    business_id INT NOT NULL,
    user_id VARCHAR(6) NOT NULL,
    message TEXT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (business_id) REFERENCES business(business_id),
    FOREIGN KEY (user_id) REFERENCES user_locations(user_id)
);

CREATE TABLE IF NOT EXISTS business_questions (
    question_id INTEGER PRIMARY KEY,
    business_id INTEGER NOT NULL,
    user_id VARCHAR(6) NOT NULL,
    question_text TEXT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (business_id) REFERENCES business(business_id),
    FOREIGN KEY (user_id) REFERENCES user_locations(user_id)
);

CREATE TABLE IF NOT EXISTS business_answers (
    answer_id INTEGER PRIMARY KEY,
    question_id INTEGER NOT NULL,
    user_id VARCHAR(6) NOT NULL,
    answer_text TEXT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (question_id) REFERENCES business_questions(question_id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES user_locations(user_id)
);

CREATE TABLE IF NOT EXISTS business_feedback (
    feedback_id INTEGER PRIMARY KEY,
    business_id INTEGER NOT NULL,
    user_id VARCHAR(6) NOT NULL,
    feedback_text TEXT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (business_id) REFERENCES business(business_id),
    FOREIGN KEY (user_id) REFERENCES user_locations(user_id)
);

CREATE TABLE IF NOT EXISTS business_feedback_reply (
    reply_id INTEGER PRIMARY KEY,
    feedback_id INTEGER NOT NULL UNIQUE,
    user_id VARCHAR(6) NOT NULL,
    reply_text TEXT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (feedback_id) REFERENCES business_feedback(feedback_id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES user_locations(user_id)
);