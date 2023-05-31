package com.cavetale.fam.sql;

import com.winthier.sql.SQLDatabase;
import com.winthier.sql.SQLRow;
import org.junit.Test;

public final class SQLTest {
    @Test
    public void test() {
        for (Class<? extends SQLRow> table : Database.getAllDatabaseTables()) {
            System.out.println(SQLDatabase.testTableCreation(table));
        }
    }
}
