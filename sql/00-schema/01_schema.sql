-- ============================================================================
--  SCHEMA for every query in this folder.
--  Dialect: ANSI SQL that runs unchanged on SQLite 3.25+, PostgreSQL and
--  MySQL 8+. Where a query needs a dialect-specific function the file says so.
--
--  Load with:   sqlite3 prep.db < 01_schema.sql && sqlite3 prep.db < 02_seed.sql
--  or:          python3 ../run_all.py          (from the sql/ folder)
-- ============================================================================

DROP TABLE IF EXISTS order_items;
DROP TABLE IF EXISTS payments;
DROP TABLE IF EXISTS orders;
DROP TABLE IF EXISTS products;
DROP TABLE IF EXISTS customers;
DROP TABLE IF EXISTS logins;
DROP TABLE IF EXISTS salary_history;
DROP TABLE IF EXISTS employees;
DROP TABLE IF EXISTS departments;
DROP TABLE IF EXISTS bank_statement;
DROP TABLE IF EXISTS ledger;

CREATE TABLE departments (
    dept_id     INTEGER PRIMARY KEY,
    dept_name   TEXT    NOT NULL,
    location    TEXT    NOT NULL
);

CREATE TABLE employees (
    emp_id      INTEGER PRIMARY KEY,
    emp_name    TEXT    NOT NULL,
    dept_id     INTEGER REFERENCES departments(dept_id),
    manager_id  INTEGER REFERENCES employees(emp_id),   -- NULL for the CEO
    salary      INTEGER NOT NULL,
    hire_date   TEXT    NOT NULL,                        -- ISO yyyy-mm-dd
    email       TEXT
);

CREATE TABLE salary_history (
    emp_id      INTEGER REFERENCES employees(emp_id),
    effective   TEXT    NOT NULL,
    salary      INTEGER NOT NULL
);

CREATE TABLE customers (
    cust_id     INTEGER PRIMARY KEY,
    cust_name   TEXT    NOT NULL,
    city        TEXT    NOT NULL,
    signup_date TEXT    NOT NULL
);

CREATE TABLE products (
    product_id  INTEGER PRIMARY KEY,
    product     TEXT    NOT NULL,
    category    TEXT    NOT NULL,
    unit_price  INTEGER NOT NULL
);

CREATE TABLE orders (
    order_id    INTEGER PRIMARY KEY,
    cust_id     INTEGER REFERENCES customers(cust_id),
    order_date  TEXT    NOT NULL,
    amount      INTEGER NOT NULL
);

CREATE TABLE order_items (
    order_id    INTEGER REFERENCES orders(order_id),
    product_id  INTEGER REFERENCES products(product_id),
    qty         INTEGER NOT NULL
);

CREATE TABLE payments (
    payment_id  INTEGER PRIMARY KEY,
    cust_id     INTEGER REFERENCES customers(cust_id),
    paid_at     TEXT    NOT NULL,                        -- ISO yyyy-mm-dd hh:mm:ss
    amount      INTEGER NOT NULL
);

CREATE TABLE logins (
    login_id    INTEGER PRIMARY KEY,
    user_id     INTEGER NOT NULL,
    login_at    TEXT    NOT NULL                         -- ISO yyyy-mm-dd hh:mm:ss
);

-- Indexes an interviewer would expect on this shape of schema.
CREATE INDEX idx_emp_dept      ON employees(dept_id);
CREATE INDEX idx_emp_manager   ON employees(manager_id);
CREATE INDEX idx_orders_cust   ON orders(cust_id, order_date);
CREATE INDEX idx_payments_cust ON payments(cust_id, paid_at);
CREATE INDEX idx_logins_user   ON logins(user_id, login_at);

-- S24 reconciliation: what the bank says happened vs what our system posted.
-- Money is INTEGER here (Oracle NUMBER(18,2), PostgreSQL NUMERIC(18,2)); never FLOAT.
CREATE TABLE bank_statement (
    stmt_id     INTEGER PRIMARY KEY,
    txn_ref     TEXT    NOT NULL,
    value_date  TEXT    NOT NULL,                        -- ISO yyyy-mm-dd
    amount      INTEGER NOT NULL
);

CREATE TABLE ledger (
    entry_id    INTEGER PRIMARY KEY,
    txn_ref     TEXT    NOT NULL,
    posted_date TEXT    NOT NULL,                        -- ISO yyyy-mm-dd
    amount      INTEGER NOT NULL
);

-- Not UNIQUE: the seed has a duplicate posting on purpose. Named ix_ rather than idx_ so the
-- index listing in S20 (name LIKE 'idx_%') still shows only the core schema.
CREATE INDEX ix_bank_txn_ref   ON bank_statement(txn_ref);
CREATE INDEX ix_ledger_txn_ref ON ledger(txn_ref);
