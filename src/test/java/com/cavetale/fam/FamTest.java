package com.cavetale.fam;

import com.cavetale.fam.sql.Database;
import java.util.UUID;

public final class FamTest {
    public void test() {
        UUID a = UUID.randomUUID();
        UUID b = UUID.randomUUID();
        for (int i = 0; i < 8; i += 1) {
            UUID[] arr = Database.sorted(a, b);
            System.out.println(arr[0] + " " + arr[1]);
        }
    }
}
