DROP TABLE IF EXISTS order_items;
DROP TABLE IF EXISTS orders;
DROP TABLE IF EXISTS items;

CREATE TABLE IF NOT EXISTS items (
      id BIGSERIAL PRIMARY KEY,
      title VARCHAR(200) NOT NULL,
      description VARCHAR(1000) NOT NULL,
      img_path VARCHAR(500),
      price BIGINT NOT NULL);

CREATE TABLE IF NOT EXISTS orders (
      id BIGSERIAL PRIMARY KEY,
      order_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
      total_sum BIGINT NOT NULL);

CREATE TABLE IF NOT EXISTS order_items (
      id BIGSERIAL PRIMARY KEY,
      item_id BIGINT NOT NULL,
      title VARCHAR(200) NOT NULL,
      price BIGINT NOT NULL,
      count INT NOT NULL,
      order_id BIGINT NOT NULL,
      CONSTRAINT fk_order_items_order FOREIGN KEY (order_id) REFERENCES orders(id) ON DELETE CASCADE);

INSERT INTO items (title, description, img_path, price) VALUES
('Мяч футбольный', 'Профессиональный футбольный мяч, размер 5, из высококачественной синтетической кожи.', '/images/ball.jpg', 2500),
('Теннисная ракетка', 'Облегченная теннисная ракетка для начинающих и любителей.', '/images/racket.jpg', 4500),
('Беговые кроссовки', 'Удобные беговые кроссовки с амортизацией, размер 42-46.', '/images/sneakers.jpg', 8900),
('Футболка спортивная', 'Дышащая спортивная футболка из полиэстера, размеры S-XXL.', '/images/tshirt.jpg', 1200)
