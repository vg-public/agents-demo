---
description: "Use when: generating Oracle DDL scripts, seed data, sequences, and indexes for Oracle Database."
agent: "agent"
tools: [read, edit, search]
argument-hint: "Describe the table or schema change — e.g., 'Create PRODUCTS and CATEGORIES tables with Oracle sequences'"
---

# Generate Oracle SQL

Generate **Oracle SQL DDL scripts** for schema creation, including tables, sequences, indexes, and seed data.

## Instructions

1. If entities exist in code, read them to extract column definitions
2. If starting fresh, use the provided entity description

## Table Creation Template

```sql
-- create_products_table.sql

-- Sequence for primary key
CREATE SEQUENCE PRODUCT_SEQ START WITH 1 INCREMENT BY 1 NOCACHE NOCYCLE;

-- Table definition
CREATE TABLE PRODUCTS (
    ID              NUMBER(19)                  NOT NULL,
    NAME            VARCHAR2(100)               NOT NULL,
    SKU             VARCHAR2(50)                NOT NULL,
    DESCRIPTION     VARCHAR2(500),
    PRICE           NUMBER(12,2)                NOT NULL,
    QUANTITY         NUMBER(10)     DEFAULT 0    NOT NULL,
    STATUS          VARCHAR2(20)   DEFAULT 'ACTIVE' NOT NULL,
    CATEGORY_ID     NUMBER(19),
    CREATED_AT      TIMESTAMP WITH TIME ZONE    DEFAULT SYSTIMESTAMP NOT NULL,
    UPDATED_AT      TIMESTAMP WITH TIME ZONE,
    CREATED_BY      VARCHAR2(100)  DEFAULT 'system' NOT NULL,
    UPDATED_BY      VARCHAR2(100),
    CONSTRAINT PK_PRODUCTS PRIMARY KEY (ID),
    CONSTRAINT UK_PRODUCTS_SKU UNIQUE (SKU),
    CONSTRAINT CK_PRODUCTS_STATUS CHECK (STATUS IN ('ACTIVE','INACTIVE','DISCONTINUED')),
    CONSTRAINT CK_PRODUCTS_PRICE CHECK (PRICE > 0),
    CONSTRAINT FK_PRODUCTS_CATEGORY FOREIGN KEY (CATEGORY_ID) REFERENCES CATEGORIES(ID)
);

-- Indexes
CREATE INDEX IDX_PRODUCTS_STATUS ON PRODUCTS(STATUS);
CREATE INDEX IDX_PRODUCTS_CATEGORY_ID ON PRODUCTS(CATEGORY_ID);
CREATE INDEX IDX_PRODUCTS_NAME_UPPER ON PRODUCTS(UPPER(NAME));
CREATE INDEX IDX_PRODUCTS_CREATED_AT ON PRODUCTS(CREATED_AT);

-- Comments
COMMENT ON TABLE PRODUCTS IS 'Product catalog master table';
COMMENT ON COLUMN PRODUCTS.SKU IS 'Stock Keeping Unit - unique business identifier';
COMMENT ON COLUMN PRODUCTS.STATUS IS 'Product lifecycle status: ACTIVE, INACTIVE, DISCONTINUED';
```

## Seed Data Template

```sql
-- seed_products.sql

INSERT INTO CATEGORIES (ID, NAME) VALUES (CATEGORY_SEQ.NEXTVAL, 'Electronics');
INSERT INTO CATEGORIES (ID, NAME) VALUES (CATEGORY_SEQ.NEXTVAL, 'Clothing');

INSERT INTO PRODUCTS (ID, NAME, SKU, PRICE, STATUS, CATEGORY_ID)
VALUES (PRODUCT_SEQ.NEXTVAL, 'Wireless Mouse', 'ELEC-001', 29.99, 'ACTIVE',
    (SELECT ID FROM CATEGORIES WHERE NAME = 'Electronics'));

COMMIT;
```

## Naming Conventions

| Object | Convention | Example |
|--------|-----------|---------|
| Table | UPPER_SNAKE, plural | `PRODUCTS` |
| Column | UPPER_SNAKE | `CATEGORY_ID` |
| Primary Key | `PK_<TABLE>` | `PK_PRODUCTS` |
| Foreign Key | `FK_<TABLE>_<REF>` | `FK_PRODUCTS_CATEGORY` |
| Unique Key | `UK_<TABLE>_<COLS>` | `UK_PRODUCTS_SKU` |
| Check | `CK_<TABLE>_<COL>` | `CK_PRODUCTS_STATUS` |
| Index | `IDX_<TABLE>_<COLS>` | `IDX_PRODUCTS_STATUS` |
| Sequence | `<ENTITY>_SEQ` | `PRODUCT_SEQ` |

## Rules

- Always use Oracle sequences — never auto-increment
- Use `VARCHAR2` (not `VARCHAR`), `NUMBER` (not `INT`), `TIMESTAMP WITH TIME ZONE`
- Include `COMMENT ON TABLE` and `COMMENT ON COLUMN` for documentation
- Add indexes on foreign key columns, frequently filtered columns, and search columns
- Use function-based indexes for case-insensitive search (`UPPER(NAME)`)
- Save SQL scripts to `src/main/resources/db/` directory
- Each script should be idempotent or run exactly once — no conditional logic
