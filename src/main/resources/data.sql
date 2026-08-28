
-- ---------- Categories ----------

INSERT INTO categories (name, is_active)
SELECT 'Burgers', TRUE
WHERE NOT EXISTS (SELECT 1 FROM categories WHERE name = 'Burgers');

INSERT INTO categories (name, is_active)
SELECT 'Pizza', TRUE
WHERE NOT EXISTS (SELECT 1 FROM categories WHERE name = 'Pizza');

INSERT INTO categories (name, is_active)
SELECT 'Drinks', TRUE
WHERE NOT EXISTS (SELECT 1 FROM categories WHERE name = 'Drinks');

-- ---------- Menu items ----------

INSERT INTO menu_items (name, description, price, image_url, category_id, is_active)
SELECT 'Cheeseburger', 'Beef patty, aged cheddar, pickles and house sauce in a brioche bun.',
       9.50, '/images/cheeseburger.jpg', c.id, TRUE
FROM categories c
WHERE c.name = 'Burgers'
  AND NOT EXISTS (SELECT 1 FROM menu_items WHERE name = 'Cheeseburger');

INSERT INTO menu_items (name, description, price, image_url, category_id, is_active)
SELECT 'Bacon Burger', 'Double smoked bacon, beef patty, cheddar and crisp lettuce.',
       11.00, '/images/bacon-burger.jpg', c.id, TRUE
FROM categories c
WHERE c.name = 'Burgers'
  AND NOT EXISTS (SELECT 1 FROM menu_items WHERE name = 'Bacon Burger');

INSERT INTO menu_items (name, description, price, image_url, category_id, is_active)
SELECT 'Margherita Pizza', 'San Marzano tomato, fior di latte and fresh basil on a slow-proved base.',
       10.50, '/images/pizza.jpg', c.id, TRUE
FROM categories c
WHERE c.name = 'Pizza'
  AND NOT EXISTS (SELECT 1 FROM menu_items WHERE name = 'Margherita Pizza');

INSERT INTO menu_items (name, description, price, image_url, category_id, is_active)
SELECT 'Cola', 'Chilled classic cola, 0.33l.',
       3.00, '/images/cola.jpg', c.id, TRUE
FROM categories c
WHERE c.name = 'Drinks'
  AND NOT EXISTS (SELECT 1 FROM menu_items WHERE name = 'Cola');

-- ---------- Extras ----------

INSERT INTO extras (name, price, is_active)
SELECT 'Extra Cheese', 1.00, TRUE
WHERE NOT EXISTS (SELECT 1 FROM extras WHERE name = 'Extra Cheese');

INSERT INTO extras (name, price, is_active)
SELECT 'Bacon', 1.50, TRUE
WHERE NOT EXISTS (SELECT 1 FROM extras WHERE name = 'Bacon');

INSERT INTO extras (name, price, is_active)
SELECT 'Grilled Onions', 0.80, TRUE
WHERE NOT EXISTS (SELECT 1 FROM extras WHERE name = 'Grilled Onions');

-- ---------- Which extras are offered on which item ----------

INSERT INTO menu_item_extras (menu_item_id, extra_id)
SELECT m.id, e.id
FROM menu_items m
JOIN extras e ON e.name IN ('Extra Cheese', 'Bacon', 'Grilled Onions')
WHERE m.name IN ('Cheeseburger', 'Bacon Burger')
  AND NOT EXISTS (
      SELECT 1 FROM menu_item_extras mie
      WHERE mie.menu_item_id = m.id AND mie.extra_id = e.id
  );

INSERT INTO menu_item_extras (menu_item_id, extra_id)
SELECT m.id, e.id
FROM menu_items m
JOIN extras e ON e.name IN ('Extra Cheese', 'Grilled Onions')
WHERE m.name = 'Margherita Pizza'
  AND NOT EXISTS (
      SELECT 1 FROM menu_item_extras mie
      WHERE mie.menu_item_id = m.id AND mie.extra_id = e.id
  );
