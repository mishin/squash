package org.jetbrains.squash.dialects.h2.tests

import org.jetbrains.squash.definition.*
import org.jetbrains.squash.tests.*
import org.junit.*
import kotlin.test.*

class DDLTests {
    @Test fun unregisteredTableNotExists() {
        val TestTable = object : Table("test") {
            val id = integer("id").primaryKey()
            val name = varchar("name", length = 42)
        }

        withTables() {
            assertEquals (false, exists(TestTable))
        }
    }

    @Test fun tableExists() {
        val TestTable = object : Table() {
            val id = integer("id").primaryKey()
            val name = varchar("name", length = 42)
        }

        withTables(TestTable) {
            assertEquals (true, exists(TestTable))
        }
    }

/*
    @Test fun unnamedTableWithQuotesSQL() {
        val TestTable = object : Table() {
            val id = integer("id").primaryKey()
            val name = varchar("name", length = 42)
        }

        withTables(TestTable) {
            val q = identityQuoteString
            assertEquals("CREATE TABLE IF NOT EXISTS ${q}unnamedTableWithQuotesSQL\$TestTable$1$q (id INT NOT NULL, name VARCHAR(42) NOT NULL, CONSTRAINT ${q}pk_unnamedTableWithQuotesSQL\$TestTable$1$q PRIMARY KEY (id))", TestTable.ddl)
        }
    }
*/

    @Test fun namedEmptyTableWithoutQuotesSQL() {
        val TestTable = object : Table("test_named_table") {
        }

        withTables(TestTable) {
            val ddl = connection.dialect.definition.tableSQL(TestTable).sql
            assertEquals("CREATE TABLE IF NOT EXISTS test_named_table", ddl)
        }
    }

    @Test fun tableWithDifferentColumnTypesSQL() {
        val TestTable = object : Table("test_table_with_different_column_types") {
            val id = integer("id").autoIncrement()
            val name = varchar("name", 42).primaryKey()
            val age = integer("age").nullable()
            // not applicable in H2 database
            //            val testCollate = varchar("testCollate", 2, "ascii_general_ci")
        }

        withTables(TestTable) {
            val ddl = connection.dialect.definition.tableSQL(TestTable).sql
            assertEquals("CREATE TABLE IF NOT EXISTS test_table_with_different_column_types (id INT NOT NULL AUTO_INCREMENT, name VARCHAR(42) NOT NULL, age INT NULL, CONSTRAINT PK_test_table_with_different_column_types PRIMARY KEY (name))", ddl)
        }
    }

    @Test fun columnsWithDefaults() {
        val TestTable = object : Table("t") {
            val s = varchar("s", 100).default("test")
            val l = long("l").default(42)
        }

        withTables(TestTable) {
            val ddl = connection.dialect.definition.tableSQL(TestTable).sql
            assertEquals("CREATE TABLE IF NOT EXISTS t (s VARCHAR(100) NOT NULL DEFAULT ?, l BIGINT NOT NULL DEFAULT 42)", ddl)
        }
    }

    @Test fun singleColumnIndex() {
        val TestTable = object : Table("t1") {
            val id = integer("id").primaryKey()
            val name = varchar("name", 255).index()
        }

        withTables(TestTable) {
            val ddl = connection.dialect.definition.tableSQL(TestTable).sql
            assertEquals("CREATE TABLE IF NOT EXISTS t1 (id INT NOT NULL, name VARCHAR(255) NOT NULL, CONSTRAINT PK_t1 PRIMARY KEY (id))", ddl)
            val ix = connection.dialect.definition.indicesSQL(TestTable).single().sql
            assertEquals("CREATE INDEX IX_t1_name ON t1 (name)", ix)
        }
    }

    @Test fun singleColumnUniqueIndex() {
        val TestTable = object : Table("t1") {
            val id = integer("id").primaryKey()
            val name = varchar("name", 255).uniqueIndex()
        }

        withTables(TestTable) {
            val ddl = connection.dialect.definition.tableSQL(TestTable).sql
            assertEquals("CREATE TABLE IF NOT EXISTS t1 (id INT NOT NULL, name VARCHAR(255) NOT NULL, CONSTRAINT PK_t1 PRIMARY KEY (id))", ddl)
            val ix = connection.dialect.definition.indicesSQL(TestTable).single().sql
            assertEquals("CREATE UNIQUE INDEX IX_t1_name ON t1 (name)", ix)
        }
    }

    @Test fun twoColumnIndex() {
        val TestTable = object : Table("t2") {
            val id = integer("id").primaryKey()
            val lvalue = integer("lvalue")
            val rvalue = integer("rvalue")

            init {
                index(lvalue, rvalue)
            }
        }

        withTables(TestTable) {
            val index = connection.dialect.definition.indicesSQL(TestTable).single()
            assertEquals("CREATE INDEX IX_t2_lvalue_rvalue ON t2 (lvalue, rvalue)", index.sql)
        }
    }

    @Test fun twoIndices() {
        val TestTable = object : Table("t2") {
            val id = integer("id").primaryKey()
            val lvalue = integer("lvalue").index("one")
            val rvalue = integer("rvalue").index("two")
        }

        withTables(TestTable) {
            val indices = connection.dialect.definition.indicesSQL(TestTable)
            assertEquals(2, indices.size)
            assertEquals("CREATE INDEX one ON t2 (lvalue)", indices[0].sql)
            assertEquals("CREATE INDEX two ON t2 (rvalue)", indices[1].sql)
        }
    }
}

