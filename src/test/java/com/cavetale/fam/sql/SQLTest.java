package com.cavetale.fam.sql;

import com.winthier.sql.SQLDatabase;
import java.util.UUID;
import org.junit.Test;

public final class SQLTest {
    @Test
    public void test() {
        System.out.println(SQLDatabase.testTableCreation(SQLFriends.class));
    }
}
