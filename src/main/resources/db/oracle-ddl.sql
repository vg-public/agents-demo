-- ============================================================
-- Oracle DDL for Product Catalog
-- Target: Oracle 19c / 21c / 23ai
-- Usage: Run once during schema provisioning
-- ============================================================

-- Sequence for PRODUCTS primary key (allocationSize = 50 matches Hibernate config)
CREATE SEQUENCE PRODUCT_SEQ
    START WITH 1
    INCREMENT BY 50
    NOCACHE
    NOCYCLE;

-- Products table
CREATE TABLE PRODUCTS (
    ID          NUMBER(19)          NOT NULL,
    NAME        VARCHAR2(255 CHAR)  NOT NULL,
    PRICE       NUMBER(12, 2)       NOT NULL,
    SKU         VARCHAR2(50 CHAR)   NOT NULL,
    CREATED_AT  TIMESTAMP           NOT NULL,
    UPDATED_AT  TIMESTAMP,
    CONSTRAINT PK_PRODUCTS PRIMARY KEY (ID),
    CONSTRAINT UQ_PRODUCTS_SKU UNIQUE (SKU),
    CONSTRAINT CK_PRODUCTS_PRICE CHECK (PRICE > 0)
);

-- Index on SKU for fast lookups
CREATE UNIQUE INDEX IDX_PRODUCTS_SKU ON PRODUCTS (SKU);

-- Comments
COMMENT ON TABLE PRODUCTS IS 'Product catalog entries';
COMMENT ON COLUMN PRODUCTS.ID IS 'Surrogate primary key from PRODUCT_SEQ';
COMMENT ON COLUMN PRODUCTS.NAME IS 'Human-readable product name';
COMMENT ON COLUMN PRODUCTS.PRICE IS 'Unit price (max 10 integer + 2 decimal digits)';
COMMENT ON COLUMN PRODUCTS.SKU IS 'Stock Keeping Unit — uppercase alphanumeric with hyphens';
COMMENT ON COLUMN PRODUCTS.CREATED_AT IS 'Row creation timestamp (set by @PrePersist)';
COMMENT ON COLUMN PRODUCTS.UPDATED_AT IS 'Last update timestamp (set by @PreUpdate)';
