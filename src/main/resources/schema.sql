CREATE TABLE IF NOT EXISTS categories (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    is_active BOOLEAN DEFAULT TRUE
);

CREATE TABLE IF NOT EXISTS menu_items (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    description TEXT NOT NULL,
    price DECIMAL(5,2) NOT NULL,
    image_url VARCHAR(255),
    category_id BIGINT REFERENCES categories(id),
    is_active BOOLEAN DEFAULT TRUE
);

CREATE TABLE IF NOT EXISTS extras (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    price DECIMAL(5,2) NOT NULL,
    is_active BOOLEAN DEFAULT TRUE
);

CREATE TABLE IF NOT EXISTS menu_item_extras (
    menu_item_id BIGINT REFERENCES menu_items(id),
    extra_id BIGINT REFERENCES extras(id),
    PRIMARY KEY (menu_item_id, extra_id)
);

CREATE TABLE IF NOT EXISTS orders (
    id BIGSERIAL PRIMARY KEY,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    is_paid BOOLEAN DEFAULT FALSE,
    is_fulfilled BOOLEAN DEFAULT FALSE,
    total_price DECIMAL(7,2) NOT NULL,
    customer_email VARCHAR(255),
    stripe_session_id VARCHAR(255)
);

CREATE TABLE IF NOT EXISTS ordered_items (
    id BIGSERIAL PRIMARY KEY,
    quantity INT NOT NULL,
    price_at_purchase DECIMAL(5,2) NOT NULL,
    menu_item_id BIGINT REFERENCES menu_items(id),
    order_id BIGINT REFERENCES orders(id)
);

CREATE TABLE IF NOT EXISTS selected_extras (
    id BIGSERIAL PRIMARY KEY,
    price_at_purchase DECIMAL(5,2) NOT NULL,
    extra_id BIGINT REFERENCES extras(id),
    ordered_item_id BIGINT REFERENCES ordered_items(id)
);

CREATE TABLE IF NOT EXISTS users (
    id BIGSERIAL PRIMARY KEY,
    user_name VARCHAR(255) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    role VARCHAR(50) NOT NULL
);