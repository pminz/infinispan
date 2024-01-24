CREATE CACHED TABLE "PUBLIC"."ISPN_STRING_TABLE_jdbc_store_cache"(
    "ID_COLUMN" CHARACTER VARYING(255) NOT NULL,
    "DATA_COLUMN" BINARY VARYING NOT NULL,
    "TIMESTAMP_COLUMN" BIGINT NOT NULL
);
ALTER TABLE "PUBLIC"."ISPN_STRING_TABLE_jdbc_store_cache" ADD CONSTRAINT "PUBLIC"."CONSTRAINT_2" PRIMARY KEY("ID_COLUMN");
-- 5 +/- SELECT COUNT(*) FROM PUBLIC.ISPN_STRING_TABLE_jdbc_store_cache;
INSERT INTO "PUBLIC"."ISPN_STRING_TABLE_jdbc_store_cache" VALUES
('key-0', X'9801ea078a01620a2a82011e6f72672e696e66696e697370616e2e746573742e636f72652e56616c75658a01060a0130120130121c9801058a011618ffffffffffffffffff0120ffffffffffffffffff0118ffffffffffffffffff0120ffffffffffffffffff012a00', -1),
('key-2', X'9801ea078a01620a2a82011e6f72672e696e66696e697370616e2e746573742e636f72652e56616c75658a01060a0132120132121c9801058a011618ffffffffffffffffff0120ffffffffffffffffff0118ffffffffffffffffff0120ffffffffffffffffff012a00', -1),
('key-4', X'9801ea078a01620a2a82011e6f72672e696e66696e697370616e2e746573742e636f72652e56616c75658a01060a0134120134121c9801058a011618ffffffffffffffffff0120ffffffffffffffffff0118ffffffffffffffffff0120ffffffffffffffffff012a00', -1),
('key-6', X'9801ea078a01620a2a82011e6f72672e696e66696e697370616e2e746573742e636f72652e56616c75658a01060a0136120136121c9801058a011618ffffffffffffffffff0120ffffffffffffffffff0118ffffffffffffffffff0120ffffffffffffffffff012a00', -1),
('key-8', X'9801ea078a01620a2a82011e6f72672e696e66696e697370616e2e746573742e636f72652e56616c75658a01060a0138120138121c9801058a011618ffffffffffffffffff0120ffffffffffffffffff0118ffffffffffffffffff0120ffffffffffffffffff012a00', -1);
CREATE INDEX "PUBLIC"."ISPN_STRING_TABLE_jdbc_store_cache_timestamp_index" ON "PUBLIC"."ISPN_STRING_TABLE_jdbc_store_cache"("TIMESTAMP_COLUMN" NULLS FIRST);
