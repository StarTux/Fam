package com.cavetale.fam.sql;

import com.winthier.sql.SQLRow.Name;
import com.winthier.sql.SQLRow.NotNull;
import com.winthier.sql.SQLRow;
import java.util.UUID;
import lombok.Data;

@Data @NotNull @Name("players")
public final class SQLPlayer implements SQLRow {
    @Id private Integer id;
    @Keyed private UUID uuid;
    @Nullable @MediumText private String statusMessage;

    public SQLPlayer() { }

    public SQLPlayer(final UUID uuid) {
        this.uuid = uuid;
    }
}
