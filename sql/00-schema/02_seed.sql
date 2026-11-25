-- ============================================================================
--  SEED DATA.  Small on purpose: every expected output in this folder can be
--  verified by hand in under a minute.
--  Deliberate quirks baked in, because the questions depend on them:
--    * Farah has NULL dept_id            -> JOIN vs LEFT JOIN, NOT IN + NULL
--    * Zoya and Kabir are duplicate rows -> dedup questions
--    * salary 95000 appears twice        -> RANK vs DENSE_RANK vs ROW_NUMBER
--    * customer 5 has never ordered      -> anti-join
--    * product 5 was never sold          -> anti-join
--    * user 7 logs in with a >30min gap  -> sessionisation
-- ============================================================================

INSERT INTO departments (dept_id, dept_name, location) VALUES
 (1,'Engineering','Pune'), (2,'Finance','Mumbai'), (3,'HR','Indore'), (4,'Legal','Pune');

INSERT INTO employees (emp_id, emp_name, dept_id, manager_id, salary, hire_date, email) VALUES
 (1,'Rhea',   1, NULL, 210000,'2015-03-01','rhea@corp.com'),
 (2,'Arjun',  1, 1,    140000,'2017-06-15','arjun@corp.com'),
 (3,'Meera',  1, 1,     95000,'2019-01-20','meera@corp.com'),
 (4,'Sahil',  1, 2,     95000,'2020-11-02','sahil@corp.com'),
 (5,'Ishaan', 1, 2,     72000,'2022-04-11','ishaan@corp.com'),
 (6,'Tara',   2, 1,    160000,'2016-08-09','tara@corp.com'),
 (7,'Vikram', 2, 6,    118000,'2018-02-26','vikram@corp.com'),
 (8,'Zoya',   2, 6,     88000,'2021-07-19','zoya@corp.com'),
 (9,'Zoya',   2, 6,     88000,'2021-07-19','zoya@corp.com'),   -- exact duplicate row
 (10,'Nikhil',3, 1,     99000,'2017-10-05','nikhil@corp.com'),
 (11,'Kabir', 3, 10,    64000,'2023-01-16','kabir@corp.com'),
 (12,'Kabir', 3, 10,    64000,'2023-01-16','kabir@corp.com'),  -- exact duplicate row
 (13,'Farah', NULL, 1,  130000,'2019-09-30','farah@corp.com'); -- no department

INSERT INTO salary_history (emp_id, effective, salary) VALUES
 (2,'2017-06-15', 90000), (2,'2019-04-01',115000), (2,'2022-04-01',140000),
 (3,'2019-01-20', 70000), (3,'2021-07-01', 95000),
 (7,'2018-02-26', 80000), (7,'2020-01-01', 99000), (7,'2023-01-01',118000);

INSERT INTO customers (cust_id, cust_name, city, signup_date) VALUES
 (1,'Alpha Ltd','Pune','2024-11-02'), (2,'Beta Corp','Mumbai','2024-12-14'),
 (3,'Gamma LLP','Indore','2025-01-05'), (4,'Delta Inc','Pune','2025-01-22'),
 (5,'Epsilon Co','Mumbai','2025-02-10');       -- never ordered

INSERT INTO products (product_id, product, category, unit_price) VALUES
 (1,'Laptop','Hardware',65000), (2,'Monitor','Hardware',14000),
 (3,'Keyboard','Accessory',2500), (4,'Mouse','Accessory',900),
 (5,'Docking Station','Hardware',11000);        -- never sold

INSERT INTO orders (order_id, cust_id, order_date, amount) VALUES
 (101,1,'2025-01-07', 65000), (102,1,'2025-01-19', 2500),
 (103,2,'2025-01-23',140000), (104,3,'2025-02-04', 14000),
 (105,1,'2025-02-11',  3400), (106,2,'2025-02-18', 65000),
 (107,4,'2025-03-02', 16500), (108,3,'2025-03-14',  1800),
 (109,1,'2025-03-27', 65000), (110,2,'2025-04-06', 28000);

INSERT INTO order_items (order_id, product_id, qty) VALUES
 (101,1,1),(102,3,1),(103,1,2),(103,2,1),(104,2,1),(105,3,1),(105,4,1),
 (106,1,1),(107,2,1),(107,3,1),(108,4,2),(109,1,1),(110,2,2);

INSERT INTO payments (payment_id, cust_id, paid_at, amount) VALUES
 (1,1,'2025-01-07 10:04:00', 65000),
 (2,1,'2025-01-07 10:07:30', 65000),   -- duplicate within 10 minutes
 (3,2,'2025-01-23 15:20:00',140000),
 (4,3,'2025-02-04 09:11:00', 14000),
 (5,1,'2025-02-11 18:45:00',  3400),
 (6,2,'2025-02-18 11:02:00', 65000),
 (7,2,'2025-02-18 11:05:00', 65000),   -- duplicate within 10 minutes
 (8,4,'2025-03-02 13:30:00', 16500);

INSERT INTO logins (login_id, user_id, login_at) VALUES
 (1,7,'2025-05-01 09:00:00'), (2,7,'2025-05-01 09:12:00'), (3,7,'2025-05-01 09:25:00'),
 (4,7,'2025-05-01 11:40:00'), (5,7,'2025-05-01 11:55:00'),   -- new session (>30 min gap)
 (6,7,'2025-05-02 08:10:00'), (7,7,'2025-05-03 08:15:00'), (8,7,'2025-05-04 20:01:00'),
 (9,7,'2025-05-05 07:45:00'),                                -- 5 consecutive days: 01..05
 (10,8,'2025-05-01 10:00:00'), (11,8,'2025-05-03 10:00:00'), (12,8,'2025-05-04 10:00:00');
