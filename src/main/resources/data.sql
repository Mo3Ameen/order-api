INSERT INTO categories (name) VALUES 
('Burgers'), 
('Pizzas'), 
('Drinks');

INSERT INTO extras (name, price) VALUES 
('Extra Cheese', 1.50), 
('Bacon', 2.00), 
('Mushrooms', 1.00);

INSERT INTO menu_items (name, description, price, image_url, category_id) VALUES 
('Classic Cheeseburger', '100% Beef with cheddar cheese', 8.50, 'https://example.com/images/cheeseburger.jpg', 1),
('Margherita Pizza', 'Classic tomato sauce and mozzarella', 10.00, 'https://example.com/images/margherita.jpg', 2),
('Cola', 'Chilled 330ml can', 2.50, 'https://example.com/images/cola.jpg', 3);

INSERT INTO menu_item_extras (menu_item_id, extra_id) VALUES (1, 1);
INSERT INTO menu_item_extras (menu_item_id, extra_id) VALUES (1, 2);
INSERT INTO menu_item_extras (menu_item_id, extra_id) VALUES (2, 1);
INSERT INTO menu_item_extras (menu_item_id, extra_id) VALUES (2, 3);

INSERT INTO orders (is_paid, total_price, customer_email) VALUES (FALSE, 12.00, 'customer@example.com');

INSERT INTO ordered_items (quantity, price_at_purchase, order_id, menu_item_id) VALUES 
(1, 8.50, 1, 1);

INSERT INTO selected_extras (price_at_purchase, ordered_item_id, extra_id) VALUES 
(1.50, 1, 1),
(2.00, 1, 2);